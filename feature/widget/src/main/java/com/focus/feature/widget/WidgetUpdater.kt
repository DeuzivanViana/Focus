package com.focus.feature.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.SystemClock
import android.widget.RemoteViews
import com.focus.core.data.di.ApplicationScope
import com.focus.core.data.repository.SettingsRepository
import com.focus.core.data.repository.StatsRepository
import com.focus.core.model.AccentColor
import com.focus.core.model.FocusStats
import com.focus.core.model.TimerPhase
import com.focus.core.model.TimerStatus
import com.focus.core.timer.TimerController
import com.focus.core.timer.TimerService
import com.focus.core.timer.TimerState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pushes timer state, today's progress and the streak to every placed widget.
 * The countdown itself is a Chronometer inside the RemoteViews, so the widget
 * is only updated when the state changes, not every second.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val controller: TimerController,
    private val stats: StatsRepository,
    private val settings: SettingsRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private data class Snapshot(val timer: TimerState, val stats: FocusStats, val accent: AccentColor)

    @Volatile private var last: Snapshot? = null

    fun start() {
        scope.launch(Dispatchers.Main) {
            val timerKey = controller.state
                .map { it.copy(remainingMillis = if (it.status == TimerStatus.RUNNING) 0 else it.remainingMillis) }
                .distinctUntilChanged()
            combine(timerKey, stats.observeStats(), settings.settings.map { it.accent }) { t, s, a -> Snapshot(t, s, a) }
                .distinctUntilChanged()
                .collect { snap ->
                    last = snap
                    render(snap)
                }
        }
    }

    fun refresh() {
        last?.let(::render) ?: render(Snapshot(controller.state.value, FocusStats(), AccentColor.Default))
    }

    private fun render(snap: Snapshot) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, FocusWidgetProvider::class.java))
        if (ids.isEmpty()) return
        manager.updateAppWidget(ids, build(snap))
    }

    private fun build(snap: Snapshot): RemoteViews {
        val timer = controller.state.value.takeIf { it.phase == snap.timer.phase && it.status == snap.timer.status } ?: snap.timer
        val views = RemoteViews(context.packageName, R.layout.widget_focus)

        views.setTextViewText(
            R.id.widget_phase,
            if (timer.status == TimerStatus.PAUSED) context.getString(R.string.widget_paused) else context.getString(
                when (timer.phase) {
                    TimerPhase.FOCUS -> R.string.widget_phase_focus
                    TimerPhase.SHORT_BREAK -> R.string.widget_phase_short
                    TimerPhase.LONG_BREAK -> R.string.widget_phase_long
                },
            ),
        )
        val streak = snap.stats.currentStreak
        views.setTextViewText(R.id.widget_streak, context.resources.getQuantityString(R.plurals.widget_streak, streak, streak))
        views.setTextViewText(
            R.id.widget_today,
            context.getString(R.string.widget_today, duration(snap.stats.todaySeconds), duration(snap.stats.dailyGoalMinutes * 60L)),
        )

        val end = timer.endAtElapsed
        val remaining = if (timer.status == TimerStatus.RUNNING && end != null) end - SystemClock.elapsedRealtime() else timer.remainingMillis
        // Countdown: base is the end instant; when stopped the value stays frozen at "remaining".
        views.setChronometer(R.id.widget_clock, SystemClock.elapsedRealtime() + remaining.coerceAtLeast(0), null, timer.status == TimerStatus.RUNNING)
        views.setChronometerCountDown(R.id.widget_clock, true)

        views.setTextViewText(
            R.id.widget_button,
            context.getString(
                when (timer.status) {
                    TimerStatus.RUNNING -> R.string.widget_pause
                    TimerStatus.PAUSED -> R.string.widget_resume
                    TimerStatus.IDLE -> R.string.widget_start
                },
            ),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views.setColorStateList(R.id.widget_button, "setBackgroundTintList", ColorStateList.valueOf(snap.accent.argb.toInt()))
        }

        // Button: explicit, immutable intent to our own non-exported service.
        val toggle = Intent(context, TimerService::class.java).setAction(TimerService.ACTION_TOGGLE)
        views.setOnClickPendingIntent(
            R.id.widget_button,
            PendingIntent.getForegroundService(context, REQUEST_TOGGLE, toggle, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
        )
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { launch ->
            launch.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(context, REQUEST_OPEN, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
            )
        }
        return views
    }

    private fun duration(seconds: Long): String {
        val m = seconds / 60
        return if (m >= 60) "%dh %02dm".format(m / 60, m % 60) else "%dm".format(m)
    }

    private companion object {
        const val REQUEST_TOGGLE = 31
        const val REQUEST_OPEN = 32
    }
}
