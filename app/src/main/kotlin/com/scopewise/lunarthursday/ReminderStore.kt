package com.scopewise.lunarthursday

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
import java.time.LocalTime

/** Only automatic source observations are stored. No user date editing. */
object ReminderStore {
    const val ENABLED = "enabled"
    const val REMINDER_HOUR = "monthly_reminder_hour_pkt"
    const val REMINDER_MINUTE = "monthly_reminder_minute_pkt"
    const val ALARM_MODE = "ringing_alarm_mode"
    const val ALARM_SOUND = "ringing_alarm_sound"
    const val ALARM_VIBRATE = "ringing_alarm_vibration"
    const val ALARM_TONE_ID = "ringing_alarm_tone_id"
    const val ALARM_TONE_URI = "ringing_alarm_tone_uri"
    const val SNOOZE_TRIGGER = "snooze_trigger"
    const val SNOOZE_KEY = "snooze_month_key"
    const val SNOOZE_LABEL = "snooze_month_label"
    const val SNOOZE_BASIS = "snooze_source_basis"
    const val NEXT_TRIGGER = "next_trigger_millis"
    const val LAST_SENT_KEY = "last_sent_month"
    const val CONFIRMED_PREFIX = "live_confirmed_first_day_"
    const val LAST_REFRESH_ATTEMPT = "live_last_refresh_attempt_ms"
    const val LAST_REFRESH_SUCCESS = "live_last_refresh_success_ms"
    private const val FILE = "lunar_thursday_settings"

    @JvmStatic
    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    @JvmStatic
    fun reminderTime(context: Context): LocalTime {
        val p = prefs(context)
        return ReminderTimePolicy.fromSaved(p.getInt(REMINDER_HOUR, 11), p.getInt(REMINDER_MINUTE, 0))
    }

    /** Commit both fields together before rescheduling to avoid mixed values. */
    @JvmStatic
    fun setReminderTime(context: Context, hour: Int, minute: Int): Boolean {
        val valid = ReminderTimePolicy.validate(hour, minute)
        return prefs(context).edit().putInt(REMINDER_HOUR, valid.hour)
            .putInt(REMINDER_MINUTE, valid.minute).commit()
    }

    @JvmStatic
    fun confirmedStarts(context: Context): Map<String, LocalDate> {
        val result = HashMap<String, LocalDate>()
        for ((key, value) in prefs(context).all) {
            if (!key.startsWith(CONFIRMED_PREFIX) || value !is String) continue
            try {
                result[key.substring(CONFIRMED_PREFIX.length)] = LocalDate.parse(value)
            } catch (ignored: RuntimeException) {
                // Ignore a corrupt cache entry; other months still work.
            }
        }
        return result
    }

    @JvmStatic
    fun saveReportedConfirmed(context: Context, monthKey: String, firstDay: LocalDate): Boolean {
        val p = prefs(context)
        val key = CONFIRMED_PREFIX + monthKey
        if (firstDay.toString() == p.getString(key, "")) return false
        // commit() ensures the schedule uses a durable cache after this method returns.
        return p.edit().putString(key, firstDay.toString()).commit()
    }
}
