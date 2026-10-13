package app.calma.data.repository

import app.calma.core.common.FocusPhase
import app.calma.core.common.FocusRunState
import app.calma.data.local.dao.FocusDao
import app.calma.data.local.entity.ActiveSessionEntity
import app.calma.data.local.entity.FocusSessionEntity
import app.calma.domain.focus.FocusTimerSnapshot
import app.calma.domain.model.FocusSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persistence for focus sessions and the running timer.
 *
 * The running timer is a single row ([ActiveSessionEntity.ACTIVE_SESSION_ID]) so that
 * restoring after process death is a point read, and so completion can be made
 * idempotent with a conditional write.
 */
class FocusRepository(private val dao: FocusDao) {

    fun observeSessions(fromEpochMillis: Long, toEpochMillis: Long): Flow<List<FocusSession>> =
        dao.observeSessions(fromEpochMillis, toEpochMillis).map { list -> list.map(::toModel) }

    fun observeLifetimeFocusMillis(): Flow<Long> = dao.observeLifetimeFocusMillis()

    fun observeSessionCount(): Flow<Int> = dao.observeSessionCount()

    fun observeActive(): Flow<FocusTimerSnapshot?> =
        dao.observeActive().map { it?.toSnapshot() }

    suspend fun loadActive(): FocusTimerSnapshot? = dao.loadActive()?.toSnapshot()

    suspend fun saveActive(snapshot: FocusTimerSnapshot, nowElapsedMillis: Long, nowWallClockMillis: Long) {
        dao.upsertActive(snapshot.toEntity(nowElapsedMillis, nowWallClockMillis))
    }

    /**
     * Banks a finished focus block exactly once.
     *
     * @return true if a session row was written, false if the session had already been
     *         recorded (a duplicate delivery after process death).
     */
    suspend fun recordCompletedBlock(
        snapshot: FocusTimerSnapshot,
        focusedMillis: Long,
        startedAtEpochMillis: Long,
        endedAtEpochMillis: Long,
        nowElapsedMillis: Long,
        nowWallClockMillis: Long,
    ): Boolean {
        val active = snapshot.copy(runState = FocusRunState.RUNNING)
        val session = FocusSessionEntity(
            focusedMillis = focusedMillis,
            plannedFocusMillis = snapshot.focusMillis,
            breakMillis = snapshot.breakMillis,
            startedAtEpochMillis = startedAtEpochMillis,
            endedAtEpochMillis = endedAtEpochMillis,
            completed = true,
        )
        return dao.completeFocusBlock(
            active = active.toEntity(nowElapsedMillis, nowWallClockMillis),
            session = session,
        )
    }

    /** Records an early-finished block that never reached its planned duration. */
    suspend fun recordPartialBlock(
        snapshot: FocusTimerSnapshot,
        focusedMillis: Long,
        startedAtEpochMillis: Long,
        endedAtEpochMillis: Long,
        nowElapsedMillis: Long,
        nowWallClockMillis: Long,
    ): Boolean = recordCompletedBlock(
        snapshot, focusedMillis, startedAtEpochMillis, endedAtEpochMillis,
        nowElapsedMillis, nowWallClockMillis,
    )

    suspend fun sessionsBetween(fromEpochMillis: Long, toEpochMillis: Long): List<FocusSession> =
        dao.sessionsBetween(fromEpochMillis, toEpochMillis).map(::toModel)

    suspend fun lifetimeFocusMillis(): Long = dao.lifetimeFocusMillis()

    suspend fun totalSessionCount(): Int = dao.totalSessionCount()

    suspend fun allSessionEndTimes(): List<Long> = dao.allSessionEndTimes()

    suspend fun deleteActive() = dao.deleteActive()

    suspend fun deleteAllSessions() = dao.deleteAllSessions()

    private fun toModel(entity: FocusSessionEntity) = FocusSession(
        id = entity.id,
        focusedMillis = entity.focusedMillis,
        plannedFocusMillis = entity.plannedFocusMillis,
        breakMillis = entity.breakMillis,
        startedAtEpochMillis = entity.startedAtEpochMillis,
        endedAtEpochMillis = entity.endedAtEpochMillis,
    )

    private fun FocusTimerSnapshot.toEntity(
        nowElapsedMillis: Long,
        nowWallClockMillis: Long,
    ) = ActiveSessionEntity(
        id = ActiveSessionEntity.ACTIVE_SESSION_ID,
        runState = runState.name,
        phase = phase.name,
        focusMillis = focusMillis,
        breakMillis = breakMillis,
        focusedAccumulatedMillis = focusedAccumulatedMillis,
        runDeadlineElapsedMillis = segmentStartedElapsedMillis + segmentDurationMillis,
        runStartedElapsedMillis = segmentStartedElapsedMillis,
        anchorWallClockMillis = if (anchorWallClockMillis == 0L) {
            nowWallClockMillis
        } else {
            anchorWallClockMillis
        },
        anchorElapsedMillis = if (anchorElapsedMillis == 0L) {
            nowElapsedMillis
        } else {
            anchorElapsedMillis
        },
        startedAtEpochMillis = startedAtEpochMillis,
        lastTickEpochMillis = nowWallClockMillis,
    )

    private fun ActiveSessionEntity.toSnapshot() = FocusTimerSnapshot(
        runState = FocusRunState.fromName(runState),
        phase = FocusPhase.fromName(phase),
        focusMillis = focusMillis,
        breakMillis = breakMillis,
        focusedAccumulatedMillis = focusedAccumulatedMillis,
        segmentElapsedMillis = 0L,
        segmentStartedElapsedMillis = runStartedElapsedMillis,
        startedAtEpochMillis = startedAtEpochMillis,
        anchorWallClockMillis = anchorWallClockMillis,
        anchorElapsedMillis = anchorElapsedMillis,
    )
}