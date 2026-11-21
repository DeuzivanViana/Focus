package com.focus.feature.focus

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val FOCUS_ROUTE = "focus"

fun NavGraphBuilder.focusScreen(onStartRequested: () -> Unit, onOpenAchievements: () -> Unit) {
    composable(FOCUS_ROUTE) { FocusRoute(onStartRequested = onStartRequested, onOpenAchievements = onOpenAchievements) }
}
