package app.calma.domain.focus

import app.calma.core.common.FocusPhase
import app.calma.core.common.FocusRunState
import app.calma.core.common.TimeUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FocusTimerEngineTest {

    @Test
    fun `start creates running focus snapshot`() {
        val now = 100_000L
        val wall = 1_700_000_000_000L
        val snap = FocusTimerEngine.start(now, wall, 25 * 60_000L, 5 * 60_000L)

        assertEquals(FocusRunState.RUNNING, snap.runState)
        assertEquals(FocusPhase.FOCUS, snap.phase)
        assertEquals(25 * 60_000L, snap.focusMillis)
        assertEquals(5 * 60_000L, snap.breakMillis)
        assertEquals(0L, snap.focusedAccumulatedMillis)
        assertEquals(0L, snap.segmentElapsedMillis)
        assertEquals(now, snap.segmentStartedElapsedMillis)
        assertEquals(wall, snap.startedAtEpochMillis)
        assertEquals(wall, snap.anchorWallClockMillis)
        assertEquals(now, snap.anchorElapsedMillis)
    }

    @Test
    fun `pause freezes segment elapsed`() {
        val now = 100_000L
        val snap = FocusTimerEngine.start(now, 1_700_000_000_000L, 60_000L, 0L)
        val paused = FocusTimerEngine.pause(snap, now + 30_000L)

        assertEquals(FocusRunState.PAUSED, paused.runState)
        assertEquals(30_000L, paused.segmentElapsedMillis)
        assertEquals(now, paused.segmentStartedElapsedMillis)
    }

    @Test
    fun `resume restarts from frozen elapsed`() {
        val now = 100_000L
        val snap = FocusTimerEngine.start(now, 1_700_000_000_000L, 60_000L, 0L)
        val paused = FocusTimerEngine.pause(snap, now + 20_000L)
        val resumed = FocusTimerEngine.resume(paused, now + 25_000L)

        assertEquals(FocusRunState.RUNNING, resumed.runState)
        // segmentStartedElapsedMillis adjusted so elapsed = frozen 20s
        assertEquals(now + 5_000L, resumed.segmentStartedElapsedMillis)
    }

    @Test
    fun `remainingMillis decreases while running`() {
        val now = 100_000L
        val snap = FocusTimerEngine.start(now, 1_700_000_000_000L, 60_000L, 0L)
        val rem1 = FocusTimerEngine.remainingMillis(snap, now + 10_000L)
        val rem2 = FocusTimerEngine.remainingMillis(snap, now + 30_000L)

        assertTrue(rem1 > rem2)
        assertEquals(50_000L, rem1)
        assertEquals(30_000L, rem2)
    }

    @Test
    fun `remainingMillis zero at deadline`() {
        val now = 100_000L
        val snap = FocusTimerEngine.start(now, 1_700_000_000_000L, 1_000L, 0L)
        val rem = FocusTimerEngine.remainingMillis(snap, now + 1_500L)
        assertEquals(0L, rem)
    }

    @Test
    fun `bankedFocusMillis accumulates during focus phase`() {
        val now = 100_000L
        val snap = FocusTimerEngine.start(now, 1_700_000_000_000L, 60_000L, 0L)
        val banked = FocusTimerEngine.bankedFocusMillis(snap, now + 25_000L)
        assertEquals(25_000L, banked)
    }

    @Test
    fun `bankedFocusMillis does not increase during break`() {
        val now = 100_000L
        val snap = FocusTimerEngine.start(now, 1_700_000_000_000L, 60_000L, 30_000L)
        // Advance to break phase by simulating completion
        val breakSnap = snap.copy(
            phase = app.calma.core.common.FocusPhase.BREAK,
            focusedAccumulatedMillis = 60_000L,
            segmentStartedElapsedMillis = now + 60_000L,
        )
        val banked = FocusTimerEngine.bankedFocusMillis(breakSnap, now + 60_000L + 10_000L)
        assertEquals(60_000L, banked)
    }

    @Test
    fun `advance completes focus block and starts break`() {
        val now = 100_000L
        val wall = 1_700_000_000_000L
        val snap = FocusTimerEngine.start(now, wall, 1_000L, 500L)
        val event = FocusTimerEngine.advance(snap, now + 1_000L, wall + 1_000L)

        assertTrue(event is FocusEvent.FocusCompleted)
        val completed = event as FocusEvent.FocusCompleted
        assertEquals(1_000L, completed.focusedMillis)
        assertEquals(FocusPhase.BREAK, completed.snapshot.phase)
        assertEquals(FocusRunState.RUNNING, completed.snapshot.runState)
    }

    @Test
    fun `advance finishes session when no break`() {
        val now = 100_000L
        val wall = 1_700_000_000_000L
        val snap = FocusTimerEngine.start(now, wall, 1_000L, 0L)
        val event = FocusTimerEngine.advance(snap, now + 1_000L, wall + 1_000L)

        assertTrue(event is FocusEvent.SessionFinished)
        val finished = event as FocusEvent.SessionFinished
        assertEquals(1_000L, finished.focusedMillis)
    }

    @Test
    fun `finishEarly banks actual elapsed`() {
        val now = 100_000L
        val wall = 1_700_000_000_000L
        val snap = FocusTimerEngine.start(now, wall, 60_000L, 0L)
        val event = FocusTimerEngine.finishEarly(snap, now + 15_000L, wall + 15_000L)

        assertTrue(event is FocusEvent.SessionFinished)
        val finished = event as FocusEvent.SessionFinished
        assertEquals(15_000L, finished.focusedMillis)
    }

    @Test
    fun `cancel discards session under 1s`() {
        val now = 100_000L
        val snap = FocusTimerEngine.start(now, 1_700_000_000_000L, 60_000L, 0L)
        val banked = FocusTimerEngine.cancel(snap, now + 500L)
        assertEquals(0L, banked)
    }

    @Test
    fun `reboot detected when elapsed moves backward`() {
        val now = 100_000L
        val snap = FocusTimerEngine.start(now, 1_700_000_000_000L, 60_000L, 0L)
        val reboot = now - 1 // elapsed reset
        assertTrue(FocusTimerEngine.isReboot(snap, reboot))
    }

    @Test
    fun `reboot reconciliation uses wall clock`() {
        val startElapsed = 100_000L
        val startWall = 1_700_000_000_000L
        val snap = FocusTimerEngine.start(startElapsed, startWall, 60_000L, 0L)
        // Simulate reboot: elapsed reset to small value, wall clock advanced by 30s
        val rebootElapsed = 10_000L
        val rebootWall = startWall + 30_000L
        val event = FocusTimerEngine.reconcileAfterReboot(snap, rebootElapsed, rebootWall)

        assertTrue(event is FocusEvent.ReconciledAfterReboot)
        val rec = event as FocusEvent.ReconciledAfterReboot
        // Should have banked 30s (but capped at focus duration)
        assertEquals(30_000L, rec.focusedMillis)
        assertEquals(FocusRunState.PAUSED, rec.snapshot.runState)
    }

    @Test
    fun `sanitize clamps focus minutes`() {
        assertEquals(1, FocusTimerEngine.sanitizeFocusMinutes(0))
        assertEquals(25, FocusTimerEngine.sanitizeFocusMinutes(25))
        assertEquals(180, FocusTimerEngine.sanitizeFocusMinutes(200))
    }

    @Test
    fun `sanitize clamps break minutes`() {
        assertEquals(0, FocusTimerEngine.sanitizeBreakMinutes(-5))
        assertEquals(5, FocusTimerEngine.sanitizeBreakMinutes(5))
        assertEquals(60, FocusTimerEngine.sanitizeBreakMinutes(100))
    }
}
