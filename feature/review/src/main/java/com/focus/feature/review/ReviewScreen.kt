package com.focus.feature.review

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
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
import com.focus.core.model.ReviewItem
import com.focus.core.model.Subject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun ReviewRoute(viewModel: ReviewViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val askNotifications = rememberNotificationPermissionRequest()
    ReviewScreen(
        state = state,
        onAdd = { viewModel.add(it); askNotifications() },
        onComplete = viewModel::complete,
        onDelete = viewModel::delete,
    )
}

@Composable
fun ReviewScreen(
    state: ReviewUiState,
    onAdd: (ReviewItem) -> Unit,
    onComplete: (ReviewItem) -> Unit,
    onDelete: (Long) -> Unit,
    today: LocalDate = LocalDate.now(),
) {
    var creating by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<ReviewItem?>(null) }
    var showFinished by rememberSaveable { mutableStateOf(false) }
    val subjects = state.subjects.associateBy { it.id }
    val active = state.reviews.filter { !it.isFinished }.sortedBy { it.dueOn }
    val due = active.filter { !it.dueOn!!.isAfter(today) }
    val upcoming = active.filter { it.dueOn!!.isAfter(today) }
    val finished = state.reviews.filter { it.isFinished }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.review_title), style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.review_subtitle), style = MaterialTheme.typography.bodySmall)
            }
            ToolIconButton(FocusIcons.Plus, stringResource(R.string.review_add), { creating = true })
        }
        Spacer(Modifier.height(20.dp))

        if (state.loaded && state.reviews.isEmpty()) {
            FocusCard(Modifier.fillMaxWidth()) {
                Icon(FocusIcons.Repeat, contentDescription = null, tint = FocusColors.TextSecondary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.review_empty), style = MaterialTheme.typography.bodyMedium, color = FocusColors.TextSecondary)
                Spacer(Modifier.height(14.dp))
                PrimaryButton(stringResource(R.string.review_add), { creating = true })
            }
        } else if (state.loaded) {
            SectionLabel(stringResource(R.string.review_section_today))
            Spacer(Modifier.height(8.dp))
            FocusCard(Modifier.fillMaxWidth().animateContentSize(), contentPadding = PaddingValues(vertical = 6.dp)) {
                if (due.isEmpty()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(FocusIcons.Check, contentDescription = null, tint = FocusColors.Accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.review_all_done), style = MaterialTheme.typography.bodyMedium, color = FocusColors.TextSecondary)
                    }
                }
                due.forEachIndexed { i, r ->
                    if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = FocusColors.Outline)
                    ReviewRow(r, subjects[r.subjectId], today, showDone = true, onDone = { onComplete(r) }, onLongPress = { confirmDelete = r })
                }
            }
            if (upcoming.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                SectionLabel(stringResource(R.string.review_section_upcoming))
                Spacer(Modifier.height(8.dp))
                FocusCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 6.dp)) {
                    upcoming.forEachIndexed { i, r ->
                        if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = FocusColors.Outline)
                        ReviewRow(r, subjects[r.subjectId], today, showDone = false, onDone = {}, onLongPress = { confirmDelete = r })
                    }
                }
            }
            if (finished.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .clickable { showFinished = !showFinished }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel(stringResource(R.string.review_section_finished, finished.size), Modifier.weight(1f))
                    Icon(if (showFinished) FocusIcons.ChevronDown else FocusIcons.ChevronRight, contentDescription = null, tint = FocusColors.TextTertiary, modifier = Modifier.size(16.dp))
                }
                if (showFinished) {
                    Spacer(Modifier.height(8.dp))
                    FocusCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 6.dp)) {
                        finished.forEachIndexed { i, r ->
                            if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = FocusColors.Outline)
                            ReviewRow(r, subjects[r.subjectId], today, showDone = false, onDone = {}, onLongPress = { confirmDelete = r })
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.review_hint), style = MaterialTheme.typography.bodySmall, color = FocusColors.TextTertiary)
        }
    }

    if (creating) {
        AddReviewDialog(state.subjects, today, onDismiss = { creating = false }, onSave = { creating = false; onAdd(it) })
    }
    confirmDelete?.let { r ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            containerColor = FocusColors.SurfaceRaised,
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.review_delete_title), style = MaterialTheme.typography.titleLarge) },
            text = { Text(r.title, style = MaterialTheme.typography.bodyMedium, color = FocusColors.TextSecondary) },
            confirmButton = { TextButton(onClick = { confirmDelete = null; onDelete(r.id) }) { Text(stringResource(R.string.review_delete), color = FocusColors.Error) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text(stringResource(R.string.review_cancel), color = FocusColors.TextPrimary) } },
        )
    }
}

@Composable
private fun ReviewRow(r: ReviewItem, subject: Subject?, today: LocalDate, showDone: Boolean, onDone: () -> Unit, onLongPress: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val fmt = remember(locale) { DateTimeFormatter.ofPattern("EEE, d MMM", locale) }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onLongPress)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorDot(subject?.color?.toColor() ?: FocusColors.TextTertiary, size = 8.dp)
                Spacer(Modifier.width(8.dp))
                Text(r.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StageMarks(r.stage)
                Spacer(Modifier.width(10.dp))
                val dueOn = r.dueOn
                val (text, color) = when {
                    dueOn == null -> stringResource(R.string.review_finished_on) to FocusColors.TextTertiary
                    dueOn.isBefore(today) -> {
                        val days = ChronoUnit.DAYS.between(dueOn, today).toInt()
                        pluralStringResource(R.plurals.review_overdue, days, days) to FocusColors.Error
                    }
                    dueOn == today -> stringResource(R.string.review_due_today) to FocusColors.Accent
                    else -> fmt.format(dueOn) to FocusColors.TextSecondary
                }
                Text(text, style = MaterialTheme.typography.bodySmall, color = color)
            }
        }
        if (showDone) {
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(FocusColors.AccentDim)
                    .clickable(onClick = onDone)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(FocusIcons.Check, contentDescription = null, tint = FocusColors.Accent, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.review_done), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** "1d 7d 30d" with the steps already done filled with the accent. */
@Composable
private fun StageMarks(stage: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        ReviewItem.INTERVALS.forEachIndexed { i, days ->
            val done = i < stage
            val current = i == stage
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(
                        when {
                            done -> FocusColors.AccentMuted
                            current -> FocusColors.AccentDim
                            else -> FocusColors.SurfaceHigh
                        },
                    )
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
                Text(
                    stringResource(R.string.review_interval, days),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (done || current) FocusColors.TextPrimary else FocusColors.TextTertiary,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddReviewDialog(subjects: List<Subject>, today: LocalDate, onDismiss: () -> Unit, onSave: (ReviewItem) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var subjectId by rememberSaveable { mutableStateOf<Long?>(null) }
    var daysAgo by rememberSaveable { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FocusColors.SurfaceRaised,
        shape = MaterialTheme.shapes.large,
        title = { Text(stringResource(R.string.review_add), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.review_field_title), style = MaterialTheme.typography.bodySmall)
                FocusTextField(title, { title = it }, Modifier.fillMaxWidth(), placeholder = stringResource(R.string.review_field_title_hint), maxLength = ReviewItem.MAX_TITLE_LENGTH)
                if (subjects.isNotEmpty()) {
                    Text(stringResource(R.string.review_field_subject), style = MaterialTheme.typography.bodySmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SelectChip(stringResource(R.string.review_no_subject), FocusColors.TextTertiary, subjectId == null) { subjectId = null }
                        subjects.forEach { s -> SelectChip(s.name, s.color.toColor(), subjectId == s.id) { subjectId = s.id } }
                    }
                }
                Text(stringResource(R.string.review_field_studied), style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(R.string.review_today, R.string.review_yesterday, R.string.review_two_days).forEachIndexed { i, label ->
                        SelectChip(stringResource(label), FocusColors.Accent, daysAgo == i) { daysAgo = i }
                    }
                }
                val studied = today.minusDays(daysAgo.toLong())
                Text(
                    stringResource(R.string.review_schedule_preview, *ReviewItem.INTERVALS.map { studied.plusDays(it.toLong()).dayOfMonth.toString() + "/" + studied.plusDays(it.toLong()).monthValue }.toTypedArray()),
                    style = MaterialTheme.typography.bodySmall,
                    color = FocusColors.TextTertiary,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = {
                onSave(ReviewItem(title = title, subjectId = subjectId, studiedOn = today.minusDays(daysAgo.toLong())))
            }) { Text(stringResource(R.string.review_save), color = if (title.isNotBlank()) FocusColors.Accent else FocusColors.TextTertiary) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.review_cancel), color = FocusColors.TextPrimary) } },
    )
}
