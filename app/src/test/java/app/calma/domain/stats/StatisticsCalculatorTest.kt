package app.calma.domain.stats

import app.calma.core.common.StatsRange
import app.calma.core.common.TimeUtils
import app.calma.domain.model.CalmSession
import app.calma.domain.model.FocusSession
import app.calma.domain.model.MoodEntry
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatisticsCalculatorTest {

    private val zone = ZoneId.of("UTC")
    private val day = StatisticsCalculator.MILLIS_PER_DAY
    private val today = TimeUtils.startOfDay(System.currentTimeMillis(), zone)
    private val yesterday = today - day

    @Test
    fun `summarize week aggregates focus correctly`() {
        val sessions = listOf(
            FocusSession(1, 25 * 60_000L, 25 * 60_000L, 0, yesterday, yesterday + 1),
            FocusSession(2, 45 * 60_000L, 45 * 60_000L, 0, today - 2 * day, today - 2 * day + 1),
            FocusSession(3, 10 * 60_000L, 25 * 60_000L, 0, today - 10 * day, today - 10 * day + 1), // outside week
        )
        val calm = listOf<CalmSession>()
        val moods = listOf<MoodEntry>()

        val summary = StatisticsCalculator.summarize(
            range = StatsRange.WEEK,
            nowEpochMillis = today + day,
            sessions = sessions,
            calmSessions = calm,
            moodEntries = moods,
            allSessionEndTimes = sessions.map { it.endedAtEpochMillis },
            lifetimeFocusMillis = 0, lifetimeSessions = 0,
            lifetimeCalmMillis = 0, lifetimeCalmSessions = 0,
            moodEntryCount = 0,
        )

        assertEquals(70 * 60_000L, summary.totalFocusMillis)
        assertEquals(2, summary.completedSessions)
        assertEquals(35 * 60_000L, summary.averageSessionMillis)
        assertEquals(7, summary.dailyFocus.size)
    }

    @Test
    fun `summarize streak counts consecutive days`() {
        val sessions = listOf(
            FocusSession(1, 1, 1, 0, 0, today - day), // yesterday
            FocusSession(2, 1, 1, 0, 0, today - 2 * day), // day before
            FocusSession(3, 1, 1, 0, 0, today - 4 * day), // gap
        )
        val endTimes = sessions.map { it.endedAtEpochMillis }

        val streak = StatisticsCalculator.currentStreak(endTimes, today, ZoneId.of("UTC"))
        assertEquals(2, streak)
    }

    @Test
    fun `summarize mood trend averages per day`() {
        val moods = listOf(
            MoodEntry(1, 5, 1, 4, 4, null, today - day, today - day), // yesterday
            MoodEntry(2, 3, 3, 3, 3, null, today - day, today - day), // also yesterday
            MoodEntry(3, 1, 5, 2, 2, null, today - 2 * day, today - 2 * day), // day before
        )

        val summary = StatisticsCalculator.summarize(
            range = StatsRange.WEEK,
            nowEpochMillis = today + day,
            sessions = listOf(),
            calmSessions = listOf(),
            moodEntries = moods,
            allSessionEndTimes = listOf(),
            lifetimeFocusMillis = 0, lifetimeSessions = 0,
            lifetimeCalmMillis = 0, lifetimeCalmSessions = 0,
            moodEntryCount = 0,
        )

        // yesterday average mood = (5+3)/2 = 4, day before = 1
        assertEquals(2, summary.moodTrend.size)
        assertEquals(1f, summary.moodTrend[0].value)
        assertEquals(4f, summary.moodTrend[1].value)
    }

    @Test
    fun `suggestCalmExercise rotates through unseen`() {
        val history = listOf("breathe_slow", "five_senses", "look_around")
        val suggested = StatisticsCalculator.suggestCalmExercise(history, "look_around")
        // Should pick first unseen after last
        assertEquals("be_here_now", suggested)
    }
}