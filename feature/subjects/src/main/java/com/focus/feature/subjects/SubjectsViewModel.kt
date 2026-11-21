package com.focus.feature.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focus.core.data.repository.StatsRepository
import com.focus.core.data.repository.SubjectRepository
import com.focus.core.model.AccentColor
import com.focus.core.model.StudyTask
import com.focus.core.model.Subject
import com.focus.core.model.SubjectTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubjectItem(val time: SubjectTime, val tasks: List<StudyTask>)

data class SubjectsUiState(
    val items: List<SubjectItem> = emptyList(),
    val loaded: Boolean = false,
)

@HiltViewModel
class SubjectsViewModel @Inject constructor(
    private val repository: SubjectRepository,
    stats: StatsRepository,
) : ViewModel() {

    val uiState: StateFlow<SubjectsUiState> =
        combine(stats.observeStats(), repository.observeTasks()) { s, tasks ->
            val bySubject = tasks.groupBy { it.subjectId }
            SubjectsUiState(
                items = s.subjectTimes.map { t -> SubjectItem(t, t.subject?.let { bySubject[it.id] }.orEmpty()) },
                loaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubjectsUiState())

    fun addSubject(name: String, color: AccentColor) = launchSafely { repository.addSubject(name, color) }
    fun updateSubject(subject: Subject) = launchSafely { repository.updateSubject(subject) }
    fun deleteSubject(id: Long) = launchSafely { repository.deleteSubject(id) }
    fun addTask(subjectId: Long, title: String) = launchSafely { repository.addTask(subjectId, title) }
    fun toggleTask(task: StudyTask) = launchSafely { repository.updateTask(task.copy(done = !task.done)) }
    fun deleteTask(id: Long) = launchSafely { repository.deleteTask(id) }

    /** Blank names are rejected by the repository; ignore those silently. */
    private fun launchSafely(block: suspend () -> Unit) {
        viewModelScope.launch { runCatching { block() } }
    }
}
