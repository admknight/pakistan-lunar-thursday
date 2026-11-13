package com.scopewise.lunarthursday

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Plans the optional reminders that are not tied to a Hijri date: the five
 * daily prayers, Thursday evening and Friday. All are off until switched on.
 */
object ExtraReminderPlanner {
    enum class Kind { FAJR, DHUHR, ASR, MAGHRIB, ISHA, THURSDAY_EVENING, FRIDAY }

    class Options(
        @JvmField val prayerAlerts: Boolean,
        @JvmField val thursdayEvening: Boolean,
        @JvmField val friday: Boolean,
        /** Chosen city, or null. Prayer alerts need it. */
        @JvmField val latitude: Double?,
        @JvmField val longitude: Double?,
        @JvmField val method: PrayerTimes.Method,
        /** Time of the Friday reminder: the user's chosen reminder time. */
        @JvmField val fridayTime: LocalTime
    )

    class Next internal constructor(
        @JvmField val kind: Kind,
        @JvmField val at: Instant,
        @JvmField val localTime: LocalTime
    )

    /** Used for Thursday evening when no city is chosen; not a claimed Maghrib time. */
    @JvmField val EVENING_WITHOUT_CITY: LocalTime = LocalTime.of(18, 30)

    /** The earliest enabled reminder strictly after [now], looking nine days ahead. */
    @JvmStatic
    fun next(now: Instant, options: Options): Next? {
        val today = now.atZone(LunarEngine.PAKISTAN).toLocalDate()
        var best: Next? = null
        fun offer(kind: Kind, date: LocalDate, time: LocalTime?) {
            if (time == null) return
            val at = ZonedDateTime.of(date, time, LunarEngine.PAKISTAN).toInstant()
            if (at.isAfter(now) && (best == null || at.isBefore(best!!.at))) best = Next(kind, at, time)
        }
        val hasCity = options.latitude != null && options.longitude != null
        for (offset in 0..8) {
            val date = today.plusDays(offset.toLong())
            val day = if (hasCity && (options.prayerAlerts || options.thursdayEvening)) {
                try { PrayerTimes.compute(date, options.latitude!!, options.longitude!!, options.method) }
                catch (invalid: RuntimeException) { null }
            } else null
            if (options.prayerAlerts && day != null) {
                offer(Kind.FAJR, date, day.fajr)
                offer(Kind.DHUHR, date, day.dhuhr)
                offer(Kind.ASR, date, day.asr)
                offer(Kind.MAGHRIB, date, day.maghrib)
                offer(Kind.ISHA, date, day.isha)
            }
            if (options.thursdayEvening && date.dayOfWeek == DayOfWeek.THURSDAY) {
                offer(Kind.THURSDAY_EVENING, date, day?.maghrib ?: EVENING_WITHOUT_CITY)
            }
            if (options.friday && date.dayOfWeek == DayOfWeek.FRIDAY) {
                offer(Kind.FRIDAY, date, options.fridayTime)
            }
        }
        return best
    }
}
