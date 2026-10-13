package app.calma.domain.breathing

import app.calma.core.common.BreathPhase

/**
 * A breathing pattern expressed as phase durations in milliseconds.
 *
 * `holdAfterInhale` and `holdAfterExhale` may legitimately be zero, which makes the phase
 * disappear from the cycle. This is what lets a "4-7-8" style pattern skip the first hold
 * without special-casing the scheduler.
 */
data class BreathingPattern(
    val id: String,
    val inhaleMillis: Long,
    val holdAfterInhaleMillis: Long,
    val exhaleMillis: Long,
    val holdAfterExhaleMillis: Long,
) {
    /** Phases with a non-zero duration, in cycle order. */
    val activePhases: List<BreathPhase>
        get() = buildList {
            if (inhaleMillis > 0) add(BreathPhase.INHALE)
            if (holdAfterInhaleMillis > 0) add(BreathPhase.HOLD_AFTER_INHALE)
            if (exhaleMillis > 0) add(BreathPhase.EXHALE)
            if (holdAfterExhaleMillis > 0) add(BreathPhase.HOLD_AFTER_EXHALE)
        }

    val cycleMillis: Long
        get() = inhaleMillis + holdAfterInhaleMillis + exhaleMillis + holdAfterExhaleMillis

    /** One full breath in seconds, used in labels: inhale + hold + exhale. */
    val breathsPerMinute: Double
        get() {
            val productive = inhaleMillis + exhaleMillis
            if (productive <= 0L || holdAfterInhaleMillis + holdAfterExhaleMillis > 0L) {
                // With holds present the cycle time dominates; report the honest cycle rate.
                if (cycleMillis <= 0L) return 0.0
                return 60_000.0 / cycleMillis.toDouble()
            }
            return 60_000.0 / productive.toDouble()
        }

    companion object {
        /**
         * Built-in patterns. Every one is gentle: no breath holds beyond 7 seconds and no
         * ratio more extreme than 1:2, which avoids hyperventilation-style patterns.
         */
        val BUILT_IN: List<BreathingPattern> = listOf(
            BreathingPattern("calm_4_6", 4_000, 0, 6_000, 0),
            BreathingPattern("box_4", 4_000, 4_000, 4_000, 4_000),
            BreathingPattern("relax_4_7", 4_000, 7_000, 8_000, 0),
            BreathingPattern("soft_3_3_2", 3_000, 3_000, 3_000, 2_000),
            BreathingPattern("long_exhale_4_8", 4_000, 0, 8_000, 0),
        )

        fun byId(id: String): BreathingPattern =
            BUILT_IN.firstOrNull { it.id == id } ?: BUILT_IN.first()

        /** 1, 3 and 5 minute exercises requested by the product spec. */
        val DURATION_OPTIONS_SECONDS = listOf(60, 180, 300)
    }
}

/** Immutable snapshot of a running breathing session, recomputed from elapsed time. */
data class BreathSessionState(
    val runState: RunState = RunState.IDLE,
    val phase: BreathPhase = BreathPhase.INHALE,
    /** 0f..1f within the current phase; drives the circle animation. */
    val phaseProgress: Float = 0f,
    /** Whole seconds left in the current phase, rounded up so it only shows 0 at the end. */
    val phaseRemainingSeconds: Int = 0,
    /** Milliseconds left in the whole exercise. */
    val remainingMillis: Long = 0L,
    val cycleIndex: Int = 0,
    val totalCycles: Int = 0,
) {
    enum class RunState { IDLE, RUNNING, PAUSED, FINISHED }

    val isActive: Boolean get() = runState == RunState.RUNNING || runState == RunState.PAUSED
}

/**
 * Pure scheduler for guided breathing.
 *
 * Every value is derived from a single [elapsedMillis] input rather than accumulated tick
 * by tick. That is what makes the exercise immune to Compose recomposition, dropped frames
 * and Activity restarts: re-entering the screen with a larger elapsed time simply yields
 * the correct phase, with no drift.
 */
object BreathingScheduler {

    data class Snapshot(
        val phase: BreathPhase,
        val phaseProgress: Float,
        val phaseRemainingSeconds: Int,
        val cycleIndex: Int,
        val totalCycles: Int,
        val finished: Boolean,
        /** Effective elapsed time after clamping to the exercise duration. */
        val elapsedMillis: Long,
    )

    /**
     * @param elapsedMillis time since the session started, excluding paused time.
     * @param pattern the active pattern.
     * @param durationMillis total exercise length.
     */
    fun snapshot(
        elapsedMillis: Long,
        pattern: BreathingPattern,
        durationMillis: Long,
    ): Snapshot {
        val phases = pattern.activePhases
        val cycleMillis = pattern.cycleMillis
        val totalCycles = if (cycleMillis <= 0L) 0 else (durationMillis / cycleMillis).toInt()

        if (durationMillis <= 0L || cycleMillis <= 0L || phases.isEmpty()) {
            return Snapshot(
                phase = BreathPhase.INHALE,
                phaseProgress = 1f,
                phaseRemainingSeconds = 0,
                cycleIndex = 0,
                totalCycles = 0,
                finished = true,
                elapsedMillis = 0L,
            )
        }

        if (elapsedMillis >= durationMillis) {
            return Snapshot(
                phase = phases.last(),
                phaseProgress = 1f,
                phaseRemainingSeconds = 0,
                cycleIndex = (totalCycles - 1).coerceAtLeast(0),
                totalCycles = totalCycles,
                finished = true,
                elapsedMillis = durationMillis,
            )
        }

        val safeElapsed = elapsedMillis.coerceAtLeast(0L)
        val cycleIndex = (safeElapsed / cycleMillis).toInt()
        var offsetInCycle = safeElapsed % cycleMillis
        var phase = phases.first()
        var phaseDuration = 0L
        var phaseStart = 0L

        for (candidate in phases) {
            val duration = durationOf(candidate, pattern)
            if (offsetInCycle < duration) {
                phase = candidate
                phaseDuration = duration
                phaseStart = durationOfBefore(candidate, pattern)
                break
            }
            offsetInCycle -= duration
        }

        if (phaseDuration <= 0L) {
            // Defensive: only reachable if a pattern has all-zero durations.
            phase = phases.first()
            phaseDuration = durationOf(phase, pattern).coerceAtLeast(1L)
            phaseStart = 0L
        }

        val intoPhase = (safeElapsed - (cycleIndex * cycleMillis) - phaseStart).coerceAtLeast(0L)
        val progress = (intoPhase.toFloat() / phaseDuration.toFloat()).coerceIn(0f, 1f)
        val remainingMillis = phaseDuration - intoPhase
        val remainingSeconds = ((remainingMillis + 999L) / 1000L).toInt()

        return Snapshot(
            phase = phase,
            phaseProgress = progress,
            phaseRemainingSeconds = remainingSeconds.coerceAtLeast(0),
            cycleIndex = cycleIndex,
            totalCycles = totalCycles,
            finished = false,
            elapsedMillis = safeElapsed,
        )
    }

    /** Number of complete breaths achieved in [elapsedMillis]. */
    fun completedCycles(elapsedMillis: Long, pattern: BreathingPattern): Int {
        val cycleMillis = pattern.cycleMillis
        if (cycleMillis <= 0L) return 0
        return (elapsedMillis / cycleMillis).toInt()
    }

    /**
     * Target circle radius, 0f..1f of the maximum.
     * Inhale expands, holds stay, exhale contracts. Used to size the breathing ring.
     */
    fun circleScale(phase: BreathPhase, phaseProgress: Float, pattern: BreathingPattern): Float {
        val p = phaseProgress.coerceIn(0f, 1f)
        return when (phase) {
            BreathPhase.INHALE -> 0.45f + 0.55f * p
            BreathPhase.HOLD_AFTER_INHALE -> 1f
            BreathPhase.EXHALE -> 1f - 0.55f * p
            BreathPhase.HOLD_AFTER_EXHALE -> 0.45f
        }
    }

    private fun durationOf(phase: BreathPhase, pattern: BreathingPattern): Long = when (phase) {
        BreathPhase.INHALE -> pattern.inhaleMillis
        BreathPhase.HOLD_AFTER_INHALE -> pattern.holdAfterInhaleMillis
        BreathPhase.EXHALE -> pattern.exhaleMillis
        BreathPhase.HOLD_AFTER_EXHALE -> pattern.holdAfterExhaleMillis
    }

    /** Sum of every phase that comes before [phase] in the cycle. */
    private fun durationOfBefore(phase: BreathPhase, pattern: BreathingPattern): Long {
        var total = 0L
        for (candidate in pattern.activePhases) {
            if (candidate == phase) break
            total += durationOf(candidate, pattern)
        }
        return total
    }
}