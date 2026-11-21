package com.focus.feature.subjects

import com.focus.core.designsystem.theme.toColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focus.core.designsystem.component.ColorDot
import com.focus.core.designsystem.component.ColorSwatchPicker
import com.focus.core.designsystem.component.FocusCard
import com.focus.core.designsystem.component.FocusTextField
import com.focus.core.designsystem.component.PrimaryButton
import com.focus.core.designsystem.component.ToolIconButton
import com.focus.core.designsystem.icon.FocusIcons
import com.focus.core.designsystem.theme.FocusColors
import com.focus.core.designsystem.util.formatDuration
import com.focus.core.model.AccentColor
import com.focus.core.model.StudyTask
import com.focus.core.model.Subject

@Composable
fun SubjectsRoute(viewModel: SubjectsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SubjectsScreen(
        state = state,
        onAddSubject = viewModel::addSubject,
        onUpdateSubject = viewModel::updateSubject,
        onDeleteSubject = viewModel::deleteSubject,
        onAddTask = viewModel::addTask,
        onToggleTask = viewModel::toggleTask,
        onDeleteTask = viewModel::deleteTask,
    )
}

@Composable
fun SubjectsScreen(
    state: SubjectsUiState,
    onAddSubject: (String, AccentColor) -> Unit,
    onUpdateSubject: (Subject) -> Unit,
    onDeleteSubject: (Long) -> Unit,
    onAddTask: (Long, String) -> Unit,
    onToggleTask: (StudyTask) -> Unit,
    onDeleteTask: (Long) -> Unit,
    initiallyExpanded: Long? = null,
) {
    var editing by remember { mutableStateOf<Subject?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val maxSeconds = (state.items.maxOfOrNull { it.time.last7DaysSeconds } ?: 0L).coerceAtLeast(1L)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.subjects_title), style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.subjects_subtitle), style = MaterialTheme.typography.bodySmall)
            }
            ToolIconButton(FocusIcons.Plus, stringResource(R.string.subjects_add), { creating = true })
        }
        Spacer(Modifier.height(20.dp))

        if (state.loaded && state.items.none { it.time.subject != null }) {
            FocusCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.subjects_empty), style = MaterialTheme.typography.bodyMedium, color = FocusColors.TextSecondary)
                Spacer(Modifier.height(14.dp))
                PrimaryButton(stringResource(R.string.subjects_add), { creating = true })
            }
            Spacer(Modifier.height(10.dp))
        }

        state.items.forEach { item ->
            val subject = item.time.subject
            SubjectCard(
                item = item,
                maxSeconds = maxSeconds,
                expanded = subject != null && expanded == subject.id,
                onToggleExpanded = { if (subject != null) expanded = if (expanded == subject.id) null else subject.id },
                onEdit = { editing = subject },
                onAddTask = { title -> subject?.let { onAddTask(it.id, title) } },
                onToggleTask = onToggleTask,
                onDeleteTask = onDeleteTask,
            )
            Spacer(Modifier.height(10.dp))
        }
    }

    if (creating) {
        SubjectDialog(
            initial = null,
            onDismiss = { creating = false },
            onSave = { name, color -> creating = false; onAddSubject(name, color) },
            onDelete = null,
        )
    }
    editing?.let { subject ->
        SubjectDialog(
            initial = subject,
            onDismiss = { editing = null },
            onSave = { name, color -> editing = null; onUpdateSubject(subject.copy(name = name, color = color)) },
            onDelete = { editing = null; onDeleteSubject(subject.id) },
        )
    }
}

@Composable
private fun SubjectCard(
    item: SubjectItem,
    maxSeconds: Long,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onEdit: () -> Unit,
    onAddTask: (String) -> Unit,
    onToggleTask: (StudyTask) -> Unit,
    onDeleteTask: (Long) -> Unit,
) {
    val subject = item.time.subject
    val color = subject?.let { it.color.toColor() } ?: FocusColors.TextTertiary
    FocusCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable(enabled = subject != null, onClick = onToggleExpanded),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ColorDot(color, size = 10.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(subject?.name ?: stringResource(R.string.subjects_no_subject), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(R.string.subjects_time, formatDuration(item.time.last7DaysSeconds), formatDuration(item.time.totalSeconds)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (subject != null) {
                ToolIconButton(FocusIcons.Edit, stringResource(R.string.subjects_cd_edit), onEdit)
                Spacer(Modifier.width(6.dp))
                Icon(
                    if (expanded) FocusIcons.ChevronDown else FocusIcons.ChevronRight,
                    contentDescription = null,
                    tint = FocusColors.TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        val meter by animateFloatAsState(item.time.last7DaysSeconds.toFloat() / maxSeconds, tween(600), label = "subject")
        SubjectMeter(meter, color)
        if (subject != null) {
            Spacer(Modifier.height(10.dp))
            val done = item.tasks.count { it.done }
            Text(
                if (item.tasks.isEmpty()) stringResource(R.string.subjects_no_tasks) else stringResource(R.string.subjects_tasks_done, done, item.tasks.size),
                style = MaterialTheme.typography.bodySmall,
                color = FocusColors.TextTertiary,
            )
        }
        AnimatedVisibility(expanded, enter = expandVertically(tween(220)) + fadeIn(tween(220)), exit = shrinkVertically(tween(180)) + fadeOut(tween(120))) {
          Column {
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = FocusColors.Outline)
            item.tasks.forEach { task ->
                TaskRow(task, color, { onToggleTask(task) }, { onDeleteTask(task.id) })
            }
            var newTask by rememberSaveable { mutableStateOf("") }
            val submit = {
                if (newTask.isNotBlank()) {
                    onAddTask(newTask)
                    newTask = ""
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FocusTextField(
                    value = newTask,
                    onValueChange = { newTask = it },
                    placeholder = stringResource(R.string.subjects_add_task),
                    maxLength = StudyTask.MAX_TITLE_LENGTH,
                    onDone = submit,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                ToolIconButton(FocusIcons.Plus, stringResource(R.string.subjects_cd_add_task), submit)
            }
          }
        }
    }
}

@Composable
private fun SubjectMeter(fraction: Float, color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(FocusColors.SurfaceHigh),
    ) {
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(color),
            )
        }
    }
}

@Composable
private fun TaskRow(task: StudyTask, color: Color, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onToggle)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (task.done) FocusIcons.BoxChecked else FocusIcons.Box,
            contentDescription = null,
            tint = if (task.done) color else FocusColors.TextSecondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            task.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (task.done) FocusColors.TextTertiary else FocusColors.TextPrimary,
            textDecoration = if (task.done) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
        )
        Icon(
            FocusIcons.Trash,
            contentDescription = stringResource(R.string.subjects_cd_delete_task),
            tint = FocusColors.TextTertiary,
            modifier = Modifier
                .size(32.dp)
                .clip(MaterialTheme.shapes.small)
                .clickable(onClick = onDelete)
                .padding(7.dp),
        )
    }
}

@Composable
private fun SubjectDialog(
    initial: Subject?,
    onDismiss: () -> Unit,
    onSave: (String, AccentColor) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var color by rememberSaveable { mutableStateOf(initial?.color ?: AccentColor.Default) }
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = FocusColors.SurfaceRaised,
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.subjects_delete_title), style = MaterialTheme.typography.titleLarge) },
            text = { Text(stringResource(R.string.subjects_delete_text), style = MaterialTheme.typography.bodyMedium, color = FocusColors.TextSecondary) },
            confirmButton = { TextButton(onClick = onDelete) { Text(stringResource(R.string.subjects_delete), color = FocusColors.Error) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.subjects_cancel), color = FocusColors.TextPrimary) } },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FocusColors.SurfaceRaised,
        shape = MaterialTheme.shapes.large,
        title = {
            Text(
                stringResource(if (initial == null) R.string.subjects_add else R.string.subjects_edit),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.subjects_name), style = MaterialTheme.typography.bodySmall)
                FocusTextField(name, { name = it }, Modifier.fillMaxWidth(), maxLength = Subject.MAX_NAME_LENGTH)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.subjects_color), style = MaterialTheme.typography.bodySmall)
                ColorSwatchPicker(
                    colors = AccentColor.entries.map { it.toColor() },
                    selectedIndex = color.ordinal,
                    onSelect = { color = AccentColor.entries[it] },
                    swatchSize = 30.dp,
                )
                if (onDelete != null) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { confirmDelete = true }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        Text(stringResource(R.string.subjects_delete), color = FocusColors.Error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, color) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.subjects_save), color = if (name.isNotBlank()) FocusColors.Accent else FocusColors.TextTertiary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.subjects_cancel), color = FocusColors.TextPrimary) }
        },
    )
}
