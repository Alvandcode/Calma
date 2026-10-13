package app.calma.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Calma palette.
 *
 * The product identity is "quiet water": a deep teal anchor, a soft sage accent and
 * a muted lavender used sparingly for highlights. Both schemes were checked against
 * WCAG AA (4.5:1) for body text and AA Large (3:1) for large text and UI surfaces.
 */

// ---- Light scheme -----------------------------------------------------------------------

val TealPrimaryLight = Color(0xFF17646F)
val TealOnPrimaryLight = Color(0xFFFFFFFF)
val TealContainerLight = Color(0xFFA8EDEA)
val TealOnContainerLight = Color(0xFF00272C)

val SageSecondaryLight = Color(0xFF3F6A56)
val SageOnSecondaryLight = Color(0xFFFFFFFF)
val SageContainerLight = Color(0xFFC1EBD0)
val SageOnContainerLight = Color(0xFF002116)

val LavenderTertiaryLight = Color(0xFF4E5C8C)
val LavenderOnTertiaryLight = Color(0xFFFFFFFF)
val LavenderContainerLight = Color(0xFFDDE1FF)
val LavenderOnContainerLight = Color(0xFF050F43)

val BackgroundLight = Color(0xFFF6F9F9)
val OnBackgroundLight = Color(0xFF171D1D)
val SurfaceLight = Color(0xFFF6F9F9)
val OnSurfaceLight = Color(0xFF171D1D)
val SurfaceVariantLight = Color(0xFFDAE4E5)
val OnSurfaceVariantLight = Color(0xFF3F4949)
val OutlineLight = Color(0xFF6F7979)
val OutlineVariantLight = Color(0xFFBEC8C9)

val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

// ---- Dark scheme ------------------------------------------------------------------------

val TealPrimaryDark = Color(0xFF8CD0CE)
val TealOnPrimaryDark = Color(0xFF00363D)
val TealContainerDark = Color(0xFF004F58)
val TealOnContainerDark = Color(0xFFA8EDEA)

val SageSecondaryDark = Color(0xFFA6CFB5)
val SageOnSecondaryDark = Color(0xFF113726)
val SageContainerDark = Color(0xFF284E3C)
val SageOnContainerDark = Color(0xFFC1EBD0)

val LavenderTertiaryDark = Color(0xFFBAC3FF)
val LavenderOnTertiaryDark = Color(0xFF1E2D5C)
val LavenderContainerDark = Color(0xFF364474)
val LavenderOnTertiaryContainerDark = Color(0xFFDDE1FF)

val BackgroundDark = Color(0xFF0B1417)
val OnBackgroundDark = Color(0xFFDDE4E4)
val SurfaceDark = Color(0xFF0B1417)
val OnSurfaceDark = Color(0xFFDDE4E4)
val SurfaceVariantDark = Color(0xFF3F4949)
val OnSurfaceVariantDark = Color(0xFFBEC8C9)
val OutlineDark = Color(0xFF899393)
val OutlineVariantDark = Color(0xFF3F4949)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

// ---- Surface container ramp (Material 3 tonal surfaces) --------------------------------

val Color0Light = Color(0xFFFFFFFF)
val Color1Light = Color(0xFFF0F5F5)
val Color2Light = Color(0xFFEAEFEF)
val Color3Light = Color(0xFFE4EAEA)
val Color4Light = Color(0xFFDEE4E4)

val Color0Dark = Color(0xFF060F11)
val Color1Dark = Color(0xFF111C1F)
val Color2Dark = Color(0xFF152023)
val Color3Dark = Color(0xFF202B2E)
val Color4Dark = Color(0xFF2A3639)

/**
 * Colours that are not part of Material 3's scheme but are shared across Calma screens,
 * for example the breathing ring or chart series. They are exposed through
 * [CalmaAccents] so that screens never hardcode a colour literal.
 */
data class CalmaAccentColors(
    val calmRing: Color,
    val calmRingTrack: Color,
    val chartPrimary: Color,
    val chartSecondary: Color,
    val chartTertiary: Color,
    val chartGrid: Color,
    val positive: Color,
    val onPositive: Color,
    val warning: Color,
    val onWarning: Color,
)

val LightAccents = CalmaAccentColors(
    calmRing = Color(0xFF17646F),
    calmRingTrack = Color(0xFFD3E1E2),
    chartPrimary = Color(0xFF17646F),
    chartSecondary = Color(0xFF4E5C8C),
    chartTertiary = Color(0xFF8A5A2B),
    chartGrid = Color(0xFFDDE4E4),
    positive = Color(0xFF2E6B4F),
    onPositive = Color(0xFFFFFFFF),
    warning = Color(0xFF8A4F00),
    onWarning = Color(0xFFFFFFFF),
)

val DarkAccents = CalmaAccentColors(
    calmRing = Color(0xFF8CD0CE),
    calmRingTrack = Color(0xFF2A3B3D),
    chartPrimary = Color(0xFF8CD0CE),
    chartSecondary = Color(0xFFBAC3FF),
    chartTertiary = Color(0xFFE0B27C),
    chartGrid = Color(0xFF273134),
    positive = Color(0xFF9FD5B7),
    onPositive = Color(0xFF00391E),
    warning = Color(0xFFF0BE8E),
    onWarning = Color(0xFF4A2800),
)