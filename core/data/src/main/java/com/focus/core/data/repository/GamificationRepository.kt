package com.focus.core.data.repository

import com.focus.core.data.gamification.GamificationCalculator
import com.focus.core.model.GamificationState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GamificationRepository @Inject constructor(
    private val sessions: SessionRepository,
    private val settings: SettingsRepository,
) {
    fun observe(): Flow<GamificationState> =
        combine(sessions.observeSessions(), settings.settings) { list, s ->
            GamificationCalculator.compute(list, s.dailyGoalMinutes, ZoneId.systemDefault(), s.restDays)
        }.distinctUntilChanged().flowOn(Dispatchers.Default)
}
