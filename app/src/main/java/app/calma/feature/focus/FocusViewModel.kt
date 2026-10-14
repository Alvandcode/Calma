package app.calma.feature.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.calma.core.common.FocusPhase
import app.calma.core.common.FocusRunState
import app.calma.core.common.TimeProvider
import app.calma.data.prefs.SettingsStore
import app.calma.data.repository.FocusRepository
import app.calma.domain.focus.FocusEvent
import app.calma.domain.focus.FocusTimerEngine
import app.calma.domain.focus.FocusTimerSnapshot
import app.calma.domain.model.FocusTimerState
import app.calma.domain.model.FocusPresets
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** One-shot UI event. */
sealed interface FocusUiEvent {
    data class Completed(val focusedMillis: Long, val hadBreak: Boolean) : FocusUiEvent
    data object Discarded : FocusUiEvent
    data object BreakFinished : FocusUiEvent
}

/** UI state for the focus screen. */
data class FocusUiState(
    val timer: FocusTimerState = FocusTimerState.Idle,
    val selectedFocusMinutes: Int = 25,
    val selectedBreakMinutes: Int = 5,
    val todayFocusMillis: Long = 0L,
    val todaySessionCount: Int = 0,
    val restoredSession: Boolean = false,
) {
    val isIdle: Boolean get() = !timer.isActive
}

class FocusViewModel(
    private val repository: FocusRepository,
    private val settingsStore: SettingsStore,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val _timerState = MutableStateFlow(FocusTimerState.Idle)
    val timerState: StateFlow<FocusTimerState> = _timerState.asStateFlow()

    private val _selectedFocusMinutes = MutableStateFlow(25)
    val selectedFocusMinutes: StateFlow<Int> = _selectedFocusMinutes.asStateFlow()

    private val _selectedBreakMinutes = MutableStateFlow(5)
    val selectedBreakMinutes: StateFlow<Int> = _selectedBreakMinutes.asStateFlow()

    private val _restoredSession = MutableStateFlow(false)
    val restoredSession: StateFlow<Boolean> = _restoredSession.asStateFlow()

    private val _events = MutableStateFlow<FocusUiEvent?>(null)
    val events: StateFlow<FocusUiEvent?> = _events.asStateFlow()

    /** Authoritative timer state; mirrored into [timerState] for the UI. */
    private var snapshot: FocusTimerSnapshot = FocusTimerSnapshot()
    private var tickJob: Job? = null

    init {
        viewModelScope.launch {
            settingsStore.settings.collect { settings ->
                if (!snapshot.isActive) {
                    _selectedFocusMinutes.value = settings.defaultFocusMinutes
                    _selectedBreakMinutes.value = settings.defaultBreakMinutes
                }
            }
        }
        viewModelScope.launch {
            runCatching { restoreIfNeeded() }
                .onFailure { e ->
                    android.util.Log.e("FocusViewModel", "Failed to restore session", e)
                    resetToIdle()
                }
        }
    }

    // ---- UI inputs ---------------------------------------------------------------------

    fun selectFocusPreset(minutes: Int) {
        if (snapshot.isActive) return
        _selectedFocusMinutes.value = FocusTimerEngine.sanitizeFocusMinutes(minutes)
        viewModelScope.launch { settingsStore.setDefaultFocusMinutes(_selectedFocusMinutes.value) }
    }

    fun selectBreakPreset(minutes: Int) {
        if (snapshot.isActive) return
        _selectedBreakMinutes.value = FocusTimerEngine.sanitizeBreakMinutes(minutes)
        viewModelScope.launch { settingsStore.setDefaultBreakMinutes(_selectedBreakMinutes.value) }
    }

    fun start() {
        if (snapshot.isActive) return
        snapshot = FocusTimerEngine.start(
            nowElapsedMillis = timeProvider.elapsedRealtime(),
            nowWallClockMillis = timeProvider.currentTimeMillis(),
            focusMillis = _selectedFocusMinutes.value * 60_000L,
            breakMillis = _selectedBreakMinutes.value * 60_000L,
        )
        _restoredSession.value = false
        persist()
        publish()
        startTicking()
    }

    fun pause() {
        if (snapshot.runState != FocusRunState.RUNNING) return
        snapshot = FocusTimerEngine.pause(snapshot, timeProvider.elapsedRealtime())
        persist()
        publish()
        stopTicking()
    }

    fun resume() {
        if (snapshot.runState != FocusRunState.PAUSED) return
        snapshot = FocusTimerEngine.resume(snapshot, timeProvider.elapsedRealtime())
        persist()
        publish()
        startTicking()
    }

    /** Ends the session early, keeping whatever focus time was actually accumulated. */
    fun finish() {
        val event = FocusTimerEngine.finishEarly(
            snapshot = snapshot,
            nowElapsedMillis = timeProvider.elapsedRealtime(),
            nowWallClockMillis = timeProvider.currentTimeMillis(),
        )
        stopTicking()
        viewModelScope.launch {
            when (event) {
                is FocusEvent.FocusCompleted -> {
                    record(event.focusedMillis)
                    snapshot = FocusTimerEngine.startBreak(
                        snapshot,
                        timeProvider.elapsedRealtime(),
                        event.focusedMillis,
                    )
                    persist()
                    publish()
                    _events.value = FocusUiEvent.Completed(event.focusedMillis, hadBreak = true)
                    startTicking()
                }

                is FocusEvent.SessionFinished -> {
                    record(event.focusedMillis)
                    resetToIdle()
                    _events.value = if (event.focusedMillis >= 1000L) {
                        FocusUiEvent.Completed(event.focusedMillis, hadBreak = false)
                    } else {
                        FocusUiEvent.Discarded
                    }
                }

                else -> Unit
            }
        }
    }

    /** Throws the session away without recording it. */
    fun cancel() {
        stopTicking()
        resetToIdle()
        _events.value = FocusUiEvent.Discarded
    }

    fun consumeEvent() {
        _events.value = null
    }

    /** Restores an in-progress session after process death or a configuration change. */
    fun reconcile() {
        viewModelScope.launch { restoreIfNeeded(force = true) }
    }

    // ---- internals ---------------------------------------------------------------------

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (isActive) {
                val event = FocusTimerEngine.advance(
                    snapshot = snapshot,
                    nowElapsedMillis = timeProvider.elapsedRealtime(),
                    nowWallClockMillis = timeProvider.currentTimeMillis(),
                )
                when (event) {
                    is FocusEvent.Continue -> {
                        snapshot = event.snapshot
                        publish()
                        persistThrottled()
                        delay(TICK_MS)
                    }

                    is FocusEvent.FocusCompleted -> {
                        snapshot = event.snapshot
                        publish()
                        record(event.focusedMillis)
                        _events.value = FocusUiEvent.Completed(event.focusedMillis, hadBreak = true)
                        persist()
                    }

                    is FocusEvent.SessionFinished -> {
                        record(event.focusedMillis)
                        _events.value = if (event.focusedMillis >= 1000L) {
                            FocusUiEvent.Completed(event.focusedMillis, hadBreak = false)
                        } else {
                            FocusUiEvent.Discarded
                        }
                        resetToIdle()
                        stopTicking()
                        return@launch
                    }

                    is FocusEvent.ReconciledAfterReboot -> {
                        snapshot = event.snapshot
                        publish()
                        persist()
                        stopTicking()
                    }
                }
            }
        }
    }

    private fun stopTicking() {
        tickJob?.cancel()
        tickJob = null
    }

    private suspend fun restoreIfNeeded(force: Boolean = false) {
        val stored = repository.loadActive() ?: return
        if (stored.runState == FocusRunState.COMPLETED) {
            repository.deleteActive()
            return
        }
        if (!force && snapshot.isActive) return

        val nowElapsed = timeProvider.elapsedRealtime()
        val nowWall = timeProvider.currentTimeMillis()
        val event = FocusTimerEngine.advance(stored, nowElapsed, nowWall)

        when (event) {
            is FocusEvent.FocusCompleted -> {
                // The app was closed when the focus block ended. Record it exactly once.
                recordFocus(stored, event.focusedMillis)
                snapshot = FocusTimerEngine.startBreak(stored, nowElapsed, event.focusedMillis)
                _restoredSession.value = true
                _events.value = FocusUiEvent.Completed(event.focusedMillis, hadBreak = true)
                persist()
                publish()
                if (stored.runState == FocusRunState.RUNNING) startTicking()
            }

            is FocusEvent.SessionFinished -> {
                recordFocus(stored, event.focusedMillis)
                resetToIdle()
                _events.value = FocusUiEvent.Completed(event.focusedMillis, hadBreak = false)
            }

            is FocusEvent.ReconciledAfterReboot -> {
                snapshot = event.snapshot
                _restoredSession.value = true
                persist()
                publish()
            }

            is FocusEvent.Continue -> {
                snapshot = event.snapshot
                _restoredSession.value = true
                _selectedFocusMinutes.value =
                    FocusTimerEngine.sanitizeFocusMinutes((stored.focusMillis / 60_000L).toInt())
                _selectedBreakMinutes.value =
                    FocusTimerEngine.sanitizeBreakMinutes((stored.breakMillis / 60_000L).toInt())
                publish()
                if (stored.runState == FocusRunState.RUNNING) startTicking()
            }
        }
    }

    private suspend fun record(focusedMillis: Long) {
        recordFocus(snapshot, focusedMillis)
    }

    /**
     * Writes a completed focus block.
     *
     * [FocusRepository.recordCompletedBlock] is idempotent: it refuses to insert when the
     * stored session is already marked COMPLETED, which is what stops a restored session
     * from being logged twice.
     */
    private suspend fun recordFocus(source: FocusTimerSnapshot, focusedMillis: Long) {
        if (focusedMillis < 1_000L) return
        repository.recordCompletedBlock(
            snapshot = source,
            focusedMillis = focusedMillis,
            startedAtEpochMillis = source.startedAtEpochMillis,
            endedAtEpochMillis = timeProvider.currentTimeMillis(),
            nowElapsedMillis = timeProvider.elapsedRealtime(),
            nowWallClockMillis = timeProvider.currentTimeMillis(),
        )
    }

    private fun resetToIdle() {
        snapshot = FocusTimerSnapshot(
            runState = FocusRunState.IDLE,
            focusMillis = _selectedFocusMinutes.value * 60_000L,
            breakMillis = _selectedBreakMinutes.value * 60_000L,
        )
        viewModelScope.launch { repository.deleteActive() }
        publish()
    }

    private var lastPersistElapsed = 0L

    /** Persisting every 250 ms would be wasteful; once a second is plenty for recovery. */
    private fun persistThrottled() {
        val now = timeProvider.elapsedRealtime()
        if (now - lastPersistElapsed < PERSIST_INTERVAL_MS) return
        lastPersistElapsed = now
        viewModelScope.launch { persist() }
    }

    private fun persist() {
        viewModelScope.launch {
            repository.saveActive(
                snapshot = snapshot,
                nowElapsedMillis = timeProvider.elapsedRealtime(),
                nowWallClockMillis = timeProvider.currentTimeMillis(),
            )
        }
    }

    private fun publish() {
        val nowElapsed = timeProvider.elapsedRealtime()
        _timerState.value = FocusTimerState(
            runState = when (snapshot.runState) {
                FocusRunState.IDLE -> FocusTimerState.RunState.IDLE
                FocusRunState.RUNNING -> FocusTimerState.RunState.RUNNING
                FocusRunState.PAUSED -> FocusTimerState.RunState.PAUSED
                FocusRunState.COMPLETED -> FocusTimerState.RunState.FINISHED
            },
            phase = when (snapshot.phase) {
                FocusPhase.FOCUS -> FocusTimerState.Phase.FOCUS
                FocusPhase.BREAK -> FocusTimerState.Phase.BREAK
            },
            totalMillis = snapshot.segmentDurationMillis,
            remainingMillis = FocusTimerEngine.remainingMillis(snapshot, nowElapsed),
            elapsedMillis = FocusTimerEngine.elapsedInSegment(snapshot, nowElapsed),
            focusedMillis = FocusTimerEngine.bankedFocusMillis(snapshot, nowElapsed),
        )
    }

    override fun onCleared() {
        stopTicking()
        super.onCleared()
    }

    companion object {
        const val TICK_MS = 250L
        const val PERSIST_INTERVAL_MS = 1_000L

        fun factory(
            repository: FocusRepository,
            settingsStore: SettingsStore,
            timeProvider: TimeProvider,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                FocusViewModel(repository, settingsStore, timeProvider) as T
        }
    }
}