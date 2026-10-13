package app.calma.core.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The breathing ring.
 *
 * The circle radius is driven directly by [scale] rather than by an `Animatable`, so the
 * phase timing comes from a single monotonic clock and cannot drift when frames are dropped.
 * A very short tween smooths the edges between frames without shifting the deadline.
 */
@Composable
fun BreathingRing(
    scale: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 260.dp,
    strokeWidth: Dp = 18.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    progress: Float? = null,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null,
) {
    val animatedScale by animateFloatAsState(
        targetValue = scale.coerceIn(0.05f, 1f),
        animationSpec = tween(durationMillis = 120, easing = LinearEasing),
        label = "ringScale",
    )
    val animatedProgress by animateFloatAsState(
        targetValue = (progress ?: 1f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 120, easing = LinearEasing),
        label = "ringProgress",
    )

    Box(
        modifier = modifier
            .size(diameter)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = Stroke(width = strokeWidth.toPx())
            val radius = (size.minDimension / 2f) * animatedScale
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(color = trackColor, radius = radius, center = center, style = stroke)

            if (animatedProgress > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = stroke,
                )
            }
        }
    }
}

/**
 * Fires a haptic pulse once each time [trigger] changes to a new positive value.
 *
 * Used for breathing phase changes. It is a no-op when haptics are disabled, which the
 * caller checks, so the composable never touches the vibrator on its own.
 */
@Composable
fun rememberPhaseHaptic(trigger: Int, enabled: Boolean): () -> Unit {
    val haptic = LocalHapticFeedback.current
    var last by remember(trigger) { mutableFloatStateOf(0f) }
    return remember(haptic, trigger, enabled) {
        {
            if (enabled && trigger > 0 && last != trigger.toFloat()) {
                last = trigger.toFloat()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }
}

/** Keeps a running session of frame callbacks for a lightweight visual timer. */
@Composable
fun RememberTickerEffect(
    intervalMillis: Long = 100L,
    onTick: () -> Unit,
) {
    var counter by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(intervalMillis) {
        while (true) {
            kotlinx.coroutines.delay(intervalMillis)
            counter += 1f
            onTick()
        }
    }
}