package com.scopewise.lunarthursday;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/** Only automatic feed observations are stored. No user date editing. */
final class ReminderStore {
    static final String ENABLED = "enabled";
    static final String REMINDER_HOUR = "monthly_reminder_hour_pkt";
    static final String REMINDER_MINUTE = "monthly_reminder_minute_pkt";
    static final String ALARM_MODE = "ringing_alarm_mode";
    static final String ALARM_SOUND = "ringing_alarm_sound";
    static final String ALARM_VIBRATE = "ringing_alarm_vibration";
    static final String ALARM_TONE_ID = "ringing_alarm_tone_id";
    static final String ALARM_TONE_URI = "ringing_alarm_tone_uri";
    static final String SNOOZE_TRIGGER = "snooze_trigger";
    static final String SNOOZE_KEY = "snooze_month_key";
    static final String SNOOZE_LABEL = "snooze_month_label";
    static final String SNOOZE_BASIS = "snooze_source_basis";
    static final String NEXT_TRIGGER = "next_trigger_millis";
    static final String LAST_SENT_KEY = "last_sent_month";
    static final String CONFIRMED_PREFIX = "live_confirmed_first_day_";
    static final String LAST_REFRESH_ATTEMPT = "live_last_refresh_attempt_ms";
    static final String LAST_REFRESH_SUCCESS = "live_last_refresh_success_ms";
    private static final String FILE = "lunar_thursday_settings";

    private ReminderStore() { }

    static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static LocalTime reminderTime(Context context) {
        SharedPreferences p = prefs(context);
        return ReminderTimePolicy.fromSaved(
                p.getInt(REMINDER_HOUR, 11), p.getInt(REMINDER_MINUTE, 0));
    }

    /** Commit both fields together before rescheduling to avoid mixed values. */
    static boolean setReminderTime(Context context, int hour, int minute) {
        LocalTime valid = ReminderTimePolicy.validate(hour, minute);
        return prefs(context).edit().putInt(REMINDER_HOUR, valid.getHour())
                .putInt(REMINDER_MINUTE, valid.getMinute()).commit();
    }

    static Map<String, LocalDate> confirmedStarts(Context context) {
        Map<String, LocalDate> result = new HashMap<>();
        for (Map.Entry<String, ?> p : prefs(context).getAll().entrySet()) {
            if (!p.getKey().startsWith(CONFIRMED_PREFIX) || !(p.getValue() instanceof String)) continue;
            try {
                result.put(p.getKey().substring(CONFIRMED_PREFIX.length()),
                        LocalDate.parse((String) p.getValue()));
            } catch (RuntimeException ignored) {
                // Ignore a corrupt cache entry; other months still work.
            }
        }
        return result;
    }

    static boolean saveReportedConfirmed(Context context, String monthKey, LocalDate firstDay) {
        SharedPreferences p = prefs(context);
        String key = CONFIRMED_PREFIX + monthKey;
        if (firstDay.toString().equals(p.getString(key, ""))) return false;
        // commit() ensures the schedule uses a durable cache after this method returns.
        return p.edit().putString(key, firstDay.toString()).commit();
    }
}
