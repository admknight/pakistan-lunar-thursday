package com.scopewise.lunarthursday

import java.time.LocalDate

/**
 * Works out which civil days a Hijri month can still start on, and whether its
 * first Thursday is already certain.
 *
 * A Hijri month has 29 or 30 days. Once a month start is confirmed, the next
 * month can only begin 29 or 30 days later, and the two outcomes give a
 * different first Thursday only when the earlier one is itself a Thursday.
 * This is arithmetic on confirmed starts; it never claims a moon sighting.
 */
object MonthStartCandidates {
    /** How far back a confirmed start is still used before falling back to the forecast. */
    const val MAX_LOOKBACK_MONTHS = 3

    enum class Basis {
        /** This month's own start is confirmed. */
        CONFIRMED,
        /** Derived from a confirmed start one to [MAX_LOOKBACK_MONTHS] months earlier. */
        FROM_CONFIRMED_EARLIER_MONTH,
        /** No recent confirmation: forecast or calculated start, plus or minus one day. */
        FORECAST_ONLY
    }

    class Result internal constructor(
        @JvmField val hijriYear: Int,
        @JvmField val hijriMonth: Int,
        possibleStarts: List<LocalDate>,
        @JvmField val basis: Basis
    ) {
        /** Possible first days, earliest first, without duplicates. */
        @JvmField val possibleStarts: List<LocalDate> = possibleStarts.distinct().sorted()

        /** Possible first Thursdays, earliest first, without duplicates. */
        @JvmField val possibleThursdays: List<LocalDate> =
            this.possibleStarts.map { LunarEngine.firstThursday(it) }.distinct().sorted()

        /** True when every possible start leads to the same first Thursday. */
        val isFirstThursdayCertain: Boolean get() = possibleThursdays.size == 1

        val earliestThursday: LocalDate get() = possibleThursdays.first()
        val latestThursday: LocalDate get() = possibleThursdays.last()

        fun key(): String = "$hijriYear-$hijriMonth"
    }

    @JvmStatic
    fun forMonth(hijriYear: Int, hijriMonth: Int, confirmedStarts: Map<String, LocalDate>): Result {
        require(hijriYear >= 1 && hijriMonth in 1..12) { "Invalid Hijri year/month" }

        confirmedStarts["$hijriYear-$hijriMonth"]?.let {
            return Result(hijriYear, hijriMonth, listOf(it), Basis.CONFIRMED)
        }

        var year = hijriYear
        var month = hijriMonth
        for (monthsBack in 1..MAX_LOOKBACK_MONTHS) {
            if (month == 1) { month = 12; year -= 1 } else month -= 1
            if (year < 1) break
            val anchor = confirmedStarts["$year-$month"] ?: continue
            // Each intervening month is 29 or 30 days long.
            val starts = (29L * monthsBack..30L * monthsBack).map { anchor.plusDays(it) }
            return Result(hijriYear, hijriMonth, starts, Basis.FROM_CONFIRMED_EARLIER_MONTH)
        }

        val forecast = LunarEngine.month(hijriYear, hijriMonth, emptyMap()).start
        val starts = listOf(forecast.minusDays(1), forecast, forecast.plusDays(1))
        return Result(hijriYear, hijriMonth, starts, Basis.FORECAST_ONLY)
    }
}
