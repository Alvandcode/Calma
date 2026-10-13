package app.calma.domain.stats

import app.calma.core.common.StatsRange
import app.calma.core.common.TimeUtils
import app.calma.domain.model.CalmSession
import app.calma.domain.model.FocusSession
import app.calma.domain.model.MoodEntry
import java.time.ZoneId

/** One bar in the daily focus chart. */
data class DailyFocus(
    val dayStartEpochMillis: Long,
    val focusedMillis: Long,
    val sessionCount: Int,
)

/** One point in a mood / stress trend line. */
data class MoodPoint(
    val dayStartEpochMillis: Long,
    val value: Float,
)

/** Everything the Progress screen renders, computed on-device. */
data class StatisticsSummary(
    val range: StatsRange = StatsRange.WEEK,
    val totalFocusMillis: Long = 0L,
    val completedSessions: Int = 0,
    val averageSessionMillis: Long = 0L,
    val calmSessions: Int = 0,
    val calmMillis: Long = 0L,
    val activeDays: Int = 0,
    val currentStreakDays: Int = 0,
    val dailyFocus: List<DailyFocus> = emptyList(),
    val moodTrend: List<MoodPoint> = emptyList(),
    val stressTrend: List<MoodPoint> = emptyList(),
    val lifetimeFocusMillis: Long = 0L,
    val lifetimeSessions: Int = 0,
    val lifetimeCalmMillis: Long = 0L,
    val lifetimeCalmSessions: Int = 0,
    val moodEntryCount: Int = 0,
) {
    val isEmpty: Boolean
        get() = totalFocusMillis <= 0L &&
            calmSessions == 0 &&
            moodTrend.isEmpty() &&
            stressTrend.isEmpty() &&
            dailyFocus.all { it.focusedMillis <= 0L }
}

/**
 * Statistics aggregation.
 *
 * Kept free of Android and of Room types so it can be exhaustively unit tested. Callers
 * hand it already-loaded rows, which also means deleting a record immediately changes the
 * result: the numbers are never cached or stored separately.
 */
object StatisticsCalculator {

    /**
     * @param nowEpochMillis reference "now".
     * @param sessions completed focus sessions inside the range.
     * @param calmSessions finished calm exercises inside the range.
     * @param moodEntries check-ins inside the range, oldest first.
     * @param allSessionEndTimes every session end time ever recorded, used for the streak.
     */
    fun summarize(
        range: StatsRange,
        nowEpochMillis: Long,
        sessions: List<FocusSession>,
        calmSessions: List<CalmSession>,
        moodEntries: List<MoodEntry>,
        allSessionEndTimes: List<Long>,
        zone: ZoneId = TimeUtils.DEFAULT_ZONE,
        lifetimeFocusMillis: Long = 0L,
        lifetimeSessions: Int = 0,
        lifetimeCalmMillis: Long = 0L,
        lifetimeCalmSessions: Int = 0,
        moodEntryCount: Int = 0,
    ): StatisticsSummary {
        val today = TimeUtils.startOfDay(nowEpochMillis, zone)
        val rangeStart = today - (range.days - 1L) * MILLIS_PER_DAY

        val inRange = sessions.filter {
            it.endedAtEpochMillis >= rangeStart && it.endedAtEpochMillis < today + MILLIS_PER_DAY
        }

        val totalFocus = inRange.sumOf { it.focusedMillis }
        val completed = inRange.size
        val average = if (completed > 0) totalFocus / completed else 0L

        val dailyFocus = buildDailyFocus(range, rangeStart, today, inRange, zone)
        val calmInRange = calmSessions.filter {
            it.endedAtEpochMillis >= rangeStart && it.endedAtEpochMillis < today + MILLIS_PER_DAY
        }

        val moodInRange = moodEntries.filter {
            it.recordedAtEpochMillis >= rangeStart && it.recordedAtEpochMillis < today + MILLIS_PER_DAY
        }

        return StatisticsSummary(
            range = range,
            totalFocusMillis = totalFocus,
            completedSessions = completed,
            averageSessionMillis = average,
            calmSessions = calmInRange.size,
            calmMillis = calmInRange.sumOf { it.durationMillis },
            activeDays = dailyFocus.count { it.focusedMillis > 0L },
            currentStreakDays = currentStreak(allSessionEndTimes, today, zone),
            dailyFocus = dailyFocus,
            moodTrend = buildTrend(moodInRange, range.days) { it.mood },
            stressTrend = buildTrend(moodInRange, range.days) { it.stress },
            lifetimeFocusMillis = lifetimeFocusMillis,
            lifetimeSessions = lifetimeSessions,
            lifetimeCalmMillis = lifetimeCalmMillis,
            lifetimeCalmSessions = lifetimeCalmSessions,
            moodEntryCount = moodEntryCount,
        )
    }

    /**
     * Builds one entry per day in the range so the chart always has a full set of bars,
     * including zero days. A gap therefore reads as "no session", never as "no data".
     */
    private fun buildDailyFocus(
        range: StatsRange,
        rangeStart: Long,
        today: Long,
        sessions: List<FocusSession>,
        zone: ZoneId,
    ): List<DailyFocus> {
        val byDay = sessions.groupBy { TimeUtils.startOfDay(it.endedAtEpochMillis, zone) }
        return (0 until range.days).map { offset ->
            val dayStart = rangeStart + offset * MILLIS_PER_DAY
            val daySessions = byDay[dayStart].orEmpty()
            DailyFocus(
                dayStartEpochMillis = dayStart,
                focusedMillis = daySessions.sumOf { it.focusedMillis },
                sessionCount = daySessions.size,
            )
        }.also { if (range == StatsRange.WEEK) require(it.size == 7) }
    }

    /**
     * Averages each day's check-ins so multiple entries per day do not overweight the
     * trend. Days without an entry are omitted, which the chart renders as a gap rather
     * than as a drop to zero.
     */
    private fun buildTrend(
        entries: List<MoodEntry>,
        days: Int,
        selector: (MoodEntry) -> Int,
    ): List<MoodPoint> {
        if (entries.isEmpty()) return emptyList()
        return entries
            .groupBy { TimeUtils.startOfDay(it.recordedAtEpochMillis) }
            .entries
            .sortedBy { it.key }
            .takeLast(days)
            .map { (day, dayEntries) ->
                MoodPoint(
                    dayStartEpochMillis = day,
                    value = dayEntries.map(selector).average().toFloat(),
                )
            }
    }

    /**
     * Consecutive days with at least one completed session, counting back from today.
     * Today not being active yet does not break the streak: the walk starts at yesterday.
     */
    fun currentStreak(
        allSessionEndTimes: List<Long>,
        todayStartEpochMillis: Long,
        zone: ZoneId = TimeUtils.DEFAULT_ZONE,
    ): Int {
        if (allSessionEndTimes.isEmpty()) return 0
        val activeDays = allSessionEndTimes
            .map { TimeUtils.startOfDay(it, zone) }
            .toSortedSet()
        if (activeDays.isEmpty()) return 0

        var cursor = todayStartEpochMillis
        if (cursor !in activeDays) {
            cursor -= MILLIS_PER_DAY
            if (cursor !in activeDays) return 0
        }

        var streak = 0
        while (cursor in activeDays) {
            streak++
            cursor -= MILLIS_PER_DAY
        }
        return streak
    }

    /** Suggests a calm exercise without any randomness, AI or network access. */
    fun suggestCalmExercise(
        history: List<String>,
        lastExerciseId: String?,
    ): String {
        val ROTATION = listOf(
            "breathe_slow",
            "five_senses",
            "look_around",
            "be_here_now",
            "let_thoughts_pass",
        )
        // Start from what the user did last, then walk to the first unseen exercise.
        val startIndex = ROTATION.indexOf(lastExerciseId).takeIf { it >= 0 }?.plus(1) ?: 0
        for (offset in ROTATION.indices) {
            val candidate = ROTATION[(startIndex + offset) % ROTATION.size]
            if (candidate !in history.takeLast(3)) return candidate
        }
        return ROTATION[startIndex % ROTATION.size]
    }

    const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
}