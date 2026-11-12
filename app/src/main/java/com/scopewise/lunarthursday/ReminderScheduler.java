package com.scopewise.lunarthursday;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.time.Instant;
import java.util.List;

/** Schedules only the next occurrence, then rolls forward after notification. */
final class ReminderScheduler {
    static final String ACTION_FIRE = "com.scopewise.lunarthursday.FIRE_REMINDER";
    static final String EXTRA_TRIGGER = "trigger_millis";
    static final String EXTRA_MONTH = "month_label";
    static final String EXTRA_MONTH_KEY = "month_key";
    static final String EXTRA_BASIS = "basis";
    private static final int REQUEST_CODE = 1017;

    private ReminderScheduler() { }

    static boolean exactAlarmsAllowed(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return alarms != null &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms());
    }

    static void sync(Context context) {
        // Cancel old PendingIntent on schedule changes, boot, and when switched off.
        cancel(context);
        SharedPreferences p = ReminderStore.prefs(context);
        if (!p.getBoolean(ReminderStore.ENABLED, false)) return;
        java.time.LocalTime chosenTime = ReminderStore.reminderTime(context);
        List<LunarEngine.Month> next = LunarEngine.upcoming(
                Instant.now(), ReminderStore.confirmedStarts(context), 1, chosenTime);
        if (next.isEmpty()) return;
        LunarEngine.Month month = next.get(0);
        long when = month.remindAt(chosenTime).toEpochMilli();
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        Intent intent = new Intent(context, ReminderReceiver.class).setAction(ACTION_FIRE);
        intent.putExtra(EXTRA_TRIGGER, when);
        intent.putExtra(EXTRA_MONTH, month.label());
        intent.putExtra(EXTRA_MONTH_KEY, month.key());
        intent.putExtra(EXTRA_BASIS, month.basis.name());
        PendingIntent pi = PendingIntent.getBroadcast(context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            if (exactAlarmsAllowed(context)) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            }
        } catch (SecurityException permissionChanged) {
            // Exact-alarm access may have been revoked between check and use.
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        }
        p.edit().putLong(ReminderStore.NEXT_TRIGGER, when).apply();
    }

    static void cancel(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class).setAction(ACTION_FIRE);
        PendingIntent pi = PendingIntent.getBroadcast(context, REQUEST_CODE, intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (alarms != null && pi != null) alarms.cancel(pi);
        if (pi != null) pi.cancel();
        ReminderStore.prefs(context).edit().remove(ReminderStore.NEXT_TRIGGER).apply();
    }
}
