package com.focus.app

import com.focus.core.data.goals.GoalCalculator
import com.focus.core.data.reflection.ReflectionCalculator
import com.focus.core.model.Goal
import com.focus.core.model.ReminderSettings
import com.focus.core.model.ReviewItem
import com.focus.core.model.RoutineBlock
import com.focus.feature.review.ReviewScreen
import com.focus.feature.review.ReviewUiState
import com.focus.feature.routine.RoutineScreen
import com.focus.feature.routine.RoutineUiState
import com.focus.core.model.ThemeMode
import com.focus.feature.blocker.BlockMode
import com.focus.feature.blocker.BlockScreen
import com.focus.feature.blocker.BlockerScreen
import com.focus.feature.blocker.BlockerSettings
import com.focus.feature.blocker.BlockerUiState
import com.focus.feature.blocker.InstalledApp
import com.focus.feature.blocker.Permissions
import com.focus.feature.goals.GoalsScreen
import com.focus.feature.goals.GoalsUiState
import com.focus.feature.reflection.InsightsScreen
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import com.focus.core.data.stats.StatsCalculator
import com.focus.core.designsystem.theme.FocusColors
import com.focus.core.designsystem.theme.FocusTheme
import com.focus.core.model.AccentColor
import com.focus.core.model.FocusSession
import com.focus.core.model.Subject
import com.focus.core.model.StudyTask
import com.focus.core.data.gamification.GamificationCalculator
import com.focus.core.data.repository.FocusSelection
import com.focus.feature.subjects.SubjectsScreen
import com.focus.feature.subjects.SubjectsUiState
import com.focus.feature.subjects.SubjectItem
import com.focus.feature.achievements.AchievementsScreen
import androidx.compose.ui.graphics.Color
import com.focus.core.designsystem.component.ChartType
import com.focus.feature.stats.CompareMode
import com.focus.core.model.TimerPhase
import com.focus.core.model.TimerSettings
import com.focus.core.model.TimerStyle
import com.focus.core.model.TimerStatus
import com.focus.core.timer.TimerState
import com.focus.feature.focus.FocusScreen
import com.focus.feature.focus.FocusUiState
import com.focus.feature.settings.SettingsScreen
import com.focus.feature.stats.StatsScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/**
 * Checks that the main screens compose without errors and saves a PNG preview to
 * app/build/screens/ for visual review.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "pt-rBR-w411dp-h891dp-xxhdpi")
class ScreenRenderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.now(zone)

    private val english = Subject(1, "Inglês", AccentColor.TEAL)
    private val math = Subject(2, "Cálculo", AccentColor.AMBER)
    private val history = Subject(3, "História", AccentColor.VIOLET)

    private val sampleSessions = (0..20).flatMap { d ->
        val n = if (d % 6 == 3) 0 else (d * 7 % 5) + 1
        (0 until n).map { k ->
            val start = today.minusDays(d.toLong()).atTime(8 + k * 2 + d % 3, 0).atZone(zone).toInstant().toEpochMilli()
            FocusSession(startedAt = start, endedAt = start + 25 * 60_000, focusedSeconds = 25 * 60, completed = k != 2, subjectId = listOf(1L, 2L, 3L, null)[(d + k) % 4])
        }
    }

    private val sampleStatsWithSubjects = StatsCalculator.compute(sampleSessions, 120, today, zone, listOf(english, math, history))
    private val sampleGame = GamificationCalculator.compute(sampleSessions, 120, zone)

    private val sampleStats = run {
        val sessions = (0..20).flatMap { d ->
            val n = if (d % 6 == 3) 0 else (d * 7 % 5) + 1
            (0 until n).map { k ->
                val start = today.minusDays(d.toLong()).atTime(8 + k * 2 + d % 3, 0).atZone(zone).toInstant().toEpochMilli()
                FocusSession(startedAt = start, endedAt = start + 25 * 60_000, focusedSeconds = 25 * 60, completed = k != 2)
            }
        }
        StatsCalculator.compute(sessions, 120, today, zone)
    }

    private fun capture(name: String, dir: String = "screens") {
        compose.waitForIdle()
        val view = compose.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        view.draw(canvas)
        // Dialogs live in their own windows: draw them on top, with the usual dim.
        otherRootViews(view).forEach { root ->
            canvas.drawColor(android.graphics.Color.argb(150, 0, 0, 0))
            val loc = IntArray(2).also(root::getLocationOnScreen)
            val x = if (loc[0] == 0 && root.width < view.width) (view.width - root.width) / 2 else loc[0]
            val y = if (loc[1] == 0 && root.height < view.height) (view.height - root.height) / 2 else loc[1]
            canvas.save()
            canvas.translate(x.toFloat(), y.toFloat())
            root.draw(canvas)
            canvas.restore()
        }
        val out = File("build/$dir/$name.png").apply { parentFile?.mkdirs() }
        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun otherRootViews(main: android.view.View): List<android.view.View> = runCatching {
        val cls = Class.forName("android.view.WindowManagerGlobal")
        val global = cls.getMethod("getInstance").invoke(null)
        val field = cls.getDeclaredField("mViews").apply { isAccessible = true }
        (field.get(global) as List<android.view.View>).filter { it !== main && it.isShown && it.width > 0 }
    }.getOrDefault(emptyList())

    private fun show(accent: AccentColor = AccentColor.BLUE, dark: Boolean = true, oled: Boolean = false, content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent {
            FocusTheme(accent = accent, dark = dark, oled = oled) { Box(Modifier.fillMaxSize().background(FocusColors.Background)) { content() } }
        }
    }

    @Test
    fun focusScreenRunning() {
        show {
            FocusScreen(
                state = FocusUiState(
                    timer = TimerState(
                        phase = TimerPhase.FOCUS,
                        status = TimerStatus.RUNNING,
                        totalMillis = 25 * 60_000L,
                        remainingMillis = 17 * 60_000L + 42_000,
                        completedFocusInCycle = 2,
                    ),
                    stats = sampleStats,
                ),
                onToggle = {}, onStop = {}, onSkip = {},
            )
        }
        capture("focus")
    }

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h1900dp-xxhdpi")
    fun statsScreen() {
        show { StatsScreen(sampleStats) }
        capture("stats")
    }

    @Test
    fun statsScreenEmpty() {
        show { StatsScreen(StatsCalculator.compute(emptyList(), 120, today, zone)) }
        capture("stats_empty")
    }

    @Test
    fun settingsScreen() {
        show { SettingsScreen(TimerSettings(), 0, {}, {}, {}, {}, {}) }
        capture("settings")
    }

    @Test
    fun focusWithSubjectAndLevel() {
        show {
            FocusScreen(
                state = FocusUiState(
                    timer = TimerState(),
                    stats = sampleStatsWithSubjects,
                    gamification = sampleGame,
                    subjects = listOf(english, math),
                    tasks = listOf(StudyTask(10, 1, "Phrasal verbs")),
                    selection = FocusSelection(1, 10),
                ),
                onToggle = {}, onStop = {}, onSkip = {},
            )
        }
        capture("focus_subject_level")
    }

    @Test
    fun subjectsScreen() {
        show {
            SubjectsScreen(
                state = SubjectsUiState(
                    items = sampleStatsWithSubjects.subjectTimes.map { t ->
                        SubjectItem(t, if (t.subject?.id == 1L) listOf(StudyTask(10, 1, "Phrasal verbs"), StudyTask(11, 1, "Listening: podcast", done = true)) else emptyList())
                    },
                    loaded = true,
                ),
                onAddSubject = { _, _ -> }, onUpdateSubject = {}, onDeleteSubject = {}, onAddTask = { _, _ -> }, onToggleTask = {}, onDeleteTask = {},
                initiallyExpanded = 1,
            )
        }
        capture("subjects")
    }

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h1600dp-xxhdpi")
    fun achievementsScreen() {
        show { AchievementsScreen(sampleGame, onBack = {}) }
        capture("achievements")
    }

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h1500dp-xxhdpi")
    fun statsWithSubjects() {
        show { StatsScreen(sampleStatsWithSubjects, game = sampleGame) }
        capture("stats_subjects")
    }

    @Test
    fun statsCompareLine() {
        show { StatsScreen(sampleStats, initialChartType = ChartType.LINE, initialCompare = CompareMode.WEEK) }
        capture("stats_compare_line")
    }

    @Test
    fun statsCompareBarsMonthViolet() {
        show(AccentColor.VIOLET) { StatsScreen(sampleStats, initialChartType = ChartType.BARS, initialCompare = CompareMode.MONTH) }
        capture("stats_compare_bars_violet")
    }

    @Test
    @Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
    fun focusScreenEnglishTeal() {
        show(AccentColor.TEAL) {
            FocusScreen(
                state = FocusUiState(timer = TimerState(), stats = sampleStats),
                onToggle = {}, onStop = {}, onSkip = {},
            )
        }
        capture("focus_en_teal")
    }

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h891dp-xxhdpi")
    fun settingsPortugueseAmber() {
        show(AccentColor.AMBER) { SettingsScreen(TimerSettings(accent = AccentColor.AMBER), 1, {}, {}, {}, {}, {}) }
        capture("settings_pt_amber")
    }

    private val ratedSessions = sampleSessions.mapIndexed { i, s ->
        val hour = java.time.Instant.ofEpochMilli(s.startedAt).atZone(zone).hour
        s.copy(id = i + 1L, rating = ((if (hour < 12) 4 else 3) + i % 2).coerceAtMost(5), note = if (i % 9 == 0) "Celular longe ajudou muito. Revisão de verbos rendeu." else null)
    }

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h1300dp-xxhdpi")
    fun goalsScreen() {
        val goals = listOf(
            Goal(1, "100 h de inglês", 1, 100 * 60, today.minusDays(20), today.plusDays(80)),
            Goal(2, "Cálculo para a prova", 2, 30 * 60, today.minusDays(20), today.plusDays(10)),
            Goal(3, "Ler 20 h de história", 3, 20 * 60, today.minusDays(5), today.plusDays(60)),
            Goal(4, "Primeiras 5 h", null, 5 * 60, today.minusDays(20), today.plusDays(1)),
        )
        val subjects = listOf(english, math, history)
        val progress = goals.map { g -> GoalCalculator.progress(g, subjects.firstOrNull { it.id == g.subjectId }, sampleSessions, today, zone) }
        show { GoalsScreen(GoalsUiState(progress, subjects, loaded = true), onSave = {}, onDelete = {}, today = today) }
        capture("goals")
    }

    @Test
    fun goalsEmpty() {
        show { GoalsScreen(GoalsUiState(loaded = true), onSave = {}, onDelete = {}) }
        capture("goals_empty")
    }

    // The reflection prompt is not rendered here: under Robolectric, any text field inside a
    // dialog window never reaches idle. It is a test-environment limitation, not an app loop.

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h1500dp-xxhdpi")
    fun insightsScreen() {
        val insights = ReflectionCalculator.insights(ratedSessions, listOf(english, math, history), today, zone)
        show { InsightsScreen(insights, onBack = {}) }
        capture("insights")
    }

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h1500dp-xxhdpi")
    fun blockerScreen() {
        val apps = listOf("Instagram", "YouTube", "TikTok", "X", "WhatsApp", "Netflix").mapIndexed { i, n -> InstalledApp("com.example.app$i", n, null) }
        show {
            BlockerScreen(
                state = BlockerUiState(
                    settings = BlockerSettings(enabled = true, mode = BlockMode.BLOCK, packages = setOf("com.example.app0", "com.example.app2")),
                    permissions = Permissions(usageAccess = true, blockScreen = false),
                    apps = apps,
                ),
                onBack = {}, onEnabled = {}, onMode = {}, onBlocked = { _, _ -> }, onOpenUsageAccess = {}, onOpenOverlay = {},
            )
        }
        capture("blocker")
    }

    @Test
    fun blockerUsageExplanation() {
        show {
            BlockerScreen(
                state = BlockerUiState(apps = emptyList()),
                onBack = {}, onEnabled = {}, onMode = {}, onBlocked = { _, _ -> }, onOpenUsageAccess = {}, onOpenOverlay = {},
                initialExplain = 0,
            )
        }
        capture("blocker_explain_usage")
    }

    @Test
    fun blockScreen() {
        show { BlockScreen("Instagram", remainingMillis = { 14 * 60_000L + 32_000 }, onBackToFocus = {}, onHome = {}) }
        capture("block_screen")
    }

    // ---- Google Play assets (build/store/) ----

    private fun sampleGoalProgress(): GoalsUiState {
        val subjects = listOf(english, math, history)
        val goals = listOf(
            Goal(1, "25 h de inglês", 1, 25 * 60, today.minusDays(20), today.plusDays(80)),
            Goal(2, "Cálculo para a prova", 2, 8 * 60, today.minusDays(20), today.plusDays(10)),
            Goal(3, "Ler 22 h de história", 3, 22 * 60, today.minusDays(5), today.plusDays(60)),
        )
        return GoalsUiState(goals.map { g -> GoalCalculator.progress(g, subjects.firstOrNull { it.id == g.subjectId }, sampleSessions, today, zone) }, subjects, loaded = true)
    }

    private fun storeShot(index: Int) {
        when (index) {
            1 -> show {
                FocusScreen(
                    state = FocusUiState(
                        timer = TimerState(TimerPhase.FOCUS, TimerStatus.RUNNING, 25 * 60_000L, 17 * 60_000L + 42_000, completedFocusInCycle = 2),
                        stats = sampleStatsWithSubjects,
                        gamification = sampleGame,
                        subjects = listOf(english, math),
                        tasks = listOf(StudyTask(10, 1, "Phrasal verbs")),
                        selection = FocusSelection(1, 10),
                    ),
                    onToggle = {}, onStop = {}, onSkip = {},
                )
            }
            2 -> show { StatsScreen(sampleStatsWithSubjects, game = sampleGame) }
            3 -> show { GoalsScreen(sampleGoalProgress(), onSave = {}, onDelete = {}, today = today) }
            4 -> show { InsightsScreen(ReflectionCalculator.insights(ratedSessions, listOf(english, math, history), today, zone), onBack = {}) }
            5 -> show {
                SubjectsScreen(
                    state = SubjectsUiState(sampleStatsWithSubjects.subjectTimes.map { SubjectItem(it, emptyList()) }, loaded = true),
                    onAddSubject = { _, _ -> }, onUpdateSubject = {}, onDeleteSubject = {}, onAddTask = { _, _ -> }, onToggleTask = {}, onDeleteTask = {},
                )
            }
            6 -> show { AchievementsScreen(sampleGame, onBack = {}) }
            7 -> show {
                BlockerScreen(
                    state = BlockerUiState(
                        settings = BlockerSettings(enabled = true, mode = BlockMode.WARN, packages = setOf("com.example.app0", "com.example.app2")),
                        permissions = Permissions(usageAccess = true, blockScreen = true),
                        apps = listOf("Instagram", "YouTube", "TikTok", "X", "Netflix").mapIndexed { i, n -> InstalledApp("com.example.app$i", n, null) },
                    ),
                    onBack = {}, onEnabled = {}, onMode = {}, onBlocked = { _, _ -> }, onOpenUsageAccess = {}, onOpenOverlay = {},
                )
            }
        }
    }

    // 1080x1920: within Play's 2:1 limit for phone screenshots. One test per shot (setContent runs once per test).
    @Test
    @Config(qualifiers = "pt-rBR-w360dp-h640dp-xxhdpi")
    fun store_pt_1() {
        storeShot(1)
        capture("pt_1", dir = "store")
    }

    @Test
    @Config(qualifiers = "pt-rBR-w360dp-h640dp-xxhdpi")
    fun store_pt_2() {
        storeShot(2)
        capture("pt_2", dir = "store")
    }

    @Test
    @Config(qualifiers = "pt-rBR-w360dp-h640dp-xxhdpi")
    fun store_pt_3() {
        storeShot(3)
        capture("pt_3", dir = "store")
    }

    @Test
    @Config(qualifiers = "pt-rBR-w360dp-h640dp-xxhdpi")
    fun store_pt_4() {
        storeShot(4)
        capture("pt_4", dir = "store")
    }

    @Test
    @Config(qualifiers = "pt-rBR-w360dp-h640dp-xxhdpi")
    fun store_pt_5() {
        storeShot(5)
        capture("pt_5", dir = "store")
    }

    @Test
    @Config(qualifiers = "pt-rBR-w360dp-h640dp-xxhdpi")
    fun store_pt_6() {
        storeShot(6)
        capture("pt_6", dir = "store")
    }

    @Test
    @Config(qualifiers = "pt-rBR-w360dp-h640dp-xxhdpi")
    fun store_pt_7() {
        storeShot(7)
        capture("pt_7", dir = "store")
    }

    @Test
    @Config(qualifiers = "en-w360dp-h640dp-xxhdpi")
    fun store_en_1() {
        storeShot(1)
        capture("en_1", dir = "store")
    }

    @Test
    @Config(qualifiers = "en-w360dp-h640dp-xxhdpi")
    fun store_en_2() {
        storeShot(2)
        capture("en_2", dir = "store")
    }

    @Test
    @Config(qualifiers = "en-w360dp-h640dp-xxhdpi")
    fun store_en_3() {
        storeShot(3)
        capture("en_3", dir = "store")
    }

    @Test
    @Config(qualifiers = "en-w360dp-h640dp-xxhdpi")
    fun store_en_4() {
        storeShot(4)
        capture("en_4", dir = "store")
    }

    @Test
    @Config(qualifiers = "en-w360dp-h640dp-xxhdpi")
    fun store_en_5() {
        storeShot(5)
        capture("en_5", dir = "store")
    }

    @Test
    @Config(qualifiers = "en-w360dp-h640dp-xxhdpi")
    fun store_en_6() {
        storeShot(6)
        capture("en_6", dir = "store")
    }

    @Test
    @Config(qualifiers = "en-w360dp-h640dp-xxhdpi")
    fun store_en_7() {
        storeShot(7)
        capture("en_7", dir = "store")
    }

    @Test
    @Config(qualifiers = "w512dp-h512dp-mdpi")
    fun storeIcon() {
        show {
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = androidx.compose.ui.Alignment.Center) {
                androidx.compose.foundation.Image(
                    androidx.compose.ui.res.painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        capture("icon_512", dir = "store")
    }

    @Test
    @Config(qualifiers = "pt-rBR-w1024dp-h500dp-mdpi")
    fun storeFeatureGraphic() {
        show {
            androidx.compose.foundation.layout.Row(
                Modifier.fillMaxSize().background(Color.Black).padding(horizontal = 72.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                androidx.compose.foundation.Image(
                    androidx.compose.ui.res.painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(300.dp),
                )
                androidx.compose.foundation.layout.Spacer(Modifier.size(24.dp))
                androidx.compose.foundation.layout.Column {
                    androidx.compose.material3.Text("Focus", style = androidx.compose.material3.MaterialTheme.typography.displayLarge, color = FocusColors.TextPrimary)
                    androidx.compose.material3.Text(
                        "Pomodoro, metas e estatísticas de estudo.\nSem conta, sem anúncios, offline.",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        color = FocusColors.TextSecondary,
                    )
                }
            }
        }
        capture("feature_graphic_1024x500", dir = "store")
    }

    // ---- Light theme ----

    @Test
    fun lightFocus() {
        show(dark = false) {
            FocusScreen(
                state = FocusUiState(
                    timer = TimerState(TimerPhase.FOCUS, TimerStatus.RUNNING, 25 * 60_000L, 17 * 60_000L + 42_000, completedFocusInCycle = 2),
                    stats = sampleStatsWithSubjects,
                    gamification = sampleGame,
                    subjects = listOf(english, math),
                    tasks = listOf(StudyTask(10, 1, "Phrasal verbs")),
                    selection = FocusSelection(1, 10),
                ),
                onToggle = {}, onStop = {}, onSkip = {},
            )
        }
        capture("light_focus")
    }

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h1500dp-xxhdpi")
    fun lightStats() {
        show(dark = false) { StatsScreen(sampleStatsWithSubjects, game = sampleGame) }
        capture("light_stats")
    }

    @Test
    fun lightStatsCompareCoral() {
        show(accent = AccentColor.CORAL, dark = false) { StatsScreen(sampleStats, initialChartType = ChartType.LINE, initialCompare = CompareMode.WEEK) }
        capture("light_stats_compare_coral")
    }

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h1300dp-xxhdpi")
    fun lightGoals() {
        show(dark = false) { GoalsScreen(sampleGoalProgress(), onSave = {}, onDelete = {}, today = today) }
        capture("light_goals")
    }

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h1700dp-xxhdpi")
    fun lightSettings() {
        show(accent = AccentColor.TEAL, dark = false) { SettingsScreen(TimerSettings(accent = AccentColor.TEAL, themeMode = ThemeMode.LIGHT), 0, {}, {}, {}, {}, {}) }
        capture("light_settings")
    }

    @Test
    fun lightSubjects() {
        show(dark = false) {
            SubjectsScreen(
                state = SubjectsUiState(sampleStatsWithSubjects.subjectTimes.map { SubjectItem(it, emptyList()) }, loaded = true),
                onAddSubject = { _, _ -> }, onUpdateSubject = {}, onDeleteSubject = {}, onAddTask = { _, _ -> }, onToggleTask = {}, onDeleteTask = {},
            )
        }
        capture("light_subjects")
    }

    // ---- Routine and reviews ----

    private val sampleBlocks = listOf(
        RoutineBlock(1, "Cálculo", 2, setOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.FRIDAY), 19 * 60, 50, 10),
        RoutineBlock(2, "Listening", 1, java.time.DayOfWeek.entries.toSet() - java.time.DayOfWeek.SUNDAY, 7 * 60 + 30, 25, 5),
        RoutineBlock(3, "Leitura de história", 3, setOf(java.time.DayOfWeek.TUESDAY, java.time.DayOfWeek.THURSDAY, java.time.DayOfWeek.SATURDAY), 14 * 60, 40, null),
        RoutineBlock(4, "Revisões", null, setOf(java.time.DayOfWeek.SUNDAY, java.time.DayOfWeek.WEDNESDAY), 10 * 60, 30, 0),
    )

    private val sampleReviews = listOf(
        ReviewItem(1, "Cap. 3: derivadas", 2, today.minusDays(1), 0),
        ReviewItem(2, "Phrasal verbs", 1, today.minusDays(9), 1),
        ReviewItem(3, "Revolução Francesa", 3, today.minusDays(3), 1),
        ReviewItem(4, "Limites", 2, today.minusDays(20), 2),
        ReviewItem(5, "Present perfect", 1, today.minusDays(40), 3),
    )

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h1500dp-xxhdpi")
    fun routineScreen() {
        show { RoutineScreen(RoutineUiState(sampleBlocks, listOf(english, math, history), loaded = true), onSave = {}, onDelete = {}, now = today.atTime(19, 20)) }
        capture("routine")
    }

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h1500dp-xxhdpi")
    fun routineScreenLight() {
        show(dark = false) { RoutineScreen(RoutineUiState(sampleBlocks, listOf(english, math, history), loaded = true), onSave = {}, onDelete = {}, now = today.atTime(19, 20)) }
        capture("light_routine")
    }

    @Test
    fun routineEmpty() {
        show { RoutineScreen(RoutineUiState(loaded = true), onSave = {}, onDelete = {}) }
        capture("routine_empty")
    }

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h1200dp-xxhdpi")
    fun reviewScreen() {
        show { ReviewScreen(ReviewUiState(sampleReviews, listOf(english, math, history), loaded = true), onAdd = {}, onComplete = {}, onDelete = {}, today = today) }
        capture("reviews")
    }

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h1200dp-xxhdpi")
    fun reviewScreenLight() {
        show(dark = false) { ReviewScreen(ReviewUiState(sampleReviews, listOf(english, math, history), loaded = true), onAdd = {}, onComplete = {}, onDelete = {}, today = today) }
        capture("light_reviews")
    }

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h2200dp-xxhdpi")
    fun settingsWithReminders() {
        show { SettingsScreen(TimerSettings(), 0, {}, {}, {}, {}, {}, reminders = ReminderSettings()) }
        capture("settings_reminders")
    }

    // ---- Timer styles ----

    private fun timerStyleShot(style: TimerStyle, dark: Boolean, name: String) {
        show(dark = dark) {
            FocusScreen(
                state = FocusUiState(
                    timer = TimerState(TimerPhase.FOCUS, TimerStatus.RUNNING, 25 * 60_000L, 17 * 60_000L + 42_000, completedFocusInCycle = 2),
                    stats = sampleStats,
                    timerStyle = style,
                ),
                onToggle = {}, onStop = {}, onSkip = {},
            )
        }
        capture(name, dir = "timer_styles")
    }

    @Test fun timerTicks() = timerStyleShot(TimerStyle.TICKS, true, "ticks")
    @Test fun timerGauge() = timerStyleShot(TimerStyle.GAUGE, true, "gauge")
    @Test fun timerDial() = timerStyleShot(TimerStyle.DIAL, true, "dial")
    @Test fun timerMinimal() = timerStyleShot(TimerStyle.MINIMAL, true, "minimal")
    @Test fun timerRing() = timerStyleShot(TimerStyle.RING, true, "ring")
    @Test fun timerTicksLight() = timerStyleShot(TimerStyle.TICKS, false, "light_ticks")
    @Test fun timerGaugeLight() = timerStyleShot(TimerStyle.GAUGE, false, "light_gauge")
    @Test fun timerDialLight() = timerStyleShot(TimerStyle.DIAL, false, "light_dial")

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h1600dp-xxhdpi")
    fun settingsTimerStyle() {
        show { SettingsScreen(TimerSettings(timerStyle = TimerStyle.GAUGE), 0, {}, {}, {}, {}, {}) }
        capture("settings_timer_style", dir = "timer_styles")
    }

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h1500dp-xxhdpi")
    fun licensesScreen() {
        show { com.focus.feature.settings.LicensesRoute(onBack = {}) }
        capture("licenses", dir = "timer_styles")
    }

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h1700dp-xxhdpi")
    fun blockerRestricted() {
        val apps = listOf("Instagram", "YouTube", "TikTok").mapIndexed { i, n -> InstalledApp("com.example.app$i", n, null) }
        show {
            BlockerScreen(
                state = BlockerUiState(
                    settings = BlockerSettings(enabled = true, mode = BlockMode.WARN, packages = setOf("com.example.app0")),
                    permissions = Permissions(usageAccess = false, blockScreen = false, usageRestricted = true),
                    apps = apps,
                ),
                onBack = {}, onEnabled = {}, onMode = {}, onBlocked = { _, _ -> }, onOpenUsageAccess = {}, onOpenOverlay = {},
            )
        }
        capture("blocker_restricted")
    }

    // ---- Heatmap, rest days, focus sounds ----

    private val yearStats = run {
        // A year of irregular study: busier on weekdays, Saturdays as rest days.
        val rnd = java.util.Random(7)
        val sessions = (0..364).flatMap { d ->
            val date = today.minusDays(d.toLong())
            val weekday = date.dayOfWeek.value <= 5
            val n = when {
                date.dayOfWeek == java.time.DayOfWeek.SATURDAY -> 0
                rnd.nextInt(10) < (if (d < 120) 2 else 4) -> 0
                weekday -> rnd.nextInt(6)
                else -> rnd.nextInt(3)
            }
            (0 until n).map { k ->
                val start = date.atTime(8 + k * 2, 0).atZone(zone).toInstant().toEpochMilli()
                FocusSession(startedAt = start, endedAt = start + 25 * 60_000, focusedSeconds = 25 * 60, completed = true)
            }
        }
        StatsCalculator.compute(sessions, 120, today, zone, restDays = setOf(java.time.DayOfWeek.SATURDAY))
    }

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h2600dp-xxhdpi")
    fun statsHeatmap() {
        show { StatsScreen(yearStats) }
        capture("stats_heatmap", dir = "v090")
    }

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h2600dp-xxhdpi")
    fun statsHeatmapLight() {
        show(dark = false, accent = AccentColor.TEAL) { StatsScreen(yearStats) }
        capture("light_stats_heatmap", dir = "v090")
    }

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h1600dp-xxhdpi")
    fun settingsRestDays() {
        show { SettingsScreen(TimerSettings(restDays = setOf(java.time.DayOfWeek.SATURDAY, java.time.DayOfWeek.SUNDAY)), 0, {}, {}, {}, {}, {}) }
        capture("settings_rest_days", dir = "v090")
    }

    private fun focusWithSound(dark: Boolean, name: String) {
        show(dark = dark) {
            FocusScreen(
                state = FocusUiState(
                    timer = TimerState(TimerPhase.FOCUS, TimerStatus.RUNNING, 25 * 60_000L, 17 * 60_000L + 42_000, completedFocusInCycle = 2),
                    stats = sampleStats,
                    sound = com.focus.core.model.FocusSound.RAIN,
                ),
                onToggle = {}, onStop = {}, onSkip = {},
            )
        }
        capture(name, dir = "v090")
    }

    @Test fun focusSoundChip() = focusWithSound(true, "focus_sound")
    @Test fun focusSoundChipLight() = focusWithSound(false, "light_focus_sound")

    // ---- OLED theme ----

    @Test
    fun oledFocus() {
        show(oled = true) {
            FocusScreen(
                state = FocusUiState(
                    timer = TimerState(TimerPhase.FOCUS, TimerStatus.RUNNING, 25 * 60_000L, 17 * 60_000L + 42_000, completedFocusInCycle = 2),
                    stats = sampleStatsWithSubjects,
                    gamification = sampleGame,
                    subjects = listOf(english, math),
                    selection = FocusSelection(1, null),
                ),
                onToggle = {}, onStop = {}, onSkip = {},
            )
        }
        capture("oled_focus", dir = "oled")
    }

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h1900dp-xxhdpi")
    fun oledStats() {
        show(oled = true) { StatsScreen(sampleStatsWithSubjects) }
        capture("oled_stats", dir = "oled")
    }

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h1600dp-xxhdpi")
    fun oledSettings() {
        show(oled = true) { SettingsScreen(TimerSettings(themeMode = com.focus.core.model.ThemeMode.OLED), 0, {}, {}, {}, {}, {}) }
        capture("oled_settings", dir = "oled")
    }

    // ---- README images (English) ----

    @Test
    @Config(qualifiers = "en-w411dp-h2600dp-xxhdpi")
    fun readmeHeatmap() {
        show { StatsScreen(yearStats) }
        capture("readme_heatmap", dir = "readme")
    }

    @Test
    @Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
    fun readmeLight() {
        show(dark = false, accent = AccentColor.TEAL) {
            GoalsScreen(sampleGoalProgress(), onSave = {}, onDelete = {}, today = today)
        }
        capture("readme_light_goals", dir = "readme")
    }

    @Test
    @Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
    fun readmeOled() {
        show(oled = true) {
            FocusScreen(
                state = FocusUiState(
                    timer = TimerState(TimerPhase.FOCUS, TimerStatus.RUNNING, 25 * 60_000L, 17 * 60_000L + 42_000, completedFocusInCycle = 2),
                    stats = sampleStatsWithSubjects,
                    gamification = sampleGame,
                    subjects = listOf(english, math),
                    selection = FocusSelection(1, null),
                    timerStyle = TimerStyle.GAUGE,
                    sound = com.focus.core.model.FocusSound.RAIN,
                ),
                onToggle = {}, onStop = {}, onSkip = {},
            )
        }
        capture("readme_oled_focus", dir = "readme")
    }

    @Test
    fun bottomBar() {
        val items = listOf(
            com.focus.app.navigation.BottomBarItem("Focus", com.focus.core.designsystem.icon.FocusIcons.Timer),
            com.focus.app.navigation.BottomBarItem("Plan", com.focus.core.designsystem.icon.FocusIcons.Book),
            com.focus.app.navigation.BottomBarItem("Progress", com.focus.core.designsystem.icon.FocusIcons.Chart),
            com.focus.app.navigation.BottomBarItem("Settings", com.focus.core.designsystem.icon.FocusIcons.Tune),
        )
        compose.setContent {
            androidx.compose.foundation.layout.Column {
                listOf(Triple(true, false, 0), Triple(true, true, 2), Triple(false, false, 1), Triple(true, false, -1)).forEach { (dark, oled, sel) ->
                    FocusTheme(accent = AccentColor.BLUE, dark = dark, oled = oled) {
                        com.focus.app.navigation.FocusBottomBar(items = items, selectedIndex = sel, onSelect = {})
                    }
                }
            }
        }
        capture("bottombar")
    }

    // ---- README gallery (English, full app frame with the bottom bar) ----

    private val enEnglish = Subject(1, "English", AccentColor.TEAL)
    private val enMath = Subject(2, "Calculus", AccentColor.AMBER)
    private val enHistory = Subject(3, "History", AccentColor.VIOLET)
    private val enSubjects = listOf(enEnglish, enMath, enHistory)
    private val enStats = StatsCalculator.compute(sampleSessions, 120, today, zone, enSubjects)

    /** Renders [content] the way the app does: status bar gap, screen, then the floating bottom bar. */
    private fun appFrame(
        tab: Int,
        planTab: Int = -1,
        dark: Boolean = true,
        oled: Boolean = false,
        accent: AccentColor = AccentColor.BLUE,
        content: @androidx.compose.runtime.Composable () -> Unit,
    ) {
        compose.setContent {
            FocusTheme(accent = accent, dark = dark, oled = oled) {
                androidx.compose.foundation.layout.Column(Modifier.fillMaxSize().background(FocusColors.Background)) {
                    androidx.compose.foundation.layout.Spacer(Modifier.size(24.dp))
                    androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                        if (planTab >= 0) {
                            com.focus.core.designsystem.component.SegmentedToggle(
                                options = listOf(R.string.plan_subjects, R.string.plan_goals, R.string.plan_routine, R.string.plan_reviews)
                                    .map { androidx.compose.ui.res.stringResource(it) },
                                selectedIndex = planTab,
                                onSelect = {},
                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp).fillMaxWidth(),
                                fill = true,
                            )
                        }
                        Box(Modifier.weight(1f)) { content() }
                    }
                    val icons = com.focus.core.designsystem.icon.FocusIcons
                    com.focus.app.navigation.FocusBottomBar(
                        items = listOf(
                            com.focus.app.navigation.BottomBarItem(androidx.compose.ui.res.stringResource(R.string.nav_focus), icons.Timer),
                            com.focus.app.navigation.BottomBarItem(androidx.compose.ui.res.stringResource(R.string.nav_plan), icons.Book),
                            com.focus.app.navigation.BottomBarItem(androidx.compose.ui.res.stringResource(R.string.nav_progress), icons.Chart),
                            com.focus.app.navigation.BottomBarItem(androidx.compose.ui.res.stringResource(R.string.nav_settings), icons.Tune),
                        ),
                        selectedIndex = tab,
                        onSelect = {},
                    )
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun EnFocus(style: TimerStyle = TimerStyle.RING, sound: com.focus.core.model.FocusSound = com.focus.core.model.FocusSound.NONE) {
        FocusScreen(
            state = FocusUiState(
                timer = TimerState(TimerPhase.FOCUS, TimerStatus.RUNNING, 25 * 60_000L, 17 * 60_000L + 42_000, completedFocusInCycle = 2),
                stats = enStats,
                gamification = sampleGame,
                subjects = listOf(enEnglish, enMath),
                tasks = listOf(StudyTask(10, 1, "Phrasal verbs")),
                selection = FocusSelection(1, 10),
                timerStyle = style,
                sound = sound,
            ),
            onToggle = {}, onStop = {}, onSkip = {},
        )
    }

    private fun enGoals(): GoalsUiState {
        val goals = listOf(
            Goal(1, "25 h of English", 1, 25 * 60, today.minusDays(20), today.plusDays(80)),
            Goal(2, "Calculus exam", 2, 8 * 60, today.minusDays(20), today.plusDays(10)),
            Goal(3, "Read 22 h of history", 3, 22 * 60, today.minusDays(5), today.plusDays(60)),
        )
        return GoalsUiState(goals.map { g -> GoalCalculator.progress(g, enSubjects.firstOrNull { it.id == g.subjectId }, sampleSessions, today, zone) }, enSubjects, loaded = true)
    }

    private fun gallery(name: String) = capture(name, dir = "gallery")

    @Test @Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
    fun galleryFocus() { appFrame(0) { EnFocus(sound = com.focus.core.model.FocusSound.RAIN) }; gallery("focus") }

    @Test @Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
    fun galleryStats() { appFrame(2) { StatsScreen(enStats, game = sampleGame) }; gallery("stats") }

    @Test @Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
    fun gallerySubjects() {
        appFrame(1, planTab = 0) {
            SubjectsScreen(
                state = SubjectsUiState(
                    items = enStats.subjectTimes.map { t ->
                        SubjectItem(t, if (t.subject?.id == 1L) listOf(StudyTask(10, 1, "Phrasal verbs"), StudyTask(11, 1, "Listening: podcast", done = true)) else emptyList())
                    },
                    loaded = true,
                ),
                onAddSubject = { _, _ -> }, onUpdateSubject = {}, onDeleteSubject = {}, onAddTask = { _, _ -> }, onToggleTask = {}, onDeleteTask = {},
                initiallyExpanded = 1,
            )
        }
        gallery("subjects")
    }

    @Test @Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
    fun galleryGoals() { appFrame(1, planTab = 1) { GoalsScreen(enGoals(), onSave = {}, onDelete = {}, today = today) }; gallery("goals") }

    @Test @Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
    fun galleryRoutine() {
        val blocks = listOf(
            RoutineBlock(1, "Calculus", 2, setOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.FRIDAY), 19 * 60, 50, 10),
            RoutineBlock(2, "Listening", 1, java.time.DayOfWeek.entries.toSet() - java.time.DayOfWeek.SUNDAY, 7 * 60 + 30, 25, 5),
            RoutineBlock(3, "History reading", 3, setOf(java.time.DayOfWeek.TUESDAY, java.time.DayOfWeek.THURSDAY, java.time.DayOfWeek.SATURDAY), 14 * 60, 40, null),
            RoutineBlock(4, "Reviews", null, setOf(java.time.DayOfWeek.SUNDAY, java.time.DayOfWeek.WEDNESDAY), 10 * 60, 30, 0),
        )
        appFrame(1, planTab = 2) { RoutineScreen(RoutineUiState(blocks, enSubjects, loaded = true), onSave = {}, onDelete = {}, now = today.atTime(19, 20)) }
        gallery("routine")
    }

    @Test @Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
    fun galleryReviews() {
        val items = listOf(
            ReviewItem(1, "Ch. 3: derivatives", 2, today.minusDays(1), 0),
            ReviewItem(2, "Phrasal verbs", 1, today.minusDays(9), 1),
            ReviewItem(3, "French Revolution", 3, today.minusDays(3), 1),
            ReviewItem(4, "Limits", 2, today.minusDays(20), 2),
            ReviewItem(5, "Present perfect", 1, today.minusDays(40), 3),
        )
        appFrame(1, planTab = 3) { ReviewScreen(ReviewUiState(items, enSubjects, loaded = true), onAdd = {}, onComplete = {}, onDelete = {}, today = today) }
        gallery("reviews")
    }

    @Test @Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
    fun galleryInsights() {
        val rated = sampleSessions.mapIndexed { i, s ->
            val hour = java.time.Instant.ofEpochMilli(s.startedAt).atZone(zone).hour
            s.copy(id = i + 1L, rating = ((if (hour < 12) 4 else 3) + i % 2).coerceAtMost(5), note = if (i % 9 == 0) "Phone in another room helped a lot." else null)
        }
        appFrame(-1) { InsightsScreen(ReflectionCalculator.insights(rated, enSubjects, today, zone), onBack = {}) }
        gallery("reflection")
    }

    @Test @Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
    fun galleryAchievements() { appFrame(-1) { AchievementsScreen(sampleGame, onBack = {}) }; gallery("achievements") }

    @Test @Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
    fun galleryBlocker() {
        appFrame(-1) {
            BlockerScreen(
                state = BlockerUiState(
                    settings = BlockerSettings(enabled = true, mode = BlockMode.WARN, packages = setOf("com.example.app0", "com.example.app2")),
                    permissions = Permissions(usageAccess = true, blockScreen = true),
                    apps = listOf("Instagram", "YouTube", "TikTok", "X", "Netflix").mapIndexed { i, n -> InstalledApp("com.example.app$i", n, null) },
                ),
                onBack = {}, onEnabled = {}, onMode = {}, onBlocked = { _, _ -> }, onOpenUsageAccess = {}, onOpenOverlay = {},
            )
        }
        gallery("blocker")
    }

    @Test @Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
    fun gallerySettings() { appFrame(3) { SettingsScreen(TimerSettings(), 0, {}, {}, {}, {}, {}) }; gallery("settings") }

    @Test @Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
    fun galleryLight() { appFrame(0, dark = false, accent = AccentColor.TEAL) { EnFocus(TimerStyle.TICKS) }; gallery("light") }

    @Test @Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
    fun galleryOled() { appFrame(0, oled = true, accent = AccentColor.VIOLET) { EnFocus(TimerStyle.GAUGE) }; gallery("oled") }

    @Test @Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
    fun galleryLightStats() { appFrame(2, dark = false, accent = AccentColor.CORAL) { StatsScreen(enStats, game = sampleGame) }; gallery("light_stats") }

    private fun galleryStyle(style: TimerStyle) { appFrame(0) { EnFocus(style) }; gallery("style_" + style.name.lowercase()) }
    @Test @Config(qualifiers = "en-w411dp-h891dp-xxhdpi") fun galleryStyleRing() = galleryStyle(TimerStyle.RING)
    @Test @Config(qualifiers = "en-w411dp-h891dp-xxhdpi") fun galleryStyleTicks() = galleryStyle(TimerStyle.TICKS)
    @Test @Config(qualifiers = "en-w411dp-h891dp-xxhdpi") fun galleryStyleGauge() = galleryStyle(TimerStyle.GAUGE)
    @Test @Config(qualifiers = "en-w411dp-h891dp-xxhdpi") fun galleryStyleDial() = galleryStyle(TimerStyle.DIAL)
    @Test @Config(qualifiers = "en-w411dp-h891dp-xxhdpi") fun galleryStyleMinimal() = galleryStyle(TimerStyle.MINIMAL)
}
