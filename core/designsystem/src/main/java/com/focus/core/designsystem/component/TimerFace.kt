package com.focus.core.designsystem.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.focus.core.designsystem.theme.FocusColors
import com.focus.core.designsystem.theme.LocalFocusPalette
import com.focus.core.model.TimerStyle
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Colors one face needs, read once outside the Canvas (they come from composable getters). */
private data class FaceColors(val accent: Color, val accentDim: Color, val track: Color, val trackSoft: Color)

/**
 * Countdown face in one of the [TimerStyle]s, with [content] (digits, status) centered on top.
 * [progress] is the elapsed fraction 0..1; it is eased so the once-per-second ticks sweep smoothly.
 * Switching style cross-fades. [compact] draws thinner strokes for small previews.
 */
@Composable
fun TimerFace(
    style: TimerStyle,
    progress: Float,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(durationMillis = 700), label = "face")
    val colors = FaceColors(
        accent = FocusColors.Accent,
        accentDim = FocusColors.AccentDim,
        // On the light page the surface step is too faint for a track; use a mid grey there.
        track = if (LocalFocusPalette.current.isDark) FocusColors.SurfaceHigh else lerp(FocusColors.Outline, FocusColors.OutlineStrong, 0.5f),
        trackSoft = if (LocalFocusPalette.current.isDark) FocusColors.Outline else FocusColors.OutlineStrong,
    )
    val scale = if (compact) 0.5f else 1f
    Box(modifier, contentAlignment = Alignment.Center) {
        Crossfade(style, animationSpec = tween(250), label = "faceStyle") { s ->
            Canvas(Modifier.fillMaxSize()) {
                when (s) {
                    TimerStyle.RING -> drawRing(animated, colors, 3.dp.toPx() * scale.coerceAtLeast(0.7f))
                    TimerStyle.TICKS -> drawTicks(animated, colors, scale)
                    TimerStyle.GAUGE -> drawGauge(animated, colors, (if (compact) 5.dp else 10.dp).toPx())
                    TimerStyle.DIAL -> drawDial(animated, colors, scale)
                    TimerStyle.MINIMAL -> drawMinimal(animated, colors, if (compact) 3.dp.toPx() else 4.dp.toPx())
                }
            }
        }
        content()
    }
}

private fun DrawScope.arcBox(stroke: Float): Pair<Offset, Size> {
    val d = min(size.width, size.height) - stroke
    return Offset((size.width - d) / 2f, (size.height - d) / 2f) to Size(d, d)
}

private fun DrawScope.drawRing(p: Float, c: FaceColors, stroke: Float) {
    val (topLeft, box) = arcBox(stroke)
    drawArc(c.trackSoft, 0f, 360f, false, topLeft, box, style = Stroke(width = 1.dp.toPx()))
    if (p > 0f) drawArc(c.accent, -90f, 360f * p, false, topLeft, box, style = Stroke(stroke, cap = StrokeCap.Round))
}

/** 60 ticks; the one at the boundary fades in, so the fill still moves continuously. */
private fun DrawScope.drawTicks(p: Float, c: FaceColors, scale: Float) {
    val count = 60
    val r = min(size.width, size.height) / 2f
    val long = 14.dp.toPx() * scale
    val short = 8.dp.toPx() * scale
    val width = 2.dp.toPx() * scale.coerceAtLeast(0.6f)
    val filled = p * count
    for (i in 0 until count) {
        val angle = Math.toRadians(-90.0 + i * 360.0 / count)
        val len = if (i % 5 == 0) long else short
        val outer = r - width
        val inner = outer - len
        val fill = (filled - i).coerceIn(0f, 1f)
        val color = lerp(c.track, c.accent, fill)
        val cx = center.x
        val cy = center.y
        drawLine(
            color,
            Offset(cx + (cos(angle) * inner).toFloat(), cy + (sin(angle) * inner).toFloat()),
            Offset(cx + (cos(angle) * outer).toFloat(), cy + (sin(angle) * outer).toFloat()),
            strokeWidth = width,
            cap = StrokeCap.Round,
        )
    }
}

/** Thick 270 degree arc, open at the bottom, with a small knob at the tip. */
private fun DrawScope.drawGauge(p: Float, c: FaceColors, stroke: Float) {
    val (topLeft, box) = arcBox(stroke)
    val start = 135f
    val sweepAll = 270f
    drawArc(c.track, start, sweepAll, false, topLeft, box, style = Stroke(stroke, cap = StrokeCap.Round))
    if (p > 0f) {
        drawArc(c.accent, start, sweepAll * p, false, topLeft, box, style = Stroke(stroke, cap = StrokeCap.Round))
    }
    val angle = Math.toRadians((start + sweepAll * p).toDouble())
    val r = box.width / 2f
    val tip = Offset(center.x + (cos(angle) * r).toFloat(), center.y + (sin(angle) * r).toFloat())
    drawCircle(c.accent, radius = stroke * 0.9f, center = tip)
    drawCircle(Color.White.copy(alpha = 0.85f), radius = stroke * 0.35f, center = tip)
}

/** Kitchen-timer dial: 12 marks and a soft wedge for the elapsed time, edged by a hand. */
private fun DrawScope.drawDial(p: Float, c: FaceColors, scale: Float) {
    val r = min(size.width, size.height) / 2f
    drawCircle(c.track, radius = r, style = Stroke(1.dp.toPx()))
    val markLen = 6.dp.toPx() * scale
    for (i in 0 until 12) {
        val a = Math.toRadians(-90.0 + i * 30.0)
        val o = r - 6.dp.toPx() * scale
        drawLine(
            if (i == 0) c.accent else c.trackSoft,
            Offset(center.x + (cos(a) * (o - markLen)).toFloat(), center.y + (sin(a) * (o - markLen)).toFloat()),
            Offset(center.x + (cos(a) * o).toFloat(), center.y + (sin(a) * o).toFloat()),
            strokeWidth = 2.dp.toPx() * scale.coerceAtLeast(0.6f),
            cap = StrokeCap.Round,
        )
    }
    val inner = r - 16.dp.toPx() * scale
    val topLeft = Offset(center.x - inner, center.y - inner)
    val box = Size(inner * 2, inner * 2)
    if (p > 0f) drawArc(c.accentDim, -90f, 360f * p, true, topLeft, box)
    // The hand stays on the outer part of the dial so it never crosses the digits.
    val a = Math.toRadians(-90.0 + 360.0 * p)
    val handFrom = inner * 0.78f
    drawLine(
        c.accent,
        Offset(center.x + (cos(a) * handFrom).toFloat(), center.y + (sin(a) * handFrom).toFloat()),
        Offset(center.x + (cos(a) * inner).toFloat(), center.y + (sin(a) * inner).toFloat()),
        strokeWidth = 2.5.dp.toPx() * scale.coerceAtLeast(0.6f),
        cap = StrokeCap.Round,
    )
}

/** Only a short line under the digits. */
private fun DrawScope.drawMinimal(p: Float, c: FaceColors, stroke: Float) {
    val w = size.width * 0.5f
    val y = size.height * 0.84f
    val x0 = (size.width - w) / 2f
    drawLine(c.track, Offset(x0, y), Offset(x0 + w, y), strokeWidth = stroke, cap = StrokeCap.Round)
    if (p > 0f) drawLine(c.accent, Offset(x0, y), Offset(x0 + w * p, y), strokeWidth = stroke, cap = StrokeCap.Round)
}

/** Fixed sample progress used by the style picker previews. */
const val TIMER_PREVIEW_PROGRESS: Float = 0.62f
