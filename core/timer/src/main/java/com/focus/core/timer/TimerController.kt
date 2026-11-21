package com.focus.core.timer

import android.os.SystemClock
import android.util.Log
import com.focus.core.data.di.ApplicationScope
import com.focus.core.data.repository.FocusSelection
import com.focus.core.data.repository.SelectionRepository
import com.focus.core.data.repository.SessionRepository
import com.focus.core.data.repository.SettingsRepository
import com.focus.core.model.FocusSession
import com.focus.core.model.TimerPhase
import com.focus.core.model.TimerSettings
import com.focus.core.model.TimerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the pomodoro. The UI and [TimerService] only observe
 * [state] and call the commands. All commands run on the main thread.
 *
 * Time is derived from an end instant in elapsedRealtime, so the clock never
 * drifts even if the process goes without CPU for a while.
 */
@Singleton
class TimerController @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val sessionRepository: SessionRepository,
    private val selectionRepository: SelectionRepository,
    private val notifier: TimerNotifier,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private var settings = TimerSettings()
    private var tickJob: Job? = null
    private var focusStartedAtWall: Long = 0L
    private var selection = FocusSelection()

    init {
        appScope.launch(Dispatchers.Main.immediate) {
            selectionRepository.selection.collect { selection = it }
        }
        appScope.launch(Dispatchers.Main.immediate) {
            settingsRepository.settings.collect { s ->
                settings = s
                notifier.accentArgb = s.accent.argb.toInt()
                val st = _state.value
                _state.value = if (st.status == TimerStatus.IDLE) {
                    val d = durationFor(st.phase)
                    st.copy(totalMillis = d, remainingMillis = d, sessionsBeforeLongBreak = s.sessionsBeforeLongBreak)
                } else {
                    st.copy(sessionsBeforeLongBreak = s.sessionsBeforeLongBreak)
                }
            }
        }
    }

    fun toggle() = when (_state.value.status) {
        TimerStatus.RUNNING -> pause()
        else -> start()
    }

    fun start() {
        val st = _state.value
        when (st.status) {
            TimerStatus.RUNNING -> Unit
            TimerStatus.PAUSED -> run(st)
            TimerStatus.IDLE -> {
                if (st.phase == TimerPhase.FOCUS) focusStartedAtWall = System.currentTimeMillis()
                val d = durationFor(st.phase)
                run(st.copy(totalMillis = d, remainingMillis = d))
            }
        }
    }

    fun pause() {
        val st = _state.value
        if (st.status != TimerStatus.RUNNING) return
        tickJob?.cancel()
        _state.value = st.copy(status = TimerStatus.PAUSED, remainingMillis = currentRemaining(st), endAtElapsed = null)
    }

    /** Ends the current cycle. Time already focused (>= 1 min) is saved as a partial session. */
    fun stop() {
        val st = _state.value
        tickJob?.cancel()
        if (st.phase == TimerPhase.FOCUS && st.isActive) savePartial(st)
        val d = durationFor(TimerPhase.FOCUS)
        _state.value = TimerState(
            phase = TimerPhase.FOCUS,
            totalMillis = d,
            remainingMillis = d,
            completedFocusInCycle = 0,
            sessionsBeforeLongBreak = settings.sessionsBeforeLongBreak,
        )
    }

    /** Skips to the next phase. Skipping a focus does not count as a completed pomodoro. */
    fun skip() {
        val st = _state.value
        tickJob?.cancel()
        if (st.phase == TimerPhase.FOCUS && st.isActive) savePartial(st)
        advance(st, focusCompleted = false, autoStart = false)
    }

    private fun run(st: TimerState) {
        val end = SystemClock.elapsedRealtime() + st.remainingMillis
        _state.value = st.copy(status = TimerStatus.RUNNING, endAtElapsed = end)
        notifier.ensureServiceRunning()
        startTicking()
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = appScope.launch(Dispatchers.Main.immediate) {
            while (isActive) {
                val st = _state.value
                if (st.status != TimerStatus.RUNNING || st.endAtElapsed == null) break
                val remaining = st.endAtElapsed - SystemClock.elapsedRealtime()
                if (remaining <= 0L) {
                    onPhaseFinished(st)
                    break
                }
                _state.value = st.copy(remainingMillis = remaining)
                delay(minOf(remaining, TICK_MS))
            }
        }
    }

    private fun onPhaseFinished(st: TimerState) {
        val focusCompleted = st.phase == TimerPhase.FOCUS
        if (focusCompleted) {
            record(
                FocusSession(
                    startedAt = focusStartedAtWall,
                    endedAt = System.currentTimeMillis(),
                    focusedSeconds = st.totalMillis / 1000,
                    completed = true,
                ),
            )
        }
        val autoStart = if (focusCompleted) settings.autoStartBreaks else settings.autoStartFocus
        val next = advance(st, focusCompleted, autoStart)
        notifier.notifyPhaseFinished(finished = st.phase, next = next.phase)
    }

    private fun advance(st: TimerState, focusCompleted: Boolean, autoStart: Boolean): TimerState {
        val n = settings.sessionsBeforeLongBreak
        val count = if (focusCompleted) st.completedFocusInCycle + 1 else st.completedFocusInCycle
        val (nextPhase, nextCount) = when (st.phase) {
            TimerPhase.FOCUS ->
                if (focusCompleted && count >= n) TimerPhase.LONG_BREAK to count
                else TimerPhase.SHORT_BREAK to count
            TimerPhase.SHORT_BREAK -> TimerPhase.FOCUS to count
            TimerPhase.LONG_BREAK -> TimerPhase.FOCUS to 0
        }
        val d = durationFor(nextPhase)
        val next = TimerState(
            phase = nextPhase,
            totalMillis = d,
            remainingMillis = d,
            completedFocusInCycle = nextCount,
            sessionsBeforeLongBreak = n,
        )
        if (autoStart) {
            // Go straight to RUNNING without passing through IDLE (keeps the service alive).
            if (nextPhase == TimerPhase.FOCUS) focusStartedAtWall = System.currentTimeMillis()
            run(next)
        } else {
            _state.value = next
        }
        return next
    }

    private fun savePartial(st: TimerState) {
        val focusedMs = st.totalMillis - currentRemaining(st)
        if (focusedMs < MIN_PARTIAL_MS) return
        record(
            FocusSession(
                startedAt = focusStartedAtWall,
                endedAt = System.currentTimeMillis(),
                focusedSeconds = focusedMs / 1000,
                completed = false,
            ),
        )
    }

    private fun record(session: FocusSession) {
        val linked = session.copy(subjectId = selection.subjectId, taskId = selection.taskId)
        appScope.launch {
            runCatching { sessionRepository.record(linked) }
                .onFailure { Log.w(TAG, "Failed to save session", it) }
        }
    }

    private fun currentRemaining(st: TimerState): Long {
        val end = st.endAtElapsed ?: return st.remainingMillis
        return (end - SystemClock.elapsedRealtime()).coerceIn(0L, st.totalMillis)
    }

    private fun durationFor(phase: TimerPhase): Long = settings.minutesFor(phase) * 60_000L

    private companion object {
        const val TAG = "TimerController"
        const val TICK_MS = 250L
        const val MIN_PARTIAL_MS = 60_000L
    }
}
