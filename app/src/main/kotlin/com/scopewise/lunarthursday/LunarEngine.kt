package com.scopewise.lunarthursday

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Collections

/**
 * Pure scheduler. Month starts in the embedded 2026-2027 PMD tables are
 * FORECASTS, not announcements from the Central Ruet-e-Hilal Committee.
 * For dates not covered by PMD data, the tabular Islamic calendar is used only
 * as an APPROXIMATION. A background HTTPS check may replace a month start
 * only when a source explicitly reports confirmation.
 */
object LunarEngine {
    @JvmField val PAKISTAN: ZoneId = ZoneId.of("Asia/Karachi")
    @JvmField val REMINDER_TIME: LocalTime = LocalTime.of(11, 0)

    enum class Basis { PMD_FORECAST, CALCULATED_ESTIMATE, LIVE_REPORTED_CONFIRMED }

    class Month internal constructor(
        @JvmField val hijriYear: Int,
        @JvmField val hijriMonth: Int,
        @JvmField val start: LocalDate,
        @JvmField val basis: Basis
    ) {
        fun key(): String = "$hijriYear-$hijriMonth"
        fun label(): String = monthName(hijriMonth) + " " + hijriYear + " AH"
        fun thursday(): LocalDate = firstThursday(start)

        /** Expected Hijri day on a Pakistan civil date, relative to this month start. */
        fun hijriDay(civilDate: LocalDate): Int {
            val day = ChronoUnit.DAYS.between(start, civilDate) + 1L
            if (day < 1 || day > 30) {
                throw IllegalArgumentException("Civil date outside expected Hijri month")
            }
            return day.toInt()
        }

        fun remindAt(): Instant = remindAt(REMINDER_TIME)

        /** PKT wall time chosen by the user; calendar day remains first Thursday. */
        fun remindAt(chosenTime: LocalTime?): Instant {
            if (chosenTime == null) throw IllegalArgumentException("Reminder time required")
            return ZonedDateTime.of(thursday(), chosenTime, PAKISTAN).toInstant()
        }
    }

    private val PMD_FIRST_DAYS: Map<String, LocalDate> = run {
        val m = HashMap<String, LocalDate>()
        fun put(year: Int, month: Int, iso: String) { m["$year-$month"] = LocalDate.parse(iso) }
        // Pakistan Meteorological Department 2026 expected first days:
        // https://cdpc.pmd.gov.pk/probable2026.pdf (03 Oct 2025)
        put(1447, 8, "2026-01-21")
        put(1447, 9, "2026-02-19")
        put(1447, 10, "2026-03-21")
        put(1447, 11, "2026-04-21")
        put(1447, 12, "2026-05-18")
        put(1448, 1, "2026-06-17")
        put(1448, 2, "2026-07-16")
        put(1448, 3, "2026-08-15")
        put(1448, 4, "2026-09-14")
        put(1448, 5, "2026-10-13")
        put(1448, 6, "2026-11-12")
        put(1448, 7, "2026-12-11")
        // Pakistan Meteorological Department 2027 expected first days:
        // https://weather.gov.pk/storage/uploads/cdpc/lunar_calendar/pdf/probable%202027.pdf
        // Published 30 Oct 2026
        put(1448, 8, "2027-01-10")
        put(1448, 9, "2027-02-08")
        put(1448, 10, "2027-03-10")
        put(1448, 11, "2027-04-09")
        put(1448, 12, "2027-05-08")
        put(1449, 1, "2027-06-06")
        put(1449, 2, "2027-07-06")
        put(1449, 3, "2027-08-04")
        put(1449, 4, "2027-09-03")
        put(1449, 5, "2027-10-03")
        put(1449, 6, "2027-11-01")
        put(1449, 7, "2027-11-30")
        put(1449, 8, "2027-12-30")
        Collections.unmodifiableMap(m)
    }

    @JvmStatic
    fun firstThursday(monthStart: LocalDate): LocalDate {
        val days = (DayOfWeek.THURSDAY.value - monthStart.dayOfWeek.value + 7) % 7
        return monthStart.plusDays(days.toLong())
    }

    /** JDN -> Gregorian LocalDate; arithmetic/tabular estimate, not sighting. */
    @JvmStatic
    fun estimatedFirstDay(hijriYear: Int, hijriMonth: Int): LocalDate {
        if (hijriYear < 1 || hijriMonth < 1 || hijriMonth > 12) {
            throw IllegalArgumentException("Invalid Hijri year/month")
        }
        val monthOffset = Math.ceil(29.5 * (hijriMonth - 1)).toLong()
        val jdn = 1 + monthOffset + (hijriYear - 1L) * 354 +
            Math.floorDiv(3L + 11L * hijriYear, 30L) + 1948439L
        return LocalDate.ofEpochDay(jdn - 2440588L)
    }

    @JvmStatic
    fun month(year: Int, month: Int, confirmedStarts: Map<String, LocalDate>): Month {
        val key = "$year-$month"
        val confirmed = confirmedStarts[key]
        val pmd = PMD_FIRST_DAYS[key]
        if (confirmed != null) return Month(year, month, confirmed, Basis.LIVE_REPORTED_CONFIRMED)
        if (pmd != null) return Month(year, month, pmd, Basis.PMD_FORECAST)
        return Month(year, month, estimatedFirstDay(year, month), Basis.CALCULATED_ESTIMATE)
    }

    /** A provisional Hijri civil date sourced from the same month-start data as reminders. */
    class HijriDate internal constructor(
        @JvmField val month: Month,
        @JvmField val day: Int
    )

    /**
     * Find the Hijri date expected on a Pakistan civil day. Use the most recent
     * source-selected month start within 30 days, never a second conversion API.
     * Return null if no defensible start exists; do not display day 31+.
     */
    @JvmStatic
    fun expectedOnDate(civilDate: LocalDate?, confirmedStarts: Map<String, LocalDate>): HijriDate? {
        if (civilDate == null) throw IllegalArgumentException("Civil date is required")
        val approximateYear = civilDate.year - 579
        var best: Month? = null
        for (year in Math.max(1, approximateYear - 5)..approximateYear + 7) {
            for (m in 1..12) {
                val candidate = month(year, m, confirmedStarts)
                val elapsed = ChronoUnit.DAYS.between(candidate.start, civilDate)
                if (elapsed in 0..29 && (best == null || candidate.start.isAfter(best.start))) {
                    best = candidate
                }
            }
        }
        return if (best == null) null else HijriDate(best, best.hijriDay(civilDate))
    }

    /** Upcoming one-time reminder occurrences, sorted by Pakistan local date. */
    @JvmStatic
    fun upcoming(now: Instant, confirmedStarts: Map<String, LocalDate>, limit: Int): List<Month> =
        upcoming(now, confirmedStarts, limit, REMINDER_TIME)

    /** Scheduling at a user-selected Pakistan wall time, never a different date. */
    @JvmStatic
    fun upcoming(now: Instant, confirmedStarts: Map<String, LocalDate>, limit: Int,
                 chosenTime: LocalTime?): List<Month> {
        if (chosenTime == null) throw IllegalArgumentException("Reminder time required")
        if (limit < 1) return emptyList()
        val gregorianYear = now.atZone(PAKISTAN).year
        // Bounding the search to nearby Hijri years prevents pathological work.
        val estimatedHijriYear = gregorianYear - 579 // broad center; filtered by date below
        val months = ArrayList<Month>()
        for (year in Math.max(1, estimatedHijriYear - 5)..estimatedHijriYear + 7) {
            for (mon in 1..12) {
                val candidate = month(year, mon, confirmedStarts)
                if (candidate.remindAt(chosenTime).isAfter(now)) months.add(candidate)
            }
        }
        // Stable sort, as before, so equal times keep their generation order.
        months.sortWith(Comparator.comparing<Month, Instant> { it.remindAt(chosenTime) })
        if (months.size <= limit) return months
        return ArrayList(months.subList(0, limit))
    }

    @JvmStatic
    fun monthName(month: Int): String = when (month) {
        1 -> "Muharram"
        2 -> "Safar"
        3 -> "Rabi al-Awwal"
        4 -> "Rabi al-Thani"
        5 -> "Jumada al-Awwal"
        6 -> "Jumada al-Thani"
        7 -> "Rajab"
        8 -> "Sha'ban"
        9 -> "Ramadan"
        10 -> "Shawwal"
        11 -> "Dhu al-Qa'dah"
        12 -> "Dhu al-Hijjah"
        else -> throw IllegalArgumentException("Invalid Hijri month")
    }

    @JvmStatic
    fun basisDescription(basis: Basis): String = when (basis) {
        Basis.PMD_FORECAST -> "PMD published forecast - not a confirmed sighting"
        Basis.LIVE_REPORTED_CONFIRMED -> "Live provider reports confirmation (not a government API)"
        else -> "Calculated estimate - NOT a Pakistan sighting confirmation"
    }

    @JvmStatic
    fun publishedStarts(): Map<String, LocalDate> = PMD_FIRST_DAYS
}
