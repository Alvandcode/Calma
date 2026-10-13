package app.calma.domain.breathing

import app.calma.core.common.BreathPhase
import app.calma.core.common.BreathRunState
import app.calma.domain.breathing.BreathingScheduler.Snapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BreathingSchedulerTest {

    private val pattern = app.calma.domain.breathing.BreathingPattern(
        id = "test_4_2_6_0",
        inhaleMillis = 4_000,
        holdAfterInhaleMillis = 2_000,
        exhaleMillis = 6_000,
        holdAfterExhaleMillis = 0,
    )

    @Test
    fun `pattern active phases in correct order`() {
        assertEquals(
            listOf(BreathPhase.INHALE, BreathPhase.HOLD_AFTER_INHALE, BreathPhase.EXHALE),
            pattern.activePhases
        )
    }

    @Test
    fun `pattern cycle duration sums phases`() {
        assertEquals(12_000L, pattern.cycleMillis)
    }

    @Test
    fun `snapshot at start is first phase`() {
        val snap = BreathingScheduler.snapshot(0L, pattern, 60_000L)
        assertEquals(BreathPhase.INHALE, snap.phase)
        assertEquals(0f, snap.phaseProgress)
        assertEquals(4, snap.phaseRemainingSeconds) // ceil(4000/1000)
        assertEquals(0, snap.cycleIndex)
    }

    @Test
    fun `snapshot mid inhale`() {
        val snap = BreathingScheduler.snapshot(2_000L, pattern, 60_000L)
        assertEquals(BreathPhase.INHALE, snap.phase)
        assertEquals(0.5f, snap.phaseProgress)
        assertEquals(2, snap.phaseRemainingSeconds)
    }

    @Test
    fun `snapshot at inhale end transitions to hold`() {
        val snap = BreathingScheduler.snapshot(4_000L, pattern, 60_000L)
        assertEquals(BreathPhase.HOLD_AFTER_INHALE, snap.phase)
        assertEquals(0f, snap.phaseProgress)
        assertEquals(2, snap.phaseRemainingSeconds)
    }

    @Test
    fun `snapshot mid exhale`() {
        val snap = BreathingScheduler.snapshot(8_000L, pattern, 60_000L)
        // 4 inhale + 2 hold + 2 into exhale
        assertEquals(BreathPhase.EXHALE, snap.phase)
        assertEquals(2_000f / 6_000f, snap.phaseProgress)
    }

    @Test
    fun `snapshot finishes at duration`() {
        val snap = BreathingScheduler.snapshot(60_000L, pattern, 60_000L)
        assertEquals(BreathPhase.EXHALE, snap.phase)
        assertEquals(1f, snap.phaseProgress)
        assertEquals(0, snap.phaseRemainingSeconds)
        assertTrue(snap.finished)
    }

    @Test
    fun `completedCycles counts full cycles`() {
        assertEquals(0, BreathingScheduler.completedCycles(0L, pattern))
        assertEquals(0, BreathingScheduler.completedCycles(11_999L, pattern))
        assertEquals(1, BreathingScheduler.completedCycles(12_000L, pattern))
        assertEquals(4, BreathingScheduler.completedCycles(48_000L, pattern))
    }

    @Test
    fun `circleScale inhale expands`() {
        val s0 = BreathingScheduler.circleScale(BreathPhase.INHALE, 0f, pattern)
        val s1 = BreathingScheduler.circleScale(BreathPhase.INHALE, 1f, pattern)
        assertEquals(0.45f, s0)
        assertEquals(1f, s1)
    }

    @Test
    fun `circleScale holdAfterInhale stays at 1`() {
        assertEquals(1f, BreathingScheduler.circleScale(BreathPhase.HOLD_AFTER_INHALE, 0.5f, pattern))
    }

    @Test
    fun `circleScale exhale contracts`() {
        val s0 = BreathingScheduler.circleScale(BreathPhase.EXHALE, 0f, pattern)
        val s1 = BreathingScheduler.circleScale(BreathPhase.EXHALE, 1f, pattern)
        assertEquals(1f, s0)
        assertEquals(0.45f, s1)
    }
}