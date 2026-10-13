package app.calma.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.calma.core.ui.theme.LocalCalmaAccents
import kotlin.math.max

/** A single bar / data point in a Calma chart. */
data class ChartBar(val label: String, val value: Float, val valueLabel: String = "")

/**
 * Minimal bar chart.
 *
 * Calma draws its charts with Compose `Canvas` rather than adding a charting library:
 * the app only needs two simple shapes, and this keeps the APK small and the theme fully
 * under our control.
 *
 * Zero-valued days are drawn as a faint stub so a gap reads as "no session" instead of
 * disappearing.
 */
@Composable
fun CalmaBarChart(
    bars: List<ChartBar>,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    barColor: Color = LocalCalmaAccents.current.chartPrimary,
    emptyColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    description: String = "",
) {
    val maxValue = max(bars.maxOfOrNull { it.value } ?: 0f, 0.0001f)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .semantics { contentDescription = description },
        ) {
            val count = bars.size.coerceAtLeast(1)
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.55f).coerceAtMost(48.dp.toPx())
            val radius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2, barWidth / 2)

            bars.forEachIndexed { index, bar ->
                val centerX = slotWidth * index + slotWidth / 2f
                val left = centerX - barWidth / 2f
                if (bar.value <= 0f) {
                    drawRoundRect(
                        color = emptyColor,
                        topLeft = Offset(left, size.height - 4.dp.toPx()),
                        size = Size(barWidth, 4.dp.toPx()),
                        cornerRadius = radius,
                    )
                } else {
                    val barHeight = (size.height - 8.dp.toPx()) * (bar.value / maxValue)
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = radius,
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            bars.forEach { bar ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = CalmaSpacingXs),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = bar.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Trend line for the 1..5 mood and stress scales.
 *
 * Gaps are honoured: a day without a check-in simply breaks the line instead of being
 * drawn as a drop to zero, which would misrepresent the data.
 */
@Composable
fun CalmaTrendChart(
    values: List<Float?>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp,
    lineColor: Color = LocalCalmaAccents.current.chartSecondary,
    gridColor: Color = LocalCalmaAccents.current.chartGrid,
    description: String = "",
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = description },
    ) {
        val horizontalPadding = 8.dp.toPx()
        val verticalPadding = 12.dp.toPx()
        val usableWidth = size.width - horizontalPadding * 2
        val usableHeight = size.height - verticalPadding * 2

        // Guide lines at 1..5.
        for (level in 1..5) {
            val y = verticalPadding + usableHeight * (1f - (level - 1) / 4f)
            drawLine(
                color = gridColor,
                start = Offset(horizontalPadding, y),
                end = Offset(size.width - horizontalPadding, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        if (values.size < 2) return@Canvas

        val stepX = usableWidth / (values.size - 1)
        fun xAt(index: Int) = horizontalPadding + stepX * index
        fun yAt(value: Float) = verticalPadding + usableHeight * (1f - (value - 1f) / 4f)

        var path: Path? = null
        values.forEachIndexed { index, value ->
            if (value == null) {
                path = null
                return@forEachIndexed
            }
            val point = Offset(xAt(index), yAt(value))
            if (path == null) {
                path = Path().apply { moveTo(point.x, point.y) }
            } else {
                path?.lineTo(point.x, point.y)
            }
        }
        path?.let {
            drawPath(
                path = it,
                color = lineColor,
                style = Stroke(width = 2.5.dp.toPx()),
            )
        }

        values.forEachIndexed { index, value ->
            if (value == null) return@forEachIndexed
            drawCircle(
                color = lineColor,
                radius = 3.5.dp.toPx(),
                center = Offset(xAt(index), yAt(value)),
            )
        }
    }

    if (labels.isNotEmpty()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            labels.forEach { label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = CalmaSpacingXs),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private val CalmaSpacingXs = 4.dp