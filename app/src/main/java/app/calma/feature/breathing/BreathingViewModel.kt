package app.calma.feature.breathing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.calma.core.common.BreathPhase
import app.calma.core.common.BreathRunState
import app.calma.core.common.TimeProvider
import app.calma.data.prefs.SettingsStore
import app.calma.data.repository.CalmRepository
import app.calma.domain.breathing.BreathSessionState
import app.calma.domain.breathing.BreathingPattern
import app.calma.domain.breathing.BreathingScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class BreathingUiState(
    val pattern: BreathingPattern = BreathingPattern.BUILT_IN.first(),
    val durationMillis: Long = 180_000L,
    val session: BreathSessionState = BreathSessionState(),
    val selectedPatternId: String = BreathingPattern.BUILT_IN.first().id,
    val selectedDurationSeconds: Int = 180,
) {
    val isIdle: Boolean get() = session.runState == BreathSessionState.RunState.IDLE
}

/**
 * Drives a breathing exercise.
 *
 * The phase is recomputed from a single monotonic value on every tick rather than
 * accumulated, so recomposition, rotation and a slow frame cannot desynchronise the circle
 * from the countdown.
 */
class BreathingViewModel(
    private val calmRepository: CalmRepository,
    private val settingsStore: SettingsStore,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BreathingUiState())
    val uiState: StateFlow<BreathingUiState> = _uiState.asStateFlow()

    private var pattern: BreathingPattern = BreathingPattern.BUILT_IN.first()
    private var durationMillis: Long = 180_000L

    /** Monotonic start of the running segment; paused time is excluded. */
    private var runningSinceElapsed: Long = 0L
    /** Accumulated running time before the current segment (pause adds to this). */
    private var bankedElapsed: Long = 0L
    private var tickJob: Job? = null

    /** Increments on every phase change; the UI uses it to fire a single haptic pulse. */
    private val _phaseChangeCount = MutableStateFlow(0)
    val phaseChangeCount: StateFlow<Int> = _phaseChangeCount.asStateFlow()

    private var lastPhase: BreathPhase? = null
    private var recorded = false

    init {
        viewModelScope.launch {
            val settings = settingsStore.settings.first()
            pattern = BreathingPattern.byId(settings.lastBreathingPatternId)
            durationMillis = settings.lastBreathingDurationSeconds * 1000L
            _uiState.value = _uiState.value.copy(
                pattern = pattern,
                durationMillis = durationMillis,
                selectedPatternId = pattern.id,
                selectedDurationSeconds = settings.lastBreathingDurationSeconds,
            )
        }
    }

    fun selectPattern(id: String) {
        if (_uiState.value.session.isActive) return
        pattern = BreathingPattern.byId(id)
        _uiState.value = _uiState.value.copy(pattern = pattern, selectedPatternId = pattern.id)
        viewModelScope.launch { settingsStore.setBreathingPattern(pattern.id) }
    }

    fun selectDurationSeconds(seconds: Int) {
        if (_uiState.value.session.isActive) return
        durationMillis = seconds.coerceAtLeast(30) * 1000L
        _uiState.value = _uiState.value.copy(
            durationMillis = durationMillis,
            selectedDurationSeconds = (durationMillis / 1000L).toInt(),
        )
        viewModelScope.launch { settingsStore.setBreathingDuration(seconds) }
    }

    fun applyCustomPattern(inhale: Int, hold: Int, exhale: Int, rest: Int) {
        if (_uiState.value.session.isActive) return
        // Braces are required: `$hold_` would otherwise parse as a single identifier `hold_`.
        val custom = BreathingPattern(
            id = "custom_${inhale}_${hold}_${exhale}_${rest}",
            inhaleMillis = inhale.coerceAtLeast(1) * 1000L,
            holdAfterInhaleMillis = hold.coerceAtLeast(0) * 1000L,
            exhaleMillis = exhale.coerceAtLeast(1) * 1000L,
            holdAfterExhaleMillis = rest.coerceAtLeast(0) * 1000L,
        )
        pattern = custom
        _uiState.value = _uiState.value.copy(pattern = custom, selectedPatternId = custom.id)
        viewModelScope.launch { settingsStore.setBreathingPattern(custom.id) }
    }

    fun start() {
        recorded = false
        bankedElapsed = 0L
        runningSinceElapsed = timeProvider.elapsedRealtime()
        lastPhase = null
        tickJob?.cancel()
        _uiState.value = _uiState.value.copy(
            session = BreathSessionState(runState = BreathSessionState.RunState.RUNNING),
        )
        tick()
        tickJob = viewModelScope.launch {
            while (isActive) {
                delay(TICK_MS)
                tick()
            }
        }
    }

    fun pause() {
        if (_uiState.value.session.runState != BreathSessionState.RunState.RUNNING) return
        bankedElapsed += timeProvider.elapsedRealtime() - runningSinceElapsed
        tickJob?.cancel()
        tickJob = null
        publish(BreathSessionState.RunState.PAUSED)
    }

    fun resume() {
        if (_uiState.value.session.runState != BreathSessionState.RunState.PAUSED) return
        runningSinceElapsed = timeProvider.elapsedRealtime()
        publish(BreathSessionState.RunState.RUNNING)
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (isActive) {
                delay(TICK_MS)
                tick()
            }
        }
    }

    /** Ends the exercise early. Time already breathed is still recorded. */
    fun stop() {
        tickJob?.cancel()
        tickJob = null
        val elapsed = currentElapsed()
        recordIfNeeded(elapsed, finished = false)
        _uiState.value = _uiState.value.copy(
            session = BreathSessionState(runState = BreathSessionState.RunState.IDLE),
        )
        lastPhase = null
    }

    private fun currentElapsed(): Long {
        val base = if (_uiState.value.session.runState == BreathSessionState.RunState.RUNNING) {
            bankedElapsed + (timeProvider.elapsedRealtime() - runningSinceElapsed)
        } else {
            bankedElapsed
        }
        return base.coerceIn(0L, durationMillis)
    }

    private fun tick() {
        val elapsed = currentElapsed()
        val snapshot = BreathingScheduler.snapshot(elapsed, pattern, durationMillis)
        if (snapshot.phase != lastPhase) {
            lastPhase = snapshot.phase
            _phaseChangeCount.value += 1
        }
        if (snapshot.finished) {
            publish(
                state = BreathSessionState.RunState.FINISHED,
                snapshot = snapshot,
                elapsed = elapsed,
            )
            recordIfNeeded(elapsed, finished = true)
            tickJob?.cancel()
            tickJob = null
            return
        }
        publish(BreathSessionState.RunState.RUNNING, snapshot, elapsed)
    }

    private fun publish(
        state: BreathSessionState.RunState,
        snapshot: BreathingScheduler.Snapshot? = null,
        elapsed: Long = currentElapsed(),
    ) {
        val computed = snapshot ?: BreathingScheduler.snapshot(elapsed, pattern, durationMillis)
        _uiState.value = _uiState.value.copy(
            session = BreathSessionState(
                runState = state,
                phase = computed.phase,
                phaseProgress = computed.phaseProgress,
                phaseRemainingSeconds = computed.phaseRemainingSeconds,
                remainingMillis = (durationMillis - computed.elapsedMillis).coerceAtLeast(0L),
                cycleIndex = computed.cycleIndex,
                totalCycles = computed.totalCycles,
            ),
        )
    }

    /** Records the exercise at most once per run. */
    private fun recordIfNeeded(elapsed: Long, finished: Boolean) {
        if (recorded) return
        if (elapsed < MIN_RECORDED_MILLIS) return
        recorded = true
        val cycles = BreathingScheduler.completedCycles(elapsed, pattern)
        viewModelScope.launch {
            calmRepository.record(
                exerciseId = "breathing_${pattern.id}",
                durationMillis = elapsed,
                completedCycles = cycles,
                endedAtEpochMillis = timeProvider.currentTimeMillis(),
            )
        }
    }

    override fun onCleared() {
        tickJob?.cancel()
        super.onCleared()
    }

    companion object {
        const val TICK_MS = 50L
        const val MIN_RECORDED_MILLIS = 5_000L

        fun factory(
            calmRepository: CalmRepository,
            settingsStore: SettingsStore,
            timeProvider: TimeProvider,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                BreathingViewModel(calmRepository, settingsStore, timeProvider) as T
        }
    }
}