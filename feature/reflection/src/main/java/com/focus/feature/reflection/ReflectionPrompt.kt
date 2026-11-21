package com.focus.feature.reflection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.window.DialogProperties
import com.focus.core.designsystem.component.FocusTextField
import com.focus.core.designsystem.theme.FocusColors
import com.focus.core.designsystem.util.formatDuration
import com.focus.core.model.FocusSession

/** Host this once at the app root: it shows itself after a finished session. */
@Composable
fun ReflectionPromptHost(viewModel: ReflectionPromptViewModel = hiltViewModel()) {
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    pending?.let { session ->
        ReflectionPrompt(
            session = session,
            onSubmit = { rating, note -> viewModel.submit(session.id, rating, note) },
            onSkip = { viewModel.skip(session.id) },
        )
    }
}

@Composable
fun ReflectionPrompt(session: FocusSession, onSubmit: (Int, String) -> Unit, onSkip: () -> Unit) {
    var rating by rememberSaveable(session.id) { mutableIntStateOf(0) }
    var note by rememberSaveable(session.id) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onSkip,
        properties = DialogProperties(dismissOnClickOutside = false),
        containerColor = FocusColors.SurfaceRaised,
        shape = MaterialTheme.shapes.large,
        title = { Text(stringResource(R.string.reflection_prompt_title), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.reflection_prompt_subtitle, formatDuration(session.focusedSeconds)),
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(4.dp))
                RatingRow(rating, onRate = { rating = it })
                Text(
                    if (rating == 0) " " else stringResource(RATING_LABELS[rating - 1]),
                    style = MaterialTheme.typography.labelMedium,
                    color = FocusColors.TextSecondary,
                )
                FocusTextField(
                    note,
                    { note = it },
                    Modifier.fillMaxWidth(),
                    placeholder = stringResource(R.string.reflection_note_hint),
                    maxLength = FocusSession.MAX_NOTE_LENGTH,
                    singleLine = false,
                )
                Text(
                    "${note.length}/${FocusSession.MAX_NOTE_LENGTH}",
                    style = MaterialTheme.typography.labelSmall,
                    color = FocusColors.TextTertiary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(rating, note) }, enabled = rating > 0) {
                Text(stringResource(R.string.reflection_save), color = if (rating > 0) FocusColors.Accent else FocusColors.TextTertiary)
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) { Text(stringResource(R.string.reflection_skip), color = FocusColors.TextSecondary) }
        },
    )
}

internal val RATING_LABELS = listOf(
    R.string.reflection_rating_1,
    R.string.reflection_rating_2,
    R.string.reflection_rating_3,
    R.string.reflection_rating_4,
    R.string.reflection_rating_5,
)
