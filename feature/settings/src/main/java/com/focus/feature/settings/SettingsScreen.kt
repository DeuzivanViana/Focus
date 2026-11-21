package com.focus.feature.settings

import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import com.focus.core.designsystem.component.TIMER_PREVIEW_PROGRESS
import com.focus.core.designsystem.component.TimerFace
import com.focus.core.model.TimerStyle
import com.focus.core.designsystem.theme.toColor
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focus.core.data.backup.ImportResult
import com.focus.core.data.backup.ProgressBackup
import com.focus.core.designsystem.component.FocusCard
import com.focus.core.designsystem.component.GhostIconButton
import com.focus.core.designsystem.component.SectionLabel
import com.focus.core.designsystem.component.SegmentedToggle
import com.focus.core.designsystem.icon.FocusIcons
import com.focus.core.designsystem.theme.FocusColors
import com.focus.core.designsystem.util.formatDuration
import com.focus.core.model.AccentColor
import com.focus.core.model.ThemeMode
import com.focus.core.model.ReminderSettings
import com.focus.core.designsystem.util.rememberNotificationPermissionRequest
import com.focus.core.model.TimerSettings
import java.time.LocalDate

/** Offered languages. Empty tag = follow the system language. */
private val LANGUAGE_TAGS = listOf("", "pt-BR", "en")

@Composable
fun SettingsRoute(onOpenBlocker: () -> Unit, onOpenPrivacy: () -> Unit, onOpenLicenses: () -> Unit = {}, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val askNotifications = rememberNotificationPermissionRequest()
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ProgressBackup.MIME_TYPE)) { uri ->
        if (uri != null) viewModel.export(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.import(uri)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { e ->
            val msg = when (e) {
                is BackupEvent.Exported -> context.getString(R.string.settings_export_done, e.count)
                BackupEvent.ExportFailed -> context.getString(R.string.settings_export_failed)
                is BackupEvent.Imported -> when (val r = e.result) {
                    is ImportResult.Success -> context.getString(R.string.settings_import_done, r.imported, r.total)
                    ImportResult.Tampered -> context.getString(R.string.settings_import_tampered)
                    ImportResult.Invalid -> context.getString(R.string.settings_import_invalid)
                    ImportResult.TooLarge -> context.getString(R.string.settings_import_too_large)
                    ImportResult.UnsupportedVersion -> context.getString(R.string.settings_import_unsupported)
                }
            }
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    val currentTag = remember { AppCompatDelegate.getApplicationLocales().toLanguageTags() }
    SettingsScreen(
        settings = settings,
        languageIndex = LANGUAGE_TAGS.indexOfFirst { it.isNotEmpty() && currentTag.startsWith(it.substringBefore('-')) }.takeIf { it >= 0 } ?: 0,
        onLanguage = { i ->
            // AppCompat persists the choice and recreates the screen in the new language.
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(LANGUAGE_TAGS[i]))
        },
        onUpdate = viewModel::update,
        onExport = { exportLauncher.launch("focus-${LocalDate.now()}.${ProgressBackup.FILE_EXTENSION}") },
        onImport = { importLauncher.launch(arrayOf("*/*")) },
        onClearHistory = viewModel::clearHistory,
        onOpenBlocker = onOpenBlocker,
        onOpenPrivacy = onOpenPrivacy,
        onOpenLicenses = onOpenLicenses,
        reminders = reminders,
        onUpdateReminders = { t -> viewModel.updateReminders(t); askNotifications() },
    )
}

@Composable
fun SettingsScreen(
    settings: TimerSettings,
    languageIndex: Int,
    onLanguage: (Int) -> Unit,
    onUpdate: ((TimerSettings) -> TimerSettings) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onClearHistory: () -> Unit,
    onOpenBlocker: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    reminders: ReminderSettings = ReminderSettings(),
    onUpdateReminders: ((ReminderSettings) -> ReminderSettings) -> Unit = {},
    onOpenLicenses: () -> Unit = {},
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val min: @Composable (Int) -> String = { stringResource(R.string.settings_minutes, it) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(20.dp))

        Section(stringResource(R.string.settings_section_timer)) {
            Stepper(stringResource(R.string.settings_focus), min(settings.focusMinutes), settings.focusMinutes, TimerSettings.FOCUS_RANGE, 5) { v -> onUpdate { it.copy(focusMinutes = v) } }
            Divider()
            Stepper(stringResource(R.string.settings_short_break), min(settings.shortBreakMinutes), settings.shortBreakMinutes, TimerSettings.SHORT_BREAK_RANGE, 1) { v -> onUpdate { it.copy(shortBreakMinutes = v) } }
            Divider()
            Stepper(stringResource(R.string.settings_long_break), min(settings.longBreakMinutes), settings.longBreakMinutes, TimerSettings.LONG_BREAK_RANGE, 5) { v -> onUpdate { it.copy(longBreakMinutes = v) } }
            Divider()
            Stepper(stringResource(R.string.settings_cycle), "${settings.sessionsBeforeLongBreak}", settings.sessionsBeforeLongBreak, TimerSettings.CYCLE_RANGE, 1) { v -> onUpdate { it.copy(sessionsBeforeLongBreak = v) } }
        }

        Section(stringResource(R.string.settings_section_goal)) {
            Stepper(stringResource(R.string.settings_daily_goal), formatDuration(settings.dailyGoalMinutes * 60L), settings.dailyGoalMinutes, TimerSettings.GOAL_RANGE, 15) { v -> onUpdate { it.copy(dailyGoalMinutes = v) } }
            Divider()
            Text(stringResource(R.string.settings_rest_days), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(12.dp))
            RestDayPicker(settings.restDays) { days -> onUpdate { it.copy(restDays = days) } }
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.settings_rest_days_hint), style = MaterialTheme.typography.bodySmall, color = FocusColors.TextTertiary)
        }

        Section(stringResource(R.string.settings_section_automation)) {
            Toggle(stringResource(R.string.settings_auto_breaks), settings.autoStartBreaks) { v -> onUpdate { it.copy(autoStartBreaks = v) } }
            Divider()
            Toggle(stringResource(R.string.settings_auto_focus), settings.autoStartFocus) { v -> onUpdate { it.copy(autoStartFocus = v) } }
        }

        Section(stringResource(R.string.settings_section_appearance)) {
            Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(10.dp))
            SegmentedToggle(
                options = listOf(
                    stringResource(R.string.settings_theme_system),
                    stringResource(R.string.settings_theme_dark),
                    stringResource(R.string.settings_theme_oled),
                    stringResource(R.string.settings_theme_light),
                ),
                selectedIndex = settings.themeMode.ordinal,
                onSelect = { i -> onUpdate { it.copy(themeMode = ThemeMode.entries[i]) } },
                modifier = Modifier.fillMaxWidth(),
                fill = true,
            )
            if (settings.themeMode == ThemeMode.OLED) {
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.settings_theme_oled_hint), style = MaterialTheme.typography.bodySmall, color = FocusColors.TextTertiary)
            }
            Divider()
            Text(stringResource(R.string.settings_timer_style), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(12.dp))
            TimerStylePicker(settings.timerStyle) { style -> onUpdate { it.copy(timerStyle = style) } }
            Divider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.settings_accent), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(stringResource(accentName(settings.accent)), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(14.dp))
            AccentPicker(settings.accent) { a -> onUpdate { it.copy(accent = a) } }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.settings_accent_hint), style = MaterialTheme.typography.bodySmall, color = FocusColors.TextTertiary)
        }

        Section(stringResource(R.string.settings_section_language)) {
            SegmentedToggle(
                options = listOf(stringResource(R.string.settings_lang_system), "Português", "English"),
                selectedIndex = languageIndex,
                onSelect = onLanguage,
            )
        }

        Section(stringResource(R.string.settings_section_reminders)) {
            Toggle(stringResource(R.string.settings_remind_routine), reminders.routine) { v -> onUpdateReminders { it.copy(routine = v) } }
            Divider()
            Toggle(stringResource(R.string.settings_remind_reviews), reminders.reviews) { v -> onUpdateReminders { it.copy(reviews = v) } }
            if (reminders.reviews) {
                Spacer(Modifier.height(10.dp))
                Stepper(
                    stringResource(R.string.settings_remind_reviews_time),
                    "%02d:%02d".format(reminders.reviewMinute / 60, reminders.reviewMinute % 60),
                    reminders.reviewMinute,
                    0..(23 * 60 + 30),
                    30,
                ) { v -> onUpdateReminders { it.copy(reviewMinute = v) } }
            }
            Divider()
            Toggle(stringResource(R.string.settings_remind_weekly), reminders.weeklySummary) { v -> onUpdateReminders { it.copy(weeklySummary = v) } }
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.settings_reminders_hint), style = MaterialTheme.typography.bodySmall, color = FocusColors.TextTertiary)
        }

        Section(stringResource(R.string.settings_section_protection)) {
            ActionRow(FocusIcons.Shield, stringResource(R.string.settings_blocker), stringResource(R.string.settings_blocker_desc), onOpenBlocker)
        }

        Section(stringResource(R.string.settings_section_data)) {
            ActionRow(FocusIcons.Upload, stringResource(R.string.settings_export), stringResource(R.string.settings_export_desc), onExport)
            Divider()
            ActionRow(FocusIcons.Download, stringResource(R.string.settings_import), stringResource(R.string.settings_import_desc), onImport)
        }

        Section(stringResource(R.string.settings_section_privacy)) {
            Text(stringResource(R.string.settings_privacy_text), style = MaterialTheme.typography.bodyMedium, color = FocusColors.TextSecondary)
            Spacer(Modifier.height(12.dp))
            ActionRow(FocusIcons.Lock, stringResource(R.string.settings_privacy_policy), stringResource(R.string.settings_privacy_policy_desc), onOpenPrivacy)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { confirmClear = true }, contentPadding = PaddingValues(0.dp)) {
                Text(stringResource(R.string.settings_clear), color = FocusColors.Error, style = MaterialTheme.typography.labelLarge)
            }
        }

        Section(stringResource(R.string.settings_section_about)) {
            ActionRow(FocusIcons.Book, stringResource(R.string.settings_licenses), stringResource(R.string.settings_licenses_desc), onOpenLicenses)
        }

        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.settings_version, APP_VERSION),
            style = MaterialTheme.typography.bodySmall,
            color = FocusColors.TextTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = FocusColors.SurfaceRaised,
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.settings_clear_title), style = MaterialTheme.typography.titleLarge) },
            text = { Text(stringResource(R.string.settings_clear_text), style = MaterialTheme.typography.bodyMedium, color = FocusColors.TextSecondary) },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; onClearHistory() }) { Text(stringResource(R.string.settings_delete), color = FocusColors.Error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.settings_cancel), color = FocusColors.TextPrimary) }
            },
        )
    }
}

private const val APP_VERSION = "0.9.1"

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    SectionLabel(title)
    Spacer(Modifier.height(8.dp))
    FocusCard(Modifier.fillMaxWidth()) { content() }
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun Divider() = HorizontalDivider(Modifier.padding(vertical = 12.dp), color = FocusColors.Outline)

@Composable
private fun Stepper(label: String, valueText: String, value: Int, range: IntRange, step: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostIconButton(FocusIcons.Minus, stringResource(R.string.settings_decrease, label), { onChange((value - step).coerceIn(range)) }, Modifier.size(36.dp), enabled = value > range.first)
            Text(valueText, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 64.dp))
            GhostIconButton(FocusIcons.Plus, stringResource(R.string.settings_increase, label), { onChange((value + step).coerceIn(range)) }, Modifier.size(36.dp), enabled = value < range.last)
        }
    }
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = FocusColors.OnAccent,
                checkedTrackColor = FocusColors.Accent,
                uncheckedThumbColor = FocusColors.TextSecondary,
                uncheckedTrackColor = FocusColors.SurfaceHigh,
                uncheckedBorderColor = FocusColors.Outline,
            ),
        )
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, description: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(FocusColors.SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = FocusColors.TextPrimary, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(2.dp))
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentPicker(selected: AccentColor, onSelect: (AccentColor) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AccentColor.entries.forEach { a ->
            val color = a.toColor()
            val isSelected = a == selected
            val name = stringResource(accentName(a))
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, if (isSelected) color else Color.Transparent, CircleShape)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable(onClickLabel = name) { onSelect(a) },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) Icon(FocusIcons.Check, contentDescription = name, tint = FocusColors.OnAccent, modifier = Modifier.size(16.dp))
            }
        }
    }
}

private fun accentName(a: AccentColor): Int = when (a) {
    AccentColor.BLUE -> R.string.accent_blue
    AccentColor.INDIGO -> R.string.accent_indigo
    AccentColor.VIOLET -> R.string.accent_violet
    AccentColor.CYAN -> R.string.accent_cyan
    AccentColor.TEAL -> R.string.accent_teal
    AccentColor.GREEN -> R.string.accent_green
    AccentColor.AMBER -> R.string.accent_amber
    AccentColor.CORAL -> R.string.accent_coral
    AccentColor.ROSE -> R.string.accent_rose
}

/** Five small live previews of the timer faces; the selected one gets a tinted backdrop. */
@Composable
private fun TimerStylePicker(selected: TimerStyle, onSelect: (TimerStyle) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TimerStyle.entries.forEach { style ->
            val isSelected = style == selected
            val bg by animateColorAsState(if (isSelected) FocusColors.AccentDim else Color.Transparent, tween(200), label = "styleBg")
            val name = stringResource(timerStyleName(style))
            Column(
                Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.small)
                    .background(bg)
                    .clickable(onClickLabel = name) { onSelect(style) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TimerFace(style, TIMER_PREVIEW_PROGRESS, Modifier.size(48.dp), compact = true) {
                    Text("25", style = MaterialTheme.typography.labelSmall, color = FocusColors.TextPrimary)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    name,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                    color = if (isSelected) FocusColors.TextPrimary else FocusColors.TextSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun timerStyleName(style: TimerStyle): Int = when (style) {
    TimerStyle.RING -> R.string.timer_style_ring
    TimerStyle.TICKS -> R.string.timer_style_ticks
    TimerStyle.GAUGE -> R.string.timer_style_gauge
    TimerStyle.DIAL -> R.string.timer_style_dial
    TimerStyle.MINIMAL -> R.string.timer_style_minimal
}

/** Up to [TimerSettings.MAX_REST_DAYS] weekdays; once full, the other days are dimmed. */
@Composable
private fun RestDayPicker(selected: Set<java.time.DayOfWeek>, onChange: (Set<java.time.DayOfWeek>) -> Unit) {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val full = selected.size >= TimerSettings.MAX_REST_DAYS
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        java.time.DayOfWeek.entries.forEach { d ->
            val on = d in selected
            val enabled = on || !full
            val bg by animateColorAsState(if (on) FocusColors.Accent else FocusColors.SurfaceHigh, tween(180), label = "restDay")
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(bg)
                    .clickable(enabled = enabled, onClickLabel = d.getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, locale)) {
                        onChange(if (on) selected - d else selected + d)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    d.getDisplayName(java.time.format.TextStyle.NARROW_STANDALONE, locale),
                    style = MaterialTheme.typography.labelLarge,
                    color = when {
                        on -> FocusColors.OnAccent
                        enabled -> FocusColors.TextSecondary
                        else -> FocusColors.TextTertiary.copy(alpha = 0.5f)
                    },
                )
            }
        }
    }
}
