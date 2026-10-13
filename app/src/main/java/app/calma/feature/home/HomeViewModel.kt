package app.calma.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.calma.core.common.TimeProvider
import app.calma.core.common.TimeUtils
import app.calma.data.repository.CalmRepository
import app.calma.data.repository.FocusRepository
import app.calma.data.repository.MoodRepository
import app.calma.domain.exercise.CalmExerciseCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val todayFocusMillis: Long = 0L,
    val todaySessionCount: Int = 0,
    val todayCalmCount: Int = 0,
    val todayCheckIns: Int = 0,
    val suggestedCalmExerciseId: String? = null,
    val isLoading: Boolean = true,
)

class HomeViewModel(
    private val focusRepository: FocusRepository,
    private val calmRepository: CalmRepository,
    private val moodRepository: MoodRepository,
    private val calmExercises: CalmExerciseCatalog,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val now = timeProvider.currentTimeMillis()
            val dayStart = TimeUtils.startOfDay(now)
            val dayEnd = dayStart + MILLIS_PER_DAY

            val sessions = focusRepository.sessionsBetween(dayStart, dayEnd)
            val calmSessions = calmRepository.between(dayStart, dayEnd)
            val checkIns = moodRepository.between(dayStart, dayEnd)

            val history = calmRepository.recentExerciseIds()
            val suggested = app.calma.domain.stats.StatisticsCalculator
                .suggestCalmExercise(history, null)
                .takeIf { calmExercises.byId(it) != null }
                ?: calmExercises.exercises.first().id

            _uiState.value = HomeUiState(
                todayFocusMillis = sessions.sumOf { it.focusedMillis },
                todaySessionCount = sessions.size,
                todayCalmCount = calmSessions.size,
                todayCheckIns = checkIns.size,
                suggestedCalmExerciseId = suggested,
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
            calmExercises: CalmExerciseCatalog,
            timeProvider: TimeProvider,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HomeViewModel(
                    focusRepository,
                    calmRepository,
                    moodRepository,
                    calmExercises,
                    timeProvider,
                ) as T
        }
    }
}