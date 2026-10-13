package app.calma.domain.focus

import app.calma.core.common.FocusPhase
import app.calma.core.common.FocusRunState

/**
 * Immutable snapshot of a focus session. Persisted as a single row, so it can be
 * restored after process death.
 *
 * Timing model
 * ------------
 * All counting uses `SystemClock.elapsedRealtime()`, which is monotonic and keeps
 * counting while the device is asleep. Wall-clock time is stored only as a fallback for
 * the rare case where the device rebooted (which resets `elapsedRealtime()`), detected by
 * comparing [anchorElapsedMillis] against the current value.
 *
 * Nothing here accumulates: [segmentElapsedMillis] is recomputed from the anchor on every
 * tick, so processor delays, dropped frames and Compose recompositions cannot introduce drift.
 */
data class FocusTimerSnapshot(
    val runState: FocusRunState = FocusRunState.IDLE,
    val phase: FocusPhase = FocusPhase.FOCUS,
    /** Planned length of a focus block. */
    val focusMillis: Long = 25 * 60_000L,
    /** Planned break length; `0` means the session ends right after focus. */
    val breakMillis: Long = 5 * 60_000L,
    /** Focus milliseconds banked by previous blocks of the same session. */
    val focusedAccumulatedMillis: Long = 0L,
    /** Elapsed time in the current segment. Frozen while paused, recomputed while running. */
    val segmentElapsedMillis: Long = 0L,
    /** `elapsedRealtime()` at which the current run segment started. */
    val segmentStartedElapsedMillis: Long = 0L,
    val startedAtEpochMillis: Long = 0L,
    /** Wall-clock anchor paired with [anchorElapsedMillis] for reboot detection. */
    val anchorWallClockMillis: Long = 0L,
    val anchorElapsedMillis: Long = 0L,
) {
    val segmentDurationMillis: Long
        get() = if (phase == FocusPhase.FOCUS) focusMillis else breakMillis

    val isRunning: Boolean get() = runState == FocusRunState.RUNNING

    val isActive: Boolean
        get() = runState == FocusRunState.RUNNING || runState == FocusRunState.PAUSED
}

/** What the engine wants the caller to do after evaluating the clock. */
sealed interface FocusEvent {
    /** Nothing special; keep ticking with the (possibly updated) snapshot. */
    data class Continue(val snapshot: FocusTimerSnapshot) : FocusEvent

    /** A focus block finished. Persist a session, then [FocusTimerEngine.startBreak]. */
    data class FocusCompleted(val focusedMillis: Long, val snapshot: FocusTimerSnapshot) : FocusEvent

    /** The break finished (or there was none). The session is over. */
    data class SessionFinished(val focusedMillis: Long) : FocusEvent

    /** The device rebooted; the wall clock is the only trustworthy source left. */
    data class ReconciledAfterReboot(
        val focusedMillis: Long,
        val wasCompleted: Boolean,
        val snapshot: FocusTimerSnapshot,
    ) : FocusEvent
}

/**
 * Pure focus-timer state machine.
 *
 * Every function is `(snapshot, nowElapsedRealtime) -> result`, which makes the whole
 * engine testable without Android, and keeps the ViewModel free of timing arithmetic.
 */
object FocusTimerEngine {

    /** Starts a fresh focus block. */
    fun start(
        nowElapsedMillis: Long,
        nowWallClockMillis: Long,
        focusMillis: Long,
        breakMillis: Long,
    ): FocusTimerSnapshot = FocusTimerSnapshot(
        runState = FocusRunState.RUNNING,
        phase = FocusPhase.FOCUS,
        focusMillis = focusMillis.coerceAtLeast(1_000L),
        breakMillis = breakMillis.coerceAtLeast(0L),
        focusedAccumulatedMillis = 0L,
        segmentElapsedMillis = 0L,
        segmentStartedElapsedMillis = nowElapsedMillis,
        startedAtEpochMillis = nowWallClockMillis,
        anchorWallClockMillis = nowWallClockMillis,
        anchorElapsedMillis = nowElapsedMillis,
    )

    /**
     * Freezes the current segment. Called when the user pauses, or when the app leaves
     * the foreground for a reason that would invalidate the monotonic anchor.
     */
    fun pause(snapshot: FocusTimerSnapshot, nowElapsedMillis: Long): FocusTimerSnapshot {
        if (snapshot.runState != FocusRunState.RUNNING) return snapshot
        return snapshot.copy(
            runState = FocusRunState.PAUSED,
            segmentElapsedMillis = elapsedInSegment(snapshot, nowElapsedMillis),
        )
    }

    /** Restarts the frozen segment from where it stopped. */
    fun resume(snapshot: FocusTimerSnapshot, nowElapsedMillis: Long): FocusTimerSnapshot {
        if (snapshot.runState != FocusRunState.PAUSED) return snapshot
        return snapshot.copy(
            runState = FocusRunState.RUNNING,
            segmentStartedElapsedMillis = nowElapsedMillis - snapshot.segmentElapsedMillis,
        )
    }

    /**
     * Milliseconds elapsed inside the current segment.
     * Recomputed from the anchor whenever running, so it never drifts.
     */
    fun elapsedInSegment(snapshot: FocusTimerSnapshot, nowElapsedMillis: Long): Long {
        if (snapshot.runState != FocusRunState.RUNNING) return snapshot.segmentElapsedMillis
        val raw = nowElapsedMillis - snapshot.segmentStartedElapsedMillis
        return raw.coerceIn(0L, snapshot.segmentDurationMillis.coerceAtLeast(0L))
    }

    fun remainingMillis(snapshot: FocusTimerSnapshot, nowElapsedMillis: Long): Long =
        (snapshot.segmentDurationMillis - elapsedInSegment(snapshot, nowElapsedMillis))
            .coerceAtLeast(0L)

    /** Total focus time that would be recorded if the session ended right now. */
    fun bankedFocusMillis(snapshot: FocusTimerSnapshot, nowElapsedMillis: Long): Long {
        val currentSegment = if (snapshot.phase == FocusPhase.FOCUS) {
            elapsedInSegment(snapshot, nowElapsedMillis).coerceAtMost(snapshot.focusMillis)
        } else {
            0L
        }
        return snapshot.focusedAccumulatedMillis + currentSegment
    }

    /** Progress through the whole session (focus + break), used by the ring. */
    fun sessionProgress(snapshot: FocusTimerSnapshot, nowElapsedMillis: Long): Float {
        val total = snapshot.focusMillis + snapshot.breakMillis
        if (total <= 0L) return 0f
        val done = bankedFocusMillis(snapshot, nowElapsedMillis) +
            if (snapshot.phase == FocusPhase.BREAK) elapsedInSegment(snapshot, nowElapsedMillis) else 0L
        return (done.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Advances the state machine to [nowElapsedMillis].
     *
     * Handles three cases in one call:
     * 1. ordinary running -> refreshed snapshot,
     * 2. focus segment reached its deadline -> [FocusEvent.FocusCompleted],
     * 3. break reached its deadline, or a reboot was detected -> completion.
     */
    fun advance(
        snapshot: FocusTimerSnapshot,
        nowElapsedMillis: Long,
        nowWallClockMillis: Long,
    ): FocusEvent {
        if (!snapshot.isActive) return FocusEvent.Continue(snapshot)

        if (isReboot(snapshot, nowElapsedMillis)) {
            return reconcileAfterReboot(snapshot, nowElapsedMillis, nowWallClockMillis)
        }

        if (!snapshot.isRunning) return FocusEvent.Continue(snapshot)

        val elapsed = elapsedInSegment(snapshot, nowElapsedMillis)
        if (elapsed < snapshot.segmentDurationMillis) {
            return FocusEvent.Continue(snapshot.copy(segmentElapsedMillis = elapsed))
        }

        return if (snapshot.phase == FocusPhase.FOCUS) {
            val banked = snapshot.focusedAccumulatedMillis + snapshot.focusMillis
            if (snapshot.breakMillis <= 0L) {
                FocusEvent.SessionFinished(banked)
            } else {
                FocusEvent.FocusCompleted(banked, startBreak(snapshot, nowElapsedMillis, banked))
            }
        } else {
            FocusEvent.SessionFinished(snapshot.focusedAccumulatedMillis)
        }
    }

    /** Ends the focus block early and banks whatever was actually focused. */
    fun finishEarly(
        snapshot: FocusTimerSnapshot,
        nowElapsedMillis: Long,
        nowWallClockMillis: Long,
    ): FocusEvent {
        val banked = bankedFocusMillis(snapshot, nowElapsedMillis)
        if (banked < 1_000L) {
            // Under one second is not a session worth recording.
            return FocusEvent.SessionFinished(0L)
        }
        if (snapshot.breakMillis <= 0L) return FocusEvent.SessionFinished(banked)
        return FocusEvent.FocusCompleted(banked, startBreak(snapshot, nowElapsedMillis, banked))
    }

    /**
     * Enters the break phase after [focusedMillis] has been banked.
     *
     * Public because callers that complete a block outside the tick loop (early finish,
     * and restoring a session that ended while the app was closed) need the exact same
     * transition the engine would have made itself.
     */
    fun startBreak(
        snapshot: FocusTimerSnapshot,
        nowElapsedMillis: Long,
        focusedMillis: Long,
    ): FocusTimerSnapshot = snapshot.copy(
        runState = FocusRunState.RUNNING,
        phase = FocusPhase.BREAK,
        focusedAccumulatedMillis = focusedMillis,
        segmentElapsedMillis = 0L,
        segmentStartedElapsedMillis = nowElapsedMillis,
    )

    fun cancel(snapshot: FocusTimerSnapshot, nowElapsedMillis: Long): Long =
        if (bankedFocusMillis(snapshot, nowElapsedMillis) >= 1_000L) {
            bankedFocusMillis(snapshot, nowElapsedMillis)
        } else {
            0L
        }

    /**
     * `elapsedRealtime()` only moves backwards after a reboot, so a current value smaller
     * than the stored anchor is a reliable signal that monotonic time was reset.
     */
    fun isReboot(snapshot: FocusTimerSnapshot, nowElapsedMillis: Long): Boolean =
        snapshot.isActive &&
            snapshot.anchorElapsedMillis > 0L &&
            nowElapsedMillis < snapshot.anchorElapsedMillis

    /**
     * Recovers a session after a reboot. `elapsedRealtime` is useless at that point, so the
     * wall clock is used once to work out how much of the block had elapsed, then the
     * session is parked in a paused state with a fresh monotonic anchor.
     */
    fun reconcileAfterReboot(
        snapshot: FocusTimerSnapshot,
        nowElapsedMillis: Long,
        nowWallClockMillis: Long,
    ): FocusEvent.ReconciledAfterReboot {
        val wallElapsed = (nowWallClockMillis - snapshot.anchorWallClockMillis)
            .coerceIn(0L, snapshot.segmentDurationMillis)
        val banked = (snapshot.focusedAccumulatedMillis + wallElapsed)
            .coerceAtLeast(0L)

        val completed = wallElapsed >= snapshot.segmentDurationMillis
        val recovered = snapshot.copy(
            runState = FocusRunState.PAUSED,
            segmentElapsedMillis = wallElapsed,
            segmentStartedElapsedMillis = nowElapsedMillis,
            anchorElapsedMillis = nowElapsedMillis,
            anchorWallClockMillis = nowWallClockMillis,
        )
        return FocusEvent.ReconciledAfterReboot(
            focusedMillis = banked,
            wasCompleted = completed,
            snapshot = recovered,
        )
    }

    /** Clamps a user-entered custom duration into a range the UI can render sensibly. */
    fun sanitizeFocusMinutes(minutes: Int): Int = minutes.coerceIn(1, 180)

    fun sanitizeBreakMinutes(minutes: Int): Int = minutes.coerceIn(0, 60)
}