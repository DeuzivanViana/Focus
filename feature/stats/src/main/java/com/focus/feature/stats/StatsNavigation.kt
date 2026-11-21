package com.focus.feature.stats

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val STATS_ROUTE = "stats"

fun NavGraphBuilder.statsScreen(onOpenAchievements: () -> Unit, onOpenInsights: () -> Unit) {
    composable(STATS_ROUTE) { StatsRoute(onOpenAchievements = onOpenAchievements, onOpenInsights = onOpenInsights) }
}
