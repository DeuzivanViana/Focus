package com.focus.feature.reflection

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val INSIGHTS_ROUTE = "insights"

fun NavGraphBuilder.insightsScreen(onBack: () -> Unit) {
    composable(INSIGHTS_ROUTE) { InsightsRoute(onBack = onBack) }
}
