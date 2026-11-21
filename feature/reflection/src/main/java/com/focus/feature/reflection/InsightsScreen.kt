package com.focus.feature.reflection

import com.focus.core.designsystem.theme.toColor
import androidx.compose.foundation.background
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.focus.core.designsystem.component.SectionLabel
import com.focus.core.designsystem.component.StatTile
import com.focus.core.designsystem.component.ToolIconButton
import com.focus.core.designsystem.icon.FocusIcons
import com.focus.core.designsystem.theme.FocusColors
import com.focus.core.model.DayPart
import com.focus.core.model.FocusSession
import com.focus.core.model.ReflectionInsights
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun InsightsRoute(onBack: () -> Unit, viewModel: InsightsViewModel = hiltViewModel()) {
    val insights by viewModel.insights.collectAsStateWithLifecycle()
    InsightsScreen(insights, onBack)
}

@Composable
fun InsightsScreen(insights: ReflectionInsights?, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ToolIconButton(FocusIcons.ArrowLeft, stringResource(R.string.reflection_cd_back), onBack)
            Spacer(Modifier.width(14.dp))
            Text(stringResource(R.string.insights_title), style = MaterialTheme.typography.headlineLarge)
        }
        Spacer(Modifier.height(20.dp))
        if (insights == null) return@Column
        if (insights.ratedSessions == 0) {
            FocusCard(Modifier.fillMaxWidth()) {
                Icon(FocusIcons.Insight, contentDescription = null, tint = FocusColors.TextSecondary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.insights_empty), style = MaterialTheme.typography.bodyMedium, color = FocusColors.TextSecondary)
            }
            return@Column
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                stringResource(R.string.insights_average_30),
                insights.averageRating30Days?.let(::formatRating) ?: "–",
                Modifier.weight(1f),
                caption = stringResource(R.string.insights_out_of_5),
                highlight = true,
            )
            StatTile(stringResource(R.string.insights_rated), "${insights.ratedSessions}", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))

        insightSentence(insights)?.let { sentence ->
            FocusCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(FocusIcons.Insight, contentDescription = null, tint = FocusColors.Accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(sentence, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        FocusCard(Modifier.fillMaxWidth()) {
            SectionLabel(stringResource(R.string.insights_by_day_part))
            Spacer(Modifier.height(12.dp))
            DayPart.entries.forEach { part ->
                RatingBar(stringResource(part.label), insights.byDayPart[part], FocusColors.Accent, part == insights.bestDayPart)
            }
        }
        Spacer(Modifier.height(10.dp))

        if (insights.bySubject.isNotEmpty()) {
            FocusCard(Modifier.fillMaxWidth()) {
                SectionLabel(stringResource(R.string.insights_by_subject))
                Spacer(Modifier.height(12.dp))
                insights.bySubject.forEach { (subject, avg) ->
                    RatingBar(
                        subject?.name ?: stringResource(R.string.insights_no_subject),
                        avg,
                        subject?.let { it.color.toColor() } ?: FocusColors.TextTertiary,
                        highlight = false,
                        dot = true,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        FocusCard(Modifier.fillMaxWidth()) {
            SectionLabel(stringResource(R.string.insights_completion))
            Spacer(Modifier.height(12.dp))
            RatingBar(stringResource(R.string.insights_completed), insights.completedAverage, FocusColors.Accent, false)
            RatingBar(stringResource(R.string.insights_interrupted), insights.interruptedAverage, FocusColors.Comparison, false)
        }

        if (insights.recentNotes.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            FocusCard(Modifier.fillMaxWidth()) {
                SectionLabel(stringResource(R.string.insights_notes))
                Spacer(Modifier.height(4.dp))
                insights.recentNotes.forEachIndexed { i, s ->
                    if (i > 0) HorizontalDivider(color = FocusColors.Outline)
                    NoteRow(s)
                }
            }
        }
    }
}

@Composable
private fun insightSentence(i: ReflectionInsights): String? {
    val best = i.bestDayPart ?: return null
    if (i.byDayPart.size < 2) return null
    val worst = i.byDayPart.minByOrNull { it.value }!!
    if (i.byDayPart.getValue(best) - worst.value < 0.3f) return stringResource(R.string.insights_sentence_even)
    return stringResource(
        R.string.insights_sentence_best,
        stringResource(best.label).lowercase(),
        formatRating(i.byDayPart.getValue(best)),
        stringResource(worst.key.label).lowercase(),
    )
}

private val DayPart.label: Int
    get() = when (this) {
        DayPart.MORNING -> R.string.insights_morning
        DayPart.AFTERNOON -> R.string.insights_afternoon
        DayPart.EVENING -> R.string.insights_evening
        DayPart.NIGHT -> R.string.insights_night
    }

private fun formatRating(v: Float): String = "%.1f".format(v)

@Composable
private fun RatingBar(label: String, value: Float?, color: Color, highlight: Boolean, dot: Boolean = false) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.width(112.dp), verticalAlignment = Alignment.CenterVertically) {
            if (dot) {
                ColorDot(color, size = 8.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = if (highlight) FocusColors.TextPrimary else FocusColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(FocusColors.SurfaceHigh),
        ) {
            if (value != null) {
                Box(
                    Modifier
                        .fillMaxWidth(((value - 1f) / 4f).coerceIn(0.04f, 1f))
                        .height(6.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(if (highlight || dot) color else color.copy(alpha = 0.55f)),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            value?.let(::formatRating) ?: "–",
            style = MaterialTheme.typography.labelMedium,
            color = if (value == null) FocusColors.TextTertiary else FocusColors.TextPrimary,
            modifier = Modifier.width(28.dp),
        )
    }
}

@Composable
private fun NoteRow(s: FocusSession) {
    val locale = LocalConfiguration.current.locales[0]
    val fmt = remember(locale) { DateTimeFormatter.ofPattern("d MMM, HH:mm", locale) }
    Column(Modifier.padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                fmt.format(Instant.ofEpochMilli(s.startedAt).atZone(ZoneId.systemDefault())),
                style = MaterialTheme.typography.labelMedium,
                color = FocusColors.TextTertiary,
                modifier = Modifier.weight(1f),
            )
            RatingRow(s.rating ?: 0, onRate = null, starSize = 12.dp)
        }
        Spacer(Modifier.height(4.dp))
        Text(s.note.orEmpty(), style = MaterialTheme.typography.bodyMedium)
    }
}
