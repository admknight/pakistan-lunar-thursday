package com.scopewise.lunarthursday

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * One alarm for the next optional reminder: a prayer time, Thursday evening
 * or Friday. Separate from the first-Thursday and observance alarms.
 */
object ExtraReminderScheduler {
    const val PRAYER_ALERTS = "extra_prayer_alerts"
    const val THURSDAY_EVENING = "extra_thursday_evening"
    const val FRIDAY = "extra_friday"
    const val ACTION_FIRE = "com.scopewise.lunarthursday.FIRE_EXTRA_REMINDER"
    const val EXTRA_TRIGGER = "extra_trigger_millis"
    const val EXTRA_KIND = "extra_kind"
    const val NEXT_TRIGGER = "extra_next_trigger_millis"
    private const val REQUEST_CODE = 1019

    @JvmStatic
    fun isOn(context: Context, key: String): Boolean = ReminderStore.prefs(context).getBoolean(key, false)

    @JvmStatic
    fun setOn(context: Context, key: String, on: Boolean) {
        ReminderStore.prefs(context).edit().putBoolean(key, on).apply()
        sync(context)
    }

    internal fun options(context: Context): ExtraReminderPlanner.Options {
        val place = HijriClock.cityCoordinates(context)
        return ExtraReminderPlanner.Options(
            isOn(context, PRAYER_ALERTS), isOn(context, THURSDAY_EVENING), isOn(context, FRIDAY),
            place?.get(0), place?.get(1), PrayerStore.method(context), ReminderStore.reminderTime(context))
    }

    /** Cancel and re-arm from current settings. Never throws. */
    @JvmStatic
    fun sync(context: Context) {
        try {
            cancel(context)
            val settings = ReminderStore.prefs(context)
            if (!settings.getBoolean(ReminderStore.ENABLED, false)) return
            val next = ExtraReminderPlanner.next(Instant.now(), options(context)) ?: return
            val alarms = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val at = next.at.toEpochMilli()
            val intent = Intent(context, ExtraReminderReceiver::class.java).setAction(ACTION_FIRE)
                .putExtra(EXTRA_TRIGGER, at)
                .putExtra(EXTRA_KIND, next.kind.name)
            val pending = PendingIntent.getBroadcast(context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            try {
                if (ReminderScheduler.exactAlarmsAllowed(context)) {
                    alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
                } else {
                    alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
                }
            } catch (permissionChanged: SecurityException) {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            }
            settings.edit().putLong(NEXT_TRIGGER, at).apply()
        } catch (ignored: RuntimeException) {
            // Optional reminders must never break the first-Thursday reminder.
        }
    }

    @JvmStatic
    fun cancel(context: Context) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(context, ExtraReminderReceiver::class.java).setAction(ACTION_FIRE)
        val pending = PendingIntent.getBroadcast(context, REQUEST_CODE, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pending != null) {
            alarms?.cancel(pending)
            pending.cancel()
        }
        ReminderStore.prefs(context).edit().remove(NEXT_TRIGGER).apply()
    }
}

/** Shows the reminder as an ordinary notification, then arms the next one. */
class ExtraReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ExtraReminderScheduler.ACTION_FIRE) return
        try {
            announce(context, intent)
        } catch (ignored: RuntimeException) {
            // Fall through to re-arm.
        }
        ExtraReminderScheduler.sync(context)
    }

    private fun announce(context: Context, intent: Intent) {
        val settings = ReminderStore.prefs(context)
        if (!settings.getBoolean(ReminderStore.ENABLED, false)) return
        val at = intent.getLongExtra(ExtraReminderScheduler.EXTRA_TRIGGER, -1L)
        if (at <= 0 || at != settings.getLong(ExtraReminderScheduler.NEXT_TRIGGER, -1L)) return
        val kind = try {
            ExtraReminderPlanner.Kind.valueOf(intent.getStringExtra(ExtraReminderScheduler.EXTRA_KIND) ?: return)
        } catch (unknown: IllegalArgumentException) { return }
        val now = System.currentTimeMillis()
        val weekly = kind == ExtraReminderPlanner.Kind.THURSDAY_EVENING || kind == ExtraReminderPlanner.Kind.FRIDAY
        // A prayer alert that arrives more than twenty minutes late is no longer useful.
        val lateLimit = if (weekly) 6 * 60 * 60 * 1000L else 20 * 60 * 1000L
        if (now < at - 60_000L || now - at > lateLimit) return
        // The setting may have been switched off since the alarm was armed.
        val key = when (kind) {
            ExtraReminderPlanner.Kind.THURSDAY_EVENING -> ExtraReminderScheduler.THURSDAY_EVENING
            ExtraReminderPlanner.Kind.FRIDAY -> ExtraReminderScheduler.FRIDAY
            else -> ExtraReminderScheduler.PRAYER_ALERTS
        }
        if (!ExtraReminderScheduler.isOn(context, key)) return

        val localized = AppLanguage.wrap(context)
        val clock = DateTimeFormatter.ofPattern("h:mm a", AppLanguage.locale(context))
            .format(Instant.ofEpochMilli(at).atZone(LunarEngine.PAKISTAN).toLocalTime())
        val title = when (kind) {
            ExtraReminderPlanner.Kind.FAJR -> localized.getString(R.string.prayer_fajr)
            ExtraReminderPlanner.Kind.DHUHR -> localized.getString(R.string.prayer_dhuhr)
            ExtraReminderPlanner.Kind.ASR -> localized.getString(R.string.prayer_asr)
            ExtraReminderPlanner.Kind.MAGHRIB -> localized.getString(R.string.prayer_maghrib)
            ExtraReminderPlanner.Kind.ISHA -> localized.getString(R.string.prayer_isha)
            ExtraReminderPlanner.Kind.THURSDAY_EVENING -> localized.getString(R.string.extra_thursday_title)
            ExtraReminderPlanner.Kind.FRIDAY -> localized.getString(R.string.extra_friday_title)
        }
        val body = if (weekly) clock else localized.getString(R.string.extra_prayer_body, clock)
        ReminderNotifications.extra(context, title, body)
    }
}
