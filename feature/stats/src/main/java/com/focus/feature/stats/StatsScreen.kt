package com.focus.feature.stats

import com.focus.core.designsystem.theme.toColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focus.core.designsystem.component.CartesianChart
import com.focus.core.designsystem.component.ChartSeries
import com.focus.core.designsystem.component.ChartType
import com.focus.core.designsystem.component.FocusCard
import com.focus.core.designsystem.component.SectionLabel
import com.focus.core.designsystem.component.SegmentedToggle
import com.focus.core.designsystem.component.StatTile
import com.focus.core.designsystem.component.ToolIconButton
import com.focus.core.designsystem.component.formatTick
import com.focus.core.designsystem.icon.FocusIcons
import com.focus.core.designsystem.theme.FocusColors
import com.focus.core.designsystem.util.formatDuration
import com.focus.core.model.FocusSession
import com.focus.core.model.FocusStats
import com.focus.core.model.GamificationState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun StatsRoute(onOpenAchievements: () -> Unit, onOpenInsights: () -> Unit, viewModel: StatsViewModel = hiltViewModel()) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val game by viewModel.gamification.collectAsStateWithLifecycle()
    StatsScreen(stats ?: FocusStats(), game = game, onOpenAchievements = onOpenAchievements, onOpenInsights = onOpenInsights)
}

/** Active comparison: none, or the current period (7/30) against the previous one of the same length. */
enum class CompareMode { NONE, WEEK, MONTH }

@Composable
fun StatsScreen(
    stats: FocusStats,
    initialChartType: ChartType = ChartType.BARS,
    initialCompare: CompareMode = CompareMode.NONE,
    game: GamificationState = GamificationState(),
    onOpenAchievements: () -> Unit = {},
    onOpenInsights: () -> Unit = {},
) {
    var range by rememberSaveable { mutableIntStateOf(if (initialCompare == CompareMode.MONTH) 1 else 0) }
    var chartType by rememberSaveable { mutableStateOf(initialChartType) }
    var compare by rememberSaveable { mutableStateOf(initialCompare != CompareMode.NONE) }
    var showCompareDialog by rememberSaveable { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.stats_subtitle), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(20.dp))

        LinkCard(FocusIcons.Trophy, stringResource(R.string.stats_level_card, game.level, game.unlockedCount, game.achievements.size), onOpenAchievements)
        Spacer(Modifier.height(12.dp))
        LinkCard(FocusIcons.Insight, stringResource(R.string.stats_reflection_card), onOpenInsights)
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                stringResource(R.string.stats_today), formatDuration(stats.todaySeconds), Modifier.weight(1f),
                caption = stringResource(R.string.stats_goal_percent, (stats.todayGoalProgress * 100).roundToInt()), highlight = true,
            )
            StatTile(stringResource(R.string.stats_this_week), formatDuration(stats.weekSeconds), Modifier.weight(1f), caption = stringResource(R.string.stats_week_caption))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                stringResource(R.string.stats_streak), stringResource(R.string.stats_days_short, stats.currentStreak), Modifier.weight(1f),
                caption = stringResource(R.string.stats_best_streak, stats.bestStreak),
            )
            StatTile(stringResource(R.string.stats_total), formatDuration(stats.totalSeconds), Modifier.weight(1f), caption = stringResource(R.string.stats_pomodoros, stats.completedSessions))
        }

        Spacer(Modifier.height(24.dp))
        FocusCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(stringResource(R.string.stats_focus_hours), Modifier.weight(1f))
                ToolIconButton(FocusIcons.Chart, stringResource(R.string.stats_cd_bars), { chartType = ChartType.BARS }, selected = chartType == ChartType.BARS)
                Spacer(Modifier.width(6.dp))
                ToolIconButton(FocusIcons.LineChart, stringResource(R.string.stats_cd_line), { chartType = ChartType.LINE }, selected = chartType == ChartType.LINE)
                Spacer(Modifier.width(12.dp))
                ToolIconButton(FocusIcons.Compare, stringResource(R.string.stats_cd_compare), { showCompareDialog = true }, selected = compare)
            }
            Spacer(Modifier.height(14.dp))
            SegmentedToggle(listOf(stringResource(R.string.stats_7d), stringResource(R.string.stats_30d)), range, { range = it })
            Spacer(Modifier.height(18.dp))

            val days = if (range == 0) stats.last7Days else stats.last30Days
            val previous = if (range == 0) stats.previous7Days else stats.previous30Days
            val accent = FocusColors.Accent
            val series = buildList {
                add(ChartSeries(days.map { it.focusedSeconds / 3600f }, accent))
                if (compare) add(ChartSeries(previous.map { it.focusedSeconds / 3600f }, FocusColors.Comparison, isComparison = true))
            }
            CartesianChart(
                series = series,
                xLabels = days.map {
                    if (range == 0) it.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).trimEnd('.').take(3)
                    else it.date.dayOfMonth.toString()
                },
                type = chartType,
                goal = stats.dailyGoalMinutes / 60f,
                labelEvery = if (range == 0) 1 else 5,
                highlightIndex = days.lastIndex,
                yFormatter = { "${formatTick(it)}h" },
            )
            Spacer(Modifier.height(12.dp))
            if (compare) {
                ComparisonSummary(
                    current = days.sumOf { it.focusedSeconds },
                    previous = previous.sumOf { it.focusedSeconds },
                    accent = accent,
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                stringResource(R.string.stats_goal_line, formatDuration(stats.dailyGoalMinutes * 60L)),
                style = MaterialTheme.typography.bodySmall,
                color = FocusColors.TextTertiary,
            )
        }

        Spacer(Modifier.height(12.dp))
        YearHeatmap(stats.yearDays, stats.dailyGoalMinutes, stats.restDays)

        val subjectRows = stats.subjectTimes.filter { it.last30DaysSeconds > 0 }
        if (subjectRows.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            FocusCard(Modifier.fillMaxWidth()) {
                SectionLabel(stringResource(R.string.stats_by_subject))
                Spacer(Modifier.height(14.dp))
                val max = subjectRows.maxOf { it.last30DaysSeconds }.toFloat()
                subjectRows.forEachIndexed { i, t ->
                    val color = t.subject?.let { it.color.toColor() } ?: FocusColors.TextTertiary
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LegendDot(color)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            t.subject?.name ?: stringResource(R.string.stats_no_subject),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                        Text(formatDuration(t.last30DaysSeconds), style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(MaterialTheme.shapes.extraSmall).background(FocusColors.SurfaceHigh)) {
                        Box(Modifier.fillMaxWidth(t.last30DaysSeconds / max).height(4.dp).clip(MaterialTheme.shapes.extraSmall).background(color))
                    }
                    if (i != subjectRows.lastIndex) Spacer(Modifier.height(14.dp))
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(stringResource(R.string.stats_avg30), formatDuration(stats.averageDailySeconds30), Modifier.weight(1f))
            StatTile(stringResource(R.string.stats_goal_hit), "${stats.goalDaysLast7}/7", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                stringResource(R.string.stats_completion), "${(stats.completionRate * 100).roundToInt()}%", Modifier.weight(1f),
                caption = stringResource(R.string.stats_completion_caption),
            )
            StatTile(
                stringResource(R.string.stats_best_day),
                stats.bestDay?.let { formatDuration(it.focusedSeconds) } ?: "—",
                Modifier.weight(1f),
                caption = stats.bestDay?.date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)),
            )
        }

        Spacer(Modifier.height(24.dp))
        FocusCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SectionLabel(stringResource(R.string.stats_by_hour), Modifier.weight(1f))
                Text(
                    stats.peakHour?.let { stringResource(R.string.stats_peak, "%02d:00".format(it)) } ?: stringResource(R.string.stats_no_data),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(18.dp))
            CartesianChart(
                series = listOf(ChartSeries(stats.hourDistribution.map { it / 60f }, FocusColors.Accent)),
                xLabels = (0..23).map { "%02dh".format(it) },
                type = ChartType.LINE,
                height = 150.dp,
                labelEvery = 6,
                highlightIndex = stats.peakHour,
                yFormatter = { "${formatTick(it)}m" },
            )
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.stats_by_hour_caption), style = MaterialTheme.typography.bodySmall, color = FocusColors.TextTertiary)
        }

        Spacer(Modifier.height(24.dp))
        SectionLabel(stringResource(R.string.stats_recent))
        Spacer(Modifier.height(8.dp))
        if (stats.recentSessions.isEmpty()) {
            Text(stringResource(R.string.stats_empty), style = MaterialTheme.typography.bodyMedium, color = FocusColors.TextSecondary)
        } else {
            FocusCard(Modifier.fillMaxWidth()) {
                stats.recentSessions.forEachIndexed { i, s ->
                    SessionRow(s, locale)
                    if (i != stats.recentSessions.lastIndex) {
                        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = FocusColors.Outline)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (showCompareDialog) {
        CompareDialog(
            current = when {
                !compare -> CompareMode.NONE
                range == 0 -> CompareMode.WEEK
                else -> CompareMode.MONTH
            },
            onDismiss = { showCompareDialog = false },
            onSelect = { mode ->
                showCompareDialog = false
                compare = mode != CompareMode.NONE
                if (mode == CompareMode.WEEK) range = 0
                if (mode == CompareMode.MONTH) range = 1
            },
        )
    }
}

@Composable
private fun LinkCard(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(FocusColors.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon in a tinted circle: gives the row a clear anchor without a frame.
        Box(Modifier.size(36.dp).clip(androidx.compose.foundation.shape.CircleShape).background(FocusColors.AccentDim), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = FocusColors.Accent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Icon(FocusIcons.ChevronRight, contentDescription = null, tint = FocusColors.TextSecondary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ComparisonSummary(current: Long, previous: Long, accent: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        LegendDot(accent)
        Text(" ${stringResource(R.string.stats_current)} ${formatDuration(current)}", style = MaterialTheme.typography.bodySmall, color = FocusColors.TextPrimary)
        Spacer(Modifier.width(14.dp))
        LegendDot(FocusColors.Comparison)
        Text(" ${stringResource(R.string.stats_previous)} ${formatDuration(previous)}", style = MaterialTheme.typography.bodySmall)
    }
    Spacer(Modifier.height(4.dp))
    val text = when {
        previous == 0L && current == 0L -> stringResource(R.string.stats_change_flat)
        previous == 0L -> stringResource(R.string.stats_change_new)
        else -> {
            val pct = ((current - previous) * 100.0 / previous).roundToInt()
            when {
                pct > 0 -> stringResource(R.string.stats_change_up, pct)
                pct < 0 -> stringResource(R.string.stats_change_down, abs(pct))
                else -> stringResource(R.string.stats_change_flat)
            }
        }
    }
    Text(text, style = MaterialTheme.typography.titleSmall, color = if (current >= previous) accent else FocusColors.TextSecondary)
}

@Composable
private fun LegendDot(color: Color) {
    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
}

@Composable
private fun CompareDialog(current: CompareMode, onDismiss: () -> Unit, onSelect: (CompareMode) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FocusColors.SurfaceRaised,
        shape = MaterialTheme.shapes.large,
        title = { Text(stringResource(R.string.stats_compare_title), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    CompareMode.NONE to R.string.stats_compare_none,
                    CompareMode.WEEK to R.string.stats_compare_7,
                    CompareMode.MONTH to R.string.stats_compare_30,
                ).forEach { (mode, label) ->
                    val selected = mode == current
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(if (selected) FocusColors.AccentDim else FocusColors.SurfaceHigh)
                            .clickable { onSelect(mode) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(label), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        if (selected) Icon(FocusIcons.Check, null, tint = FocusColors.Accent, modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.stats_cancel), color = FocusColors.TextPrimary) }
        },
    )
}

@Composable
private fun SessionRow(s: FocusSession, locale: Locale) {
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(locale)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(Instant.ofEpochMilli(s.startedAt).atZone(ZoneId.systemDefault()).format(formatter), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(if (s.completed) R.string.stats_complete else R.string.stats_interrupted), style = MaterialTheme.typography.bodySmall)
        }
        Text(
            formatDuration(s.focusedSeconds),
            style = MaterialTheme.typography.titleSmall,
            color = if (s.completed) FocusColors.Accent else FocusColors.TextSecondary,
        )
    }
}
