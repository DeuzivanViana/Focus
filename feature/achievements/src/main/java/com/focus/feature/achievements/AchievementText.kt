package com.focus.feature.achievements

import androidx.annotation.StringRes
import com.focus.core.model.Achievement

/** String resources for each achievement. */
internal data class AchievementText(@StringRes val title: Int, @StringRes val description: Int)

internal fun Achievement.text(): AchievementText = when (this) {
    Achievement.FIRST_FOCUS -> AchievementText(R.string.ach_first_focus_title, R.string.ach_first_focus_desc)
    Achievement.POMODOROS_10 -> AchievementText(R.string.ach_pomodoros_10_title, R.string.ach_pomodoros_10_desc)
    Achievement.POMODOROS_50 -> AchievementText(R.string.ach_pomodoros_50_title, R.string.ach_pomodoros_50_desc)
    Achievement.POMODOROS_100 -> AchievementText(R.string.ach_pomodoros_100_title, R.string.ach_pomodoros_100_desc)
    Achievement.POMODOROS_500 -> AchievementText(R.string.ach_pomodoros_500_title, R.string.ach_pomodoros_500_desc)
    Achievement.HOURS_10 -> AchievementText(R.string.ach_hours_10_title, R.string.ach_hours_10_desc)
    Achievement.HOURS_50 -> AchievementText(R.string.ach_hours_50_title, R.string.ach_hours_50_desc)
    Achievement.HOURS_100 -> AchievementText(R.string.ach_hours_100_title, R.string.ach_hours_100_desc)
    Achievement.HOURS_500 -> AchievementText(R.string.ach_hours_500_title, R.string.ach_hours_500_desc)
    Achievement.STREAK_3 -> AchievementText(R.string.ach_streak_3_title, R.string.ach_streak_3_desc)
    Achievement.STREAK_7 -> AchievementText(R.string.ach_streak_7_title, R.string.ach_streak_7_desc)
    Achievement.STREAK_30 -> AchievementText(R.string.ach_streak_30_title, R.string.ach_streak_30_desc)
    Achievement.STREAK_100 -> AchievementText(R.string.ach_streak_100_title, R.string.ach_streak_100_desc)
    Achievement.GOAL_DAYS_7 -> AchievementText(R.string.ach_goal_days_7_title, R.string.ach_goal_days_7_desc)
    Achievement.GOAL_DAYS_30 -> AchievementText(R.string.ach_goal_days_30_title, R.string.ach_goal_days_30_desc)
    Achievement.FULL_CYCLE -> AchievementText(R.string.ach_full_cycle_title, R.string.ach_full_cycle_desc)
    Achievement.DEEP_DAY -> AchievementText(R.string.ach_deep_day_title, R.string.ach_deep_day_desc)
    Achievement.EARLY_BIRD -> AchievementText(R.string.ach_early_bird_title, R.string.ach_early_bird_desc)
    Achievement.NIGHT_OWL -> AchievementText(R.string.ach_night_owl_title, R.string.ach_night_owl_desc)
}
