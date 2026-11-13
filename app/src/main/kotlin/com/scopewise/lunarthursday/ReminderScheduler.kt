package com.scopewise.lunarthursday

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.Instant

/** Schedules only the next occurrence, then rolls forward after notification. */
object ReminderScheduler {
    const val ACTION_FIRE = "com.scopewise.lunarthursday.FIRE_REMINDER"
    const val EXTRA_TRIGGER = "trigger_millis"
    const val EXTRA_MONTH = "month_label"
    const val EXTRA_MONTH_KEY = "month_key"
    const val EXTRA_BASIS = "basis"
    const val EXTRA_KIND = "reminder_kind"
    const val EXTRA_EXPECTED_DATE = "expected_thursday"
    private const val REQUEST_CODE = 1017

    @JvmStatic
    fun exactAlarmsAllowed(context: Context): Boolean {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        return alarms != null &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms())
    }

    @JvmStatic
    fun sync(context: Context) {
        // Stored dates or settings may have changed; keep any home-screen widget current.
        HijriWidgetProvider.refreshAll(context)
        // Observance reminders use their own alarm and re-arm from the same data.
        EventReminderScheduler.sync(context)
        ExtraReminderScheduler.sync(context)
        // Cancel old PendingIntent on schedule changes, boot, and when switched off.
        cancel(context)
        val p = ReminderStore.prefs(context)
        if (!p.getBoolean(ReminderStore.ENABLED, false)) return
        val chosenTime = ReminderStore.reminderTime(context)
        // Plans from every day the month can still start on, so a month whose
        // first Thursday depends on the sighting is never passed in silence.
        val next = ReminderPlanner.next(Instant.now(), ReminderStore.confirmedStarts(context), chosenTime) ?: return
        val month = next.month
        val at = next.triggerAt(chosenTime).toEpochMilli()
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_FIRE)
        intent.putExtra(EXTRA_TRIGGER, at)
        intent.putExtra(EXTRA_MONTH, month.label())
        intent.putExtra(EXTRA_MONTH_KEY, month.key())
        intent.putExtra(EXTRA_BASIS, month.basis.name)
        intent.putExtra(EXTRA_KIND, next.kind.name)
        intent.putExtra(EXTRA_EXPECTED_DATE, next.expectedThursday.toString())
        val pi = PendingIntent.getBroadcast(context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try {
            if (exactAlarmsAllowed(context)) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (permissionChanged: SecurityException) {
            // Exact-alarm access may have been revoked between check and use.
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
        p.edit().putLong(ReminderStore.NEXT_TRIGGER, at).apply()
    }

    @JvmStatic
    fun cancel(context: Context) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_FIRE)
        val pi = PendingIntent.getBroadcast(context, REQUEST_CODE, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (alarms != null && pi != null) alarms.cancel(pi)
        pi?.cancel()
        ReminderStore.prefs(context).edit().remove(ReminderStore.NEXT_TRIGGER).apply()
    }
}
