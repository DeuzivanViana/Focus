package com.focus.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.focus.core.designsystem.theme.FocusColors
import com.focus.core.designsystem.theme.LocalFocusPalette
import androidx.compose.ui.graphics.Color

@Composable
/**
 * Container: separated from the page by its tone, not by a border (Material 3 tonal
 * surfaces, Refactoring UI "use fewer borders").
 */
fun FocusCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(FocusColors.Surface)
            .padding(contentPadding),
        content = content,
    )
}

/** Section heading in sentence case: hierarchy by weight and color instead of all caps. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
        color = FocusColors.TextSecondary,
        modifier = modifier,
    )
}

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    highlight: Boolean = false,
) {
    FocusCard(modifier = modifier, contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = if (highlight) FocusColors.Accent else FocusColors.TextPrimary,
            maxLines = 1,
        )
        if (caption != null) {
            Spacer(Modifier.height(2.dp))
            Text(caption, style = MaterialTheme.typography.bodySmall, color = FocusColors.TextTertiary, maxLines = 1)
        }
    }
}

@Composable
fun LinearMeter(progress: Float, modifier: Modifier = Modifier) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(durationMillis = 600, easing = FastOutSlowInEasing), label = "meter")
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(FocusColors.SurfaceHigh),
    ) {
        if (p > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(p)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(FocusColors.Accent),
            )
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = FocusColors.Accent, contentColor = FocusColors.OnAccent),
        contentPadding = PaddingValues(horizontal = 24.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
        }
        // Light label on the accent in both themes (the dark theme's established look).
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (LocalFocusPalette.current.isDark) FocusColors.TextPrimary else Color.White)
    }
}

@Composable
fun GhostIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    // Tonal (filled with a surface step) instead of outlined.
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = FocusColors.SurfaceHigh,
            contentColor = FocusColors.TextPrimary,
            disabledContainerColor = FocusColors.Surface,
            disabledContentColor = FocusColors.TextTertiary,
        ),
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun SegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    fill: Boolean = false,
) {
    Row(
        modifier
            .clip(CircleShape)
            .background(FocusColors.SurfaceHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { i, label ->
            val selected = i == selectedIndex
            // Track is a recessed surface step; the selected segment is lifted to the card tone.
            val bg by animateColorAsState(if (selected) selectedSegment() else FocusColors.SurfaceHigh, tween(180), label = "segment")
            val fg by animateColorAsState(if (selected) FocusColors.TextPrimary else FocusColors.TextSecondary, tween(180), label = "segmentText")
            Box(
                Modifier
                    .then(if (fill) Modifier.weight(1f) else Modifier)
                    .clip(CircleShape)
                    .background(bg)
                    .clickable { onSelect(i) }
                    .padding(horizontal = if (fill) 4.dp else 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = fg,
                )
            }
        }
    }
}

/** Compact icon button for toolbars (e.g. chart type). */
@Composable
fun ToolIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    Box(
        modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (selected) FocusColors.AccentDim else FocusColors.SurfaceHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (selected) FocusColors.Accent else FocusColors.TextSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Selected segment: white on the light theme's grey track, one step up on the dark one. */
@Composable
private fun selectedSegment(): Color =
    if (LocalFocusPalette.current.isDark) FocusColors.OutlineStrong else Color.White

/** Small status label: tinted fill instead of an outline. */
@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
        color = color,
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
