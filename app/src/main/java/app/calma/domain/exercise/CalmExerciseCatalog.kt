package app.calma.domain.exercise

/**
 * A short guided exercise from the "Calm now" flow.
 *
 * Content is written to be read a screen at a time, without therapeutic claims and
 * without any dependency on the network, an AI service or a login.
 */
data class CalmExercise(
    val id: String,
    val titleKey: String,
    val subtitleKey: String,
    /** One line per step; the UI shows them one at a time. */
    val steps: List<String>,
) {
    /** Exercises that drive an animation show a breathing ring on the first step. */
    val isBreathing: Boolean get() = id == "breathe_slow"
}

/** All quick-calm content, bundled with the app. */
class CalmExerciseCatalog {

    val exercises: List<CalmExercise> = listOf(
        CalmExercise(
            id = "breathe_slow",
            titleKey = "calm_breathe_title",
            subtitleKey = "calm_breathe_sub",
            steps = listOf(
                "calm_step_mindful_1",
                "calm_step_mindful_2",
                "calm_step_release_3",
                "calm_step_mindful_5",
            ),
        ),
        CalmExercise(
            id = "five_senses",
            titleKey = "calm_senses_title",
            subtitleKey = "calm_senses_sub",
            steps = listOf(
                "calm_step_senses_1",
                "calm_step_senses_2",
                "calm_step_senses_3",
                "calm_step_senses_4",
                "calm_step_senses_5",
                "calm_step_senses_6",
            ),
        ),
        CalmExercise(
            id = "look_around",
            titleKey = "calm_surroundings_title",
            subtitleKey = "calm_surroundings_sub",
            steps = listOf(
                "calm_step_surround_1",
                "calm_step_surround_2",
                "calm_step_surround_3",
                "calm_step_surround_4",
                "calm_step_surround_5",
            ),
        ),
        CalmExercise(
            id = "be_here_now",
            titleKey = "calm_mindful_title",
            subtitleKey = "calm_mindful_sub",
            steps = listOf(
                "calm_step_mindful_1",
                "calm_step_mindful_2",
                "calm_step_mindful_3",
                "calm_step_mindful_4",
                "calm_step_mindful_5",
            ),
        ),
        CalmExercise(
            id = "let_thoughts_pass",
            titleKey = "calm_release_title",
            subtitleKey = "calm_release_sub",
            steps = listOf(
                "calm_step_release_1",
                "calm_step_release_2",
                "calm_step_release_3",
                "calm_step_release_4",
                "calm_step_release_5",
            ),
        ),
    )

    fun byId(id: String): CalmExercise? = exercises.firstOrNull { it.id == id }
}