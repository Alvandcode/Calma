package app.calma.domain.model

/** A completed focus session. */
data class FocusSession(
    val id: Long,
    val focusedMillis: Long,
    val plannedFocusMillis: Long,
    val breakMillis: Long,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
)

/** What the focus screen is showing right now. */
data class FocusTimerState(
    val runState: RunState = RunState.IDLE,
    val phase: Phase = Phase.FOCUS,
    val totalMillis: Long = 0L,
    val remainingMillis: Long = 0L,
    val elapsedMillis: Long = 0L,
    /** Focus time banked before the current break, shown during a break. */
    val focusedMillis: Long = 0L,
) {
    enum class RunState { IDLE, RUNNING, PAUSED, FINISHED }

    enum class Phase { FOCUS, BREAK }

    val progress: Float
        get() = if (totalMillis <= 0L) 0f
        else (elapsedMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)

    val isActive: Boolean get() = runState == RunState.RUNNING || runState == RunState.PAUSED

    companion object {
        val Idle = FocusTimerState()
    }
}

/** User-selectable focus presets, in minutes. */
object FocusPresets {
    val minutes = listOf(15, 25, 45, 60)

    /** Snaps an arbitrary value onto the nearest preset so the picker always has a selection. */
    fun nearest(minutes: Int): Int =
        minutes.coerceIn(1, 180).let { value ->
            minutes_list().minByOrNull { kotlin.math.abs(it - value) } ?: 25
        }

    private fun minutes_list() = minutes
}

/** User-selectable break lengths, in minutes. `0` means "no break". */
object BreakPresets {
    val minutes = listOf(0, 5, 10, 15)
}

/** A daily check-in. */
data class MoodEntry(
    val id: Long = 0L,
    val mood: Int = 3,
    val stress: Int = 3,
    val energy: Int = 3,
    val concentration: Int = 3,
    val note: String? = null,
    val recordedAtEpochMillis: Long = 0L,
    val updatedAtEpochMillis: Long = 0L,
) {
    fun isValid(): Boolean =
        mood in 1..5 && stress in 1..5 && energy in 1..5 && concentration in 1..5
}

/** A finished calm exercise. */
data class CalmSession(
    val id: Long,
    val exerciseId: String,
    val durationMillis: Long,
    val completedCycles: Int,
    val endedAtEpochMillis: Long,
)

/** A saved sound mix. */
data class SoundMix(
    val id: Long,
    val name: String,
    val createdAtEpochMillis: Long,
    val layers: List<MixLayer>,
)

data class MixLayer(
    val soundId: String,
    val volume: Float,
)