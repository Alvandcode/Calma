package app.calma.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.calma.core.common.StatsRange
import app.calma.core.common.TimeProvider
import app.calma.core.common.TimeUtils
import app.calma.data.repository.CalmRepository
import app.calma.data.repository.FocusRepository
import app.calma.data.repository.MoodRepository
import app.calma.domain.stats.StatisticsCalculator
import app.calma.domain.stats.StatisticsSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StatisticsUiState(
    val range: StatsRange = StatsRange.WEEK,
    val summary: StatisticsSummary = StatisticsSummary(),
    val isLoading: Boolean = true,
)

/**
 * Builds the Progress screen from local data only.
 *
 * Every number is recomputed from the rows on each refresh, so deleting a session or a
 * check-in immediately and correctly changes the totals.
 */
class StatisticsViewModel(
    private val focusRepository: FocusRepository,
    private val calmRepository: CalmRepository,
    private val moodRepository: MoodRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private var range: StatsRange = StatsRange.WEEK

    init {
        refresh()
    }

    fun selectRange(value: StatsRange) {
        if (range == value) return
        range = value
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val now = timeProvider.currentTimeMillis()
            val todayStart = TimeUtils.startOfDay(now)
            val rangeStart = todayStart - (range.days - 1L) * MILLIS_PER_DAY
            val rangeEnd = todayStart + MILLIS_PER_DAY

            val summary = StatisticsCalculator.summarize(
                range = range,
                nowEpochMillis = now,
                sessions = focusRepository.sessionsBetween(rangeStart, rangeEnd),
                calmSessions = calmRepository.between(rangeStart, rangeEnd),
                moodEntries = moodRepository.between(rangeStart, rangeEnd),
                allSessionEndTimes = focusRepository.allSessionEndTimes(),
                lifetimeFocusMillis = focusRepository.lifetimeFocusMillis(),
                lifetimeSessions = focusRepository.totalSessionCount(),
                lifetimeCalmMillis = calmRepository.lifetimeMillis(),
                lifetimeCalmSessions = calmRepository.count(),
                moodEntryCount = moodRepository.count(),
            )

            _uiState.value = StatisticsUiState(
                range = range,
                summary = summary,
                isLoading = false,
            )
        }
    }

    companion object {
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

        fun factory(
            focusRepository: FocusRepository,
            calmRepository: CalmRepository,
            moodRepository: MoodRepository,
            timeProvider: TimeProvider,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                StatisticsViewModel(focusRepository, calmRepository, moodRepository, timeProvider) as T
        }
    }
}