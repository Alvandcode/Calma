package app.calma.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A daily check-in. Personal notes never leave the device. */
@Entity(
    tableName = "mood_entries",
    indices = [Index(value = ["recordedAtEpochMillis"])],
)
data class MoodEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** 1..5 */
    val mood: Int,
    /** 1..5 */
    val stress: Int,
    /** 1..5 */
    val energy: Int,
    /** 1..5 */
    val concentration: Int,
    /** Free text, optional. Never logged. */
    val note: String?,
    val recordedAtEpochMillis: Long,
    /** Set when the user edits an entry, so history ordering stays stable. */
    val updatedAtEpochMillis: Long,
)

/** A finished breathing exercise or guided calm exercise. */
@Entity(
    tableName = "calm_sessions",
    indices = [Index(value = ["endedAtEpochMillis"])],
)
data class CalmSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** Breathing, five senses, grounding, ... */
    val exerciseId: String,
    val durationMillis: Long,
    val completedCycles: Int,
    val endedAtEpochMillis: Long,
)

/** One layer of a saved sound mix. */
@Entity(
    tableName = "mix_layers",
    indices = [Index(value = ["mixId"])],
)
data class MixLayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** Assigned by [app.calma.data.local.dao.SoundMixDao.saveMix] on insert. */
    val mixId: Long = 0L,
    /** Stable sound id, see app.calma.audio.AmbientSound */
    val soundId: String,
    /** 0f..1f */
    val volume: Float,
)

/** A named, reusable sound mix. */
@Entity(tableName = "sound_mixes")
data class SoundMixEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val createdAtEpochMillis: Long,
)