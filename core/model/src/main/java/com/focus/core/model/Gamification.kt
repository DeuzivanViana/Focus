package com.focus.core.model

/** What an achievement measures. */
enum class AchievementMetric {
    POMODOROS,
    TOTAL_HOURS,
    BEST_STREAK,
    GOAL_DAYS,
    POMODOROS_IN_A_DAY,
    HOURS_IN_A_DAY,
    EARLY_SESSIONS,
    LATE_SESSIONS,
}

/** Milestones. The id is stable (it is persisted), so never rename an entry. */
enum class Achievement(val metric: AchievementMetric, val target: Int) {
    FIRST_FOCUS(AchievementMetric.POMODOROS, 1),
    POMODOROS_10(AchievementMetric.POMODOROS, 10),
    POMODOROS_50(AchievementMetric.POMODOROS, 50),
    POMODOROS_100(AchievementMetric.POMODOROS, 100),
    POMODOROS_500(AchievementMetric.POMODOROS, 500),
    HOURS_10(AchievementMetric.TOTAL_HOURS, 10),
    HOURS_50(AchievementMetric.TOTAL_HOURS, 50),
    HOURS_100(AchievementMetric.TOTAL_HOURS, 100),
    HOURS_500(AchievementMetric.TOTAL_HOURS, 500),
    STREAK_3(AchievementMetric.BEST_STREAK, 3),
    STREAK_7(AchievementMetric.BEST_STREAK, 7),
    STREAK_30(AchievementMetric.BEST_STREAK, 30),
    STREAK_100(AchievementMetric.BEST_STREAK, 100),
    GOAL_DAYS_7(AchievementMetric.GOAL_DAYS, 7),
    GOAL_DAYS_30(AchievementMetric.GOAL_DAYS, 30),
    FULL_CYCLE(AchievementMetric.POMODOROS_IN_A_DAY, 4),
    DEEP_DAY(AchievementMetric.HOURS_IN_A_DAY, 4),
    EARLY_BIRD(AchievementMetric.EARLY_SESSIONS, 5),
    NIGHT_OWL(AchievementMetric.LATE_SESSIONS, 5),
}

data class AchievementProgress(
    val achievement: Achievement,
    val current: Int,
) {
    val unlocked: Boolean get() = current >= achievement.target
    val fraction: Float get() = (current.toFloat() / achievement.target).coerceIn(0f, 1f)
}

data class GamificationState(
    val xp: Long = 0,
    val level: Int = 1,
    /** Total XP at which the current level started. */
    val levelStartXp: Long = 0,
    /** Total XP needed to reach the next level. */
    val nextLevelXp: Long = 100,
    val achievements: List<AchievementProgress> = Achievement.entries.map { AchievementProgress(it, 0) },
) {
    val levelProgress: Float
        get() = ((xp - levelStartXp).toFloat() / (nextLevelXp - levelStartXp)).coerceIn(0f, 1f)

    val unlockedCount: Int get() = achievements.count { it.unlocked }
}
