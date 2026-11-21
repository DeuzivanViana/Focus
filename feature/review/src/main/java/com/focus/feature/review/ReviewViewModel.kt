package com.focus.feature.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focus.core.data.repository.PlanRepository
import com.focus.core.data.repository.SubjectRepository
import com.focus.core.model.ReviewItem
import com.focus.core.model.Subject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewUiState(
    val reviews: List<ReviewItem> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val loaded: Boolean = false,
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val plan: PlanRepository,
    subjects: SubjectRepository,
) : ViewModel() {
    val uiState: StateFlow<ReviewUiState> =
        combine(plan.observeReviews(), subjects.observeSubjects()) { r, s -> ReviewUiState(r, s, loaded = true) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewUiState())

    fun add(review: ReviewItem) = viewModelScope.launch { runCatching { plan.saveReview(review) } }
    fun complete(review: ReviewItem) = viewModelScope.launch { plan.completeReviewStep(review) }
    fun delete(id: Long) = viewModelScope.launch { plan.deleteReview(id) }
}
