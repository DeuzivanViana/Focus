package com.focus.core.data.repository

import com.focus.core.data.stats.StatsCalculator
import com.focus.core.model.FocusStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

interface StatsRepository {
    fun observeStats(): Flow<FocusStats>
}

internal class DefaultStatsRepository @Inject constructor(
    private val sessions: SessionRepository,
    private val settings: SettingsRepository,
    private val subjects: SubjectRepository,
) : StatsRepository {
    override fun observeStats(): Flow<FocusStats> =
        combine(sessions.observeSessions(), settings.settings, subjects.observeSubjects()) { list, s, subs ->
            val zone = ZoneId.systemDefault()
            StatsCalculator.compute(list, s.dailyGoalMinutes, LocalDate.now(zone), zone, subs, s.restDays)
        }.flowOn(Dispatchers.Default)
}
