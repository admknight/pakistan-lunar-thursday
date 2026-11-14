package com.scopewise.lunarthursday

import android.content.Context
import java.time.Instant

/** The chosen prayer-time method, and today's times for the city chosen in Settings. */
object PrayerStore {
    const val METHOD = "prayer_method"

    @JvmStatic
    fun method(context: Context): PrayerTimes.Method =
        PrayerTimes.Method.fromId(ReminderStore.prefs(context).getString(METHOD, null))

    @JvmStatic
    fun setMethod(context: Context, method: PrayerTimes.Method) {
        ReminderStore.prefs(context).edit().putString(METHOD, method.id).apply()
    }

    /**
     * Today's times, or null when there is neither a chosen city nor a
     * position kept from the optional weather location.
     */
    @JvmStatic
    fun today(context: Context): PrayerTimes.Day? {
        val place = HijriClock.cityCoordinates(context) ?: return null
        val today = Instant.now().atZone(LunarEngine.PAKISTAN).toLocalDate()
        return try {
            PrayerTimes.compute(today, place[0], place[1], method(context))
        } catch (invalid: RuntimeException) {
            null
        }
    }
}
