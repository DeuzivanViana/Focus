package com.focus.core.timer

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.focus.core.model.TimerPhase
import com.focus.core.model.TimerStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager = NotificationManagerCompat.from(context)

    /** Current accent color (updated by the controller when the user changes it). */
    @Volatile var accentArgb: Int = DEFAULT_ACCENT

    fun createChannels() {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_TIMER, context.getString(R.string.timer_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
                description = context.getString(R.string.timer_channel_desc)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERT, context.getString(R.string.alert_channel_name), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.alert_channel_desc)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )
    }

    /** Starts the foreground service if it is not running yet. */
    fun ensureServiceRunning() {
        if (TimerService.isRunning) return
        try {
            ContextCompat.startForegroundService(context, Intent(context, TimerService::class.java))
        } catch (e: IllegalStateException) {
            // Android 12+ may refuse to start a service from the background.
            Log.w(TAG, "Could not start the timer service", e)
        }
    }

    fun buildOngoing(state: TimerState): Notification {
        val title = phaseLabel(state.phase)
        val builder = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle(title)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setColor(accentArgb)
            .setContentIntent(openAppIntent())

        if (state.status == TimerStatus.RUNNING) {
            builder.setContentText(context.getString(R.string.status_running))
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis() + state.remainingMillis)
                .addAction(0, context.getString(R.string.action_pause), serviceIntent(TimerService.ACTION_PAUSE))
        } else {
            builder.setContentText(context.getString(R.string.status_paused, clock(state.remainingMillis)))
                .setShowWhen(false)
                .addAction(0, context.getString(R.string.action_resume), serviceIntent(TimerService.ACTION_RESUME))
        }
        builder.addAction(0, context.getString(R.string.action_skip), serviceIntent(TimerService.ACTION_SKIP))
        builder.addAction(0, context.getString(R.string.action_stop), serviceIntent(TimerService.ACTION_STOP))
        return builder.build()
    }

    fun notifyPhaseFinished(finished: TimerPhase, next: TimerPhase) {
        if (!canPostNotifications()) return
        val (title, text) = if (finished == TimerPhase.FOCUS) {
            context.getString(R.string.done_focus_title) to
                context.getString(R.string.done_focus_text, phaseLabel(next).lowercase())
        } else {
            context.getString(R.string.done_break_title) to context.getString(R.string.done_break_text)
        }
        val n = NotificationCompat.Builder(context, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle(title)
            .setContentText(text)
            .setColor(accentArgb)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(openAppIntent())
            .build()
        try {
            manager.notify(ID_ALERT, n)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission revoked", e)
        }
    }

    fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun phaseLabel(phase: TimerPhase) = context.getString(
        when (phase) {
            TimerPhase.FOCUS -> R.string.phase_focus
            TimerPhase.SHORT_BREAK -> R.string.phase_short_break
            TimerPhase.LONG_BREAK -> R.string.phase_long_break
        },
    )

    /** Explicit, immutable intent: no other app can redirect or alter it. */
    private fun openAppIntent(): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        launch.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        return PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun serviceIntent(action: String): PendingIntent {
        val intent = Intent(context, TimerService::class.java).setAction(action)
        return PendingIntent.getService(context, action.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun clock(ms: Long): String {
        val s = (ms + 999) / 1000
        return "%02d:%02d".format(s / 60, s % 60)
    }

    companion object {
        const val CHANNEL_TIMER = "timer"
        const val CHANNEL_ALERT = "timer_alerts"
        const val ID_ONGOING = 1001
        const val ID_ALERT = 1002
        private const val DEFAULT_ACCENT = 0xFF4C8DFF.toInt()
        private const val TAG = "TimerNotifier"
    }
}
