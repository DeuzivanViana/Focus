package com.focus.feature.focus

import com.focus.core.designsystem.theme.toColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.focus.core.data.repository.FocusSelection
import com.focus.core.designsystem.component.ColorDot
import com.focus.core.model.GamificationState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focus.core.designsystem.component.FocusCard
import com.focus.core.designsystem.component.GhostIconButton
import com.focus.core.designsystem.component.LinearMeter
import com.focus.core.designsystem.component.PrimaryButton
import com.focus.core.designsystem.component.TimerFace
import com.focus.core.model.FocusSound
import com.focus.core.designsystem.component.SectionLabel
import com.focus.core.designsystem.icon.FocusIcons
import com.focus.core.designsystem.theme.FocusColors
import com.focus.core.designsystem.util.formatClock
import com.focus.core.designsystem.util.formatDuration
import com.focus.core.model.TimerPhase
import com.focus.core.model.TimerStatus

@Composable
fun FocusRoute(
    onStartRequested: () -> Unit,
    onOpenAchievements: () -> Unit,
    viewModel: FocusViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FocusScreen(
        state = state,
        onToggle = {
            if (state.timer.status == TimerStatus.IDLE) onStartRequested()
            viewModel.onToggle()
        },
        onStop = viewModel::onStop,
        onSkip = viewModel::onSkip,
        onSelect = viewModel::onSelect,
        onOpenAchievements = onOpenAchievements,
        onSound = viewModel::onSound,
        onVolume = viewModel::onVolume,
        onPreview = viewModel::onPreview,
    )
}

@Composable
fun FocusScreen(
    state: FocusUiState,
    onToggle: () -> Unit,
    onStop: () -> Unit,
    onSkip: () -> Unit,
    onSelect: (FocusSelection) -> Unit = {},
    onOpenAchievements: () -> Unit = {},
    onSound: (FocusSound) -> Unit = {},
    onVolume: (Int) -> Unit = {},
    onPreview: (FocusSound?) -> Unit = {},
) {
    val timer = state.timer
    var picking by rememberSaveable { mutableStateOf(false) }
    var pickingSound by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LevelBar(state.gamification, onOpenAchievements)
        Spacer(Modifier.height(22.dp))
        PhaseSelector(current = timer.phase)
        Spacer(Modifier.height(30.dp))

        Box(
            Modifier
                .widthIn(max = 300.dp)
                .fillMaxWidth(0.82f)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            TimerFace(style = state.timerStyle, progress = timer.progress, modifier = Modifier.fillMaxSize())
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                // Paused: the clock dims a little, a quiet cue that time is not running.
                val clockAlpha by animateFloatAsState(if (timer.status == TimerStatus.PAUSED) 0.55f else 1f, tween(300), label = "clock")
                Text(
                    formatClock(timer.remainingMillis),
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.graphicsLayer { alpha = clockAlpha },
                )
                Spacer(Modifier.height(4.dp))
                val statusText = when (timer.status) {
                    TimerStatus.IDLE -> stringResource(R.string.focus_status_ready)
                    TimerStatus.RUNNING -> phaseName(timer.phase)
                    TimerStatus.PAUSED -> stringResource(R.string.focus_status_paused)
                }
                // Each label is centered in a full-width box, so labels of different lengths stay
                // centered while they cross-fade (Crossfade's own box aligns them to the start).
                Crossfade(statusText, animationSpec = tween(220), label = "status", modifier = Modifier.fillMaxWidth()) { text ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = FocusColors.TextSecondary)
                    }
                }
                Spacer(Modifier.height(14.dp))
                CycleDots(done = timer.completedFocusInCycle, total = timer.sessionsBeforeLongBreak)
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SubjectChip(state) { picking = true }
            SoundChip(state.sound) { pickingSound = true }
        }
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GhostIconButton(FocusIcons.Stop, stringResource(R.string.focus_cd_stop), onStop, enabled = timer.isActive)
            PrimaryButton(
                text = when (timer.status) {
                    TimerStatus.IDLE -> stringResource(if (timer.phase == TimerPhase.FOCUS) R.string.focus_start_focus else R.string.focus_start_break)
                    TimerStatus.RUNNING -> stringResource(R.string.focus_pause)
                    TimerStatus.PAUSED -> stringResource(R.string.focus_resume)
                },
                onClick = onToggle,
                modifier = Modifier.widthIn(min = 170.dp),
            )
            GhostIconButton(FocusIcons.Skip, stringResource(R.string.focus_cd_skip), onSkip)
        }

        Spacer(Modifier.height(32.dp))
        TodayCard(state)
    }

    if (picking) {
        SubjectPickerDialog(state, onSelect = onSelect, onDismiss = { picking = false })
    }
    if (pickingSound) {
        SoundPickerDialog(
            selected = state.sound,
            volume = state.soundVolume,
            onSelect = onSound,
            onVolume = onVolume,
            onPreview = onPreview,
            onDismiss = { pickingSound = false },
        )
    }
}

@Composable
private fun LevelBar(game: GamificationState, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(FocusIcons.Trophy, contentDescription = null, tint = FocusColors.Accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.focus_level, game.level), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.width(12.dp))
        LinearMeter(game.levelProgress, Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(R.string.focus_xp, (game.xp - game.levelStartXp).toInt(), (game.nextLevelXp - game.levelStartXp).toInt()),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun SubjectChip(state: FocusUiState, onClick: () -> Unit) {
    val subject = state.selectedSubject
    val task = state.selectedTask
    Row(
        Modifier
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(FocusColors.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorDot(subject?.let { it.color.toColor() } ?: FocusColors.TextTertiary)
        Spacer(Modifier.width(10.dp))
        Text(
            text = when {
                subject == null -> stringResource(R.string.focus_choose_subject)
                task == null -> subject.name
                else -> "${subject.name} · ${task.title}"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (subject == null) FocusColors.TextSecondary else FocusColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 190.dp),
        )
        Spacer(Modifier.width(8.dp))
        Icon(FocusIcons.ChevronDown, contentDescription = null, tint = FocusColors.TextSecondary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SubjectPickerDialog(state: FocusUiState, onSelect: (FocusSelection) -> Unit, onDismiss: () -> Unit) {
    val selection = state.selection
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FocusColors.SurfaceRaised,
        shape = MaterialTheme.shapes.large,
        title = { Text(stringResource(R.string.focus_pick_title), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PickRow(stringResource(R.string.focus_no_subject), null, selection.subjectId == null) { onSelect(FocusSelection()) }
                state.subjects.forEach { s ->
                    PickRow(s.name, s.color.toColor(), selection.subjectId == s.id) { onSelect(FocusSelection(s.id, null)) }
                }
                if (state.subjects.isEmpty()) {
                    Text(stringResource(R.string.focus_pick_empty), style = MaterialTheme.typography.bodySmall, color = FocusColors.TextTertiary)
                }
                val subject = state.selectedSubject
                val openTasks = state.tasks.filter { it.subjectId == subject?.id && !it.done }
                if (subject != null && openTasks.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.focus_pick_task), style = MaterialTheme.typography.bodySmall)
                    PickRow(stringResource(R.string.focus_no_task), null, selection.taskId == null) { onSelect(FocusSelection(subject.id, null)) }
                    openTasks.forEach { t ->
                        PickRow(t.title, null, selection.taskId == t.id) { onSelect(FocusSelection(subject.id, t.id)) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.focus_pick_done), color = FocusColors.Accent) }
        },
    )
}

@Composable
private fun PickRow(label: String, dot: Color?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) FocusColors.AccentDim else FocusColors.SurfaceHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) {
            ColorDot(dot)
            Spacer(Modifier.width(10.dp))
        }
        Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (selected) Icon(FocusIcons.Check, contentDescription = null, tint = FocusColors.Accent, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun PhaseSelector(current: TimerPhase) {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        TimerPhase.entries.forEach { phase ->
            val selected = phase == current
            val textColor by animateColorAsState(if (selected) FocusColors.TextPrimary else FocusColors.TextTertiary, tween(250), label = "phaseText")
            val markColor by animateColorAsState(if (selected) FocusColors.Accent else FocusColors.Background, tween(250), label = "phaseMark")
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    phaseName(phase),
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor,
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .size(width = 16.dp, height = 2.dp)
                        .clip(CircleShape)
                        .background(markColor),
                )
            }
        }
    }
}

@Composable
private fun CycleDots(done: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (i < done) FocusColors.Accent else FocusColors.OutlineStrong),
            )
        }
    }
}

@Composable
private fun TodayCard(state: FocusUiState) {
    val stats = state.stats
    FocusCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SectionLabel(stringResource(R.string.focus_today))
            Text(
                stringResource(R.string.focus_of_goal, formatDuration(stats.todaySeconds), formatDuration(stats.dailyGoalMinutes * 60L)),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.height(12.dp))
        LinearMeter(stats.todayGoalProgress)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth()) {
            MiniStat(stringResource(R.string.focus_streak), pluralStringResource(R.plurals.focus_days, stats.currentStreak, stats.currentStreak), Modifier.weight(1f))
            MiniStat(stringResource(R.string.focus_week), formatDuration(stats.weekSeconds), Modifier.weight(1f))
            MiniStat(stringResource(R.string.focus_sessions_today), (stats.last7Days.lastOrNull()?.sessions ?: 0).toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
internal fun phaseName(phase: TimerPhase) = stringResource(
    when (phase) {
        TimerPhase.FOCUS -> R.string.focus_phase_focus
        TimerPhase.SHORT_BREAK -> R.string.focus_phase_short
        TimerPhase.LONG_BREAK -> R.string.focus_phase_long
    },
)
