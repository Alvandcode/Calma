package app.calma.data.repository

import app.calma.audio.AmbientSound
import app.calma.data.local.dao.SoundMixDao
import app.calma.data.local.entity.MixLayerEntity
import app.calma.data.local.entity.SoundMixEntity
import app.calma.domain.model.MixLayer
import app.calma.domain.model.SoundMix
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Saved sound mixes. */
class SoundMixRepository(private val dao: SoundMixDao) {

    fun observeMixes(): Flow<List<SoundMix>> = dao.observeMixes().map { mixes ->
        mixes.map { mix ->
            SoundMix(
                id = mix.id,
                name = mix.name,
                createdAtEpochMillis = mix.createdAtEpochMillis,
                layers = emptyList(),
            )
        }
    }

    /** Loads a mix together with its layers, skipping unknown sound ids. */
    suspend fun loadMix(id: Long): SoundMix? {
        val mix = dao.getMix(id) ?: return null
        val layers = dao.layersOf(id).mapNotNull { layer ->
            val sound = AmbientSound.fromId(layer.soundId) ?: return@mapNotNull null
            MixLayer(soundId = sound.id, volume = layer.volume.coerceIn(0f, 1f))
        }
        return SoundMix(
            id = mix.id,
            name = mix.name,
            createdAtEpochMillis = mix.createdAtEpochMillis,
            layers = layers,
        )
    }

    suspend fun save(name: String, layers: List<MixLayer>, nowEpochMillis: Long): Long {
        val trimmed = name.trim().take(MAX_NAME_LENGTH)
        require(trimmed.isNotEmpty()) { "Mix name must not be blank" }
        val valid = layers.filter { AmbientSound.fromId(it.soundId) != null }
            .take(AmbientSound.MAX_ACTIVE_LAYERS)
            .map { MixLayerEntity(soundId = it.soundId, volume = it.volume.coerceIn(0f, 1f)) }
        return dao.saveMix(
            mix = SoundMixEntity(name = trimmed, createdAtEpochMillis = nowEpochMillis),
            layers = valid,
        )
    }

    suspend fun delete(id: Long) = dao.deleteMixWithLayers(id)

    suspend fun clearAll() = dao.clearAll()

    private companion object {
        const val MAX_NAME_LENGTH = 40
    }
}