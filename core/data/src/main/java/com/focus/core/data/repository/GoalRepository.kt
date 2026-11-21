package com.focus.core.data.repository

import com.focus.core.data.goals.GoalCalculator
import com.focus.core.database.GoalDao
import com.focus.core.database.asEntity
import com.focus.core.database.asModel
import com.focus.core.model.Goal
import com.focus.core.model.GoalProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepository @Inject constructor(
    private val dao: GoalDao,
    private val sessions: SessionRepository,
    private val subjects: SubjectRepository,
) {
    fun observeProgress(): Flow<List<GoalProgress>> =
        combine(dao.observeGoals(), sessions.observeSessions(), subjects.observeSubjects()) { goals, list, subs ->
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val byId = subs.associateBy { it.id }
            goals.map { g -> GoalCalculator.progress(g.asModel(), g.subjectId?.let(byId::get), list, today, zone) }
        }.flowOn(Dispatchers.Default)

    suspend fun save(goal: Goal) {
        val clean = validate(goal)
        if (clean.id == 0L) dao.insert(clean.asEntity()) else dao.update(clean.asEntity())
    }

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun getGoals(): List<Goal> = dao.getGoals().map { it.asModel() }

    private fun validate(goal: Goal): Goal {
        val title = goal.title.filterNot { it.isISOControl() }.trim().take(Goal.MAX_TITLE_LENGTH)
        require(title.isNotEmpty()) { "Title must not be blank" }
        require(!goal.deadline.isBefore(goal.startDate)) { "Deadline before start" }
        val maxMinutes = Goal.TARGET_HOURS_RANGE.last * 60
        return goal.copy(title = title, targetMinutes = goal.targetMinutes.coerceIn(60, maxMinutes))
    }
}
