package app.calma.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A completed focus session.
 *
 * Only finished, non-cancelled sessions are persisted; the running timer lives in
 * [ActiveSessionEntity] so that cancelling never leaves a row behind.
 */
@Entity(
    tableName = "focus_sessions",
    indices = [Index(value = ["endedAtEpochMillis"])],
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** Total focused time actually accumulated, excluding breaks and pauses. */
    val focusedMillis: Long,
    /** Planned focus duration, so stats can tell short sessions from cancelled ones. */
    val plannedFocusMillis: Long,
    val breakMillis: Long,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val completed: Boolean = true,
)

/**
 * The single in-progress focus session, or a row that records how the last one ended.
 *
 * Holding exactly one row (fixed id [ACTIVE_SESSION_ID]) is what prevents duplicate
 * logging after process death: completion flips `runState` to
 * [app.calma.core.common.FocusRunState.COMPLETED] in the same transaction that inserts the
 * [FocusSessionEntity], so re-delivering the tick afterwards is a no-op.
 */
@Entity(tableName = "active_session")
data class ActiveSessionEntity(
    @PrimaryKey val id: Int = ACTIVE_SESSION_ID,
    val runState: String,
    val phase: String,
    /** Duration of the focus block in ms. */
    val focusMillis: Long,
    val breakMillis: Long,
    /** Focus ms already banked before the current run segment. */
    val focusedAccumulatedMillis: Long,
    /**
     * `elapsedRealtime()` value at which the current run segment ends.
     * Persisted so the countdown survives process death; `-1` while paused.
     */
    val runDeadlineElapsedMillis: Long,
    /** `elapsedRealtime()` when the current run segment started. */
    val runStartedElapsedMillis: Long,
    /** Wall-clock anchor used to detect a device reboot (`elapsedRealtime` resets). */
    val anchorWallClockMillis: Long,
    val anchorElapsedMillis: Long,
    val startedAtEpochMillis: Long,
    val lastTickEpochMillis: Long,
) {
    companion object {
        const val ACTIVE_SESSION_ID = 1
    }
}