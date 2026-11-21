package com.focus.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focus.core.data.repository.StatsRepository
import com.focus.core.data.repository.GamificationRepository
import com.focus.core.model.FocusStats
import com.focus.core.model.GamificationState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    statsRepository: StatsRepository,
    gamificationRepository: GamificationRepository,
) : ViewModel() {
    val stats: StateFlow<FocusStats?> = statsRepository.observeStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val gamification: StateFlow<GamificationState> = gamificationRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GamificationState())
}
