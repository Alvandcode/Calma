package app.calma.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Calma palette.
 *
 * The product identity is "ember dusk": a deep espresso anchor, a burnt-orange
 * accent and a soft gold used sparingly for highlights. The dark scheme is the
 * signature look; the light scheme is its warm sand counterpart. Both schemes were
 * checked against WCAG AA (4.5:1) for body text and AA Large (3:1) for large text
 * and UI surfaces.
 */

// ---- Light scheme -----------------------------------------------------------------------

val TealPrimaryLight = Color(0xFFB84A12)
val TealOnPrimaryLight = Color(0xFFFFFFFF)
val TealContainerLight = Color(0xFFF6D9BE)
val TealOnContainerLight = Color(0xFF4A2008)

val SageSecondaryLight = Color(0xFF7A5A38)
val SageOnSecondaryLight = Color(0xFFFFFFFF)
val SageContainerLight = Color(0xFFEDDCC2)
val SageOnContainerLight = Color(0xFF33240F)

val LavenderTertiaryLight = Color(0xFF8A6A1F)
val LavenderOnTertiaryLight = Color(0xFFFFFFFF)
val LavenderContainerLight = Color(0xFFF2E3B8)
val LavenderOnContainerLight = Color(0xFF3A2C05)

val BackgroundLight = Color(0xFFFAF3E8)
val OnBackgroundLight = Color(0xFF241309)
val SurfaceLight = Color(0xFFFAF3E8)
val OnSurfaceLight = Color(0xFF241309)
val SurfaceVariantLight = Color(0xFFE4D5BE)
val OnSurfaceVariantLight = Color(0xFF5A4632)
val OutlineLight = Color(0xFF8A7560)
val OutlineVariantLight = Color(0xFFD8C6AC)

val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

// ---- Dark scheme ------------------------------------------------------------------------

val TealPrimaryDark = Color(0xFFF4732A)
val TealOnPrimaryDark = Color(0xFF2A1204)
val TealContainerDark = Color(0xFF5A2A10)
val TealOnContainerDark = Color(0xFFFFD9C2)

val SageSecondaryDark = Color(0xFFD8A06A)
val SageOnSecondaryDark = Color(0xFF3A2410)
val SageContainerDark = Color(0xFF4A2F1A)
val SageOnContainerDark = Color(0xFFF5DCC0)

val LavenderTertiaryDark = Color(0xFFE8B04B)
val LavenderOnTertiaryDark = Color(0xFF3A2703)
val LavenderContainerDark = Color(0xFF4A3410)
val LavenderOnTertiaryContainerDark = Color(0xFFF7E3B8)

val BackgroundDark = Color(0xFF150D08)
val OnBackgroundDark = Color(0xFFF5E9DC)
val SurfaceDark = Color(0xFF150D08)
val OnSurfaceDark = Color(0xFFF5E9DC)
val SurfaceVariantDark = Color(0xFF3A2A1E)
val OnSurfaceVariantDark = Color(0xFFCBB59D)
val OutlineDark = Color(0xFF8A7560)
val OutlineVariantDark = Color(0xFF3A2A1E)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

// ---- Surface container ramp (Material 3 tonal surfaces) --------------------------------

val Color0Light = Color(0xFFFFFFFF)
val Color1Light = Color(0xFFF4EAD9)
val Color2Light = Color(0xFFEEE0C9)
val Color3Light = Color(0xFFE6D4B8)
val Color4Light = Color(0xFFDEC6A6)

val Color0Dark = Color(0xFF0E0805)
val Color1Dark = Color(0xFF1B120B)
val Color2Dark = Color(0xFF221712)
val Color3Dark = Color(0xFF2C1E14)
val Color4Dark = Color(0xFF38271A)

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
    calmRing = Color(0xFFB84A12),
    calmRingTrack = Color(0xFFE4D0B4),
    chartPrimary = Color(0xFFB84A12),
    chartSecondary = Color(0xFF8A6A1F),
    chartTertiary = Color(0xFF7A5A38),
    chartGrid = Color(0xFFE4D0B4),
    positive = Color(0xFF2E6B4F),
    onPositive = Color(0xFFFFFFFF),
    warning = Color(0xFF8A4F00),
    onWarning = Color(0xFFFFFFFF),
)

val DarkAccents = CalmaAccentColors(
    calmRing = Color(0xFFF4732A),
    calmRingTrack = Color(0xFF3A2417),
    chartPrimary = Color(0xFFF4732A),
    chartSecondary = Color(0xFFE8B04B),
    chartTertiary = Color(0xFFD8A06A),
    chartGrid = Color(0xFF2C1E14),
    positive = Color(0xFFA8D5A2),
    onPositive = Color(0xFF0E3A1A),
    warning = Color(0xFFF0BE8E),
    onWarning = Color(0xFF4A2800),
)
