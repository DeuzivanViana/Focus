package com.focus.core.data.backup

import com.focus.core.data.BuildConfig

import android.content.Context
import android.net.Uri
import com.focus.core.data.repository.SessionRepository
import com.focus.core.data.repository.SettingsRepository
import com.focus.core.data.repository.SubjectRepository
import com.focus.core.model.StudyTask
import com.focus.core.model.Subject
import com.focus.core.model.AccentColor
import com.focus.core.model.ThemeMode
import com.focus.core.model.TimerStyle
import com.focus.core.model.FocusSound
import com.focus.core.model.TimerSettings
import com.focus.core.model.FocusSession
import com.focus.core.model.Goal
import com.focus.core.model.ReviewItem
import com.focus.core.model.RoutineBlock
import com.focus.core.data.repository.PlanRepository
import com.focus.core.data.repository.GoalRepository
import java.time.LocalDate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.SecureRandom
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ImportResult {
    data class Success(val imported: Int, val total: Int) : ImportResult
    data object Invalid : ImportResult
    data object Tampered : ImportResult
    data object TooLarge : ImportResult
    data object UnsupportedVersion : ImportResult
}

/**
 * Exports and imports progress as a `.focus` file.
 *
 * Format: MAGIC(6) | version(1) | IV(12) | AES-256-GCM(gzip(JSON)).
 * The header is authenticated data (AAD). Changing any byte of the file
 * invalidates the GCM tag and the import is refused. The content cannot be
 * read or edited outside the app.
 */
@Singleton
class ProgressBackup @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessions: SessionRepository,
    private val settings: SettingsRepository,
    private val subjects: SubjectRepository,
    private val goals: GoalRepository,
    private val plan: PlanRepository,
) {
    private val random = SecureRandom()

    suspend fun export(uri: Uri): Int = withContext(Dispatchers.IO) {
        val list = sessions.getAll()
        val s = settings.settings.first()
        val subjectList = subjects.getSubjects()
        val taskList = subjects.getTasks()
        val goalList = goals.getGoals()
        val blockList = plan.getBlocks()
        val reviewList = plan.getReviews()
        val json = JSONObject()
            .put("format", FORMAT)
            .put("exportedAt", System.currentTimeMillis())
            .put(
                "settings",
                JSONObject()
                    .put("focus", s.focusMinutes)
                    .put("short", s.shortBreakMinutes)
                    .put("long", s.longBreakMinutes)
                    .put("cycle", s.sessionsBeforeLongBreak)
                    .put("goal", s.dailyGoalMinutes)
                    .put("autoBreak", s.autoStartBreaks)
                    .put("autoFocus", s.autoStartFocus)
                    .put("accent", s.accent.name)
                    .put("theme", s.themeMode.name)
                    .put("timerStyle", s.timerStyle.name)
                    .put("restDays", TimerSettings.restDaysToMask(s.restDays))
                    .put("focusSound", s.focusSound.name)
                    .put("soundVolume", s.soundVolume),
            )
            .put(
                "sessions",
                JSONArray().apply {
                    list.forEach {
                        put(
                            JSONArray().put(it.startedAt).put(it.endedAt).put(it.focusedSeconds).put(if (it.completed) 1 else 0)
                                .put(it.subjectId ?: -1).put(it.taskId ?: -1).put(it.rating ?: 0).put(it.note.orEmpty()),
                        )
                    }
                },
            )
            .put(
                "subjects",
                JSONArray().apply {
                    subjectList.forEach {
                        put(JSONObject().put("id", it.id).put("name", it.name).put("color", it.color.name).put("archived", it.archived).put("createdAt", it.createdAt))
                    }
                },
            )
            .put(
                "tasks",
                JSONArray().apply {
                    taskList.forEach {
                        put(JSONObject().put("id", it.id).put("subjectId", it.subjectId).put("title", it.title).put("done", it.done).put("createdAt", it.createdAt))
                    }
                },
            )
            .put(
                "goals",
                JSONArray().apply {
                    goalList.forEach {
                        put(
                            JSONObject().put("title", it.title).put("subjectId", it.subjectId ?: -1).put("minutes", it.targetMinutes)
                                .put("start", it.startDate.toEpochDay()).put("deadline", it.deadline.toEpochDay()).put("createdAt", it.createdAt),
                        )
                    }
                },
            )
            .put(
                "routine",
                JSONArray().apply {
                    blockList.forEach {
                        put(
                            JSONObject().put("title", it.title).put("subjectId", it.subjectId ?: -1).put("days", RoutineBlock.daysToMask(it.days))
                                .put("start", it.startMinute).put("duration", it.durationMinutes).put("remind", it.remindBefore ?: -1),
                        )
                    }
                },
            )
            .put(
                "reviews",
                JSONArray().apply {
                    reviewList.forEach {
                        put(
                            JSONObject().put("title", it.title).put("subjectId", it.subjectId ?: -1).put("studied", it.studiedOn.toEpochDay())
                                .put("stage", it.stage).put("createdAt", it.createdAt),
                        )
                    }
                },
            )
        val bytes = encrypt(gzip(json.toString().toByteArray(Charsets.UTF_8)))
        val out = context.contentResolver.openOutputStream(uri, "wt") ?: error("Could not open the destination")
        out.use { it.write(bytes) }
        list.size
    }

    suspend fun import(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri) ?: return@withContext ImportResult.Invalid
        val raw = stream.use { readLimited(it) } ?: return@withContext ImportResult.TooLarge
        if (raw.size < HEADER_SIZE + IV_SIZE + 16 || !raw.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            return@withContext ImportResult.Invalid
        }
        if (raw[MAGIC.size] != VERSION) return@withContext ImportResult.UnsupportedVersion
        val plain = try {
            gunzip(decrypt(raw))
        } catch (e: AEADBadTagException) {
            return@withContext ImportResult.Tampered
        } catch (e: Exception) {
            return@withContext ImportResult.Invalid
        } ?: return@withContext ImportResult.TooLarge

        val parsed = try {
            parse(JSONObject(String(plain, Charsets.UTF_8)))
        } catch (e: Exception) {
            return@withContext ImportResult.Invalid
        }

        // Subjects and tasks are matched by name so importing twice never duplicates them.
        val subjectIdMap = HashMap<Long, Long>()
        val taskIdMap = HashMap<Long, Long>()
        if (parsed.subjects.isNotEmpty()) {
            val existing = subjects.getSubjects().associateBy { it.name.lowercase() }
            for (sub in parsed.subjects) {
                val newId = existing[sub.name.lowercase()]?.id
                    ?: runCatching { subjects.addSubject(sub.name, sub.color) }.getOrNull()
                    ?: continue
                subjectIdMap[sub.id] = newId
            }
            val existingTasks = subjects.getTasks().associateBy { it.subjectId to it.title.lowercase() }
            for (t in parsed.tasks) {
                val subjectId = subjectIdMap[t.subjectId] ?: continue
                val newId = existingTasks[subjectId to t.title.lowercase()]?.id
                    ?: runCatching { subjects.addTask(subjectId, t.title) }.getOrNull()
                    ?: continue
                if (t.done) runCatching { subjects.updateTask(t.copy(id = newId, subjectId = subjectId)) }
                taskIdMap[t.id] = newId
            }
        }
        val remapped = parsed.sessions.map { s ->
            val subjectId = s.subjectId?.let(subjectIdMap::get)
            s.copy(subjectId = subjectId, taskId = if (subjectId == null) null else s.taskId?.let(taskIdMap::get))
        }
        val imported = sessions.importMerging(remapped)
        // Goals are matched by title; invalid ones are rejected by the repository.
        if (parsed.goals.isNotEmpty()) {
            val existingTitles = goals.getGoals().map { it.title.lowercase() }.toHashSet()
            parsed.goals.filter { existingTitles.add(it.title.lowercase()) }.forEach { g ->
                runCatching { goals.save(g.copy(id = 0, subjectId = g.subjectId?.let(subjectIdMap::get))) }
            }
        }
        // Routine blocks and reviews are matched by title (+ start/study day) so a re-import adds nothing.
        if (parsed.blocks.isNotEmpty()) {
            val existing = plan.getBlocks().map { it.title.lowercase() to it.startMinute }.toHashSet()
            parsed.blocks.filter { existing.add(it.title.lowercase() to it.startMinute) }.forEach { b ->
                runCatching { plan.saveBlock(b.copy(id = 0, subjectId = b.subjectId?.let(subjectIdMap::get))) }
            }
        }
        if (parsed.reviews.isNotEmpty()) {
            val existing = plan.getReviews().map { it.title.lowercase() to it.studiedOn }.toHashSet()
            parsed.reviews.filter { existing.add(it.title.lowercase() to it.studiedOn) }.forEach { r ->
                runCatching { plan.saveReview(r.copy(id = 0, subjectId = r.subjectId?.let(subjectIdMap::get))) }
            }
        }
        val st = parsed.settings
        if (st != null) {
            settings.update { cur ->
                cur.copy(
                    focusMinutes = st.optInt("focus", cur.focusMinutes),
                    shortBreakMinutes = st.optInt("short", cur.shortBreakMinutes),
                    longBreakMinutes = st.optInt("long", cur.longBreakMinutes),
                    sessionsBeforeLongBreak = st.optInt("cycle", cur.sessionsBeforeLongBreak),
                    dailyGoalMinutes = st.optInt("goal", cur.dailyGoalMinutes),
                    autoStartBreaks = st.optBoolean("autoBreak", cur.autoStartBreaks),
                    autoStartFocus = st.optBoolean("autoFocus", cur.autoStartFocus),
                    accent = AccentColor.fromName(st.optString("accent", cur.accent.name)),
                    themeMode = ThemeMode.fromName(st.optString("theme", cur.themeMode.name)),
                    timerStyle = TimerStyle.fromName(st.optString("timerStyle", cur.timerStyle.name)),
                    restDays = TimerSettings.maskToRestDays(st.optInt("restDays", TimerSettings.restDaysToMask(cur.restDays)) and 0x7F),
                    focusSound = FocusSound.fromName(st.optString("focusSound", cur.focusSound.name)),
                    soundVolume = st.optInt("soundVolume", cur.soundVolume),
                ) // update() applies sanitized(): out-of-range values never get in.
            }
        }
        ImportResult.Success(imported, parsed.sessions.size)
    }

    private class Parsed(
        val sessions: List<FocusSession>,
        val subjects: List<Subject>,
        val tasks: List<StudyTask>,
        val goals: List<Goal>,
        val blocks: List<RoutineBlock>,
        val reviews: List<ReviewItem>,
        val settings: JSONObject?,
    )

    private fun parse(json: JSONObject): Parsed {
        val arr = json.getJSONArray("sessions")
        val list = ArrayList<FocusSession>(arr.length())
        for (i in 0 until arr.length()) {
            val a = arr.getJSONArray(i)
            list += FocusSession(
                startedAt = a.getLong(0),
                endedAt = a.getLong(1),
                focusedSeconds = a.getLong(2),
                completed = a.getInt(3) == 1,
                subjectId = a.optLong(4, -1).takeIf { it >= 0 },
                taskId = a.optLong(5, -1).takeIf { it >= 0 },
                rating = a.optInt(6, 0).takeIf { it in FocusSession.RATING_RANGE },
                note = a.optString(7, "").ifBlank { null },
            )
        }
        val subs = json.optJSONArray("subjects")?.let { sa ->
            (0 until sa.length()).map { i ->
                val o = sa.getJSONObject(i)
                Subject(
                    id = o.getLong("id"),
                    name = o.getString("name"),
                    color = AccentColor.fromName(o.optString("color")),
                    archived = o.optBoolean("archived"),
                    createdAt = o.optLong("createdAt"),
                )
            }
        }.orEmpty()
        val tasks = json.optJSONArray("tasks")?.let { ta ->
            (0 until ta.length()).map { i ->
                val o = ta.getJSONObject(i)
                StudyTask(
                    id = o.getLong("id"),
                    subjectId = o.getLong("subjectId"),
                    title = o.getString("title"),
                    done = o.optBoolean("done"),
                    createdAt = o.optLong("createdAt"),
                )
            }
        }.orEmpty()
        val goalList = json.optJSONArray("goals")?.let { ga ->
            (0 until minOf(ga.length(), MAX_GOALS)).map { i ->
                val o = ga.getJSONObject(i)
                Goal(
                    title = o.getString("title"),
                    subjectId = o.optLong("subjectId", -1).takeIf { it >= 0 },
                    targetMinutes = o.getInt("minutes"),
                    startDate = LocalDate.ofEpochDay(o.getLong("start")),
                    deadline = LocalDate.ofEpochDay(o.getLong("deadline")),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                )
            }
        }.orEmpty()
        val blocks = json.optJSONArray("routine")?.let { ra ->
            (0 until minOf(ra.length(), MAX_GOALS)).map { i ->
                val o = ra.getJSONObject(i)
                RoutineBlock(
                    title = o.getString("title"),
                    subjectId = o.optLong("subjectId", -1).takeIf { it >= 0 },
                    days = RoutineBlock.maskToDays(o.getInt("days")),
                    startMinute = o.getInt("start"),
                    durationMinutes = o.getInt("duration"),
                    remindBefore = o.optInt("remind", -1).takeIf { it >= 0 },
                )
            }
        }.orEmpty()
        val reviews = json.optJSONArray("reviews")?.let { ra ->
            (0 until minOf(ra.length(), MAX_REVIEWS)).map { i ->
                val o = ra.getJSONObject(i)
                ReviewItem(
                    title = o.getString("title"),
                    subjectId = o.optLong("subjectId", -1).takeIf { it >= 0 },
                    studiedOn = LocalDate.ofEpochDay(o.getLong("studied")),
                    stage = o.optInt("stage", 0),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                )
            }
        }.orEmpty()
        return Parsed(list, subs, tasks, goalList, blocks, reviews, json.optJSONObject("settings"))
    }

    private fun header() = MAGIC + byteArrayOf(VERSION)

    private fun encrypt(plain: ByteArray): ByteArray {
        val iv = ByteArray(IV_SIZE).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(header())
        return header() + iv + cipher.doFinal(plain)
    }

    private fun decrypt(file: ByteArray): ByteArray {
        val iv = file.copyOfRange(HEADER_SIZE, HEADER_SIZE + IV_SIZE)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(file.copyOfRange(0, HEADER_SIZE))
        return cipher.doFinal(file, HEADER_SIZE + IV_SIZE, file.size - HEADER_SIZE - IV_SIZE)
    }

    /**
     * The key comes from the build (BuildConfig.BACKUP_KEY, set from `focus.backupKey` in
     * local.properties or the FOCUS_BACKUP_KEY environment variable). It is not in the
     * source code. Builds without one use a public development key, so their backups are
     * only protected against accidental edits, not against someone who reads this repo.
     */
    private fun key(): SecretKeySpec {
        val hex = BuildConfig.BACKUP_KEY
        val k = ByteArray(32) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
        return SecretKeySpec(k, "AES")
    }

    private fun gzip(data: ByteArray): ByteArray =
        ByteArrayOutputStream().also { bos -> GZIPOutputStream(bos).use { it.write(data) } }.toByteArray()

    /** Decompresses with a size limit (protects against zip bombs). */
    private fun gunzip(data: ByteArray): ByteArray? = GZIPInputStream(data.inputStream()).use { readLimited(it, MAX_JSON_BYTES) }

    private fun readLimited(input: InputStream, limit: Int = MAX_FILE_BYTES): ByteArray? {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var total = 0
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            total += n
            if (total > limit) return null
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    companion object {
        const val MIME_TYPE = "application/octet-stream"
        const val FILE_EXTENSION = "focus"
        private val MAGIC = "FOCUSX".toByteArray(Charsets.US_ASCII)
        private const val VERSION: Byte = 1
        /** JSON payload format. 1 = sessions + settings, 2 = subjects and tasks, 3 = goals and reflections, 4 = routine and reviews. */
        private const val FORMAT = 4
        private const val MAX_REVIEWS = 5000
        private const val MAX_GOALS = 500
        private val HEADER_SIZE = MAGIC.size + 1
        private const val IV_SIZE = 12
        private const val TAG_BITS = 128
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val MAX_FILE_BYTES = 10 * 1024 * 1024
        private const val MAX_JSON_BYTES = 50 * 1024 * 1024
    }
}
