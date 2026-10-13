package app.calma.core.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.calma.R

/**
 * Locale-aware formatting helpers.
 *
 * Durations are always produced with Latin digits (see [TimeUtils.formatClock]) because
 * countdown numerals read faster that way; unit labels come from string resources so they
 * are translated.
 */
object Format {

    /**
     * Non-composable duration formatter.
     *
     * Needed from coroutines (notification building, service actions) where a
     * `stringResource` call is not available.
     */
    fun duration(context: android.content.Context, millis: Long): String {
        val parts = TimeUtils.durationParts(millis)
        return when {
            parts.hours > 0 && parts.minutes > 0 ->
                context.getString(R.string.time_hm, parts.hours, parts.minutes)
            parts.hours > 0 -> context.getString(R.string.time_m, parts.hours * 60)
            parts.minutes > 0 -> context.getString(R.string.time_m, parts.minutes)
            else -> context.getString(R.string.time_s, parts.seconds)
        }
    }

    /** `m:ss` / `h:mm:ss` countdown, direction independent. */
    @Composable
    fun clock(durationMillis: Long): String = TimeUtils.formatClock(durationMillis)

    /** e.g. `2h 15m`, `45m`, `30s` — localised units. */
    @Composable
    fun duration(millis: Long): String {
        val parts = TimeUtils.durationParts(millis)
        return when {
            parts.hours > 0 && parts.minutes > 0 ->
                stringResource(R.string.time_hm, parts.hours, parts.minutes)
            parts.hours > 0 -> stringResource(R.string.time_m, parts.hours * 60)
            parts.minutes > 0 -> stringResource(R.string.time_m, parts.minutes)
            else -> stringResource(R.string.time_s, parts.seconds)
        }
    }

    @Composable
    fun minutes(count: Int): String = stringResource(R.string.time_m, count)

    @Composable
    fun seconds(count: Int): String = stringResource(R.string.time_s, count)

    @Composable
    fun zeroDuration(): String = stringResource(R.string.time_zero)

    @Composable
    fun date(epochMillis: Long): String = TimeUtils.formatDate(epochMillis)

    @Composable
    fun dateTime(epochMillis: Long): String = TimeUtils.formatDateTime(epochMillis)

    @Composable
    fun time(epochMillis: Long): String = TimeUtils.formatTime(epochMillis)
}

/**
 * Remembers the current system time as a plain string key.
 * Used by charts that re-render on day change rather than on every tick.
 */
@Composable
fun rememberTodayStamp(): Long = remember { TimeUtils.startOfDay(System.currentTimeMillis()) }