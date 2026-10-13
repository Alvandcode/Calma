package app.calma.feature.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.calma.R
import app.calma.core.common.Format
import app.calma.core.common.StatsRange
import app.calma.core.common.TimeUtils
import app.calma.core.di.appContainer
import app.calma.core.ui.component.CalmaBarChart
import app.calma.core.ui.component.CalmaTrendChart
import app.calma.core.ui.component.ChartBar
import app.calma.core.ui.component.EmptyState
import app.calma.core.ui.component.SectionHeader
import app.calma.core.ui.component.StatTile
import app.calma.core.ui.theme.CalmaSpacing
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StatisticsScreen() {
    val container = appContainer()
    val viewModel: StatisticsViewModel = viewModel(
        factory = StatisticsViewModel.factory(
            focusRepository = container.focusRepository,
            calmRepository = container.calmRepository,
            moodRepository = container.moodRepository,
            timeProvider = container.timeProvider,
        ),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val summary = state.summary

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(CalmaSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(CalmaSpacing.lg),
    ) {
        RangeSelector(
            selected = state.range,
            onSelect = viewModel::selectRange,
        )

        if (summary.isEmpty) {
            EmptyState(
                title = stringResource(R.string.stats_empty),
                description = stringResource(R.string.stats_empty_hint),
                icon = Icons.Outlined.BarChart,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(CalmaSpacing.sm)) {
            StatTile(
                value = Format.duration(summary.totalFocusMillis),
                label = stringResource(R.string.stats_total_focus),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                value = summary.completedSessions.toString(),
                label = stringResource(R.string.stats_sessions),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(CalmaSpacing.sm)) {
            StatTile(
                value = Format.duration(summary.averageSessionMillis),
                label = stringResource(R.string.stats_average_session),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                value = summary.activeDays.toString(),
                label = stringResource(R.string.stats_active_days),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(CalmaSpacing.sm)) {
            StatTile(
                value = summary.calmSessions.toString(),
                label = stringResource(R.string.stats_calm_sessions),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                value = summary.currentStreakDays.toString(),
                label = stringResource(R.string.stats_current_streak),
                modifier = Modifier.weight(1f),
            )
        }

        SectionHeader(title = stringResource(R.string.stats_daily_focus))
        CalmaBarChart(
            bars = summary.dailyFocus.map { day ->
                ChartBar(
                    label = shortDayLabel(day.dayStartEpochMillis),
                    value = day.focusedMillis.toFloat(),
                    valueLabel = Format.duration(day.focusedMillis),
                )
            },
            description = stringResource(R.string.stats_daily_focus),
        )

        if (summary.moodTrend.isNotEmpty()) {
            SectionHeader(title = stringResource(R.string.stats_mood_trend))
            CalmaTrendChart(
                values = summary.moodTrend.map { it.value },
                labels = summary.moodTrend.map { shortDayLabel(it.dayStartEpochMillis) },
                description = stringResource(R.string.stats_mood_trend),
            )
        }

        if (summary.stressTrend.isNotEmpty()) {
            SectionHeader(title = stringResource(R.string.stats_stress_trend))
            CalmaTrendChart(
                values = summary.stressTrend.map { it.value },
                labels = summary.stressTrend.map { shortDayLabel(it.dayStartEpochMillis) },
                description = stringResource(R.string.stats_stress_trend),
            )
        }

        Text(
            text = stringResource(R.string.stats_privacy_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RangeSelector(selected: StatsRange, onSelect: (StatsRange) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(CalmaSpacing.sm)) {
        FilterChip(
            selected = selected == StatsRange.WEEK,
            onClick = { onSelect(StatsRange.WEEK) },
            label = { Text(stringResource(R.string.stats_range_week)) },
            modifier = Modifier.weight(1f),
        )
        FilterChip(
            selected = selected == StatsRange.MONTH,
            onClick = { onSelect(StatsRange.MONTH) },
            label = { Text(stringResource(R.string.stats_range_month)) },
            modifier = Modifier.weight(1f),
        )
    }
}

/** Short weekday / day-of-month label, locale aware. */
@Composable
private fun shortDayLabel(epochMillis: Long): String {
    val date = TimeUtils.localDate(epochMillis)
    val dayOfWeek = date.dayOfWeek
    return if (date.dayOfMonth == 1) {
        date.month.getDisplayName(TextStyle.SHORT, currentLocale())
    } else {
        dayOfWeek.getDisplayName(TextStyle.SHORT, currentLocale())
    }
}

@Composable
private fun currentLocale(): Locale {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    return configuration.locales[0] ?: Locale.getDefault()
}