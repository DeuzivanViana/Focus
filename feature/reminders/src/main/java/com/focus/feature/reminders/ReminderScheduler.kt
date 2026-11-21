package com.focus.feature.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.focus.core.data.plan.ReminderPlanner
import com.focus.core.data.repository.PlanRepository
import com.focus.core.data.repository.ReminderSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps one alarm set for the next reminder. Uses an inexact window alarm
 * (AlarmManager.setWindow), which needs no special permission: reminders land
 * within a few minutes of their time, which is plenty for study reminders and
 * stays within Google Play's exact-alarm policy.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val plan: PlanRepository,
    private val reminderSettings: ReminderSettingsRepository,
    private val notifier: ReminderNotifier,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var started = false

    /** Reschedules whenever the routine, the reviews or the reminder settings change. */
    fun start() {
        if (started) return
        started = true
        notifier.createChannel()
        scope.launch {
            combine(plan.observeBlocks(), plan.observeReviews(), reminderSettings.settings) { _, _, _ -> Unit }
                .collectLatest { scheduleAfter(System.currentTimeMillis()) }
        }
    }

    /** Sets the alarm for the first reminder strictly after [afterMillis], or cancels it when there is none. */
    suspend fun scheduleAfter(afterMillis: Long) {
        val next = ReminderPlanner.next(
            afterMillis,
            ZoneId.systemDefault(),
            plan.getBlocks(),
            plan.getReviews(),
            reminderSettings.settings.first(),
        ).firstOrNull()
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = pendingIntent(next?.at ?: 0L)
        if (next == null) {
            alarms.cancel(pending)
        } else {
            alarms.setWindow(AlarmManager.RTC_WAKEUP, next.at, WINDOW_MS, pending)
        }
    }

    private fun pendingIntent(at: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).putExtra(ReminderReceiver.EXTRA_AT, at)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private companion object {
        const val WINDOW_MS = 3 * 60_000L
    }
}
