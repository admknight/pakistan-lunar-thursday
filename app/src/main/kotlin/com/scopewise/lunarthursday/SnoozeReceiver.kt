package com.scopewise.lunarthursday

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** One stored snooze alarm; re-armed after reboot and guarded against stale delivery. */
class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || ACTION != intent.action) return
        val p = ReminderStore.prefs(context)
        val scheduled = p.getLong(ReminderStore.SNOOZE_TRIGGER, 0)
        val received = intent.getLongExtra(ReminderStore.SNOOZE_TRIGGER, -1)
        if (scheduled <= 0 || scheduled != received) return
        if (System.currentTimeMillis() + 1000L < scheduled) {
            rearm(context)
            return
        }
        val key = p.getString(ReminderStore.SNOOZE_KEY, "")
        val label = p.getString(ReminderStore.SNOOZE_LABEL, "Hilal Thursday")
        val basis = p.getString(ReminderStore.SNOOZE_BASIS, "")
        clear(context)
        if (p.getBoolean(ReminderStore.ENABLED, false) && p.getBoolean(ReminderStore.ALARM_MODE, false)) {
            if (!AlarmRingingService.start(context, label, key, basis, false)) {
                ReminderNotifications.show(context, label, basis)
            }
        }
    }

    companion object {
        private const val ACTION = "com.scopewise.lunarthursday.SNOOZE_FIRE"
        private const val REQUEST = 1931
        private const val TEN_MINUTES = 10 * 60 * 1000L

        @JvmStatic
        fun schedule(context: Context, key: String?, label: String?, basis: String?) {
            clear(context)
            val at = System.currentTimeMillis() + TEN_MINUTES
            val stored = ReminderStore.prefs(context).edit()
                .putLong(ReminderStore.SNOOZE_TRIGGER, at)
                .putString(ReminderStore.SNOOZE_KEY, key)
                .putString(ReminderStore.SNOOZE_LABEL, label)
                .putString(ReminderStore.SNOOZE_BASIS, basis)
                .commit()
            if (stored) rearm(context)
        }

        @JvmStatic
        fun rearm(context: Context) {
            val p = ReminderStore.prefs(context)
            val at = p.getLong(ReminderStore.SNOOZE_TRIGGER, 0L)
            if (at <= 0) return
            if (at < System.currentTimeMillis() - 60_000L) {
                clear(context)
                return
            }
            val alarms = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val i = Intent(context, SnoozeReceiver::class.java).setAction(ACTION)
                .putExtra(ReminderStore.SNOOZE_TRIGGER, at)
            val pi = PendingIntent.getBroadcast(context, REQUEST, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            try {
                if (ReminderScheduler.exactAlarmsAllowed(context)) {
                    alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                } else {
                    alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                }
            } catch (revoked: SecurityException) {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        }

        @JvmStatic
        fun clear(context: Context) {
            val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            val i = Intent(context, SnoozeReceiver::class.java).setAction(ACTION)
            val pi = PendingIntent.getBroadcast(context, REQUEST, i,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            if (pi != null) {
                manager?.cancel(pi)
                pi.cancel()
            }
            ReminderStore.prefs(context).edit()
                .remove(ReminderStore.SNOOZE_TRIGGER).remove(ReminderStore.SNOOZE_KEY)
                .remove(ReminderStore.SNOOZE_LABEL).remove(ReminderStore.SNOOZE_BASIS)
                .apply()
        }
    }
}
