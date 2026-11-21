package com.focus.feature.blocker

import androidx.compose.ui.unit.sp
import com.focus.core.designsystem.component.PrimaryButton
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focus.core.designsystem.component.FocusCard
import com.focus.core.designsystem.component.FocusTextField
import com.focus.core.designsystem.component.SectionLabel
import com.focus.core.designsystem.component.SegmentedToggle
import com.focus.core.designsystem.component.ToolIconButton
import com.focus.core.designsystem.icon.FocusIcons
import com.focus.core.designsystem.theme.FocusColors

@Composable
fun BlockerRoute(onBack: () -> Unit, viewModel: BlockerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermissions()
        onPauseOrDispose { }
    }
    BlockerScreen(
        state = state,
        onBack = onBack,
        onEnabled = { viewModel.setEnabled(it) },
        onMode = { viewModel.setMode(it) },
        onBlocked = { pkg, blocked -> viewModel.setBlocked(pkg, blocked) },
        onOpenUsageAccess = {
            context.openSettings(BlockerPermissions.usageAccessIntent(context), BlockerPermissions.usageAccessFallbackIntent())
        },
        onOpenOverlay = { context.openSettings(BlockerPermissions.overlayIntent(context), null) },
        onOpenAppInfo = { context.openSettings(BlockerPermissions.appInfoIntent(context), Intent(android.provider.Settings.ACTION_SETTINGS)) },
    )
}

private fun Context.openSettings(intent: Intent, fallback: Intent?) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        fallback?.let { runCatching { startActivity(it) } }
    }
}

/** Which explanation is on screen. Each one is shown before the system settings page opens. */
private enum class Explain { USAGE, OVERLAY }

@Composable
fun BlockerScreen(
    state: BlockerUiState,
    onBack: () -> Unit,
    onEnabled: (Boolean) -> Unit,
    onMode: (BlockMode) -> Unit,
    onBlocked: (String, Boolean) -> Unit,
    onOpenUsageAccess: () -> Unit,
    onOpenOverlay: () -> Unit,
    onOpenAppInfo: () -> Unit = {},
    initialExplain: Int = -1,
) {
    var explain by rememberSaveable { mutableStateOf(Explain.entries.getOrNull(initialExplain)) }
    var query by rememberSaveable { mutableStateOf("") }
    val s = state.settings
    val p = state.permissions

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ToolIconButton(FocusIcons.ArrowLeft, stringResource(R.string.blocker_cd_back), onBack)
            Spacer(Modifier.width(14.dp))
            Text(stringResource(R.string.blocker_title), style = MaterialTheme.typography.headlineLarge)
        }
        Spacer(Modifier.height(20.dp))

        FocusCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(FocusIcons.Shield, contentDescription = null, tint = FocusColors.Accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.blocker_switch), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(
                    // Works without Usage Access too (reminder when leaving Focus), so the
                    // switch no longer depends on it.
                    checked = s.enabled,
                    onCheckedChange = { on ->
                        onEnabled(on)
                        if (on && !p.usageAccess && !p.usageRestricted) explain = Explain.USAGE
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = FocusColors.OnAccent,
                        checkedTrackColor = FocusColors.Accent,
                        uncheckedThumbColor = FocusColors.TextSecondary,
                        uncheckedTrackColor = FocusColors.SurfaceHigh,
                        uncheckedBorderColor = FocusColors.Outline,
                    ),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.blocker_intro), style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(20.dp))

        SectionLabel(stringResource(R.string.blocker_section_permissions))
        Spacer(Modifier.height(8.dp))
        FocusCard(Modifier.fillMaxWidth()) {
            PermissionRow(
                title = stringResource(R.string.blocker_perm_usage),
                description = stringResource(R.string.blocker_perm_usage_short),
                granted = p.usageAccess,
                required = true,
                onGrant = { explain = Explain.USAGE },
            )
            if (!p.usageAccess && p.usageRestricted) {
                Spacer(Modifier.height(14.dp))
                RestrictedGuide(onOpenAppInfo = onOpenAppInfo, onOpenUsageAccess = onOpenUsageAccess)
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = FocusColors.Outline)
            PermissionRow(
                title = stringResource(R.string.blocker_perm_overlay),
                description = stringResource(R.string.blocker_perm_overlay_short),
                granted = p.blockScreen,
                required = false,
                onGrant = { explain = Explain.OVERLAY },
            )
        }
        Spacer(Modifier.height(20.dp))

        SectionLabel(stringResource(R.string.blocker_section_mode))
        Spacer(Modifier.height(8.dp))
        FocusCard(Modifier.fillMaxWidth()) {
            SegmentedToggle(
                options = listOf(stringResource(R.string.blocker_mode_warn), stringResource(R.string.blocker_mode_block)),
                selectedIndex = s.mode.ordinal,
                onSelect = { i ->
                    val mode = BlockMode.entries[i]
                    onMode(mode)
                    if (mode == BlockMode.BLOCK && !p.blockScreen) explain = Explain.OVERLAY
                },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(if (s.mode == BlockMode.WARN) R.string.blocker_mode_warn_desc else R.string.blocker_mode_block_desc),
                style = MaterialTheme.typography.bodySmall,
            )
            if (!p.usageAccess) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.blocker_without_usage), style = MaterialTheme.typography.bodySmall, color = FocusColors.Accent)
            }
            if (s.mode == BlockMode.BLOCK && !p.blockScreen) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.blocker_mode_block_fallback), style = MaterialTheme.typography.bodySmall, color = FocusColors.Error)
            }
        }
        Spacer(Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(stringResource(R.string.blocker_section_apps), Modifier.weight(1f))
            Text(stringResource(R.string.blocker_selected, s.packages.size), style = MaterialTheme.typography.labelMedium, color = FocusColors.TextTertiary)
        }
        Spacer(Modifier.height(8.dp))
        FocusTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = stringResource(R.string.blocker_search), maxLength = 40, containerColor = FocusColors.Surface)
        Spacer(Modifier.height(8.dp))
        FocusCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp)) {
            val apps = state.apps
            when {
                apps == null -> Text(stringResource(R.string.blocker_loading), Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall)
                apps.isEmpty() -> Text(stringResource(R.string.blocker_no_apps), Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall)
                else -> {
                    val q = query.trim().lowercase()
                    apps.filter { q.isEmpty() || it.label.lowercase().contains(q) }
                        .sortedByDescending { it.packageName in s.packages }
                        .forEach { app ->
                            val blocked = app.packageName in s.packages
                            AppRow(app, blocked) { onBlocked(app.packageName, !blocked) }
                        }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.blocker_privacy), style = MaterialTheme.typography.bodySmall, color = FocusColors.TextTertiary)
        Spacer(Modifier.height(16.dp))
    }

    when (explain) {
        Explain.USAGE -> ExplainDialog(
            title = stringResource(R.string.blocker_explain_usage_title),
            points = listOf(
                stringResource(R.string.blocker_explain_usage_what),
                stringResource(R.string.blocker_explain_usage_how),
                stringResource(R.string.blocker_explain_usage_privacy),
                stringResource(R.string.blocker_explain_revoke),
            ),
            steps = stringResource(if (p.usageRestricted) R.string.blocker_explain_usage_steps_restricted else R.string.blocker_explain_usage_steps),
            onConfirm = { explain = null; if (p.usageRestricted) onOpenAppInfo() else onOpenUsageAccess() },
            onDismiss = { explain = null },
        )
        Explain.OVERLAY -> ExplainDialog(
            title = stringResource(R.string.blocker_explain_overlay_title),
            points = listOf(
                stringResource(R.string.blocker_explain_overlay_what),
                stringResource(R.string.blocker_explain_overlay_how),
                stringResource(R.string.blocker_explain_revoke),
            ),
            steps = stringResource(R.string.blocker_explain_overlay_steps),
            onConfirm = { explain = null; onOpenOverlay() },
            onDismiss = { explain = null },
        )
        null -> Unit
    }
}

@Composable
private fun PermissionRow(title: String, description: String, granted: Boolean, required: Boolean, onGrant: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f, fill = false))
                if (!required) {
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.blocker_optional), style = MaterialTheme.typography.labelSmall, color = FocusColors.TextTertiary, maxLines = 1, softWrap = false)
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.width(12.dp))
        if (granted) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(FocusIcons.Check, contentDescription = null, tint = FocusColors.Accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.blocker_granted), style = MaterialTheme.typography.labelMedium, color = FocusColors.Accent)
            }
        } else {
            Box(
                Modifier
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(FocusColors.AccentDim)
                    .clickable(onClick = onGrant)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(stringResource(R.string.blocker_grant), style = MaterialTheme.typography.labelLarge, color = FocusColors.TextPrimary)
            }
        }
    }
}

@Composable
private fun AppRow(app: InstalledApp, blocked: Boolean, onToggle: () -> Unit) {
    val bitmap = remember(app.packageName) { app.icon?.let { runCatching { it.toBitmap(64, 64).asImageBitmap() }.getOrNull() } }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (bitmap != null) {
            Image(bitmap, contentDescription = null, modifier = Modifier.size(28.dp))
        } else {
            Box(Modifier.size(28.dp).clip(MaterialTheme.shapes.extraSmall).background(FocusColors.SurfaceHigh))
        }
        Spacer(Modifier.width(14.dp))
        Text(app.label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Icon(
            if (blocked) FocusIcons.BoxChecked else FocusIcons.Box,
            contentDescription = null,
            tint = if (blocked) FocusColors.Accent else FocusColors.TextTertiary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ExplainDialog(title: String, points: List<String>, steps: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FocusColors.SurfaceRaised,
        shape = MaterialTheme.shapes.large,
        icon = { Icon(FocusIcons.Shield, contentDescription = null, tint = FocusColors.Accent) },
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                points.forEach { point ->
                    Row {
                        Text("•", style = MaterialTheme.typography.bodyMedium, color = FocusColors.Accent)
                        Spacer(Modifier.width(8.dp))
                        Text(point, style = MaterialTheme.typography.bodyMedium, color = FocusColors.TextSecondary)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(steps, style = MaterialTheme.typography.bodySmall, color = FocusColors.TextTertiary)
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.blocker_open_settings), color = FocusColors.Accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.blocker_not_now), color = FocusColors.TextPrimary) } },
    )
}

/**
 * Android 13+ greys out Usage Access for apps installed from an APK ("Restricted settings").
 * Only the user can lift it, from the app's App info page; this walks them through it.
 */
@Composable
private fun RestrictedGuide(onOpenAppInfo: () -> Unit, onOpenUsageAccess: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(FocusColors.AccentDim)
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.blocker_restricted_title), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.blocker_restricted_text), style = MaterialTheme.typography.bodySmall, color = FocusColors.TextSecondary)
        Spacer(Modifier.height(12.dp))
        listOf(
            R.string.blocker_restricted_step1,
            R.string.blocker_restricted_step2,
            R.string.blocker_restricted_step3,
            R.string.blocker_restricted_step4,
        ).forEachIndexed { i, step ->
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(20.dp).clip(androidx.compose.foundation.shape.CircleShape).background(FocusColors.Accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${i + 1}", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp), color = FocusColors.OnAccent)
                }
                Spacer(Modifier.width(10.dp))
                Text(stringResource(step), style = MaterialTheme.typography.bodySmall, color = FocusColors.TextPrimary, modifier = Modifier.padding(top = 1.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PrimaryButton(stringResource(R.string.blocker_restricted_open_info), onOpenAppInfo, Modifier.height(44.dp))
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onOpenUsageAccess) { Text(stringResource(R.string.blocker_restricted_done), color = FocusColors.Accent) }
        }
    }
}
