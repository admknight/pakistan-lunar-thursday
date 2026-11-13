package com.scopewise.lunarthursday;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/** Receives the scheduled reminder; guards against stale or duplicate alarms. */
public final class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || !ReminderScheduler.ACTION_FIRE.equals(intent.getAction())) return;
        SharedPreferences p = ReminderStore.prefs(context);
        if (!p.getBoolean(ReminderStore.ENABLED, false)) return;
        long received = intent.getLongExtra(ReminderScheduler.EXTRA_TRIGGER, -1);
        long scheduled = p.getLong(ReminderStore.NEXT_TRIGGER, -2);
        if (received <= 0 || received != scheduled) return;

        // If device time has moved backward, do not notify before the saved PKT time.
        if (System.currentTimeMillis() + 1000L < received) {
            ReminderScheduler.sync(context);
            return;
        }
        String monthKey = intent.getStringExtra(ReminderScheduler.EXTRA_MONTH_KEY);
        if (monthKey == null) return;
        // Same month on a different Pakistan date means the first Thursday was
        // corrected after an earlier dispatch; that corrected day must still notify.
        if (!SentReminderPolicy.alreadySent(
                p.getString(ReminderStore.LAST_SENT_KEY, ""), monthKey, received)) {
            String label = intent.getStringExtra(ReminderScheduler.EXTRA_MONTH);
            String basis = intent.getStringExtra(ReminderScheduler.EXTRA_BASIS);
            if ("POSSIBLE".equals(intent.getStringExtra(ReminderScheduler.EXTRA_KIND))) {
                // The less likely Thursday of an unconfirmed month: a quiet notice, never the ringing alarm.
                ReminderNotifications.possible(context, label,
                        intent.getStringExtra(ReminderScheduler.EXTRA_EXPECTED_DATE));
            } else if (!p.getBoolean(ReminderStore.ALARM_MODE, false)
                    || !AlarmRingingService.start(context, label, monthKey, basis, false)) {
                ReminderNotifications.show(context, label, basis);
            }
            // Durable at-most-once dispatch per month and date; snoozing uses a distinct guarded timer.
            p.edit().putString(ReminderStore.LAST_SENT_KEY,
                    SentReminderPolicy.token(monthKey, received)).commit();
        }
        ReminderScheduler.sync(context);
    }
}
