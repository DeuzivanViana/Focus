package com.focus.feature.blocker

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val BLOCKER_ROUTE = "blocker"

fun NavGraphBuilder.blockerScreen(onBack: () -> Unit) {
    composable(BLOCKER_ROUTE) { BlockerRoute(onBack = onBack) }
}
