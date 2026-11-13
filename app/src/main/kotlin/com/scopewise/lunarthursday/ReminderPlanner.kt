package com.scopewise.lunarthursday

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Chooses the next reminder from every day a month can still start on.
 *
 * A confirmed month has one first Thursday. An unconfirmed month usually also
 * has one, because both possible start days lead to the same Thursday. Only
 * when they do not is a second, clearly worded "possible" notice planned for
 * the other Thursday, so the true first Thursday is never passed in silence.
 */
object ReminderPlanner {
    enum class Kind {
        /** The month start is confirmed. */
        CONFIRMED,
        /** Not confirmed; this is the forecast Thursday, or the only possible one. */
        EXPECTED,
        /** Not confirmed; the other Thursday this month could still begin with. */
        POSSIBLE
    }

    class Occurrence internal constructor(
        @JvmField val month: LunarEngine.Month,
        @JvmField val thursday: LocalDate,
        @JvmField val kind: Kind,
        /** The Thursday the forecast points to; equals [thursday] unless [kind] is POSSIBLE. */
        @JvmField val expectedThursday: LocalDate
    ) {
        fun triggerAt(chosenTime: LocalTime): Instant =
            ZonedDateTime.of(thursday, chosenTime, LunarEngine.PAKISTAN).toInstant()
    }

    /**
     * The month as screens and reminders should show it. Normally this is the
     * engine's month. If its forecast start is impossible after a confirmed
     * earlier month (not 29 or 30 days on), the nearest possible start is used
     * instead, so the date shown always matches the reminder that is planned.
     */
    @JvmStatic
    fun effectiveMonth(hijriYear: Int, hijriMonth: Int, confirmedStarts: Map<String, LocalDate>): LunarEngine.Month {
        val base = LunarEngine.month(hijriYear, hijriMonth, confirmedStarts)
        val possible = MonthStartCandidates.forMonth(hijriYear, hijriMonth, confirmedStarts).possibleStarts
        if (base.start in possible) return base
        val nearest = possible.minByOrNull { abs(ChronoUnit.DAYS.between(it, base.start)) }!!
        return LunarEngine.Month(hijriYear, hijriMonth, nearest, base.basis)
    }

    /** The other Thursday an unconfirmed month could still begin with, or null when there is none. */
    @JvmStatic
    fun otherThursday(hijriYear: Int, hijriMonth: Int, confirmedStarts: Map<String, LocalDate>): LocalDate? =
        occurrences(hijriYear, hijriMonth, confirmedStarts).firstOrNull { it.kind == Kind.POSSIBLE }?.thursday

    /** Upcoming months for the screens, ordered by their main reminder, using [effectiveMonth]. */
    @JvmStatic
    fun upcomingMonths(now: Instant, confirmedStarts: Map<String, LocalDate>, limit: Int,
                       chosenTime: LocalTime): List<LunarEngine.Month> {
        val approximateHijriYear = now.atZone(LunarEngine.PAKISTAN).year - 579
        val found = ArrayList<LunarEngine.Month>()
        for (year in maxOf(1, approximateHijriYear - 2)..approximateHijriYear + 3) {
            for (month in 1..12) {
                val candidate = effectiveMonth(year, month, confirmedStarts)
                if (candidate.remindAt(chosenTime).isAfter(now)) found.add(candidate)
            }
        }
        found.sortBy { it.remindAt(chosenTime) }
        return if (found.size > limit) found.subList(0, limit) else found
    }

    /** Every Thursday on which the given month may be reminded, earliest first. */
    @JvmStatic
    fun occurrences(hijriYear: Int, hijriMonth: Int, confirmedStarts: Map<String, LocalDate>): List<Occurrence> {
        val month = effectiveMonth(hijriYear, hijriMonth, confirmedStarts)
        val candidates = MonthStartCandidates.forMonth(hijriYear, hijriMonth, confirmedStarts)
        if (candidates.basis == MonthStartCandidates.Basis.CONFIRMED) {
            val thursday = candidates.earliestThursday
            return listOf(Occurrence(month, thursday, Kind.CONFIRMED, thursday))
        }
        val forecast = month.thursday()
        val expected = candidates.possibleThursdays.minByOrNull { abs(ChronoUnit.DAYS.between(it, forecast)) }!!
        return candidates.possibleThursdays.map {
            Occurrence(month, it, if (it == expected) Kind.EXPECTED else Kind.POSSIBLE, expected)
        }
    }

    /** The next occurrence strictly after [now], or null if none is found nearby. */
    @JvmStatic
    fun next(now: Instant, confirmedStarts: Map<String, LocalDate>, chosenTime: LocalTime): Occurrence? {
        val approximateHijriYear = now.atZone(LunarEngine.PAKISTAN).year - 579
        var best: Occurrence? = null
        for (year in maxOf(1, approximateHijriYear - 2)..approximateHijriYear + 3) {
            for (month in 1..12) {
                for (occurrence in occurrences(year, month, confirmedStarts)) {
                    val at = occurrence.triggerAt(chosenTime)
                    if (at.isAfter(now) && (best == null || at.isBefore(best.triggerAt(chosenTime)))) {
                        best = occurrence
                    }
                }
            }
        }
        return best
    }
}
