package app.calma.data.repository

import app.calma.data.local.dao.CalmDao
import app.calma.data.local.dao.MoodDao
import app.calma.data.local.entity.CalmSessionEntity
import app.calma.data.local.entity.MoodEntryEntity
import app.calma.domain.model.CalmSession
import app.calma.domain.model.MoodEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Daily check-ins. Entirely local; notes are never written to Logcat. */
class MoodRepository(private val dao: MoodDao) {

    fun observeRecent(limit: Int = 60): Flow<List<MoodEntry>> =
        dao.observeRecent(limit).map { list -> list.map(::toModel) }

    fun observeAll(): Flow<List<MoodEntry>> =
        dao.observeAll().map { list -> list.map(::toModel) }

    fun observeCount(): Flow<Int> = dao.observeCount()

    suspend fun count(): Int = dao.count()

    suspend fun byId(id: Long): MoodEntry? = dao.byId(id)?.let(::toModel)

    suspend fun latestOnDay(dayStartEpochMillis: Long): MoodEntry? =
        dao.latestOnDay(
            dayStartEpochMillis = dayStartEpochMillis,
            dayEndEpochMillis = dayStartEpochMillis + StatisticsConstants.MILLIS_PER_DAY,
        )?.let(::toModel)

    suspend fun between(fromEpochMillis: Long, toEpochMillis: Long): List<MoodEntry> =
        dao.between(fromEpochMillis, toEpochMillis).map(::toModel)

    suspend fun save(entry: MoodEntry, nowEpochMillis: Long): Long {
        val sanitized = entry.sanitized().toEntity(nowEpochMillis)
        return if (sanitized.id == 0L) {
            dao.insert(sanitized)
        } else {
            dao.update(sanitized)
            sanitized.id
        }
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun deleteAll() = dao.deleteAll()

    private fun toModel(entity: MoodEntryEntity) = MoodEntry(
        id = entity.id,
        mood = entity.mood,
        stress = entity.stress,
        energy = entity.energy,
        concentration = entity.concentration,
        note = entity.note,
        recordedAtEpochMillis = entity.recordedAtEpochMillis,
        updatedAtEpochMillis = entity.updatedAtEpochMillis,
    )

    /** Domain -> storage, stamping the timestamps the DAO expects. */
    private fun MoodEntry.toEntity(nowEpochMillis: Long) = MoodEntryEntity(
        id = id,
        mood = mood,
        stress = stress,
        energy = energy,
        concentration = concentration,
        note = note,
        recordedAtEpochMillis = if (recordedAtEpochMillis > 0L) {
            recordedAtEpochMillis
        } else {
            nowEpochMillis
        },
        updatedAtEpochMillis = nowEpochMillis,
    )

    /** Clamps every scale into 1..5 and trims the note, so malformed data cannot enter the DB. */
    private fun MoodEntry.sanitized() = copy(
        mood = mood.coerceIn(1, 5),
        stress = stress.coerceIn(1, 5),
        energy = energy.coerceIn(1, 5),
        concentration = concentration.coerceIn(1, 5),
        note = note?.trim()?.take(MAX_NOTE_LENGTH)?.takeIf { it.isNotBlank() },
    )

    private companion object {
        const val MAX_NOTE_LENGTH = 500
    }
}

/** Finished calm exercises. */
class CalmRepository(private val dao: CalmDao) {

    suspend fun record(
        exerciseId: String,
        durationMillis: Long,
        completedCycles: Int,
        endedAtEpochMillis: Long,
    ): Long = dao.insert(
        CalmSessionEntity(
            exerciseId = exerciseId,
            durationMillis = durationMillis,
            completedCycles = completedCycles,
            endedAtEpochMillis = endedAtEpochMillis,
        ),
    )

    suspend fun between(fromEpochMillis: Long, toEpochMillis: Long): List<CalmSession> =
        dao.between(fromEpochMillis, toEpochMillis).map { entity ->
            CalmSession(
                id = entity.id,
                exerciseId = entity.exerciseId,
                durationMillis = entity.durationMillis,
                completedCycles = entity.completedCycles,
                endedAtEpochMillis = entity.endedAtEpochMillis,
            )
        }

    fun observeLifetimeMillis(): Flow<Long> = dao.observeLifetimeMillis()

    fun observeCount(): Flow<Int> = dao.observeCount()

    suspend fun lifetimeMillis(): Long = dao.lifetimeMillis()

    suspend fun count(): Int = dao.count()

    suspend fun recentExerciseIds(): List<String> = dao.recentExerciseIds()

    suspend fun allEndTimes(): List<Long> = dao.allEndTimes()

    suspend fun deleteAll() = dao.deleteAll()
}

/** Shared numeric constants so repositories agree on what "one day" means. */
object StatisticsConstants {
    const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
}