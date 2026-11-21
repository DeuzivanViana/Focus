package com.focus.feature.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focus.core.data.repository.GoalRepository
import com.focus.core.data.repository.SubjectRepository
import com.focus.core.model.Goal
import com.focus.core.model.GoalProgress
import com.focus.core.model.Subject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalsUiState(
    val goals: List<GoalProgress> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val loaded: Boolean = false,
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val repository: GoalRepository,
    subjects: SubjectRepository,
) : ViewModel() {

    val uiState: StateFlow<GoalsUiState> =
        combine(repository.observeProgress(), subjects.observeSubjects()) { goals, subs ->
            GoalsUiState(goals = goals.sortedWith(GOAL_ORDER), subjects = subs, loaded = true)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalsUiState())

    fun save(goal: Goal) = launchSafely { repository.save(goal) }
    fun delete(id: Long) = launchSafely { repository.delete(id) }

    /** Invalid input is rejected by the repository; ignore it silently. */
    private fun launchSafely(block: suspend () -> Unit) {
        viewModelScope.launch { runCatching { block() } }
    }

    private companion object {
        /** Active goals first (closest deadline first), then finished ones. */
        val GOAL_ORDER = compareBy<GoalProgress>({ it.isFinished }, { it.goal.deadline })
    }
}

internal val GoalProgress.isFinished: Boolean
    get() = status == com.focus.core.model.GoalStatus.COMPLETED || status == com.focus.core.model.GoalStatus.EXPIRED
