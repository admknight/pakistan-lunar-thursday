package com.scopewise.lunarthursday

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.Instant
import java.time.LocalDate

/**
 * Arms one alarm for the next day with observances to announce. It uses its
 * own alarm and never touches the first-Thursday reminder.
 */
object EventReminderScheduler {
    const val ACTION_FIRE = "com.scopewise.lunarthursday.FIRE_EVENT_REMINDER"
    const val EXTRA_TRIGGER = "event_trigger_millis"
    const val EXTRA_DATE = "event_notify_date"
    const val NEXT_TRIGGER = "events_next_trigger_millis"
    const val LAST_SENT_DATE = "events_last_sent_date"
    private const val REQUEST_CODE = 1018

    /** Cancel and re-arm from current data and settings. Never throws. */
    @JvmStatic
    fun sync(context: Context) {
        try {
            cancel(context)
            val settings = ReminderStore.prefs(context)
            if (!settings.getBoolean(ReminderStore.ENABLED, false) || !EventsStore.remindersOn(context)) return
            val chosenTime = ReminderStore.reminderTime(context)
            val batch = EventReminderPlanner.next(
                EventsStore.events(context), Instant.now(), ReminderStore.confirmedStarts(context),
                EventsStore.enabledSets(context), EventsStore.remindIds(context), chosenTime) ?: return
            val alarms = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val at = batch.triggerAt(chosenTime).toEpochMilli()
            val intent = Intent(context, EventReminderReceiver::class.java).setAction(ACTION_FIRE)
                .putExtra(EXTRA_TRIGGER, at)
                .putExtra(EXTRA_DATE, batch.notifyDate.toString())
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
            // Observance reminders must never break the first-Thursday reminder.
        }
    }

    @JvmStatic
    fun cancel(context: Context) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(context, EventReminderReceiver::class.java).setAction(ACTION_FIRE)
        val pending = PendingIntent.getBroadcast(context, REQUEST_CODE, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pending != null) {
            alarms?.cancel(pending)
            pending.cancel()
        }
        ReminderStore.prefs(context).edit().remove(NEXT_TRIGGER).apply()
    }
}

/** Shows the day's observances as one quiet notification, then arms the next day. */
class EventReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != EventReminderScheduler.ACTION_FIRE) return
        try {
            announce(context, intent)
        } catch (ignored: RuntimeException) {
            // Fall through to re-arm.
        }
        EventReminderScheduler.sync(context)
    }

    private fun announce(context: Context, intent: Intent) {
        val settings = ReminderStore.prefs(context)
        if (!settings.getBoolean(ReminderStore.ENABLED, false) || !EventsStore.remindersOn(context)) return
        val at = intent.getLongExtra(EventReminderScheduler.EXTRA_TRIGGER, -1L)
        val now = System.currentTimeMillis()
        // Only the alarm that is currently armed counts, not earlier than a minute before its time
        // and not more than twelve hours late.
        if (at <= 0 || at != settings.getLong(EventReminderScheduler.NEXT_TRIGGER, -1L)) return
        if (now < at - 60_000L || now - at > 12 * 60 * 60 * 1000L) return
        val dateText = intent.getStringExtra(EventReminderScheduler.EXTRA_DATE) ?: return
        if (dateText == settings.getString(EventReminderScheduler.LAST_SENT_DATE, "")) return
        val date = LocalDate.parse(dateText)
        // Rebuilt from current data, so a late date correction or a changed setting is respected.
        val items = EventReminderPlanner.onDate(
            EventsStore.events(context), date, ReminderStore.confirmedStarts(context),
            EventsStore.enabledSets(context), EventsStore.remindIds(context))
        if (items.isEmpty()) return

        val localized = AppLanguage.wrap(context)
        val title: String
        val body = StringBuilder()
        if (items.size == 1) {
            val only = items[0]
            title = localized.getString(
                if (only.event.eve) R.string.events_notify_tonight else R.string.events_notify_today,
                EventsStore.name(context, only.event))
        } else {
            title = localized.getString(R.string.events_notify_several)
            for (item in items) {
                body.append(EventsStore.name(context, item.event))
                if (item.event.eve) body.append(" (").append(localized.getString(R.string.events_notify_tonight_tag)).append(")")
                body.append('\n')
            }
        }
        val first = items[0]
        body.append(String.format(AppLanguage.locale(context), "%d", first.month.hijriDay(first.date)))
            .append(' ').append(UiText.month(context, first.month))
        if (items.any { !it.confirmed }) {
            body.append('\n').append(localized.getString(R.string.events_details_unconfirmed))
        }
        // Mark as sent first: at most once per day even if showing it fails.
        settings.edit().putString(EventReminderScheduler.LAST_SENT_DATE, dateText).commit()
        ReminderNotifications.observance(context, title, body.toString())
    }
}
