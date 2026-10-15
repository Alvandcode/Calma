package app.calma.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import app.calma.core.common.TimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Plays and mixes the ambient sound layers.
 *
 * Design notes
 * ------------
 * * One [ExoPlayer] per active layer, capped at
 *   [AmbientSound.MAX_ACTIVE_LAYERS], because a single player with a playlist cannot give
 *   each layer an independent volume.
 * * Audio focus is requested **once** for the whole mixer. Letting every player request
 *   focus separately makes the layers duck each other.
 * * Each player is built with `handleAudioFocus = false`; this class owns focus so it can
 *   keep [MixerState] and the real playback status consistent.
 * * Transient focus loss (navigation prompt) pauses and restores; permanent loss (a phone
 *   call) stops; a duck lowers the volume instead of stopping.
 * * `ACTION_AUDIO_BECOMING_NOISY` stops playback, which is what users expect when
 *   headphones are unplugged.
 *
 * The engine is process scoped: created by the application container and shared by the
 * mixer screen and [AmbientPlaybackService].
 */
@OptIn(UnstableApi::class)
class SoundMixerEngine(
    private val context: Context,
    private val timeProvider: TimeProvider,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) {

    private val audioManager: AudioManager? =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val players = ConcurrentHashMap<AmbientSound, ExoPlayer>()
    private val _state = MutableStateFlow(MixerState())
    val state: StateFlow<MixerState> = _state.asStateFlow()

    private var focusRequest: AudioFocusRequest? = null
    private var hasFocus = false
    private var resumeOnFocusGain = false
    private var noisyReceiverRegistered = false
    private var sleepTimerJob: Job? = null

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeOnFocusGain = false
                stopAll()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                resumeOnFocusGain = _state.value.isPlaying
                pausePlayers()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> applyMasterVolume(DUCK_FACTOR)
            AudioManager.AUDIOFOCUS_GAIN -> {
                val wasDucked = players.isNotEmpty() && !_state.value.isPlaying &&
                    _state.value.activeCount > 0
                if (resumeOnFocusGain) {
                    resumeOnFocusGain = false
                    play()
                } else if (wasDucked) {
                    applyMasterVolume(1f)
                }
            }
        }
    }

    // ---- public API -----------------------------------------------------------------------

    /** Re-applies persisted per-sound volumes from DataStore. */
    fun restoreVolumes(encoded: String) {
        val decoded = MixerState.decodeVolumes(encoded, _state.value.layers)
        _state.value = decoded
        decoded.layers.forEach { layer ->
            players[layer.sound]?.volume = layer.volume * decoded.masterVolume
        }
    }

    fun currentVolumesEncoding(): String = _state.value.encodeVolumes()

    fun setMasterVolume(volume: Float) {
        _state.update { it.copy(masterVolume = volume.coerceIn(0f, 1f)) }
        applyMasterVolume(1f)
    }

    /** Adds or removes a layer, honouring [AmbientSound.MAX_ACTIVE_LAYERS]. */
    fun toggleLayer(sound: AmbientSound) {
        val current = _state.value
        val isPlaying = current.layers.any { it.sound == sound && it.isPlaying }
        if (isPlaying) {
            stopLayer(sound)
        } else {
            if (current.activeCount >= AmbientSound.MAX_ACTIVE_LAYERS) return
            if (!requestFocus()) return
            _state.update { it.withToggled(sound.id, true).copy(isPlaying = true) }
            startPlayers(setOf(sound))
        }
    }

    fun setLayerVolume(sound: AmbientSound, volume: Float) {
        _state.update { it.withVolume(sound.id, volume) }
        players[sound]?.volume = volume.coerceIn(0f, 1f) * _state.value.masterVolume
    }

    /** Resumes everything the mixer currently has selected; selects nothing if empty. */
    fun play() {
        val wanted = _state.value.layers.filter { it.isPlaying }.map { it.sound }
        if (wanted.isEmpty()) {
            val fallback = _state.value.layers
                .filter { it.volume > 0f }
                .take(AmbientSound.MAX_ACTIVE_LAYERS)
                .map { it.sound }
            if (fallback.isEmpty()) return
            _state.update { current ->
                current.copy(
                    layers = current.layers.map { it.copy(isPlaying = fallback.contains(it.sound)) },
                    isPlaying = true,
                )
            }
            if (!requestFocus()) {
                _state.update { current ->
                    current.copy(
                        layers = current.layers.map { it.copy(isPlaying = false) },
                        isPlaying = false,
                    )
                }
                return
            }
            startPlayers(fallback.toSet())
            return
        }

        if (!requestFocus()) {
            _state.update { it.copy(layers = it.layers.map { layer -> layer.copy(isPlaying = false) }) }
            return
        }
        resumeOnFocusGain = false
        _state.update { it.copy(isPlaying = true) }
        startPlayers(wanted.toSet())
    }

    fun stopAll() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _state.update {
            it.copy(
                layers = it.layers.map { layer -> layer.copy(isPlaying = false) },
                isPlaying = false,
                sleepTimerEndElapsedMillis = null,
            )
        }
        releasePlayers()
        abandonFocus()
    }

    fun setBackgroundPlaybackEnabled(enabled: Boolean) {
        _state.update { it.copy(backgroundPlaybackEnabled = enabled) }
    }

    /**
     * Schedules an automatic stop using `elapsedRealtime`, so the deadline survives the
     * device going to sleep. Passing `null` or `0` clears the timer.
     */
    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        if (minutes == null || minutes <= 0) {
            _state.update { it.copy(sleepTimerEndElapsedMillis = null) }
            return
        }
        val endElapsed = timeProvider.elapsedRealtime() + minutes * 60_000L
        _state.update { it.copy(sleepTimerEndElapsedMillis = endElapsed) }
        sleepTimerJob = scope.launch {
            while (timeProvider.elapsedRealtime() < endElapsed) {
                delay(SLEEP_TIMER_TICK_MS)
            }
            stopAll()
        }
    }

    fun sleepTimerRemainingMillis(): Long {
        val end = _state.value.sleepTimerEndElapsedMillis ?: return 0L
        return (end - timeProvider.elapsedRealtime()).coerceAtLeast(0L)
    }

    fun isPlaying(): Boolean = _state.value.isPlaying

    /** Frees every player and stops the scope. Called when the process tears down. */
    fun release() {
        stopAll()
        unregisterNoisyReceiver()
        scope.cancel()
    }

    // ---- internals ---------------------------------------------------------------------

    private fun stopLayer(sound: AmbientSound) {
        players.remove(sound)?.release()
        _state.update { current ->
            val layers = current.layers.map {
                if (it.sound == sound) it.copy(isPlaying = false) else it
            }
            current.copy(layers = layers, isPlaying = layers.any { it.isPlaying })
        }
        if (_state.value.activeCount == 0) {
            abandonFocus()
            _state.update { it.copy(sleepTimerEndElapsedMillis = null) }
        }
    }

    private fun createPlayer(sound: AmbientSound): ExoPlayer? = runCatching {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(
                Media3AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ false,
            )
            setMediaItem(MediaItem.fromUri(resUri(sound)))
            // The generated files are seamless loops, so item repeat is exact.
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Log.w(TAG, "Playback failed for ${sound.id}", error)
                }
            })
            prepare()
        }
    }.getOrNull()

    private fun startPlayers(wanted: Set<AmbientSound>) {
        registerNoisyReceiver()
        players.keys.filterNot { it in wanted }.forEach { players.remove(it)?.release() }
        wanted.forEach { sound ->
            // NB: explicit get-then-put instead of getOrPut — ConcurrentHashMap forbids
            // null values, so a failed createPlayer() would throw inside getOrPut.
            var player = players[sound]
            if (player == null) {
                player = createPlayer(sound)
                if (player == null) {
                    Log.e(TAG, "Failed to create player for ${sound.id}, skipping")
                    _state.update { it.withToggled(sound.id, false) }
                    return@forEach
                }
                players[sound] = player
            }
            val layer = _state.value.layers.first { it.sound == sound }
            player.volume = layer.volume.coerceIn(0f, 1f) * _state.value.masterVolume
            player.playWhenReady = true
        }
    }

    private fun resUri(sound: AmbientSound): Uri {
        // Entry-name form, e.g. android.resource://app.calma/raw/rain.
        // (The numeric-id form must NOT carry a ":raw" suffix in the authority —
        // that made the URI unresolvable and silently broke all playback.)
        // context.packageName resolves to the real id, including the `.debug` suffix.
        return Uri.parse("android.resource://${context.packageName}/raw/${sound.id}")
    }

    private fun releasePlayers() {
        players.values.forEach { it.release() }
        players.clear()
    }

    private fun pausePlayers() {
        players.values.forEach { it.playWhenReady = false }
        _state.update { it.copy(isPlaying = false) }
    }

    private fun applyMasterVolume(factor: Float) {
        val master = _state.value.masterVolume * factor
        _state.value.layers.forEach { layer ->
            players[layer.sound]?.volume = (layer.volume * master).coerceIn(0f, 1f)
        }
    }

    private fun requestFocus(): Boolean {
        if (hasFocus) return true
        val manager = audioManager ?: return true
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(FOCUS_ATTRIBUTES)
                .setOnAudioFocusChangeListener(audioFocusListener)
                .setWillPauseWhenDucked(false)
                .build()
            focusRequest = request
            manager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN,
            )
        }
        hasFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasFocus
    }

    private fun abandonFocus() {
        if (!hasFocus) return
        hasFocus = false
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { manager.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            manager.abandonAudioFocus(audioFocusListener)
        }
    }

    // ---- becoming noisy ---------------------------------------------------------------

    private fun registerNoisyReceiver() {
        if (noisyReceiverRegistered) return
        ContextCompat.registerReceiver(
            context,
            noisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        noisyReceiverRegistered = true
    }

    private fun unregisterNoisyReceiver() {
        if (!noisyReceiverRegistered) return
        runCatching { context.unregisterReceiver(noisyReceiver) }
        noisyReceiverRegistered = false
    }

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Headphones unplugged: stop instead of suddenly playing out of the speaker.
            stopAll()
        }
    }

    companion object {
        private const val TAG = "CalmaSoundEngine"
        private const val DUCK_FACTOR = 0.25f
        private const val SLEEP_TIMER_TICK_MS = 1_000L

        val FOCUS_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
    }
}