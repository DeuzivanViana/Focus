package com.focus.feature.goals

import com.focus.core.designsystem.theme.toColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focus.core.designsystem.component.StatusPill
import com.focus.core.designsystem.component.SelectChip
import com.focus.core.designsystem.component.ColorDot
import com.focus.core.designsystem.component.FocusCard
import com.focus.core.designsystem.component.FocusTextField
import com.focus.core.designsystem.component.PrimaryButton
import com.focus.core.designsystem.component.ToolIconButton
import com.focus.core.designsystem.icon.FocusIcons
import com.focus.core.designsystem.theme.FocusColors
import com.focus.core.designsystem.util.formatDuration
import com.focus.core.model.Goal
import com.focus.core.model.GoalProgress
import com.focus.core.model.GoalStatus
import com.focus.core.model.Subject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun GoalsRoute(viewModel: GoalsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GoalsScreen(state = state, onSave = viewModel::save, onDelete = viewModel::delete)
}

@Composable
fun GoalsScreen(
    state: GoalsUiState,
    onSave: (Goal) -> Unit,
    onDelete: (Long) -> Unit,
    today: LocalDate = LocalDate.now(),
) {
    var editing by remember { mutableStateOf<Goal?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.goals_title), style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.goals_subtitle), style = MaterialTheme.typography.bodySmall)
            }
            ToolIconButton(FocusIcons.Plus, stringResource(R.string.goals_add), { creating = true })
        }
        Spacer(Modifier.height(20.dp))

        if (state.loaded && state.goals.isEmpty()) {
            FocusCard(Modifier.fillMaxWidth()) {
                Icon(FocusIcons.Target, contentDescription = null, tint = FocusColors.TextSecondary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.goals_empty), style = MaterialTheme.typography.bodyMedium, color = FocusColors.TextSecondary)
                Spacer(Modifier.height(14.dp))
                PrimaryButton(stringResource(R.string.goals_add), { creating = true })
            }
        }

        state.goals.forEach { progress ->
            GoalCard(progress, onClick = { editing = progress.goal })
            Spacer(Modifier.height(10.dp))
        }
    }

    if (creating) {
        GoalDialog(
            initial = null,
            subjects = state.subjects,
            today = today,
            onDismiss = { creating = false },
            onSave = { creating = false; onSave(it) },
            onDelete = null,
        )
    }
    editing?.let { goal ->
        GoalDialog(
            initial = goal,
            subjects = state.subjects,
            today = today,
            onDismiss = { editing = null },
            onSave = { editing = null; onSave(it) },
            onDelete = { editing = null; onDelete(goal.id) },
        )
    }
}

@Composable
private fun rememberDateFormatter(): DateTimeFormatter {
    val locale: Locale = LocalConfiguration.current.locales[0]
    return remember(locale) { DateTimeFormatter.ofPattern("d MMM yyyy", locale) }
}

@Composable
private fun GoalCard(p: GoalProgress, onClick: () -> Unit) {
    val dates = rememberDateFormatter()
    val color = p.subject?.let { it.color.toColor() } ?: FocusColors.Accent
    FocusCard(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(p.goal.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ColorDot(color, size = 8.dp)
                    Spacer(Modifier.width(8.dp))
                    val subject = p.subject?.name ?: stringResource(R.string.goals_all_subjects)
                    val time = when {
                        p.status == GoalStatus.EXPIRED -> stringResource(R.string.goals_ended_on, dates.format(p.goal.deadline))
                        p.status == GoalStatus.COMPLETED -> stringResource(R.string.goals_deadline_on, dates.format(p.goal.deadline))
                        else -> pluralStringResource(R.plurals.goals_days_left, p.daysRemaining, p.daysRemaining)
                    }
                    Text("$subject · $time", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.width(12.dp))
            StatusBadge(p.status)
        }
        Spacer(Modifier.height(14.dp))
        Meter(p.fraction, color)
        Spacer(Modifier.height(8.dp))
        Row {
            Text(
                stringResource(R.string.goals_done_of, formatDuration(p.doneSeconds), formatHours(p.targetSeconds)),
                style = MaterialTheme.typography.bodySmall,
                color = FocusColors.TextSecondary,
                modifier = Modifier.weight(1f),
            )
            Text("${(p.fraction * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = FocusColors.TextPrimary)
        }

        if (!p.isFinished) {
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = FocusColors.Outline)
            val forecast = when {
                p.status == GoalStatus.NOT_STARTED || p.projectedFinish == null -> stringResource(R.string.goals_forecast_none)
                p.projectedFinish!!.isAfter(p.goal.deadline) ->
                    stringResource(R.string.goals_forecast_late, formatHours(p.projectedSeconds), dates.format(p.projectedFinish))
                else -> stringResource(R.string.goals_forecast_on_time, dates.format(p.projectedFinish))
            }
            InfoRow(stringResource(R.string.goals_at_this_pace), forecast)
            Spacer(Modifier.height(6.dp))
            InfoRow(stringResource(R.string.goals_needed_per_day), formatDuration(p.neededSecondsPerDay))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = FocusColors.TextTertiary, modifier = Modifier.width(120.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, color = FocusColors.TextPrimary, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatusBadge(status: GoalStatus) {
    val (label, color) = when (status) {
        GoalStatus.AHEAD -> R.string.goals_status_ahead to FocusColors.Accent
        GoalStatus.ON_TRACK -> R.string.goals_status_on_track to FocusColors.TextPrimary
        GoalStatus.BEHIND -> R.string.goals_status_behind to FocusColors.Error
        GoalStatus.COMPLETED -> R.string.goals_status_completed to FocusColors.Accent
        GoalStatus.EXPIRED -> R.string.goals_status_expired to FocusColors.TextTertiary
        GoalStatus.NOT_STARTED -> R.string.goals_status_not_started to FocusColors.TextSecondary
    }
    StatusPill(stringResource(label), color)
}

@Composable
private fun Meter(target: Float, color: Color) {
    // Fills from empty on first show, then eases to new values.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val fraction by animateFloatAsState(if (shown) target else 0f, tween(700, easing = FastOutSlowInEasing), label = "goal")
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(FocusColors.SurfaceHigh),
    ) {
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(color),
            )
        }
    }
}

/** "100h", or "1h 30m" when the value is not a whole number of hours. */
private fun formatHours(seconds: Long): String =
    if (seconds % 3600 == 0L) "${seconds / 3600}h" else formatDuration(seconds)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun GoalDialog(
    initial: Goal?,
    subjects: List<Subject>,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Goal) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val dates = rememberDateFormatter()
    var title by rememberSaveable { mutableStateOf(initial?.title.orEmpty()) }
    var subjectId by rememberSaveable { mutableStateOf(initial?.subjectId) }
    var hours by rememberSaveable { mutableStateOf(initial?.let { (it.targetMinutes / 60).toString() } ?: "") }
    var deadlineEpochDay by rememberSaveable { mutableStateOf((initial?.deadline ?: today.plusDays(30)).toEpochDay()) }
    var pickingDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val startDate = initial?.startDate ?: today
    val hoursValue = hours.toIntOrNull()
    val valid = title.isNotBlank() && hoursValue != null && hoursValue in Goal.TARGET_HOURS_RANGE

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = FocusColors.SurfaceRaised,
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.goals_delete_title), style = MaterialTheme.typography.titleLarge) },
            text = { Text(stringResource(R.string.goals_delete_text), style = MaterialTheme.typography.bodyMedium, color = FocusColors.TextSecondary) },
            confirmButton = { TextButton(onClick = onDelete) { Text(stringResource(R.string.goals_delete), color = FocusColors.Error) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.goals_cancel), color = FocusColors.TextPrimary) } },
        )
        return
    }

    if (pickingDate) {
        val minDay = maxOf(startDate, today).toEpochDay()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.ofEpochDay(deadlineEpochDay).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() >= minDay
            },
        )
        val colors = DatePickerDefaults.colors(
            containerColor = FocusColors.SurfaceRaised,
            selectedDayContainerColor = FocusColors.Accent,
            selectedDayContentColor = FocusColors.OnAccent,
            todayDateBorderColor = FocusColors.Accent,
            todayContentColor = FocusColors.Accent,
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            colors = colors,
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        deadlineEpochDay = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                    }
                    pickingDate = false
                }) { Text(stringResource(R.string.goals_ok), color = FocusColors.Accent) }
            },
            dismissButton = {
                TextButton(onClick = { pickingDate = false }) { Text(stringResource(R.string.goals_cancel), color = FocusColors.TextPrimary) }
            },
        ) {
            DatePicker(state = pickerState, colors = colors, showModeToggle = false)
        }
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FocusColors.SurfaceRaised,
        shape = MaterialTheme.shapes.large,
        title = {
            Text(stringResource(if (initial == null) R.string.goals_add else R.string.goals_edit), style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.goals_field_title), style = MaterialTheme.typography.bodySmall)
                FocusTextField(
                    title,
                    { title = it },
                    Modifier.fillMaxWidth(),
                    placeholder = stringResource(R.string.goals_field_title_hint),
                    maxLength = Goal.MAX_TITLE_LENGTH,
                )
                Text(stringResource(R.string.goals_field_subject), style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip(stringResource(R.string.goals_all_subjects), FocusColors.TextTertiary, subjectId == null) { subjectId = null }
                    subjects.forEach { s ->
                        Chip(s.name, s.color.toColor(), subjectId == s.id) { subjectId = s.id }
                    }
                }
                Text(stringResource(R.string.goals_field_hours), style = MaterialTheme.typography.bodySmall)
                FocusTextField(hours, { hours = it }, Modifier.fillMaxWidth(), placeholder = "100", maxLength = 4, numeric = true)
                Text(stringResource(R.string.goals_field_deadline), style = MaterialTheme.typography.bodySmall)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(FocusColors.Background)
                        .clickable { pickingDate = true }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(dates.format(LocalDate.ofEpochDay(deadlineEpochDay)), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Icon(FocusIcons.Calendar, contentDescription = null, tint = FocusColors.TextSecondary, modifier = Modifier.size(18.dp))
                }
                if (onDelete != null) {
                    TextButton(onClick = { confirmDelete = true }, contentPadding = PaddingValues(0.dp)) {
                        Text(stringResource(R.string.goals_delete), color = FocusColors.Error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    val base = initial ?: Goal(title = "", targetMinutes = 60, startDate = startDate, deadline = startDate)
                    onSave(
                        base.copy(
                            title = title,
                            subjectId = subjectId,
                            targetMinutes = (hoursValue ?: 1) * 60,
                            deadline = LocalDate.ofEpochDay(deadlineEpochDay),
                        ),
                    )
                },
            ) {
                Text(stringResource(R.string.goals_save), color = if (valid) FocusColors.Accent else FocusColors.TextTertiary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.goals_cancel), color = FocusColors.TextPrimary) }
        },
    )
}

@Composable
private fun Chip(label: String, color: Color, selected: Boolean, onClick: () -> Unit) = SelectChip(label, color, selected, onClick)
