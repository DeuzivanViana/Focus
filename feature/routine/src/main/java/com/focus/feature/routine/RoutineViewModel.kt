package com.focus.feature.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focus.core.data.repository.PlanRepository
import com.focus.core.data.repository.SubjectRepository
import com.focus.core.model.RoutineBlock
import com.focus.core.model.Subject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoutineUiState(
    val blocks: List<RoutineBlock> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val loaded: Boolean = false,
)

@HiltViewModel
class RoutineViewModel @Inject constructor(
    private val plan: PlanRepository,
    subjects: SubjectRepository,
) : ViewModel() {
    val uiState: StateFlow<RoutineUiState> =
        combine(plan.observeBlocks(), subjects.observeSubjects()) { b, s -> RoutineUiState(b, s, loaded = true) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoutineUiState())

    fun save(block: RoutineBlock) = viewModelScope.launch { runCatching { plan.saveBlock(block) } }
    fun delete(id: Long) = viewModelScope.launch { plan.deleteBlock(id) }
}
