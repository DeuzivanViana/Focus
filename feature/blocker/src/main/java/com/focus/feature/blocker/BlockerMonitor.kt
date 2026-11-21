package com.focus.feature.blocker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.os.PowerManager
import android.app.KeyguardManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.focus.core.model.TimerPhase
import com.focus.core.model.TimerStatus
import com.focus.core.timer.TimerController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs only while a focus phase is running and the user turned the blocker on.
 *
 * With Usage Access it watches the foreground app and reacts to the apps in the list.
 * Without it (not granted, or greyed out by Android's "Restricted settings") it falls
 * back to a permission-free reminder: if the user leaves Focus during a focus and stays
 * away for a few seconds with the screen on, one notification nudges them back.
 * Nothing is stored or sent anywhere.
 */
@Singleton
class BlockerMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timer: TimerController,
    private val repository: BlockerRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var started = false

    /** Whether any Focus screen is visible (process-wide lifecycle). */
    private val appVisible = MutableStateFlow(true)

    fun start() {
        if (started) return
        started = true
        createChannel()
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> appVisible.value = true
                    Lifecycle.Event.ON_STOP -> appVisible.value = false
                    else -> Unit
                }
            },
        )
        scope.launch {
            val focusing = timer.state
                .map { it.status == TimerStatus.RUNNING && it.phase == TimerPhase.FOCUS }
                .distinctUntilChanged()
            combine(focusing, repository.settings) { active, settings -> settings.takeIf { active && it.enabled } }
                .distinctUntilChanged()
                .collectLatest { settings ->
                    if (settings == null) {
                        cancelWarning()
                        return@collectLatest
                    }
                    if (BlockerPermissions.hasUsageAccess(context) && settings.packages.isNotEmpty()) watch(settings)
                    // No Usage Access (or it was revoked mid-focus): permission-free reminder.
                    if (!BlockerPermissions.hasUsageAccess(context)) watchLeaving()
                }
        }
    }

    /** One reminder each time the user leaves Focus and stays away, screen on and unlocked. */
    private suspend fun watchLeaving() {
        appVisible.collectLatest { visible ->
            if (visible) {
                cancelWarning()
                return@collectLatest
            }
            delay(LEAVE_GRACE_MS)
            if (isUserLookingAtAnotherApp()) warnLeft()
        }
    }

    private fun isUserLookingAtAnotherApp(): Boolean {
        val power = context.getSystemService(PowerManager::class.java)
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        // Locking the phone during a focus is fine: only warn when the screen is on and unlocked.
        return power?.isInteractive == true && keyguard?.isKeyguardLocked != true
    }

    private fun warnLeft() {
        notifyWarning(context.getString(R.string.blocker_left_text, minutesLeft()))
    }

    private suspend fun watch(settings: BlockerSettings) {
        val usage = context.getSystemService(UsageStatsManager::class.java) ?: return
        var since = System.currentTimeMillis() - LOOKBACK_MS
        var foreground: String? = null
        while (currentCoroutineContext().isActive) {
            if (!BlockerPermissions.hasUsageAccess(context)) return
            val now = System.currentTimeMillis()
            val latest = latestResumed(usage, since, now)
            since = now
            if (latest != null && latest != foreground) {
                foreground = latest
                if (latest in settings.packages) onBlockedAppOpened(latest, settings.mode)
            }
            delay(POLL_MS)
        }
    }

    private fun latestResumed(usage: UsageStatsManager, from: Long, to: Long): String? {
        val events = runCatching { usage.queryEvents(from, to) }.getOrNull() ?: return null
        val event = UsageEvents.Event()
        var last: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            @Suppress("DEPRECATION")
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) last = event.packageName
        }
        return last
    }

    private fun onBlockedAppOpened(packageName: String, mode: BlockMode) {
        if (mode == BlockMode.BLOCK && BlockerPermissions.canShowBlockScreen(context)) {
            val intent = Intent(context, BlockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(BlockActivity.EXTRA_PACKAGE, packageName)
            runCatching { context.startActivity(intent) }.onSuccess { return }
        }
        warn(packageName)
    }

    private fun warn(packageName: String) {
        notifyWarning(context.getString(R.string.blocker_warn_text, appLabel(context, packageName), minutesLeft()))
    }

    private fun minutesLeft(): Long {
        val st = timer.state.value
        return ((st.endAtElapsed ?: SystemClock.elapsedRealtime()) - SystemClock.elapsedRealtime()).coerceAtLeast(0) / 60_000 + 1
    }

    private fun notifyWarning(text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(com.focus.core.timer.R.drawable.ic_stat_focus)
            .setContentTitle(context.getString(R.string.blocker_warn_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setTimeoutAfter(WARNING_TIMEOUT_MS)
            .setContentIntent(open)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, n) }
    }

    private fun cancelWarning() = NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL, context.getString(R.string.blocker_channel_name), NotificationManager.IMPORTANCE_HIGH)
            .apply { setShowBadge(false) }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL = "blocker"
        const val NOTIFICATION_ID = 4100
        const val POLL_MS = 1_000L
        const val LOOKBACK_MS = 3_000L
        const val WARNING_TIMEOUT_MS = 15_000L
        const val LEAVE_GRACE_MS = 8_000L
    }
}
