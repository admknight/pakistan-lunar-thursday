package com.scopewise.lunarthursday;
import java.time.LocalTime;
/** User-selected Pakistan wall time, independent of the device time zone. */
final class ReminderTimePolicy {
    static final LocalTime DEFAULT = LocalTime.of(11, 0);
    private ReminderTimePolicy() { }
    static LocalTime fromSaved(int hour, int minute) {
        return hour < 0 || hour > 23 || minute < 0 || minute > 59
                ? DEFAULT : LocalTime.of(hour, minute);
    }
    static LocalTime validate(int hour, int minute) {
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59)
            throw new IllegalArgumentException("Invalid reminder time");
        return LocalTime.of(hour, minute);
    }
}
