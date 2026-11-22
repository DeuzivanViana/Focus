package com.focus.app.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focus.core.designsystem.theme.FocusColors

data class BottomBarItem(val label: String, val icon: ImageVector)

/**
 * Floating navigation bar: a rounded tonal card that sits above the page, with one
 * highlight that slides between tabs on a soft spring and covers both icon and label.
 * The selected icon gives a small bounce and its label turns bold.
 * [selectedIndex] -1 (a detail screen) hides the highlight.
 */
@Composable
fun FocusBottomBar(items: List<BottomBarItem>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .background(FocusColors.Background)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(66.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(FocusColors.Surface)
                .padding(5.dp),
        ) {
            val slot = maxWidth / items.size
            val visible = selectedIndex in items.indices
            // Remembers the last tab so the highlight fades out in place on detail screens.
            val target = if (visible) selectedIndex else remember { 0 }
            val x by animateDpAsState(
                slot * target,
                spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
                label = "navHighlightX",
            )
            val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(200), label = "navHighlightAlpha")
            Box(
                Modifier
                    .offset(x = x)
                    .width(slot)
                    .fillMaxHeight()
                    .graphicsLayer { this.alpha = alpha }
                    .clip(RoundedCornerShape(21.dp))
                    .background(FocusColors.AccentDim),
            )
            Row(Modifier.fillMaxSize()) {
                items.forEachIndexed { i, item ->
                    BarItem(item, selected = i == selectedIndex, modifier = Modifier.weight(1f)) { onSelect(i) }
                }
            }
        }
    }
}

@Composable
private fun BarItem(item: BottomBarItem, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val tint by animateColorAsState(if (selected) FocusColors.Accent else FocusColors.TextTertiary, tween(220), label = "navTint")
    val labelColor by animateColorAsState(if (selected) FocusColors.TextPrimary else FocusColors.TextTertiary, tween(220), label = "navLabel")
    // A quick squash-and-settle when the tab becomes selected.
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected) {
            bounce.snapTo(0.82f)
            bounce.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium))
        }
    }
    val lift by animateDpAsState(if (selected) (-1).dp else 0.dp, spring(dampingRatio = 0.6f), label = "navLift")
    Column(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(21.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClickLabel = item.label,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Icon(
            item.icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .offset(y = lift)
                .size(21.dp)
                .graphicsLayer {
                    scaleX = bounce.value
                    scaleY = bounce.value
                },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            item.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 0.1.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}
