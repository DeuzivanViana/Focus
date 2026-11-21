package com.focus.core.model

/** Insights computed from post-session ratings. */
data class ReflectionInsights(
    val ratedSessions: Int = 0,
    val averageRating30Days: Float? = null,
    /** Average rating per part of the day, only for parts with ratings. */
    val byDayPart: Map<DayPart, Float> = emptyMap(),
    /** Average rating per subject (null key = no subject), most rated first. */
    val bySubject: List<Pair<Subject?, Float>> = emptyList(),
    /** Average rating for completed vs interrupted sessions. */
    val completedAverage: Float? = null,
    val interruptedAverage: Float? = null,
    val recentNotes: List<FocusSession> = emptyList(),
) {
    val bestDayPart: DayPart? get() = byDayPart.maxByOrNull { it.value }?.key
}

enum class DayPart(val hours: IntRange) {
    MORNING(5..11),
    AFTERNOON(12..17),
    EVENING(18..22),
    NIGHT(23..28), // 23:00 to 04:59, handled by [of]
    ;

    companion object {
        fun of(hour: Int): DayPart = when (hour) {
            in 5..11 -> MORNING
            in 12..17 -> AFTERNOON
            in 18..22 -> EVENING
            else -> NIGHT
        }
    }
}
