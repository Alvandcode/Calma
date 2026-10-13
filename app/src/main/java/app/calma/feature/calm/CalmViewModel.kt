package app.calma.feature.calm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.calma.core.common.TimeProvider
import app.calma.data.prefs.SettingsStore
import app.calma.data.repository.CalmRepository
import app.calma.domain.exercise.CalmExercise
import app.calma.domain.exercise.CalmExerciseCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class CalmUiState(
    val exercise: CalmExercise? = null,
    val stepIndex: Int = 0,
    val finished: Boolean = false,
    val startedAtElapsed: Long = 0L,
)

/**
 * "Calm now" flow: pick an exercise, then advance through its steps one at a time.
 *
 * No network, no model inference and no randomness: the suggestion is a deterministic
 * rotation through the bundled catalog, so the feature works fully offline.
 */
class CalmViewModel(
    private val catalog: CalmExerciseCatalog,
    private val calmRepository: CalmRepository,
    private val settingsStore: SettingsStore,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalmUiState())
    val uiState: StateFlow<CalmUiState> = _uiState.asStateFlow()

    val exercises: List<CalmExercise> get() = catalog.exercises

    fun start(exerciseId: String) {
        val exercise = catalog.byId(exerciseId) ?: catalog.exercises.first()
        _uiState.value = CalmUiState(
            exercise = exercise,
            stepIndex = 0,
            finished = false,
            startedAtElapsed = timeProvider.elapsedRealtime(),
        )
        viewModelScope.launch { settingsStore.setLastCalmExercise(exercise.id) }
    }

    fun next() {
        val state = _uiState.value
        val exercise = state.exercise ?: return
        if (state.stepIndex + 1 < exercise.steps.size) {
            _uiState.value = state.copy(stepIndex = state.stepIndex + 1)
        } else {
            finish()
        }
    }

    fun previous() {
        val state = _uiState.value
        if (state.stepIndex > 0) {
            _uiState.value = state.copy(stepIndex = state.stepIndex - 1)
        }
    }

    /** Ends early; time already spent is still recorded. */
    fun stop() {
        record()
        _uiState.value = _uiState.value.copy(finished = true)
    }

    private fun finish() {
        record()
        _uiState.value = _uiState.value.copy(
            stepIndex = _uiState.value.exercise?.steps?.lastIndex ?: 0,
            finished = true,
        )
    }

    private fun record() {
        val state = _uiState.value
        val exercise = state.exercise ?: return
        val elapsed = (timeProvider.elapsedRealtime() - state.startedAtElapsed)
            .coerceAtLeast(0L)
        if (elapsed < MIN_RECORDED_MILLIS) return
        viewModelScope.launch {
            calmRepository.record(
                exerciseId = exercise.id,
                durationMillis = elapsed,
                completedCycles = state.stepIndex + 1,
                endedAtEpochMillis = timeProvider.currentTimeMillis(),
            )
        }
    }

    companion object {
        const val MIN_RECORDED_MILLIS = 5_000L

        fun factory(
            catalog: CalmExerciseCatalog,
            calmRepository: CalmRepository,
            settingsStore: SettingsStore,
            timeProvider: TimeProvider,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CalmViewModel(catalog, calmRepository, settingsStore, timeProvider) as T
        }
    }
}