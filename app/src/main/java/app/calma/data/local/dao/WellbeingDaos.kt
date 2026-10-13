package app.calma.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.calma.data.local.entity.CalmSessionEntity
import app.calma.data.local.entity.MoodEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodDao {

    @Insert
    suspend fun insert(entry: MoodEntryEntity): Long

    @Update
    suspend fun update(entry: MoodEntryEntity)

    @Query("DELETE FROM mood_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM mood_entries WHERE id = :id")
    suspend fun byId(id: Long): MoodEntryEntity?

    @Query("SELECT * FROM mood_entries ORDER BY recordedAtEpochMillis DESC")
    fun observeAll(): Flow<List<MoodEntryEntity>>

    @Query("SELECT * FROM mood_entries ORDER BY recordedAtEpochMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<MoodEntryEntity>>

    /** Latest entry recorded on the given day, if any. */
    @Query(
        """
        SELECT * FROM mood_entries
        WHERE recordedAtEpochMillis >= :dayStartEpochMillis
          AND recordedAtEpochMillis < :dayEndEpochMillis
        ORDER BY recordedAtEpochMillis DESC LIMIT 1
        """,
    )
    suspend fun latestOnDay(
        dayStartEpochMillis: Long,
        dayEndEpochMillis: Long,
    ): MoodEntryEntity?

    @Query(
        """
        SELECT * FROM mood_entries
        WHERE recordedAtEpochMillis >= :fromEpochMillis
          AND recordedAtEpochMillis < :toEpochMillis
        ORDER BY recordedAtEpochMillis ASC
        """,
    )
    suspend fun between(
        fromEpochMillis: Long,
        toEpochMillis: Long,
    ): List<MoodEntryEntity>

    @Query(
        """
        SELECT * FROM mood_entries
        WHERE recordedAtEpochMillis >= :fromEpochMillis
          AND recordedAtEpochMillis < :toEpochMillis
        ORDER BY recordedAtEpochMillis ASC
        """,
    )
    fun observeBetween(
        fromEpochMillis: Long,
        toEpochMillis: Long,
    ): Flow<List<MoodEntryEntity>>

    @Query("SELECT COUNT(*) FROM mood_entries")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM mood_entries")
    suspend fun count(): Int

    @Query("DELETE FROM mood_entries")
    suspend fun deleteAll()
}

@Dao
interface CalmDao {

    @Insert
    suspend fun insert(session: CalmSessionEntity): Long

    @Query(
        """
        SELECT * FROM calm_sessions
        WHERE endedAtEpochMillis >= :fromEpochMillis AND endedAtEpochMillis < :toEpochMillis
        ORDER BY endedAtEpochMillis DESC
        """,
    )
    suspend fun between(
        fromEpochMillis: Long,
        toEpochMillis: Long,
    ): List<CalmSessionEntity>

    @Query("SELECT COALESCE(SUM(durationMillis), 0) FROM calm_sessions")
    fun observeLifetimeMillis(): Flow<Long>

    @Query("SELECT COALESCE(SUM(durationMillis), 0) FROM calm_sessions")
    suspend fun lifetimeMillis(): Long

    @Query("SELECT COUNT(*) FROM calm_sessions")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM calm_sessions")
    suspend fun count(): Int

    @Query("SELECT exerciseId FROM calm_sessions ORDER BY endedAtEpochMillis DESC LIMIT 200")
    suspend fun recentExerciseIds(): List<String>

    @Query("SELECT endedAtEpochMillis FROM calm_sessions ORDER BY endedAtEpochMillis DESC")
    suspend fun allEndTimes(): List<Long>

    @Query("DELETE FROM calm_sessions")
    suspend fun deleteAll()
}