package app.calma.core.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import app.calma.core.common.ThemeMode

private val LightColors = lightColorScheme(
    primary = TealPrimaryLight,
    onPrimary = TealOnPrimaryLight,
    primaryContainer = TealContainerLight,
    onPrimaryContainer = TealOnContainerLight,
    secondary = SageSecondaryLight,
    onSecondary = SageOnSecondaryLight,
    secondaryContainer = SageContainerLight,
    onSecondaryContainer = SageOnContainerLight,
    tertiary = LavenderTertiaryLight,
    onTertiary = LavenderOnTertiaryLight,
    tertiaryContainer = LavenderContainerLight,
    onTertiaryContainer = LavenderOnContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerLowest = Color0Light,
    surfaceContainerLow = Color1Light,
    surfaceContainer = Color2Light,
    surfaceContainerHigh = Color3Light,
    surfaceContainerHighest = Color4Light,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
)

private val DarkColors = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = TealOnPrimaryDark,
    primaryContainer = TealContainerDark,
    onPrimaryContainer = TealOnContainerDark,
    secondary = SageSecondaryDark,
    onSecondary = SageOnSecondaryDark,
    secondaryContainer = SageContainerDark,
    onSecondaryContainer = SageOnContainerDark,
    tertiary = LavenderTertiaryDark,
    onTertiary = LavenderOnTertiaryDark,
    tertiaryContainer = LavenderContainerDark,
    onTertiaryContainer = LavenderOnTertiaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerLowest = Color0Dark,
    surfaceContainerLow = Color1Dark,
    surfaceContainer = Color2Dark,
    surfaceContainerHigh = Color3Dark,
    surfaceContainerHighest = Color4Dark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
)

val LocalCalmaAccents = staticCompositionLocalOf { LightAccents }

/**
 * Spacing scale. Calma uses a 4dp base grid; screens reference these tokens instead of
 * literals so density stays consistent across the app.
 */
object CalmaSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Root theme.
 *
 * @param themeMode user preference from DataStore.
 * @param languageTag BCP-47 tag chosen in Settings, or null to follow the system.
 *        Supplying a tag also pins [LocalLayoutDirection] so Persian layouts are RTL even
 *        when the device locale is English.
 */
@Composable
fun CalmaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    languageTag: String? = null,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (dark) DarkColors else LightColors
    val accents = if (dark) DarkAccents else LightAccents

    val systemDirection = LocalLayoutDirection.current
    val layoutDirection = when (languageTag?.lowercase()) {
        "fa", "fa-ir", "fa-af", "prs" -> LayoutDirection.Rtl
        null -> systemDirection
        else -> LayoutDirection.Ltr
    }

    val view = LocalView.current
    val context = LocalContext.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = context.findActivity()?.window ?: return@SideEffect
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
        }
    }

    CompositionLocalProvider(
        LocalCalmaAccents provides accents,
        LocalLayoutDirection provides layoutDirection,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CalmaTypography,
            shapes = CalmaShapes,
            content = content,
        )
    }
}