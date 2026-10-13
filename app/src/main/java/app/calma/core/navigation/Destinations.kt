package app.calma.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.ui.graphics.vector.ImageVector
import app.calma.R

/**
 * Primary navigation destinations.
 *
 * Five tabs, matching the information architecture: Home, Focus, Breathe, Sounds,
 * Progress. Secondary screens (mood check-in, settings, permissions, guides) are pushed
 * onto the back stack instead of taking a tab slot, which keeps the bar uncluttered.
 */
enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    HOME("home", R.string.nav_home, Icons.Outlined.Home),
    FOCUS("focus", R.string.nav_focus, Icons.Outlined.Timer),
    BREATHE("breathe", R.string.nav_breathe, Icons.Outlined.SelfImprovement),
    SOUNDS("sounds", R.string.nav_sounds, Icons.Outlined.Waves),
    STATS("stats", R.string.nav_stats, Icons.Outlined.BarChart),
}

/** Routes that are not tabs. */
object Routes {
    const val MOOD = "mood"
    const val SETTINGS = "settings"
    const val PERMISSIONS = "permissions"
    const val ABOUT = "about"
    const val PRIVACY = "privacy"
    const val HELP = "help"
    const val CALM = "calm"

    /** Detail route with the exercise id, e.g. `calm/five_senses`. */
    fun calm(exerciseId: String): String = "$CALM/$exerciseId"

    const val CALM_ARG = "exerciseId"
    const val CALM_PATTERN = "$CALM/{$CALM_ARG}"
}