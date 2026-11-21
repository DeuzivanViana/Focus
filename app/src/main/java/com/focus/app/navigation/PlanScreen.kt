package com.focus.app.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.focus.app.R
import com.focus.core.designsystem.component.SegmentedToggle
import com.focus.feature.goals.GoalsRoute
import com.focus.feature.review.ReviewRoute
import com.focus.feature.routine.RoutineRoute
import androidx.compose.foundation.layout.fillMaxWidth
import com.focus.feature.subjects.SubjectsRoute

const val PLAN_ROUTE = "plan"

/** "Plan" tab: groups the planning features. New planning screens become new segments here. */
fun NavGraphBuilder.planScreen() {
    composable(PLAN_ROUTE) { PlanScreen() }
}

@Composable
private fun PlanScreen() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        SegmentedToggle(
            options = listOf(
                stringResource(R.string.plan_subjects),
                stringResource(R.string.plan_goals),
                stringResource(R.string.plan_routine),
                stringResource(R.string.plan_reviews),
            ),
            selectedIndex = tab,
            onSelect = { tab = it },
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp).fillMaxWidth(),
            fill = true,
        )
        Crossfade(tab, Modifier.weight(1f), animationSpec = tween(200), label = "plan") { selected ->
            Box(Modifier.fillMaxSize()) {
                when (selected) {
                    0 -> SubjectsRoute()
                    1 -> GoalsRoute()
                    2 -> RoutineRoute()
                    else -> ReviewRoute()
                }
            }
        }
    }
}
