package com.focus.feature.achievements

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val ACHIEVEMENTS_ROUTE = "achievements"

fun NavGraphBuilder.achievementsScreen(onBack: () -> Unit) {
    composable(ACHIEVEMENTS_ROUTE) { AchievementsRoute(onBack = onBack) }
}
