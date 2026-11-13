package com.scopewise.lunarthursday

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Receives the scheduled reminder; guards against stale or duplicate alarms. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || ReminderScheduler.ACTION_FIRE != intent.action) return
        val p = ReminderStore.prefs(context)
        if (!p.getBoolean(ReminderStore.ENABLED, false)) return
        val received = intent.getLongExtra(ReminderScheduler.EXTRA_TRIGGER, -1)
        val scheduled = p.getLong(ReminderStore.NEXT_TRIGGER, -2)
        if (received <= 0 || received != scheduled) return

        // If device time has moved backward, do not notify before the saved PKT time.
        if (System.currentTimeMillis() + 1000L < received) {
            ReminderScheduler.sync(context)
            return
        }
        val monthKey = intent.getStringExtra(ReminderScheduler.EXTRA_MONTH_KEY) ?: return
        // Same month on a different Pakistan date means the first Thursday was
        // corrected after an earlier dispatch; that corrected day must still notify.
        if (!SentReminderPolicy.alreadySent(p.getString(ReminderStore.LAST_SENT_KEY, ""), monthKey, received)) {
            val label = intent.getStringExtra(ReminderScheduler.EXTRA_MONTH)
            val basis = intent.getStringExtra(ReminderScheduler.EXTRA_BASIS)
            if ("POSSIBLE" == intent.getStringExtra(ReminderScheduler.EXTRA_KIND)) {
                // The less likely Thursday of an unconfirmed month: a quiet notice, never the ringing alarm.
                ReminderNotifications.possible(context, label,
                    intent.getStringExtra(ReminderScheduler.EXTRA_EXPECTED_DATE))
            } else if (!p.getBoolean(ReminderStore.ALARM_MODE, false)
                || !AlarmRingingService.start(context, label, monthKey, basis, false)) {
                ReminderNotifications.show(context, label, basis)
            }
            // Durable at-most-once dispatch per month and date; snoozing uses a distinct guarded timer.
            p.edit().putString(ReminderStore.LAST_SENT_KEY, SentReminderPolicy.token(monthKey, received)).commit()
        }
        ReminderScheduler.sync(context)
    }
}
