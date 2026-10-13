package app.calma.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.calma.core.common.FocusRunState
import app.calma.data.local.entity.ActiveSessionEntity
import app.calma.data.local.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusDao {

    // ---- completed sessions ----------------------------------------------------------

    @Insert
    suspend fun insertSession(session: FocusSessionEntity): Long

    @Query("SELECT * FROM focus_sessions WHERE id = :id")
    suspend fun sessionById(id: Long): FocusSessionEntity?

    @Query(
        """
        SELECT * FROM focus_sessions
        WHERE endedAtEpochMillis >= :fromEpochMillis AND endedAtEpochMillis < :toEpochMillis
        ORDER BY endedAtEpochMillis DESC
        """,
    )
    fun observeSessions(fromEpochMillis: Long, toEpochMillis: Long): Flow<List<FocusSessionEntity>>

    @Query(
        """
        SELECT * FROM focus_sessions
        WHERE endedAtEpochMillis >= :fromEpochMillis AND endedAtEpochMillis < :toEpochMillis
        ORDER BY endedAtEpochMillis DESC
        """,
    )
    suspend fun sessionsBetween(
        fromEpochMillis: Long,
        toEpochMillis: Long,
    ): List<FocusSessionEntity>

    @Query("SELECT COALESCE(SUM(focusedMillis), 0) FROM focus_sessions")
    suspend fun lifetimeFocusMillis(): Long

    @Query("SELECT COUNT(*) FROM focus_sessions")
    suspend fun totalSessionCount(): Int

    @Query("SELECT COALESCE(SUM(focusedMillis), 0) FROM focus_sessions")
    fun observeLifetimeFocusMillis(): Flow<Long>

    @Query("SELECT COUNT(*) FROM focus_sessions")
    fun observeSessionCount(): Flow<Int>

    @Query("SELECT endedAtEpochMillis FROM focus_sessions ORDER BY endedAtEpochMillis DESC")
    suspend fun allSessionEndTimes(): List<Long>

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAllSessions()

    // ---- active session ---------------------------------------------------------------

    @Query("SELECT * FROM active_session WHERE id = 1")
    suspend fun loadActive(): ActiveSessionEntity?

    @Query("SELECT * FROM active_session WHERE id = 1")
    fun observeActive(): Flow<ActiveSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActive(session: ActiveSessionEntity)

    @Query("DELETE FROM active_session")
    suspend fun deleteActive()

    /**
     * Atomically banks a finished focus block.
     *
     * The `runState != COMPLETED` guard is the duplicate-logging defence: if the timer
     * ticks again after the process was restarted mid-transition, the write is skipped and
     * no second row is produced.
     */
    @Transaction
    suspend fun completeFocusBlock(
        active: ActiveSessionEntity,
        session: FocusSessionEntity,
    ): Boolean {
        val current = loadActive() ?: return false
        if (FocusRunState.fromName(current.runState) == FocusRunState.COMPLETED) return false
        insertSession(session)
        upsertActive(active.copy(runState = FocusRunState.COMPLETED.name))
        return true
    }
}