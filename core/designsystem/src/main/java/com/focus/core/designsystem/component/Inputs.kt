package com.focus.core.designsystem.component

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.focus.core.designsystem.icon.FocusIcons
import com.focus.core.designsystem.theme.FocusColors

/** Single-line text field in the app style. Input is capped at [maxLength] characters. */
@Composable
fun FocusTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    maxLength: Int = 80,
    onDone: (() -> Unit)? = null,
    singleLine: Boolean = true,
    numeric: Boolean = false,
    containerColor: Color? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            val text = when {
                numeric -> raw.filter(Char::isDigit)
                singleLine -> raw.replace("\n", " ")
                else -> raw
            }
            onValueChange(text.take(maxLength))
        },
        // A fixed min height instead of minLines: minLines re-measures on font loading.
        modifier = if (singleLine) modifier else modifier.heightIn(min = 104.dp),
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 5,
        placeholder = placeholder?.let { { Text(it, style = MaterialTheme.typography.bodyMedium, color = FocusColors.TextTertiary) } },
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = MaterialTheme.shapes.small,
        keyboardOptions = if (numeric) {
            KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
        } else {
            KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = if (singleLine) ImeAction.Done else ImeAction.Default)
        },
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = FocusColors.Accent,
            // Filled look at rest: the border only appears on focus.
            unfocusedBorderColor = Color.Transparent,
            cursorColor = FocusColors.Accent,
            // Recessed well: the page tone reads as "inset" inside both cards and dialogs.
            // Pass the card tone when the field sits directly on the page.
            focusedContainerColor = containerColor ?: FocusColors.Background,
            unfocusedContainerColor = containerColor ?: FocusColors.Background,
        ),
    )
}

/** Row of round color swatches. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorSwatchPicker(
    colors: List<Color>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    labels: List<String>? = null,
    swatchSize: Dp = 34.dp,
) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        colors.forEachIndexed { i, color ->
            val selected = i == selectedIndex
            Box(
                Modifier
                    .size(swatchSize)
                    .clip(CircleShape)
                    .border(1.5.dp, if (selected) color else Color.Transparent, CircleShape)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable(onClickLabel = labels?.getOrNull(i)) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(FocusIcons.Check, contentDescription = labels?.getOrNull(i), tint = FocusColors.OnAccent, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/** Small colored dot, e.g. a subject marker or chart legend. */
@Composable
fun ColorDot(color: Color, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}

/** Small selectable chip with a color dot (subject pickers). */
@Composable
fun SelectChip(label: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(if (selected) FocusColors.AccentDim else FocusColors.SurfaceHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorDot(color, size = 8.dp)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) FocusColors.TextPrimary else FocusColors.TextSecondary)
    }
}
