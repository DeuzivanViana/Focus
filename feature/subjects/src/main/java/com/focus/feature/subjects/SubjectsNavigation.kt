package com.focus.feature.subjects

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val SUBJECTS_ROUTE = "subjects"

fun NavGraphBuilder.subjectsScreen() {
    composable(SUBJECTS_ROUTE) { SubjectsRoute() }
}
