package com.focus.feature.goals

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val GOALS_ROUTE = "goals"

fun NavGraphBuilder.goalsScreen() {
    composable(GOALS_ROUTE) { GoalsRoute() }
}
