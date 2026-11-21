package com.focus.feature.reflection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.focus.core.designsystem.icon.FocusIcons
import com.focus.core.designsystem.theme.FocusColors
import com.focus.core.model.FocusSession

/** 1–5 stars. Read-only when [onRate] is null. */
@Composable
internal fun RatingRow(rating: Int, onRate: ((Int) -> Unit)?, starSize: Dp = 36.dp, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(if (onRate != null) 4.dp else 1.dp)) {
        FocusSession.RATING_RANGE.forEach { value ->
            val filled = value <= rating
            val description = stringResource(R.string.reflection_cd_rate, value)
            Icon(
                if (filled) FocusIcons.StarFilled else FocusIcons.Star,
                contentDescription = if (onRate != null) description else null,
                tint = if (filled) FocusColors.Accent else FocusColors.TextTertiary,
                modifier = Modifier
                    .size(starSize)
                    .clip(MaterialTheme.shapes.small)
                    .then(if (onRate != null) Modifier.clickable { onRate(value) } else Modifier)
                    .padding(if (onRate != null) 6.dp else 0.dp),
            )
        }
    }
}
