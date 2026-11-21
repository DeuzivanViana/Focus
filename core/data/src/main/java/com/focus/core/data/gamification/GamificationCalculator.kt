package com.focus.core.data.gamification

import com.focus.core.model.Achievement
import com.focus.core.model.AchievementMetric
import com.focus.core.model.AchievementProgress
import com.focus.core.model.FocusSession
import com.focus.core.model.GamificationState
import com.focus.core.data.stats.StatsCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * XP and achievements are derived from the session history (nothing extra is
 * stored), so they can never get out of sync and survive export/import.
 *
 * XP rules:
 *  - 1 XP per focused minute
 *  - +10 XP per completed pomodoro
 *  - +25 XP per day the daily goal was met
 *
 * Level n starts at 50 * n * (n - 1) XP: 0, 100, 300, 600, 1000...
 * (each level needs 100 XP more than the previous one).
 */
object GamificationCalculator {
    const val XP_PER_MINUTE = 1L
    const val XP_PER_POMODORO = 10L
    const val XP_PER_GOAL_DAY = 25L

    fun levelStart(level: Int): Long = 50L * level * (level - 1)

    fun levelFor(xp: Long): Int {
        var level = 1
        while (levelStart(level + 1) <= xp) level++
        return level
    }

    fun compute(sessions: List<FocusSession>, dailyGoalMinutes: Int, zone: ZoneId, restDays: Set<java.time.DayOfWeek> = emptySet()): GamificationState {
        val perDaySeconds = HashMap<LocalDate, Long>()
        val perDayPomodoros = HashMap<LocalDate, Int>()
        var early = 0
        var late = 0
        for (s in sessions) {
            val start = Instant.ofEpochMilli(s.startedAt).atZone(zone)
            val d = start.toLocalDate()
            perDaySeconds[d] = (perDaySeconds[d] ?: 0) + s.focusedSeconds
            if (s.completed) perDayPomodoros[d] = (perDayPomodoros[d] ?: 0) + 1
            if (start.hour < 7) early++
            if (start.hour >= 22) late++
        }
        val pomodoros = sessions.count { it.completed }
        val totalSeconds = sessions.sumOf { it.focusedSeconds }
        val goalSeconds = dailyGoalMinutes * 60L
        val goalDays = perDaySeconds.values.count { goalSeconds > 0 && it >= goalSeconds }

        val xp = totalSeconds / 60 * XP_PER_MINUTE + pomodoros * XP_PER_POMODORO + goalDays * XP_PER_GOAL_DAY
        val level = levelFor(xp)

        fun metric(m: AchievementMetric): Int = when (m) {
            AchievementMetric.POMODOROS -> pomodoros
            AchievementMetric.TOTAL_HOURS -> (totalSeconds / 3600).toInt()
            AchievementMetric.BEST_STREAK -> StatsCalculator.bestStreak(perDaySeconds.keys, restDays)
            AchievementMetric.GOAL_DAYS -> goalDays
            AchievementMetric.POMODOROS_IN_A_DAY -> perDayPomodoros.values.maxOrNull() ?: 0
            AchievementMetric.HOURS_IN_A_DAY -> ((perDaySeconds.values.maxOrNull() ?: 0) / 3600).toInt()
            AchievementMetric.EARLY_SESSIONS -> early
            AchievementMetric.LATE_SESSIONS -> late
        }
        val cache = AchievementMetric.entries.associateWith(::metric)

        return GamificationState(
            xp = xp,
            level = level,
            levelStartXp = levelStart(level),
            nextLevelXp = levelStart(level + 1),
            achievements = Achievement.entries.map { AchievementProgress(it, cache.getValue(it.metric)) },
        )
    }
}
