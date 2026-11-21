package com.focus.core.data.repository

import com.focus.core.database.PlanDao
import com.focus.core.database.asEntity
import com.focus.core.database.asModel
import com.focus.core.model.ReviewItem
import com.focus.core.model.RoutineBlock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Weekly routine blocks and spaced reviews. Input is validated before it reaches the database. */
@Singleton
class PlanRepository @Inject constructor(private val dao: PlanDao) {

    fun observeBlocks(): Flow<List<RoutineBlock>> = dao.observeBlocks().map { l -> l.map { it.asModel() } }
    fun observeReviews(): Flow<List<ReviewItem>> = dao.observeReviews().map { l -> l.map { it.asModel() } }
    suspend fun getBlocks(): List<RoutineBlock> = dao.getBlocks().map { it.asModel() }
    suspend fun getReviews(): List<ReviewItem> = dao.getReviews().map { it.asModel() }

    suspend fun saveBlock(block: RoutineBlock) {
        val clean = validate(block)
        if (clean.id == 0L) dao.insertBlock(clean.asEntity()) else dao.updateBlock(clean.asEntity())
    }

    suspend fun deleteBlock(id: Long) = dao.deleteBlock(id)

    suspend fun saveReview(review: ReviewItem) {
        val clean = validate(review)
        if (clean.id == 0L) dao.insertReview(clean.asEntity()) else dao.updateReview(clean.asEntity())
    }

    /** Marks the current review step as done and moves to the next interval. */
    suspend fun completeReviewStep(review: ReviewItem) {
        if (!review.isFinished) dao.updateReview(review.copy(stage = review.stage + 1).asEntity())
    }

    suspend fun deleteReview(id: Long) = dao.deleteReview(id)

    private fun cleanTitle(raw: String, max: Int): String {
        val t = raw.filterNot { it.isISOControl() }.trim().take(max)
        require(t.isNotEmpty()) { "Title must not be blank" }
        return t
    }

    private fun validate(b: RoutineBlock): RoutineBlock {
        require(b.days.isNotEmpty()) { "At least one day" }
        require(b.startMinute in 0 until 24 * 60) { "Invalid start" }
        return b.copy(
            title = cleanTitle(b.title, RoutineBlock.MAX_TITLE_LENGTH),
            durationMinutes = b.durationMinutes.coerceIn(RoutineBlock.DURATION_RANGE),
            remindBefore = b.remindBefore?.takeIf { it in RoutineBlock.REMIND_OPTIONS },
        )
    }

    private fun validate(r: ReviewItem): ReviewItem =
        r.copy(title = cleanTitle(r.title, ReviewItem.MAX_TITLE_LENGTH), stage = r.stage.coerceIn(0, ReviewItem.INTERVALS.size))
}
