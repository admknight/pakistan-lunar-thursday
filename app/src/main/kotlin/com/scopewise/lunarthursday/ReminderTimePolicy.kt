package com.scopewise.lunarthursday

import java.time.LocalTime

/** User-selected Pakistan wall time, independent of the device time zone. */
object ReminderTimePolicy {
    @JvmField val DEFAULT: LocalTime = LocalTime.of(11, 0)

    private fun valid(hour: Int, minute: Int) = hour in 0..23 && minute in 0..59

    @JvmStatic
    fun fromSaved(hour: Int, minute: Int): LocalTime =
        if (valid(hour, minute)) LocalTime.of(hour, minute) else DEFAULT

    @JvmStatic
    fun validate(hour: Int, minute: Int): LocalTime {
        require(valid(hour, minute)) { "Invalid reminder time" }
        return LocalTime.of(hour, minute)
    }
}
