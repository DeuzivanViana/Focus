package com.focus.core.model

/**
 * A recorded block of focus. [focusedSeconds] is the time actually focused
 * (pauses excluded). [completed] tells whether the pomodoro ran to the end.
 */
data class FocusSession(
    val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long,
    val focusedSeconds: Long,
    val completed: Boolean,
    val subjectId: Long? = null,
    val taskId: Long? = null,
    /** Self-rated focus quality, 1 to 5, set in the post-session reflection. */
    val rating: Int? = null,
    val note: String? = null,
) {
    companion object {
        const val MAX_NOTE_LENGTH = 280
        val RATING_RANGE = 1..5
    }
}
