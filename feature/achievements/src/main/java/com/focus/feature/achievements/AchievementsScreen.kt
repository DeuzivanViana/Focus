package com.focus.feature.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focus.core.designsystem.component.FocusCard
import com.focus.core.designsystem.component.LinearMeter
import com.focus.core.designsystem.component.ProgressRing
import com.focus.core.designsystem.component.SectionLabel
import com.focus.core.designsystem.component.ToolIconButton
import com.focus.core.designsystem.icon.FocusIcons
import com.focus.core.designsystem.theme.FocusColors
import com.focus.core.model.AchievementProgress
import com.focus.core.model.GamificationState

@Composable
fun AchievementsRoute(onBack: () -> Unit, viewModel: AchievementsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AchievementsScreen(state, onBack)
}

@Composable
fun AchievementsScreen(state: GamificationState, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ToolIconButton(FocusIcons.ArrowLeft, stringResource(R.string.achievements_cd_back), onBack)
            Spacer(Modifier.width(14.dp))
            Text(stringResource(R.string.achievements_title), style = MaterialTheme.typography.headlineLarge)
        }
        Spacer(Modifier.height(20.dp))

        FocusCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                    ProgressRing(state.levelProgress, Modifier.fillMaxSize())
                    Text("${state.level}", style = MaterialTheme.typography.headlineMedium)
                }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.achievements_level, state.level), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.achievements_xp_total, state.xp.toInt()), style = MaterialTheme.typography.bodySmall)
                    Text(
                        stringResource(R.string.achievements_xp_next, (state.nextLevelXp - state.xp).toInt(), state.level + 1),
                        style = MaterialTheme.typography.bodySmall,
                        color = FocusColors.TextTertiary,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            SectionLabel(stringResource(R.string.achievements_how))
            Spacer(Modifier.height(8.dp))
            listOf(R.string.achievements_rule_minute, R.string.achievements_rule_pomodoro, R.string.achievements_rule_goal).forEach {
                Text("·  " + stringResource(it), style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionLabel(stringResource(R.string.achievements_unlocked, state.unlockedCount, state.achievements.size))
        Spacer(Modifier.height(10.dp))
        // Unlocked first, then the closest to unlocking.
        val ordered = state.achievements.sortedWith(compareByDescending<AchievementProgress> { it.unlocked }.thenByDescending { it.fraction })
        ordered.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { AchievementTile(it, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun AchievementTile(p: AchievementProgress, modifier: Modifier) {
    val text = p.achievement.text()
    FocusCard(modifier) {
        Box(
            Modifier
                .size(36.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(if (p.unlocked) FocusColors.AccentDim else FocusColors.SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (p.unlocked) FocusIcons.Trophy else FocusIcons.Lock,
                contentDescription = null,
                tint = if (p.unlocked) FocusColors.Accent else FocusColors.TextTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(text.title),
            style = MaterialTheme.typography.titleSmall,
            color = if (p.unlocked) FocusColors.TextPrimary else FocusColors.TextSecondary,
        )
        Spacer(Modifier.height(2.dp))
        Text(stringResource(text.description), style = MaterialTheme.typography.bodySmall, minLines = 2)
        Spacer(Modifier.height(10.dp))
        LinearMeter(p.fraction)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.achievements_progress, minOf(p.current, p.achievement.target), p.achievement.target),
            style = MaterialTheme.typography.labelSmall,
            color = FocusColors.TextTertiary,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
