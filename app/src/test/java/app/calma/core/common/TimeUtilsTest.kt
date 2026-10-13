package app.calma.core.common

import java.time.ZoneId
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TimeUtilsTest {

    private val zone = ZoneId.of("UTC")

    @Test
    fun `startOfDay floors to midnight`() {
        val midday = LocalDate.of(2024, 6, 15).atTime(14, 30).atZone(zone).toInstant().toEpochMilli()
        val start = TimeUtils.startOfDay(midday, zone)
        assertEquals(LocalDate.of(2024, 6, 15).atStartOfDay(zone).toInstant().toEpochMilli(), start)
    }

    @Test
    fun `isSameDay true for same day different times`() {
        val a = LocalDate.of(2024, 6, 15).atTime(8, 0).atZone(zone).toInstant().toEpochMilli()
        val b = LocalDate.of(2024, 6, 15).atTime(20, 30).atZone(zone).toInstant().toEpochMilli()
        assertTrue(TimeUtils.isSameDay(a, b, zone))
    }

    @Test
    fun `isSameDay false for adjacent days`() {
        val a = LocalDate.of(2024, 6, 15).atTime(23, 59).atZone(zone).toInstant().toEpochMilli()
        val b = LocalDate.of(2024, 6, 16).atTime(0, 1).atZone(zone).toInstant().toEpochMilli()
        assertTrue(!TimeUtils.isSameDay(a, b, zone))
    }

    @Test
    fun `formatClock formats minutes and seconds`() {
        assertEquals("0:30", TimeUtils.formatClock(30_000L))
        assertEquals("1:30", TimeUtils.formatClock(90_000L))
        assertEquals("25:00", TimeUtils.formatClock(25 * 60_000L))
        assertEquals("1:05:00", TimeUtils.formatClock(65 * 60_000L))
    }

    @Test
    fun `durationParts splits correctly`() {
        val p = TimeUtils.durationParts(2 * 60_000L + 30_000L + 5_000L)
        assertEquals(0, p.hours)
        assertEquals(2, p.minutes)
        assertEquals(35, p.seconds)

        val p2 = TimeUtils.durationParts(90 * 60_000L)
        assertEquals(1, p2.hours)
        assertEquals(30, p2.minutes)
        assertEquals(0, p2.seconds)
    }

    @Test
    fun `secondsRemaining rounds up`() {
        assertEquals(5, TimeUtils.secondsRemaining(105_000L, 100_000L))
        assertEquals(1, TimeUtils.secondsRemaining(100_001L, 100_000L))
        assertEquals(0, TimeUtils.secondsRemaining(100_000L, 100_000L))
        assertEquals(0, TimeUtils.secondsRemaining(99_999L, 100_000L))
    }

    @Test
    fun `progress clamps`() {
        assertEquals(0f, TimeUtils.progress(0L, 100L))
        assertEquals(0.5f, TimeUtils.progress(50L, 100L))
        assertEquals(1f, TimeUtils.progress(100L, 100L))
        assertEquals(1f, TimeUtils.progress(150L, 100L))
        assertEquals(0f, TimeUtils.progress(-10L, 100L))
    }

    @Test
    fun `minutesToMillis and back`() {
        assertEquals(5 * 60_000L, TimeUtils.minutesToMillis(5))
        assertEquals(5, TimeUtils.millisToMinutes(5 * 60_000L))
        assertEquals(5, TimeUtils.millisToMinutes(5 * 60_000L - 1))
        assertEquals(6, TimeUtils.millisToMinutes(5 * 60_000L + 30_000L))
    }
}
