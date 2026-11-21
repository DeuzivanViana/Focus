package com.focus.app

import android.net.Uri
import androidx.room.Room
import com.focus.core.data.repository.GoalRepository
import com.focus.core.database.FocusDatabase
import com.focus.core.model.Goal
import com.focus.core.model.ReviewItem
import com.focus.core.model.RoutineBlock
import com.focus.core.data.repository.PlanRepository
import org.junit.After
import java.time.LocalDate
import androidx.test.core.app.ApplicationProvider
import com.focus.core.data.backup.ImportResult
import com.focus.core.data.backup.ProgressBackup
import com.focus.core.data.repository.SessionRepository
import com.focus.core.data.repository.SettingsRepository
import com.focus.core.data.repository.SubjectRepository
import com.focus.core.model.StudyTask
import com.focus.core.model.Subject
import com.focus.core.model.AccentColor
import com.focus.core.model.FocusSession
import com.focus.core.model.TimerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProgressBackupTest {
    private class FakeSessions(initial: List<FocusSession> = emptyList()) : SessionRepository {
        val items = initial.toMutableList()
        override fun observeSessions(): Flow<List<FocusSession>> = MutableStateFlow(items.toList())
        override suspend fun record(session: FocusSession) { items += session }
        override suspend fun clearAll() = items.clear()
        override suspend fun getAll() = items.toList()
        override suspend fun importMerging(sessions: List<FocusSession>): Int {
            val starts = items.map { it.startedAt }.toHashSet()
            val fresh = sessions.filter { starts.add(it.startedAt) }
            items += fresh
            return fresh.size
        }
    }

    private class FakeSettings(initial: TimerSettings) : SettingsRepository {
        val state = MutableStateFlow(initial)
        override val settings: Flow<TimerSettings> = state
        override suspend fun update(transform: (TimerSettings) -> TimerSettings) { state.value = transform(state.value).sanitized() }
    }

    private class FakeSubjects(
        val subjects: MutableList<Subject> = mutableListOf(),
        val tasks: MutableList<StudyTask> = mutableListOf(),
    ) : SubjectRepository {
        private var nextId = 100L
        override fun observeSubjects(): Flow<List<Subject>> = MutableStateFlow(subjects.toList())
        override fun observeTasks(): Flow<List<StudyTask>> = MutableStateFlow(tasks.toList())
        override suspend fun addSubject(name: String, color: AccentColor) = (nextId++).also { subjects += Subject(it, name, color) }
        override suspend fun updateSubject(subject: Subject) { subjects.replaceAll { if (it.id == subject.id) subject else it } }
        override suspend fun deleteSubject(id: Long) { subjects.removeAll { it.id == id } }
        override suspend fun addTask(subjectId: Long, title: String) = (nextId++).also { tasks += StudyTask(it, subjectId, title) }
        override suspend fun updateTask(task: StudyTask) { tasks.replaceAll { if (it.id == task.id) task else it } }
        override suspend fun deleteTask(id: Long) { tasks.removeAll { it.id == id } }
        override suspend fun getSubjects() = subjects.toList()
        override suspend fun getTasks() = tasks.toList()
    }

    private val dbs = mutableListOf<FocusDatabase>()

    @After
    fun closeDbs() = dbs.forEach { it.close() }

    /** A real GoalRepository over an in-memory database. */
    private val planDbs = HashMap<GoalRepository, PlanRepository>()

    private fun goals(sessions: SessionRepository, subjects: SubjectRepository): GoalRepository {
        val db = Room.inMemoryDatabaseBuilder(context, FocusDatabase::class.java).allowMainThreadQueries().build().also { dbs += it }
        return GoalRepository(db.goalDao(), sessions, subjects).also { planDbs[it] = PlanRepository(db.planDao()) }
    }

    private fun planOf(goals: GoalRepository) = planDbs.getValue(goals)

    private fun backup(sessions: SessionRepository, settings: SettingsRepository, subjects: SubjectRepository, goalRepo: GoalRepository? = null): ProgressBackup {
        val g = goalRepo ?: goals(sessions, subjects)
        return ProgressBackup(context, sessions, settings, subjects, g, planOf(g))
    }

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val sessions = (1..5).map { FocusSession(startedAt = it * 1_000_000L, endedAt = it * 1_000_000L + 1_500_000, focusedSeconds = 1500, completed = it != 3) }

    private fun tempUri(): Pair<File, Uri> {
        val f = File.createTempFile("backup", ".focus", context.cacheDir)
        return f to Uri.fromFile(f)
    }

    @Test
    fun roundTripRestoresSessionsAndSettingsWithoutDuplicates() = runBlocking {
        val (file, uri) = tempUri()
        val sourceSessions = FakeSessions(sessions.map { it.copy(subjectId = 1, taskId = 2, rating = 4, note = "Calm morning") })
        val sourceSubjects = FakeSubjects(mutableListOf(Subject(1, "English", AccentColor.TEAL)), mutableListOf(StudyTask(2, 1, "Verbs", done = true)))
        val sourceGoals = goals(sourceSessions, sourceSubjects)
        planOf(sourceGoals).saveBlock(RoutineBlock(title = "Verbs drill", subjectId = 1, days = setOf(java.time.DayOfWeek.MONDAY), startMinute = 1140, durationMinutes = 50))
        planOf(sourceGoals).saveReview(ReviewItem(title = "Irregular verbs", subjectId = 1, studiedOn = LocalDate.of(2026, 11, 20), stage = 1))
        sourceGoals.save(Goal(title = "100h English", subjectId = 1, targetMinutes = 6000, startDate = LocalDate.of(2026, 11, 1), deadline = LocalDate.of(2027, 3, 1)))
        val source = backup(sourceSessions, FakeSettings(TimerSettings(focusMinutes = 50, accent = AccentColor.CORAL)), sourceSubjects, sourceGoals)
        assertEquals(5, source.export(uri))
        assertTrue(!String(file.readBytes(), Charsets.ISO_8859_1).contains("1500")) // content is not readable

        val targetSessions = FakeSessions(sessions.take(2))
        val targetSettings = FakeSettings(TimerSettings())
        val targetSubjects = FakeSubjects()
        val targetGoals = goals(targetSessions, targetSubjects)
        val result = backup(targetSessions, targetSettings, targetSubjects, targetGoals).import(uri)
        assertEquals(ImportResult.Success(imported = 3, total = 5), result)
        assertEquals(5, targetSessions.items.size)
        assertEquals(50, targetSettings.state.value.focusMinutes)
        assertEquals(AccentColor.CORAL, targetSettings.state.value.accent)
        val subject = targetSubjects.subjects.single()
        assertEquals("English", subject.name)
        assertEquals(AccentColor.TEAL, subject.color)
        val task = targetSubjects.tasks.single()
        assertTrue(task.done)
        // Imported sessions point to the new ids.
        assertTrue(targetSessions.items.drop(2).all { it.subjectId == subject.id && it.taskId == task.id })
        // Reflections and goals travel too.
        assertTrue(targetSessions.items.drop(2).all { it.rating == 4 && it.note == "Calm morning" })
        val goal = targetGoals.getGoals().single()
        assertEquals("100h English", goal.title)
        assertEquals(subject.id, goal.subjectId)
        assertEquals(LocalDate.of(2027, 3, 1), goal.deadline)
        val block = planOf(targetGoals).getBlocks().single()
        assertEquals(subject.id, block.subjectId)
        assertEquals(1140, block.startMinute)
        val review = planOf(targetGoals).getReviews().single()
        assertEquals(1, review.stage)
        assertEquals(subject.id, review.subjectId)

        // Importing the same file again duplicates nothing.
        val again = backup(targetSessions, targetSettings, targetSubjects, targetGoals).import(uri)
        assertEquals(ImportResult.Success(imported = 0, total = 5), again)
        assertEquals(1, targetSubjects.subjects.size)
        assertEquals(1, targetGoals.getGoals().size)
        assertEquals(1, planOf(targetGoals).getBlocks().size)
        assertEquals(1, planOf(targetGoals).getReviews().size)
    }

    @Test
    fun anyModifiedByteIsRejected() = runBlocking {
        val (file, uri) = tempUri()
        backup(FakeSessions(sessions), FakeSettings(TimerSettings()), FakeSubjects()).export(uri)
        val bytes = file.readBytes()
        for (pos in listOf(3, 10, bytes.size / 2, bytes.size - 1)) { // header, IV, payload, tag
            val tampered = bytes.copyOf().also { it[pos] = (it[pos].toInt() xor 0x01).toByte() }
            file.writeBytes(tampered)
            val target = FakeSessions()
            val r = backup(target, FakeSettings(TimerSettings()), FakeSubjects()).import(uri)
            assertTrue("pos=$pos result=$r", r is ImportResult.Tampered || r is ImportResult.Invalid || r is ImportResult.UnsupportedVersion)
            assertEquals(0, target.items.size)
        }
    }

    @Test
    fun randomFileIsInvalid() = runBlocking {
        val (file, uri) = tempUri()
        file.writeText("{\"sessions\": []}")
        assertEquals(ImportResult.Invalid, backup(FakeSessions(), FakeSettings(TimerSettings()), FakeSubjects()).import(uri))
    }
}
