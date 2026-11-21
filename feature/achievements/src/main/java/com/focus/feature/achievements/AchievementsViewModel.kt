package com.focus.feature.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focus.core.data.repository.GamificationRepository
import com.focus.core.model.GamificationState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AchievementsViewModel @Inject constructor(repository: GamificationRepository) : ViewModel() {
    val state: StateFlow<GamificationState> = repository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GamificationState())
}
