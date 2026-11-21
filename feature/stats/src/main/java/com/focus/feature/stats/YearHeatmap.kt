package com.focus.feature.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.focus.core.designsystem.component.FocusCard
import com.focus.core.designsystem.component.SectionLabel
import com.focus.core.designsystem.theme.FocusColors
import com.focus.core.model.DayStat
import java.time.DayOfWeek
import java.time.format.TextStyle

private val CELL = 12.dp
private val GAP = 3.dp

/** Intensity 0..4 relative to the daily goal: none, under 1/4, under 1/2, under the goal, goal met. */
internal fun heatLevel(seconds: Long, goalSeconds: Long): Int = when {
    seconds <= 0 -> 0
    goalSeconds <= 0 -> 4
    seconds < goalSeconds / 4 -> 1
    seconds < goalSeconds / 2 -> 2
    seconds < goalSeconds -> 3
    else -> 4
}

/**
 * One square per day for the last year, one column per week (Monday on top). Scrolls
 * horizontally and opens on the most recent weeks. Planned rest days without focus get a dot.
 */
@Composable
internal fun YearHeatmap(days: List<DayStat>, goalMinutes: Int, restDays: Set<DayOfWeek>, modifier: Modifier = Modifier) {
    if (days.isEmpty()) return
    val locale = LocalConfiguration.current.locales[0]
    val goal = goalMinutes * 60L
    val activeDays = days.count { it.focusedSeconds > 0 }
    val weeks = days.chunked(7)
    val small = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp, fontSize = 11.sp)

    val empty = FocusColors.SurfaceHigh
    val accent = FocusColors.Accent
    val dot = FocusColors.TextTertiary
    val ramp = remember(empty, accent) { listOf(empty) + listOf(0.30f, 0.55f, 0.80f, 1f).map { lerp(empty, accent, it) } }

    // Columns fade in from the newest week backwards.
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }

    FocusCard(modifier.fillMaxWidth()) {
        SectionLabel(stringResource(R.string.stats_heatmap_title))
        Spacer(Modifier.height(4.dp))
        Text(pluralStringResource(R.plurals.stats_heatmap_active, activeDays, activeDays), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(14.dp))

        Row {
            // Weekday initials for Mon, Wed, Fri, like most calendars.
            Column(Modifier.padding(top = 16.dp)) {
                DayOfWeek.entries.forEach { d ->
                    Box(Modifier.size(width = 16.dp, height = CELL + GAP), contentAlignment = Alignment.CenterStart) {
                        if (d == DayOfWeek.MONDAY || d == DayOfWeek.WEDNESDAY || d == DayOfWeek.FRIDAY) {
                            Text(d.getDisplayName(TextStyle.NARROW_STANDALONE, locale), style = small, color = FocusColors.TextTertiary)
                        }
                    }
                }
            }
            val scroll = rememberScrollState()
            LaunchedEffect(scroll.maxValue) { scroll.scrollTo(scroll.maxValue) }
            Column(Modifier.horizontalScroll(scroll)) {
                // Month labels above the first week that starts in each month.
                Row(Modifier.height(16.dp)) {
                    weeks.forEachIndexed { i, week ->
                        val first = week.first().date
                        val label = if (i == 0 || first.month != weeks[i - 1].first().date.month) {
                            first.month.getDisplayName(TextStyle.SHORT_STANDALONE, locale).trimEnd('.')
                        } else {
                            null
                        }
                        Box(Modifier.width(CELL + GAP)) {
                            if (label != null && i < weeks.size - 1) {
                                Text(label, style = small, color = FocusColors.TextTertiary, softWrap = false, maxLines = 1, modifier = Modifier.wrapContentWidth(Alignment.Start, unbounded = true))
                            }
                        }
                    }
                }
                Canvas(Modifier.size(width = (CELL + GAP) * weeks.size, height = (CELL + GAP) * 7)) {
                    val cell = CELL.toPx()
                    val step = cell + GAP.toPx()
                    val radius = CornerRadius(3.dp.toPx())
                    weeks.forEachIndexed { w, week ->
                        val fromEnd = (weeks.size - 1 - w).toFloat() / weeks.size
                        val alpha = ((reveal.value - fromEnd) * 4f).coerceIn(0f, 1f)
                        week.forEachIndexed { r, day ->
                            val level = heatLevel(day.focusedSeconds, goal)
                            val topLeft = Offset(w * step, r * step)
                            drawRoundRect(ramp[level].copy(alpha = if (level == 0) 1f else alpha), topLeft, Size(cell, cell), radius)
                            if (level == 0 && day.date.dayOfWeek in restDays) {
                                drawCircle(dot, radius = 1.5.dp.toPx(), center = Offset(topLeft.x + cell / 2, topLeft.y + cell / 2))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (restDays.isNotEmpty()) {
                Canvas(Modifier.size(CELL)) {
                    drawRoundRect(empty, cornerRadius = CornerRadius(3.dp.toPx()))
                    drawCircle(dot, radius = 1.5.dp.toPx())
                }
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.stats_heatmap_rest), style = small, color = FocusColors.TextTertiary)
            }
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.stats_heatmap_less), style = small, color = FocusColors.TextTertiary)
            Spacer(Modifier.width(6.dp))
            ramp.forEach { c -> LegendCell(c); Spacer(Modifier.width(3.dp)) }
            Spacer(Modifier.width(3.dp))
            Text(stringResource(R.string.stats_heatmap_more), style = small, color = FocusColors.TextTertiary)
        }
    }
}

@Composable
private fun LegendCell(color: Color) {
    Canvas(Modifier.size(CELL)) { drawRoundRect(color, cornerRadius = CornerRadius(3.dp.toPx())) }
}
