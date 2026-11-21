package com.focus.feature.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.focus.core.data.plan.ReminderPlanner
import com.focus.core.data.repository.PlanRepository
import com.focus.core.data.repository.ReminderSettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

/** Fired by the reminder alarm: posts what was due at that time and sets the next alarm. */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: ReminderScheduler
    @Inject lateinit var notifier: ReminderNotifier
    @Inject lateinit var plan: PlanRepository
    @Inject lateinit var reminderSettings: ReminderSettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        val at = intent.getLongExtra(EXTRA_AT, 0L)
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (at > 0) {
                    // Recompute from local data instead of trusting the intent beyond the time.
                    ReminderPlanner.upcoming(at - 1, ZoneId.systemDefault(), plan.getBlocks(), plan.getReviews(), reminderSettings.settings.first())
                        .takeWhile { it.at == at }
                        .forEach { notifier.post(it) }
                }
                scheduler.scheduleAfter(maxOf(at, System.currentTimeMillis() - 60_000))
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_AT = "at"
    }
}

/** After a restart, an app update or a clock change, alarms must be set again. */
@AndroidEntryPoint
class RescheduleReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                scheduler.scheduleAfter(System.currentTimeMillis())
            } finally {
                pending.finish()
            }
        }
    }
}
