package com.scopewise.lunarthursday;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/** One stored snooze alarm; re-armed after reboot and guarded against stale delivery. */
public final class SnoozeReceiver extends BroadcastReceiver {
    private static final String ACTION = "com.scopewise.lunarthursday.SNOOZE_FIRE";
    private static final int REQUEST = 1931;
    private static final long TEN_MINUTES = 10 * 60 * 1000L;

    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION.equals(intent.getAction())) return;
        SharedPreferences p = ReminderStore.prefs(context);
        long scheduled = p.getLong(ReminderStore.SNOOZE_TRIGGER, 0);
        long received = intent.getLongExtra(ReminderStore.SNOOZE_TRIGGER, -1);
        if (scheduled <= 0 || scheduled != received) return;
        if (System.currentTimeMillis() + 1000L < scheduled) { rearm(context); return; }
        String key = p.getString(ReminderStore.SNOOZE_KEY, "");
        String label = p.getString(ReminderStore.SNOOZE_LABEL, "Lunar Thursday");
        String basis = p.getString(ReminderStore.SNOOZE_BASIS, "");
        clear(context);
        if (p.getBoolean(ReminderStore.ENABLED, false) && p.getBoolean(ReminderStore.ALARM_MODE, false)) {
            if (!AlarmRingingService.start(context, label, key, basis, false)) {
                ReminderNotifications.show(context, label, basis);
            }
        }
    }

    static void schedule(Context context, String key, String label, String basis) {
        clear(context);
        long when = System.currentTimeMillis() + TEN_MINUTES;
        boolean stored = ReminderStore.prefs(context).edit()
                .putLong(ReminderStore.SNOOZE_TRIGGER, when)
                .putString(ReminderStore.SNOOZE_KEY, key)
                .putString(ReminderStore.SNOOZE_LABEL, label)
                .putString(ReminderStore.SNOOZE_BASIS, basis)
                .commit();
        if (stored) rearm(context);
    }

    static void rearm(Context context) {
        SharedPreferences p = ReminderStore.prefs(context);
        long when = p.getLong(ReminderStore.SNOOZE_TRIGGER, 0L);
        if (when <= 0) return;
        if (when < System.currentTimeMillis() - 60_000L) {
            clear(context);
            return;
        }
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        Intent i = new Intent(context, SnoozeReceiver.class).setAction(ACTION)
                .putExtra(ReminderStore.SNOOZE_TRIGGER, when);
        PendingIntent pi = PendingIntent.getBroadcast(context, REQUEST, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            if (ReminderScheduler.exactAlarmsAllowed(context)) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            }
        } catch (SecurityException revoked) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        }
    }

    static void clear(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, SnoozeReceiver.class).setAction(ACTION);
        PendingIntent pi = PendingIntent.getBroadcast(context, REQUEST, i,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) {
            if (manager != null) manager.cancel(pi);
            pi.cancel();
        }
        ReminderStore.prefs(context).edit()
                .remove(ReminderStore.SNOOZE_TRIGGER).remove(ReminderStore.SNOOZE_KEY)
                .remove(ReminderStore.SNOOZE_LABEL).remove(ReminderStore.SNOOZE_BASIS)
                .apply();
    }
}
