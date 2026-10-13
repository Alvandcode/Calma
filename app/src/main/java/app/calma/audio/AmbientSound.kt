package app.calma.audio

import androidx.annotation.RawRes
import app.calma.R

/**
 * Catalogue of ambient sounds shipped with the app.
 *
 * Every file is synthesized by `tools/generate_audio.py` and licensed together with the
 * source code; see `licenses/audio-licenses.md`.
 */
enum class AmbientSound(
    val id: String,
    @RawRes val resId: Int,
    val defaultVolume: Float,
) {
    RAIN("rain", R.raw.rain, 0.55f),
    FOREST("forest", R.raw.forest, 0.45f),
    BIRDS("birds", R.raw.birds, 0.40f),
    SEA("sea", R.raw.sea, 0.50f),
    STREAM("stream", R.raw.stream, 0.50f),
    WIND("wind", R.raw.wind, 0.45f),
    WHITE_NOISE("white_noise", R.raw.white_noise, 0.35f),
    PINK_NOISE("pink_noise", R.raw.pink_noise, 0.35f),
    BROWN_NOISE("brown_noise", R.raw.brown_noise, 0.40f);

    companion object {
        /** Simultaneously playable layers. Beyond this, mixers sound muddy, not calming. */
        const val MAX_ACTIVE_LAYERS = 4

        fun fromId(id: String): AmbientSound? = entries.firstOrNull { it.id == id }

        fun fromIdOrNull(id: String): AmbientSound? = fromId(id)
    }
}

/** One row of the mixer UI. */
data class SoundLayerState(
    val sound: AmbientSound,
    val isPlaying: Boolean = false,
    val volume: Float = sound.defaultVolume,
)

/** Full mixer state. */
data class MixerState(
    val layers: List<SoundLayerState> = AmbientSound.entries.map {
        SoundLayerState(it)
    },
    val isPlaying: Boolean = false,
    val masterVolume: Float = 0.8f,
    val sleepTimerEndElapsedMillis: Long? = null,
    val backgroundPlaybackEnabled: Boolean = true,
) {
    val activeCount: Int get() = layers.count { it.isPlaying }

    val isSleepTimerActive: Boolean get() = sleepTimerEndElapsedMillis != null

    fun withVolume(soundId: String, volume: Float): MixerState = copy(
        layers = layers.map {
            if (it.sound.id == soundId) it.copy(volume = volume.coerceIn(0f, 1f)) else it
        },
    )

    fun withToggled(soundId: String, isPlaying: Boolean): MixerState {
        val updated = layers.map {
            if (it.sound.id == soundId) it.copy(isPlaying = isPlaying) else it
        }
        return copy(
            layers = updated,
            isPlaying = if (isPlaying) true else updated.any { it.isPlaying },
        )
    }

    /** Compact `id:volume` encoding persisted in DataStore, e.g. `rain:0.6;wind:0.3`. */
    fun encodeVolumes(): String = layers
        .filter { it.volume > 0f }
        .joinToString(";") { "${it.sound.id}:${it.volume.round3()}" }

    companion object {
        fun decodeVolumes(encoded: String, defaults: List<SoundLayerState>): MixerState {
            if (encoded.isBlank()) return MixerState(layers = defaults)
            val parsed = encoded.split(";").mapNotNull { part ->
                val id = part.substringBefore(':')
                val volume = part.substringAfter(':', "").toFloatOrNull() ?: return@mapNotNull null
                id to volume
            }.toMap()
            return MixerState(
                layers = defaults.map { layer ->
                    parsed[layer.sound.id]?.let { layer.copy(volume = it) } ?: layer
                },
            )
        }
    }
}

private fun Float.round3(): String = String.format(java.util.Locale.US, "%.3f", this)