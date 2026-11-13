package com.scopewise.lunarthursday

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.function.Function

/** Source refresh windows near possible last days; never proof of moon sighting. */
object MonthEndChecks {
    @JvmField val PKT: ZoneId = ZoneId.of("Asia/Karachi")
    // No location? A late-evening fallback, NOT a claimed Maghrib prayer time.
    @JvmField val UNKNOWN_LOCATION_EVENING: LocalTime = LocalTime.of(20, 45)
    @JvmField val AFTER_MIDNIGHT: LocalTime = LocalTime.of(0, 15)
    @JvmField val MORNING: LocalTime = LocalTime.of(7, 0)
    const val HORIZON_DAYS = 45L

    class Slot internal constructor(
        @JvmField val slot: Int,
        @JvmField val `when`: Instant,
        @JvmField val description: String
    )

    /**
     * Check at Maghrib+10m (or fallback), 00:15, 07:00 on both potential
     * final days, then 75m before the next selected first-Thursday reminder.
     */
    @JvmStatic
    fun plan(now: Instant?, firstCivilDay: LocalDate?, nextMonthStart: LocalDate?,
             localSunset: Function<LocalDate, LocalTime?>?, reminderTime: LocalTime?): List<Slot> {
        if (now == null || firstCivilDay == null || nextMonthStart == null ||
            localSunset == null || reminderTime == null) {
            throw IllegalArgumentException("Missing month-end planning input")
        }
        val result = ArrayList<Slot>()
        val until = now.plusSeconds(HORIZON_DAYS * 86400L)
        for (possibleEnd in 0 until 2) {
            val day = firstCivilDay.plusDays(28L + possibleEnd)
            val sunset = localSunset.apply(day)
            val evening = sunset?.plusMinutes(10) ?: UNKNOWN_LOCATION_EVENING
            add(result, possibleEnd * 3, day, evening, now, until, "month-end evening")
            add(result, possibleEnd * 3 + 1, day.plusDays(1), AFTER_MIDNIGHT, now, until, "after-midnight feed follow-up")
            add(result, possibleEnd * 3 + 2, day.plusDays(1), MORNING, now, until, "morning feed follow-up")
        }
        if (!nextMonthStart.isBefore(firstCivilDay.plusDays(28)) &&
            !nextMonthStart.isAfter(firstCivilDay.plusDays(31))) {
            val reminderDay = LunarEngine.firstThursday(nextMonthStart)
            val before = reminderDay.atTime(reminderTime).atZone(PKT).toInstant().minusSeconds(75 * 60L)
            if (before.isAfter(now) && !before.isAfter(until)) {
                result.add(Slot(6, before, "before selected reminder"))
            }
        }
        return result
    }

    private fun add(dst: MutableList<Slot>, id: Int, date: LocalDate, time: LocalTime,
                    now: Instant, horizon: Instant, purpose: String) {
        val at = date.atTime(time).atZone(PKT).toInstant()
        if (at.isAfter(now) && !at.isAfter(horizon)) dst.add(Slot(id, at, purpose))
    }
}
