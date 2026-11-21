package com.focus.feature.routine

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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focus.core.designsystem.component.ColorDot
import com.focus.core.designsystem.component.FocusCard
import com.focus.core.designsystem.component.FocusTextField
import com.focus.core.designsystem.component.PrimaryButton
import com.focus.core.designsystem.component.SectionLabel
import com.focus.core.designsystem.component.SelectChip
import com.focus.core.designsystem.component.ToolIconButton
import com.focus.core.designsystem.icon.FocusIcons
import com.focus.core.designsystem.theme.FocusColors
import com.focus.core.designsystem.theme.toColor
import com.focus.core.designsystem.util.rememberNotificationPermissionRequest
import com.focus.core.model.RoutineBlock
import com.focus.core.model.Subject
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.format.TextStyle

@Composable
fun RoutineRoute(viewModel: RoutineViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val askNotifications = rememberNotificationPermissionRequest()
    RoutineScreen(
        state = state,
        onSave = { block ->
            viewModel.save(block)
            if (block.remindBefore != null) askNotifications()
        },
        onDelete = viewModel::delete,
    )
}

@Composable
fun RoutineScreen(
    state: RoutineUiState,
    onSave: (RoutineBlock) -> Unit,
    onDelete: (Long) -> Unit,
    now: LocalDateTime = LocalDateTime.now(),
) {
    var editing by remember { mutableStateOf<RoutineBlock?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val subjects = state.subjects.associateBy { it.id }
    val today = now.dayOfWeek
    val nowMinute = now.hour * 60 + now.minute

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.routine_title), style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.routine_subtitle), style = MaterialTheme.typography.bodySmall)
            }
            ToolIconButton(FocusIcons.Plus, stringResource(R.string.routine_add), { creating = true })
        }
        Spacer(Modifier.height(20.dp))

        if (state.loaded && state.blocks.isEmpty()) {
            FocusCard(Modifier.fillMaxWidth()) {
                Icon(FocusIcons.Clock, contentDescription = null, tint = FocusColors.TextSecondary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.routine_empty), style = MaterialTheme.typography.bodyMedium, color = FocusColors.TextSecondary)
                Spacer(Modifier.height(14.dp))
                PrimaryButton(stringResource(R.string.routine_add), { creating = true })
            }
            return@Column
        }

        // Today first, then the rest of the week in order.
        val order = (0 until 7).map { today.plus(it.toLong()) }
        order.forEach { day ->
            val blocks = state.blocks.filter { day in it.days }.sortedBy { it.startMinute }
            if (blocks.isEmpty()) return@forEach
            val isToday = day == today
            SectionLabel(
                if (isToday) stringResource(R.string.routine_today, dayName(day, locale)) else dayName(day, locale),
            )
            Spacer(Modifier.height(8.dp))
            FocusCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 6.dp)) {
                blocks.forEachIndexed { i, b ->
                    if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = FocusColors.Outline)
                    val status = when {
                        !isToday -> BlockStatus.LATER
                        nowMinute >= b.endMinute -> BlockStatus.DONE
                        nowMinute >= b.startMinute -> BlockStatus.NOW
                        else -> BlockStatus.LATER
                    }
                    BlockRow(b, subjects[b.subjectId], status) { editing = b }
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }

    if (creating) {
        BlockDialog(null, state.subjects, onDismiss = { creating = false }, onSave = { creating = false; onSave(it) }, onDelete = null, defaultDay = today)
    }
    editing?.let { b ->
        BlockDialog(b, state.subjects, onDismiss = { editing = null }, onSave = { editing = null; onSave(it) }, onDelete = { editing = null; onDelete(b.id) }, defaultDay = today)
    }
}

private enum class BlockStatus { DONE, NOW, LATER }

private fun dayName(day: DayOfWeek, locale: java.util.Locale) =
    day.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }

internal fun clock(minute: Int) = "%02d:%02d".format((minute / 60) % 24, minute % 60)

@Composable
private fun BlockRow(b: RoutineBlock, subject: Subject?, status: BlockStatus, onClick: () -> Unit) {
    val color = subject?.color?.toColor() ?: FocusColors.Accent
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(64.dp)) {
            Text(clock(b.startMinute), style = MaterialTheme.typography.titleMedium, color = if (status == BlockStatus.DONE) FocusColors.TextTertiary else FocusColors.TextPrimary)
            Text(clock(b.endMinute), style = MaterialTheme.typography.bodySmall, color = FocusColors.TextTertiary)
        }
        Box(
            Modifier
                .width(3.dp)
                .height(34.dp)
                .clip(CircleShape)
                .background(if (status == BlockStatus.DONE) FocusColors.SurfaceHigh else color),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                b.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (status == BlockStatus.DONE) FocusColors.TextTertiary else FocusColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val detail = listOfNotNull(
                subject?.name?.takeIf { it != b.title },
                stringResource(R.string.routine_minutes, b.durationMinutes),
                if (status == BlockStatus.NOW) stringResource(R.string.routine_now) else null,
            ).joinToString(" · ")
            Text(detail, style = MaterialTheme.typography.bodySmall, color = if (status == BlockStatus.NOW) FocusColors.Accent else FocusColors.TextSecondary)
        }
        if (b.remindBefore != null) {
            Icon(FocusIcons.Bell, contentDescription = stringResource(R.string.routine_cd_reminder), tint = FocusColors.TextTertiary, modifier = Modifier.size(16.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun BlockDialog(
    initial: RoutineBlock?,
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onSave: (RoutineBlock) -> Unit,
    onDelete: (() -> Unit)?,
    defaultDay: DayOfWeek,
) {
    val locale = LocalConfiguration.current.locales[0]
    var title by rememberSaveable { mutableStateOf(initial?.title.orEmpty()) }
    var subjectId by rememberSaveable { mutableStateOf(initial?.subjectId) }
    var daysMask by rememberSaveable { mutableIntStateOf(RoutineBlock.daysToMask(initial?.days ?: setOf(defaultDay))) }
    var start by rememberSaveable { mutableIntStateOf(initial?.startMinute ?: (19 * 60)) }
    var duration by rememberSaveable { mutableIntStateOf(initial?.durationMinutes ?: 50) }
    var remind by rememberSaveable { mutableStateOf(if (initial == null) 10 else initial.remindBefore) }
    var pickingTime by remember { mutableStateOf(false) }
    val valid = title.isNotBlank() && daysMask != 0

    if (pickingTime) {
        val picker = rememberTimePickerState(initialHour = start / 60, initialMinute = start % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            containerColor = FocusColors.SurfaceRaised,
            shape = MaterialTheme.shapes.large,
            text = {
                TimePicker(
                    state = picker,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = FocusColors.SurfaceHigh,
                        selectorColor = FocusColors.Accent,
                        timeSelectorSelectedContainerColor = FocusColors.AccentMuted,
                        timeSelectorUnselectedContainerColor = FocusColors.SurfaceHigh,
                        timeSelectorSelectedContentColor = FocusColors.TextPrimary,
                        timeSelectorUnselectedContentColor = FocusColors.TextSecondary,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = { start = picker.hour * 60 + picker.minute; pickingTime = false }) {
                    Text(stringResource(R.string.routine_ok), color = FocusColors.Accent)
                }
            },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text(stringResource(R.string.routine_cancel), color = FocusColors.TextPrimary) } },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FocusColors.SurfaceRaised,
        shape = MaterialTheme.shapes.large,
        title = { Text(stringResource(if (initial == null) R.string.routine_add else R.string.routine_edit), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.routine_field_title), style = MaterialTheme.typography.bodySmall)
                FocusTextField(title, { title = it }, Modifier.fillMaxWidth(), placeholder = stringResource(R.string.routine_field_title_hint), maxLength = RoutineBlock.MAX_TITLE_LENGTH)
                if (subjects.isNotEmpty()) {
                    Text(stringResource(R.string.routine_field_subject), style = MaterialTheme.typography.bodySmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SelectChip(stringResource(R.string.routine_no_subject), FocusColors.TextTertiary, subjectId == null) { subjectId = null }
                        subjects.forEach { s ->
                            SelectChip(s.name, s.color.toColor(), subjectId == s.id) {
                                subjectId = s.id
                                if (title.isBlank()) title = s.name
                            }
                        }
                    }
                }
                Text(stringResource(R.string.routine_field_days), style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DayOfWeek.entries.forEach { d ->
                        val bit = 1 shl (d.value - 1)
                        val on = daysMask and bit != 0
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (on) FocusColors.Accent else FocusColors.Background)
                                .clickable { daysMask = daysMask xor bit },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                d.getDisplayName(TextStyle.NARROW_STANDALONE, locale),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (on) FocusColors.OnAccent else FocusColors.TextSecondary,
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.routine_field_start), style = MaterialTheme.typography.bodySmall)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .background(FocusColors.Background)
                                .clickable { pickingTime = true }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(clock(start), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Icon(FocusIcons.Clock, contentDescription = null, tint = FocusColors.TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.routine_field_duration), style = MaterialTheme.typography.bodySmall)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(FocusColors.Background),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            StepButton(FocusIcons.Minus) { duration = (duration - 5).coerceIn(RoutineBlock.DURATION_RANGE) }
                            Text(stringResource(R.string.routine_minutes, duration), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            StepButton(FocusIcons.Plus) { duration = (duration + 5).coerceIn(RoutineBlock.DURATION_RANGE) }
                        }
                    }
                }
                Text(stringResource(R.string.routine_field_reminder), style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SelectChip(stringResource(R.string.routine_remind_off), FocusColors.TextTertiary, remind == null) { remind = null }
                    RoutineBlock.REMIND_OPTIONS.forEach { m ->
                        val label = if (m == 0) stringResource(R.string.routine_remind_at_start) else stringResource(R.string.routine_remind_before, m)
                        SelectChip(label, FocusColors.Accent, remind == m) { remind = m }
                    }
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete, contentPadding = PaddingValues(0.dp)) { Text(stringResource(R.string.routine_delete), color = FocusColors.Error) }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(
                    RoutineBlock(
                        id = initial?.id ?: 0,
                        title = title,
                        subjectId = subjectId,
                        days = RoutineBlock.maskToDays(daysMask),
                        startMinute = start,
                        durationMinutes = duration,
                        remindBefore = remind,
                    ),
                )
            }) { Text(stringResource(R.string.routine_save), color = if (valid) FocusColors.Accent else FocusColors.TextTertiary) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.routine_cancel), color = FocusColors.TextPrimary) } },
    )
}

@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxHeight()
            .width(38.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = FocusColors.TextSecondary, modifier = Modifier.size(16.dp))
    }
}
