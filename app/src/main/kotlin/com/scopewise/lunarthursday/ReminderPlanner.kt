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

    /** Every Thursday on which the given month may be reminded, earliest first. */
    @JvmStatic
    fun occurrences(hijriYear: Int, hijriMonth: Int, confirmedStarts: Map<String, LocalDate>): List<Occurrence> {
        val month = LunarEngine.month(hijriYear, hijriMonth, confirmedStarts)
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
