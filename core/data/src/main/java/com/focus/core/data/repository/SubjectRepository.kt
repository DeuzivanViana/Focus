package com.focus.core.data.repository

import com.focus.core.database.SubjectDao
import com.focus.core.database.asEntity
import com.focus.core.database.asModel
import com.focus.core.model.AccentColor
import com.focus.core.model.StudyTask
import com.focus.core.model.Subject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface SubjectRepository {
    fun observeSubjects(): Flow<List<Subject>>
    fun observeTasks(): Flow<List<StudyTask>>
    suspend fun addSubject(name: String, color: AccentColor): Long
    suspend fun updateSubject(subject: Subject)
    suspend fun deleteSubject(id: Long)
    suspend fun addTask(subjectId: Long, title: String): Long
    suspend fun updateTask(task: StudyTask)
    suspend fun deleteTask(id: Long)
    suspend fun getSubjects(): List<Subject>
    suspend fun getTasks(): List<StudyTask>
}

internal class OfflineSubjectRepository @Inject constructor(
    private val dao: SubjectDao,
) : SubjectRepository {
    override fun observeSubjects() = dao.observeSubjects().map { l -> l.map { it.asModel() } }
    override fun observeTasks() = dao.observeTasks().map { l -> l.map { it.asModel() } }

    override suspend fun addSubject(name: String, color: AccentColor): Long =
        dao.insertSubject(Subject(name = cleanName(name, Subject.MAX_NAME_LENGTH), color = color).asEntity())

    override suspend fun updateSubject(subject: Subject) =
        dao.updateSubject(subject.copy(name = cleanName(subject.name, Subject.MAX_NAME_LENGTH)).asEntity())

    override suspend fun deleteSubject(id: Long) {
        dao.detachSessions(id) // history stays, just without a subject
        dao.deleteSubject(id) // tasks are removed by the foreign key cascade
    }

    override suspend fun addTask(subjectId: Long, title: String): Long =
        dao.insertTask(StudyTask(subjectId = subjectId, title = cleanName(title, StudyTask.MAX_TITLE_LENGTH)).asEntity())

    override suspend fun updateTask(task: StudyTask) =
        dao.updateTask(task.copy(title = cleanName(task.title, StudyTask.MAX_TITLE_LENGTH)).asEntity())

    override suspend fun deleteTask(id: Long) {
        dao.detachTask(id)
        dao.deleteTask(id)
    }

    override suspend fun getSubjects() = dao.getSubjects().map { it.asModel() }
    override suspend fun getTasks() = dao.getTasks().map { it.asModel() }

    /** Trims, collapses whitespace, strips control characters and enforces the length limit. */
    private fun cleanName(raw: String, max: Int): String {
        val cleaned = raw.filterNot { it.isISOControl() }.trim().replace(Regex("\\s+"), " ").take(max)
        require(cleaned.isNotEmpty()) { "Name must not be blank" }
        return cleaned
    }
}
