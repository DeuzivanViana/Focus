package com.focus.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.focus.core.designsystem.theme.FocusColors
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

enum class ChartType { BARS, LINE }

/** A series of values. The first is the main one (accent); the others are comparisons. */
data class ChartSeries(val values: List<Float>, val color: Color, val isComparison: Boolean = false)

/**
 * Cartesian chart (X/Y axes with labels and grid) drawn as bars or lines.
 * Rendered on a Canvas, no external libraries.
 *
 * @param xLabels label for each point on the X axis
 * @param labelEvery show one label every N points (the last one is always shown)
 * @param yFormatter text for the Y axis ticks
 * @param highlightIndex highlighted point (e.g. today or the peak hour)
 */
@Composable
fun CartesianChart(
    series: List<ChartSeries>,
    xLabels: List<String>,
    modifier: Modifier = Modifier,
    type: ChartType = ChartType.BARS,
    height: Dp = 180.dp,
    goal: Float? = null,
    labelEvery: Int = 1,
    highlightIndex: Int? = null,
    yFormatter: (Float) -> String = { formatTick(it) },
) {
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = FocusColors.TextTertiary)
    val highlightStyle = labelStyle.copy(color = FocusColors.TextPrimary)
    val accent = FocusColors.Accent
    val gridColor = FocusColors.Outline
    val axisColor = FocusColors.OutlineStrong
    val goalColor = FocusColors.TextSecondary
    val holeColor = FocusColors.Background
    // Subtle reveal: values rise from the baseline when the data or the chart type changes.
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(series, type) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(durationMillis = 550, easing = FastOutSlowInEasing))
    }
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val grow = reveal.value
        val count = xLabels.size
        if (count == 0 || series.isEmpty()) return@Canvas

        val rawMax = maxOf(series.maxOf { s -> s.values.maxOrNull() ?: 0f }, goal ?: 0f)
        val ticks = niceTicks(rawMax)
        val yMax = ticks.last()

        // Plot area: room on the left for Y labels and at the bottom for X labels.
        val yLabelWidth = ticks.maxOf { measurer.measure(yFormatter(it), labelStyle).size.width }.toFloat()
        val left = yLabelWidth + 8.dp.toPx()
        val bottomPad = 20.dp.toPx()
        val top = 6.dp.toPx()
        val plotW = size.width - left
        val plotH = size.height - bottomPad - top
        fun yOf(v: Float) = top + plotH - (v / yMax) * plotH

        // Grid + Y axis labels
        ticks.forEach { t ->
            val y = yOf(t)
            drawLine(gridColor, Offset(left, y), Offset(size.width, y), strokeWidth = 1f)
            val layout = measurer.measure(yFormatter(t), labelStyle)
            drawText(layout, topLeft = Offset(left - 8.dp.toPx() - layout.size.width, y - layout.size.height / 2f))
        }
        // X axis
        drawLine(axisColor, Offset(left, top + plotH), Offset(size.width, top + plotH), strokeWidth = 1.dp.toPx())

        val slot = plotW / count
        fun xCenter(i: Int) = left + slot * i + slot / 2f

        when (type) {
            ChartType.BARS -> drawBars(series, slot, left, { yOf(it * grow) }, top + plotH, accent, highlightIndex)
            ChartType.LINE -> drawLines(series, ::xCenter, { yOf(it * grow) }, top + plotH, highlightIndex, holeColor)
        }

        if (goal != null && goal > 0f) {
            val y = yOf(goal)
            drawLine(
                color = goalColor,
                start = Offset(left, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
            )
        }

        // X axis labels
        xLabels.forEachIndexed { i, label ->
            if (i % labelEvery != 0 && i != count - 1) return@forEachIndexed
            if (i != count - 1 && count - 1 - i < labelEvery / 2) return@forEachIndexed // avoid overlapping the last label
            val layout = measurer.measure(label, if (i == highlightIndex) highlightStyle else labelStyle)
            val x = (xCenter(i) - layout.size.width / 2f).coerceIn(left, size.width - layout.size.width)
            drawText(layout, topLeft = Offset(x, top + plotH + 5.dp.toPx()))
        }
    }
}

private fun DrawScope.drawBars(
    series: List<ChartSeries>,
    slot: Float,
    left: Float,
    yOf: (Float) -> Float,
    baseline: Float,
    accent: Color,
    highlightIndex: Int?,
) {
    val n = series.size
    val group = (slot * 0.64f).coerceAtMost(30.dp.toPx() * n)
    val gap = if (n > 1) max(1f, slot * 0.06f) else 0f
    val barW = (group - gap * (n - 1)) / n
    val radius = CornerRadius(minOf(3.dp.toPx(), barW / 2f))
    // Order: comparison on the left, current period on the right.
    val ordered = series.sortedByDescending { it.isComparison }
    for (i in 0 until (series.first().values.size)) {
        val gx = left + slot * i + (slot - group) / 2f
        ordered.forEachIndexed { k, s ->
            val v = s.values.getOrNull(i) ?: return@forEachIndexed
            if (v <= 0f) return@forEachIndexed
            val y = yOf(v)
            if (baseline - y < 0.5f) return@forEachIndexed
            val color = when {
                s.isComparison -> s.color.copy(alpha = 0.55f)
                i == highlightIndex -> accent
                else -> s.color.copy(alpha = 0.6f)
            }
            drawRoundRect(color, Offset(gx + k * (barW + gap), y), Size(barW, baseline - y), radius)
        }
    }
}

private fun DrawScope.drawLines(
    series: List<ChartSeries>,
    xOf: (Int) -> Float,
    yOf: (Float) -> Float,
    baseline: Float,
    highlightIndex: Int?,
    holeColor: Color,
) {
    // Comparison first (drawn underneath).
    series.sortedByDescending { it.isComparison }.forEach { s ->
        if (s.values.isEmpty()) return@forEach
        val line = Path()
        s.values.forEachIndexed { i, v ->
            val x = xOf(i)
            val y = yOf(v)
            if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
        }
        if (!s.isComparison) {
            val area = Path().apply {
                addPath(line)
                lineTo(xOf(s.values.lastIndex), baseline)
                lineTo(xOf(0), baseline)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(s.color.copy(alpha = 0.22f), Color.Transparent), startY = 0f, endY = baseline))
        }
        drawPath(
            line,
            color = if (s.isComparison) s.color.copy(alpha = 0.8f) else s.color,
            style = Stroke(
                width = if (s.isComparison) 1.5.dp.toPx() else 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = if (s.isComparison) PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())) else null,
            ),
        )
        if (!s.isComparison && highlightIndex != null && highlightIndex in s.values.indices) {
            val c = Offset(xOf(highlightIndex), yOf(s.values[highlightIndex]))
            drawCircle(holeColor, radius = 5.dp.toPx(), center = c)
            drawCircle(s.color, radius = 3.5.dp.toPx(), center = c)
        }
    }
}

/** "Round" ticks for the Y axis (0, 0.5, 1, 1.5...), always 3 or 4 divisions. */
internal fun niceTicks(maxValue: Float): List<Float> {
    val target = if (maxValue <= 0f) 1f else maxValue
    val rough = target / 4f
    val mag = 10.0.pow(floor(log10(rough.toDouble()))).toFloat()
    val norm = rough / mag
    val step = mag * when {
        norm <= 1f -> 1f
        norm <= 2f -> 2f
        norm <= 2.5f -> 2.5f
        norm <= 5f -> 5f
        else -> 10f
    }
    val top = ceil(target / step) * step
    val list = ArrayList<Float>()
    var v = 0f
    while (v <= top + step / 2) {
        list += v
        v += step
    }
    return list
}

fun formatTick(v: Float): String =
    if (v == floor(v)) v.toInt().toString() else "%.1f".format(v)

/** Thin progress ring used by the timer. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 3.dp,
) {
    val accent = FocusColors.Accent
    val track = FocusColors.Outline
    // Smooths the once-per-second updates into a continuous sweep.
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(durationMillis = 700), label = "ring")
    Canvas(modifier) {
        val stroke = strokeWidth.toPx()
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(
            color = track,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = 1.dp.toPx()),
        )
        val sweep = 360f * animated
        if (sweep > 0f) {
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}
