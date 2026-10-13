package app.calma.core.common

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Time helpers.
 *
 * Calma deliberately avoids `java.time` APIs below API 26; minSdk is 26, so the
 * `java.time` package is available on every supported device without desugaring.
 */
object TimeUtils {

    val DEFAULT_ZONE: ZoneId get() = ZoneId.systemDefault()

    fun startOfDay(epochMillis: Long, zone: ZoneId = DEFAULT_ZONE): Long =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
            .atStartOfDay(zone).toInstant().toEpochMilli()

    fun localDate(epochMillis: Long, zone: ZoneId = DEFAULT_ZONE): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    fun startOfWeek(epochMillis: Long, zone: ZoneId = DEFAULT_ZONE): Long =
        localDate(epochMillis, zone).with(DayOfWeek.MONDAY)
            .atStartOfDay(zone).toInstant().toEpochMilli()

    fun startOfMonth(epochMillis: Long, zone: ZoneId = DEFAULT_ZONE): Long =
        localDate(epochMillis, zone).withDayOfMonth(1)
            .atStartOfDay(zone).toInstant().toEpochMilli()

    fun endOfMonth(epochMillis: Long, zone: ZoneId = DEFAULT_ZONE): Long =
        localDate(epochMillis, zone).withDayOfMonth(1).plusMonths(1)
            .atStartOfDay(zone).toInstant().toEpochMilli()

    fun daysBetween(fromEpochMillis: Long, toEpochMillis: Long, zone: ZoneId = DEFAULT_ZONE): Int {
        val from = localDate(fromEpochMillis, zone)
        val to = localDate(toEpochMillis, zone)
        return ChronoUnit.DAYS.between(from, to).toInt()
    }

    fun isSameDay(a: Long, b: Long, zone: ZoneId = DEFAULT_ZONE): Boolean =
        localDate(a, zone) == localDate(b, zone)

    fun dateTime(epochMillis: Long, zone: ZoneId = DEFAULT_ZONE): LocalDateTime =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDateTime()

    fun toEpochMillis(dateTime: LocalDateTime, zone: ZoneId = DEFAULT_ZONE): Long =
        dateTime.atZone(zone).toInstant().toEpochMilli()

    private val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val TIME_HM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun formatDate(epochMillis: Long, zone: ZoneId = DEFAULT_ZONE): String =
        localDate(epochMillis, zone).format(ISO_DATE)

    fun formatTime(epochMillis: Long, zone: ZoneId = DEFAULT_ZONE): String =
        dateTime(epochMillis, zone).format(TIME_HM)

    fun formatDateTime(epochMillis: Long, zone: ZoneId = DEFAULT_ZONE): String =
        "${formatDate(epochMillis, zone)} ${formatTime(epochMillis, zone)}"

    /**
     * Formats [durationMillis] as `m:ss` or `h:mm:ss`, always using Latin digits so the
     * countdown stays legible regardless of locale.
     */
    fun formatClock(durationMillis: Long): String {
        val safe = durationMillis.coerceAtLeast(0L)
        val totalSeconds = safe / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    /**
     * Compact duration for summaries, e.g. `2h 15m`, `45m`, `30s`.
     * Returned as a raw value; screens wrap it in the localised `time_*` string resources
     * so units are translated.
     */
    fun durationParts(durationMillis: Long): DurationParts {
        val safe = abs(durationMillis)
        val totalSeconds = safe / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return DurationParts(hours.toInt(), minutes.toInt(), seconds.toInt())
    }

    /** Seconds remaining, rounded up, so "0" only shows when the deadline truly passed. */
    fun secondsRemaining(deadlineElapsedMs: Long, nowElapsedMs: Long): Int {
        val remaining = deadlineElapsedMs - nowElapsedMs
        if (remaining <= 0L) return 0
        return ((remaining + 999L) / 1000L).toInt()
    }

    /** Progress in `0f..1f` of [elapsed] within [total]. */
    fun progress(elapsedMillis: Long, totalMillis: Long): Float {
        if (totalMillis <= 0L) return 1f
        return (elapsedMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)
    }

    fun minutesToMillis(minutes: Int): Long = minutes * 60_000L

    fun millisToMinutes(millis: Long): Int =
        Math.round(millis / 60_000.0).toInt()

    fun millisToRoundedSeconds(millis: Long): Int = (millis / 1000.0).roundToInt()
}

data class DurationParts(val hours: Int, val minutes: Int, val seconds: Int)