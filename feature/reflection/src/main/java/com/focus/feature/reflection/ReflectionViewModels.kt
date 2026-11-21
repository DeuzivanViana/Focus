package com.focus.feature.reflection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focus.core.data.repository.ReflectionRepository
import com.focus.core.model.FocusSession
import com.focus.core.model.ReflectionInsights
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Drives the post-session prompt shown on top of the whole app. */
@HiltViewModel
class ReflectionPromptViewModel @Inject constructor(
    private val repository: ReflectionRepository,
) : ViewModel() {

    val pending: StateFlow<FocusSession?> =
        repository.observePending().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun submit(sessionId: Long, rating: Int, note: String) {
        viewModelScope.launch { runCatching { repository.submit(sessionId, rating, note) } }
    }

    fun skip(sessionId: Long) {
        viewModelScope.launch { repository.skip(sessionId) }
    }
}

@HiltViewModel
class InsightsViewModel @Inject constructor(repository: ReflectionRepository) : ViewModel() {
    val insights: StateFlow<ReflectionInsights?> =
        repository.observeInsights().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
