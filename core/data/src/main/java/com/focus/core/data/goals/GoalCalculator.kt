package com.focus.core.data.goals

import com.focus.core.model.FocusSession
import com.focus.core.model.Goal
import com.focus.core.model.GoalProgress
import com.focus.core.model.GoalStatus
import com.focus.core.model.Subject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

/** Progress and pace forecast for long-term goals. Pure, so it is easy to test. */
object GoalCalculator {

    /** Within ±5% of the required pace counts as on track. */
    private const val TOLERANCE = 0.05

    fun progress(goal: Goal, subject: Subject?, sessions: List<FocusSession>, today: LocalDate, zone: ZoneId): GoalProgress {
        val done = sessions
            .filter { goal.subjectId == null || it.subjectId == goal.subjectId }
            .filter {
                val d = Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate()
                !d.isBefore(goal.startDate) && !d.isAfter(goal.deadline)
            }
            .sumOf { it.focusedSeconds }

        val target = goal.targetMinutes * 60L
        val totalDays = ChronoUnit.DAYS.between(goal.startDate, goal.deadline).toInt() + 1
        val elapsed = (ChronoUnit.DAYS.between(goal.startDate, today).toInt() + 1).coerceIn(0, totalDays)
        val remainingDays = (ChronoUnit.DAYS.between(today, goal.deadline).toInt() + 1).coerceIn(0, totalDays)

        val pacePerDay = if (elapsed > 0) done.toDouble() / elapsed else 0.0
        // Today is already part of the elapsed pace, so only project over the days after it.
        val daysAfterToday = (remainingDays - 1).coerceAtLeast(0)
        val projected = (done + pacePerDay * daysAfterToday).toLong()
            .let { if (today.isBefore(goal.startDate)) done else it }
        val projectedFinish = when {
            done >= target -> today
            pacePerDay <= 0.0 -> null
            else -> today.plusDays(ceil((target - done) / pacePerDay).toLong())
        }
        val needed = if (remainingDays > 0) ((target - done).coerceAtLeast(0) + remainingDays - 1) / remainingDays else 0L

        val status = when {
            done >= target -> GoalStatus.COMPLETED
            today.isAfter(goal.deadline) -> GoalStatus.EXPIRED
            today.isBefore(goal.startDate) || done == 0L -> GoalStatus.NOT_STARTED
            projected >= target * (1 + TOLERANCE) -> GoalStatus.AHEAD
            projected >= target * (1 - TOLERANCE) -> GoalStatus.ON_TRACK
            else -> GoalStatus.BEHIND
        }
        return GoalProgress(goal, subject, done, elapsed, remainingDays, projected, projectedFinish, needed, status)
    }
}
