package com.scopewise.lunarthursday

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/** What the home-screen widget shows about the coming first Thursday. No Android types. */
object WidgetContent {
    class Next internal constructor(
        @JvmField val month: LunarEngine.Month,
        @JvmField val thursday: LocalDate,
        /** Whole Pakistan civil days from today; 0 on the Thursday itself. */
        @JvmField val daysLeft: Int,
        /** The other Thursday an unconfirmed month could still begin with, or null. */
        @JvmField val otherThursday: LocalDate?
    )

    /**
     * The first Thursday that is today or still ahead. Unlike the reminder, it
     * stays on today's Thursday until the Pakistan day ends.
     */
    @JvmStatic
    fun next(now: Instant, confirmedStarts: Map<String, LocalDate>): Next? {
        val month = ReminderPlanner.upcomingMonths(now, confirmedStarts, 1, LocalTime.MAX).firstOrNull()
            ?: return null
        val today = now.atZone(LunarEngine.PAKISTAN).toLocalDate()
        return Next(
            month, month.thursday(), ChronoUnit.DAYS.between(today, month.thursday()).toInt(),
            ReminderPlanner.otherThursday(month.hijriYear, month.hijriMonth, confirmedStarts))
    }
}
