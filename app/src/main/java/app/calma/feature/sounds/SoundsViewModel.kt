package app.calma.feature.sounds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.calma.audio.AmbientPlaybackService
import app.calma.audio.AmbientSound
import app.calma.audio.MixerState
import app.calma.audio.SoundMixerEngine
import app.calma.core.common.TimeProvider
import app.calma.data.prefs.SettingsStore
import app.calma.data.repository.SoundMixRepository
import app.calma.domain.model.MixLayer
import app.calma.domain.model.SoundMix
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SoundsUiState(
    val mixer: MixerState = MixerState(),
    val savedMixes: List<SoundMix> = emptyList(),
    val sleepTimerMinutes: Int? = null,
    val sleepTimerRemainingMillis: Long = 0L,
    val showSaveDialog: Boolean = false,
    val errorMessage: String? = null,
) {
    val canAddMore: Boolean get() = mixer.activeCount < AmbientSound.MAX_ACTIVE_LAYERS
    val atLayerLimit: Boolean get() = mixer.activeCount >= AmbientSound.MAX_ACTIVE_LAYERS
}

class SoundsViewModel(
    private val engine: SoundMixerEngine,
    private val soundMixRepository: SoundMixRepository,
    private val settingsStore: SettingsStore,
    private val timeProvider: TimeProvider,
    private val onBackgroundPlaybackRequested: () -> Unit,
    private val onBackgroundPlaybackStopped: () -> Unit,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SoundsUiState())
    val uiState: StateFlow<SoundsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsStore.settings.first()
            engine.restoreVolumes(settings.lastMixVolumes)
            engine.setBackgroundPlaybackEnabled(settings.soundBackgroundPlaybackEnabled)
            _uiState.value = _uiState.value.copy(
                mixer = engine.state.value,
                sleepTimerMinutes = null,
            )
        }
        viewModelScope.launch {
            engine.state.collect { mixer ->
                val previous = _uiState.value
                _uiState.value = previous.copy(
                    mixer = mixer,
                    sleepTimerRemainingMillis = engine.sleepTimerRemainingMillis(),
                    errorMessage = if (mixer.activeCount == 0) previous.errorMessage else null,
                )
                if (mixer.isPlaying != previous.mixer.isPlaying) {
                    syncBackgroundService(mixer.isPlaying)
                }
            }
        }
        viewModelScope.launch {
            soundMixRepository.observeMixes().collect { mixes ->
                _uiState.value = _uiState.value.copy(savedMixes = mixes)
            }
        }
    }

    fun toggleLayer(sound: AmbientSound) {
        engine.toggleLayer(sound)
        persistVolumes()
    }

    fun setLayerVolume(sound: AmbientSound, volume: Float) {
        engine.setLayerVolume(sound, volume)
        persistVolumes()
    }

    fun play() {
        engine.play()
        persistVolumes()
    }

    fun stopAll() {
        engine.stopAll()
        _uiState.value = _uiState.value.copy(sleepTimerMinutes = null)
        persistVolumes()
    }

    fun setSleepTimer(minutes: Int?) {
        engine.setSleepTimer(minutes)
        _uiState.value = _uiState.value.copy(
            sleepTimerMinutes = minutes,
            sleepTimerRemainingMillis = engine.sleepTimerRemainingMillis(),
        )
    }

    fun showSaveDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSaveDialog = show)
    }

    fun saveMix(name: String) {
        val layers = _uiState.value.mixer.layers
            .filter { it.volume > 0f }
            .take(AmbientSound.MAX_ACTIVE_LAYERS)
            .map { MixLayer(soundId = it.sound.id, volume = it.volume) }
        if (layers.isEmpty()) {
            _uiState.value = _uiState.value.copy(showSaveDialog = false)
            return
        }
        viewModelScope.launch {
            soundMixRepository.save(name, layers, timeProvider.currentTimeMillis())
            _uiState.value = _uiState.value.copy(showSaveDialog = false)
        }
    }

    fun deleteMix(mix: SoundMix) {
        viewModelScope.launch { soundMixRepository.delete(mix.id) }
    }

    /** Applies a saved mix: stops what plays and starts exactly the stored layers. */
    fun applyMix(mix: SoundMix) {
        viewModelScope.launch {
            val full = soundMixRepository.loadMix(mix.id) ?: return@launch
            engine.stopAll()
            full.layers.forEach { layer ->
                AmbientSound.fromId(layer.soundId)?.let { engine.setLayerVolume(it, layer.volume) }
            }
            full.layers.mapNotNull { AmbientSound.fromId(it.soundId) }
                .take(AmbientSound.MAX_ACTIVE_LAYERS)
                .forEach { engine.toggleLayer(it) }
            persistVolumes()
        }
    }

    fun setBackgroundPlaybackEnabled(enabled: Boolean) {
        engine.setBackgroundPlaybackEnabled(enabled)
        viewModelScope.launch { settingsStore.setBackgroundPlaybackEnabled(enabled) }
        if (!enabled && engine.isPlaying()) syncBackgroundService(true)
    }

    fun refreshSleepTimer() {
        _uiState.value = _uiState.value.copy(
            sleepTimerRemainingMillis = engine.sleepTimerRemainingMillis(),
        )
    }

    private fun persistVolumes() {
        val encoded = engine.currentVolumesEncoding()
        viewModelScope.launch { settingsStore.setMixVolumes(encoded) }
    }

    private fun syncBackgroundService(playing: Boolean) {
        if (!playing) {
            onBackgroundPlaybackStopped()
            return
        }
        if (_uiState.value.mixer.backgroundPlaybackEnabled) onBackgroundPlaybackRequested()
    }

    /** Called from the Activity when the app returns to the foreground. */
    fun onAppResumed() {
        refreshSleepTimer()
    }

    override fun onCleared() {
        super.onCleared()
    }

    companion object {
        fun factory(
            engine: SoundMixerEngine,
            soundMixRepository: SoundMixRepository,
            settingsStore: SettingsStore,
            timeProvider: TimeProvider,
            onBackgroundPlaybackRequested: () -> Unit,
            onBackgroundPlaybackStopped: () -> Unit,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SoundsViewModel(
                    engine,
                    soundMixRepository,
                    settingsStore,
                    timeProvider,
                    onBackgroundPlaybackRequested,
                    onBackgroundPlaybackStopped,
                ) as T
        }
    }
}