package com.focus.feature.focus

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.focus.core.designsystem.icon.FocusIcons
import com.focus.core.designsystem.theme.FocusColors
import com.focus.core.model.FocusSound

internal fun soundName(sound: FocusSound): Int = when (sound) {
    FocusSound.NONE -> R.string.sound_none
    FocusSound.RAIN -> R.string.sound_rain
    FocusSound.WHITE -> R.string.sound_white
    FocusSound.PINK -> R.string.sound_pink
    FocusSound.BROWN -> R.string.sound_brown
    FocusSound.OCEAN -> R.string.sound_ocean
}

private fun soundDescription(sound: FocusSound): Int? = when (sound) {
    FocusSound.NONE -> null
    FocusSound.RAIN -> R.string.sound_rain_desc
    FocusSound.WHITE -> R.string.sound_white_desc
    FocusSound.PINK -> R.string.sound_pink_desc
    FocusSound.BROWN -> R.string.sound_brown_desc
    FocusSound.OCEAN -> R.string.sound_ocean_desc
}

/** Round button next to the subject chip; tinted while a sound is chosen. */
@Composable
internal fun SoundChip(sound: FocusSound, onClick: () -> Unit) {
    val on = sound != FocusSound.NONE
    val bg by animateColorAsState(if (on) FocusColors.AccentDim else FocusColors.Surface, tween(200), label = "soundChip")
    val label = stringResource(R.string.sound_cd, stringResource(soundName(sound)))
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(FocusIcons.Sound, contentDescription = label, tint = if (on) FocusColors.Accent else FocusColors.TextSecondary, modifier = Modifier.size(18.dp))
    }
}

/**
 * Choose the focus sound and its volume. The highlighted sound plays as a preview while
 * the dialog is open; during a focus it plays by itself.
 */
@Composable
internal fun SoundPickerDialog(
    selected: FocusSound,
    volume: Int,
    onSelect: (FocusSound) -> Unit,
    onVolume: (Int) -> Unit,
    onPreview: (FocusSound?) -> Unit,
    onDismiss: () -> Unit,
) {
    var vol by remember { mutableFloatStateOf(volume.toFloat()) }
    DisposableEffect(Unit) { onDispose { onPreview(null) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FocusColors.SurfaceRaised,
        shape = MaterialTheme.shapes.large,
        title = { Text(stringResource(R.string.sound_title), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FocusSound.entries.forEach { s ->
                    val isSelected = s == selected
                    val bg by animateColorAsState(if (isSelected) FocusColors.AccentDim else FocusColors.SurfaceHigh, tween(180), label = "soundRow")
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(bg)
                            .clickable {
                                onSelect(s)
                                onPreview(s.takeIf { it != FocusSound.NONE })
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(soundName(s)), style = MaterialTheme.typography.bodyMedium)
                            soundDescription(s)?.let {
                                Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = FocusColors.TextTertiary)
                            }
                        }
                        if (isSelected) Icon(FocusIcons.Check, contentDescription = null, tint = FocusColors.Accent, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(FocusIcons.Sound, contentDescription = null, tint = FocusColors.TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Slider(
                        value = vol,
                        onValueChange = { vol = it; onVolume(it.toInt()) },
                        valueRange = 0f..100f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = FocusColors.Accent,
                            activeTrackColor = FocusColors.Accent,
                            inactiveTrackColor = FocusColors.SurfaceHigh,
                        ),
                    )
                }
                Text(stringResource(R.string.sound_hint), style = MaterialTheme.typography.bodySmall, color = FocusColors.TextTertiary)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.focus_pick_done), color = FocusColors.Accent) }
        },
    )
}
