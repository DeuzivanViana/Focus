package com.focus.core.model

/** A study subject (e.g. "English", "Calculus"). Sessions can be linked to one. */
data class Subject(
    val id: Long = 0,
    val name: String,
    val color: AccentColor = AccentColor.Default,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val MAX_NAME_LENGTH = 40
    }
}

/** A to-do item inside a subject. Can be selected as the target of a focus session. */
data class StudyTask(
    val id: Long = 0,
    val subjectId: Long,
    val title: String,
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val MAX_TITLE_LENGTH = 80
    }
}

/** Focus time aggregated for one subject. [subject] is null for sessions without a subject. */
data class SubjectTime(
    val subject: Subject?,
    val last7DaysSeconds: Long,
    val last30DaysSeconds: Long,
    val totalSeconds: Long,
)
