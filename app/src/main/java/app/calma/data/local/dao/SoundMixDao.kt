package app.calma.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import app.calma.data.local.entity.MixLayerEntity
import app.calma.data.local.entity.SoundMixEntity
import kotlinx.coroutines.flow.Flow

/** A saved mix together with its layers. */
data class SoundMixWithLayers(
    val mix: SoundMixEntity,
    val layers: List<MixLayerEntity>,
)

@Dao
interface SoundMixDao {

    @Insert
    suspend fun insertMix(mix: SoundMixEntity): Long

    @Insert
    suspend fun insertLayers(layers: List<MixLayerEntity>)

    @Query("SELECT * FROM sound_mixes ORDER BY createdAtEpochMillis DESC")
    fun observeMixes(): Flow<List<SoundMixEntity>>

    @Query("SELECT * FROM sound_mixes WHERE id = :mixId")
    suspend fun getMix(mixId: Long): SoundMixEntity?

    @Query("SELECT * FROM mix_layers WHERE mixId = :mixId ORDER BY id ASC")
    suspend fun layersOf(mixId: Long): List<MixLayerEntity>

    @Query("DELETE FROM sound_mixes WHERE id = :mixId")
    suspend fun deleteMix(mixId: Long)

    @Query("DELETE FROM mix_layers WHERE mixId = :mixId")
    suspend fun deleteLayers(mixId: Long)

    @Transaction
    suspend fun deleteMixWithLayers(mixId: Long) {
        deleteLayers(mixId)
        deleteMix(mixId)
    }

    @Transaction
    suspend fun saveMix(mix: SoundMixEntity, layers: List<MixLayerEntity>): Long {
        val mixId = insertMix(mix)
        insertLayers(layers.map { it.copy(mixId = mixId) })
        return mixId
    }

    @Query("DELETE FROM mix_layers")
    suspend fun deleteAllLayers()

    @Query("DELETE FROM sound_mixes")
    suspend fun deleteAllMixes()

    @Transaction
    suspend fun clearAll() {
        deleteAllLayers()
        deleteAllMixes()
    }
}