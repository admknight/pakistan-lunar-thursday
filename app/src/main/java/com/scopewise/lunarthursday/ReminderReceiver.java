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

        // If device time has moved backward, do not notify before 11:00 PKT.
        if (System.currentTimeMillis() + 1000L < received) {
            ReminderScheduler.sync(context);
            return;
        }
        String monthKey = intent.getStringExtra(ReminderScheduler.EXTRA_MONTH_KEY);
        if (monthKey == null) return;
        if (!monthKey.equals(p.getString(ReminderStore.LAST_SENT_KEY, ""))) {
            String label = intent.getStringExtra(ReminderScheduler.EXTRA_MONTH);
            String basis = intent.getStringExtra(ReminderScheduler.EXTRA_BASIS);
            if (!p.getBoolean(ReminderStore.ALARM_MODE, false)
                    || !AlarmRingingService.start(context, label, monthKey, basis, false)) {
                ReminderNotifications.show(context, label, basis);
            }
            // Durable at-most-once monthly dispatch; snoozing uses a distinct guarded timer.
            p.edit().putString(ReminderStore.LAST_SENT_KEY, monthKey).commit();
        }
        ReminderScheduler.sync(context);
    }
}
