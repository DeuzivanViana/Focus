package com.focus.app.navigation

import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import com.focus.app.R
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.focus.core.designsystem.icon.FocusIcons
import com.focus.core.designsystem.theme.FocusColors
import com.focus.feature.achievements.ACHIEVEMENTS_ROUTE
import com.focus.feature.achievements.achievementsScreen
import com.focus.feature.focus.FOCUS_ROUTE
import com.focus.feature.blocker.BLOCKER_ROUTE
import com.focus.feature.blocker.blockerScreen
import com.focus.feature.reflection.INSIGHTS_ROUTE
import com.focus.feature.reflection.ReflectionPromptHost
import com.focus.feature.reflection.insightsScreen
import com.focus.feature.focus.focusScreen
import com.focus.feature.settings.SETTINGS_ROUTE
import com.focus.feature.settings.settingsScreen
import com.focus.feature.settings.PRIVACY_ROUTE
import com.focus.feature.settings.privacyPolicyScreen
import com.focus.feature.settings.licensesScreen
import com.focus.feature.settings.LICENSES_ROUTE
import com.focus.feature.stats.STATS_ROUTE
import com.focus.feature.stats.statsScreen

/** Top-level destinations. New feature = new entry here + a NavGraphBuilder extension. */
private enum class TopLevel(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    FOCUS(FOCUS_ROUTE, R.string.nav_focus, FocusIcons.Timer),
    PLAN(PLAN_ROUTE, R.string.nav_plan, FocusIcons.Book),
    STATS(STATS_ROUTE, R.string.nav_progress, FocusIcons.Chart),
    SETTINGS(SETTINGS_ROUTE, R.string.nav_settings, FocusIcons.Tune),
}

@Composable
fun FocusApp(onStartRequested: () -> Unit) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    Column(
        Modifier
            .fillMaxSize()
            .background(FocusColors.Background)
            .statusBarsPadding(),
    ) {
        NavHost(
            navController = navController,
            startDestination = FOCUS_ROUTE,
            modifier = Modifier.weight(1f),
            // Short cross-fade between screens: noticeable, never in the way.
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(160)) },
        ) {
            val openAchievements = { navController.navigate(ACHIEVEMENTS_ROUTE) { launchSingleTop = true } }
            focusScreen(onStartRequested, onOpenAchievements = openAchievements)
            val back: () -> Unit = { navController.popBackStack() }
            planScreen()
            statsScreen(
                onOpenAchievements = openAchievements,
                onOpenInsights = { navController.navigate(INSIGHTS_ROUTE) { launchSingleTop = true } },
            )
            settingsScreen(
                onOpenBlocker = { navController.navigate(BLOCKER_ROUTE) { launchSingleTop = true } },
                onOpenPrivacy = { navController.navigate(PRIVACY_ROUTE) { launchSingleTop = true } },
                onOpenLicenses = { navController.navigate(LICENSES_ROUTE) { launchSingleTop = true } },
            )
            privacyPolicyScreen(onBack = back)
            licensesScreen(onBack = back)
            achievementsScreen(onBack = back)
            insightsScreen(onBack = back)
            blockerScreen(onBack = back)
        }
        // Tonal bar instead of a divider line (Material 3 navigation bar).
        Row(
            Modifier
                .fillMaxWidth()
                .background(FocusColors.Surface)
                .navigationBarsPadding()
                .height(72.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopLevel.entries.forEach { dest ->
                val selected = current == dest.route
                NavItem(stringResource(dest.label), dest.icon, selected, Modifier.weight(1f)) {
                    if (!selected) {
                        navController.navigate(dest.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }
        }
    }
    // Post-session reflection appears over any screen right after a focus ends.
    ReflectionPromptHost()
}

@Composable
private fun NavItem(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val tint by animateColorAsState(if (selected) FocusColors.Accent else FocusColors.TextTertiary, tween(200), label = "navTint")
    val labelColor by animateColorAsState(if (selected) FocusColors.TextPrimary else FocusColors.TextTertiary, tween(200), label = "navLabel")
    val pill by animateColorAsState(if (selected) FocusColors.AccentDim else Color.Transparent, tween(220), label = "navPill")
    val pillWidth by animateDpAsState(if (selected) 56.dp else 32.dp, tween(220), label = "navPillWidth")
    Box(
        modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Active indicator: a soft pill that widens behind the selected icon.
            Box(Modifier.width(pillWidth).height(30.dp).clip(CircleShape).background(pill), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}
