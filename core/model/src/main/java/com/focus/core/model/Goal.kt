package com.focus.core.model

import java.time.LocalDate

/**
 * A long-term goal such as "100 h of English by March". When [subjectId] is null,
 * every focus session counts toward it.
 */
data class Goal(
    val id: Long = 0,
    val title: String,
    val subjectId: Long? = null,
    val targetMinutes: Int,
    val startDate: LocalDate,
    val deadline: LocalDate,
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val MAX_TITLE_LENGTH = 60
        val TARGET_HOURS_RANGE = 1..2000
    }
}

enum class GoalStatus { AHEAD, ON_TRACK, BEHIND, COMPLETED, EXPIRED, NOT_STARTED }

data class GoalProgress(
    val goal: Goal,
    val subject: Subject?,
    val doneSeconds: Long,
    val daysElapsed: Int,
    val daysRemaining: Int,
    /** Projected total at the deadline if the current pace holds. */
    val projectedSeconds: Long,
    /** Date the target is reached at the current pace, null when there is no pace yet. */
    val projectedFinish: LocalDate?,
    /** Focus needed per remaining day to finish on time. */
    val neededSecondsPerDay: Long,
    val status: GoalStatus,
) {
    val targetSeconds: Long get() = goal.targetMinutes * 60L
    val fraction: Float get() = (doneSeconds.toFloat() / targetSeconds).coerceIn(0f, 1f)
}
