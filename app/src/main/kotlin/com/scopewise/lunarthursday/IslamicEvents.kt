package com.scopewise.lunarthursday

import java.time.LocalDate
import org.json.JSONObject

/**
 * Islamic observances by Hijri date, read from assets/events.json.
 *
 * The list is project-maintained data, not an authoritative calendar. Civil
 * dates come from the same month starts the reminders use, so an unconfirmed
 * month's events can still move by a day.
 */
object IslamicEvents {
    const val SCHEMA = 1
    const val MAX_BYTES = 256 * 1024
    const val SET_SHARED = "shared"
    const val SET_A = "a"
    const val SET_B = "b"
    private val SETS = setOf(SET_SHARED, SET_A, SET_B)
    private val ID = Regex("[a-z0-9]+(?:-[a-z0-9]+)*")

    class Event internal constructor(
        @JvmField val id: String,
        /** 1 to 12, or 0 for every month. */
        @JvmField val month: Int,
        /** 1 to 30, or 0 for the last day of the month. */
        @JvmField val day: Int,
        @JvmField val set: String,
        @JvmField val nameEn: String,
        @JvmField val nameUr: String?,
        @JvmField val noteEn: String?,
        /** The observance is the night before the listed day. */
        @JvmField val eve: Boolean,
        /** Length in days, starting on [day]. */
        @JvmField val days: Int
    )

    class Occurrence internal constructor(
        @JvmField val event: Event,
        @JvmField val month: LunarEngine.Month,
        /** First civil day of the observance in Pakistan. */
        @JvmField val date: LocalDate,
        /** True when the Hijri month's start is confirmed, so the date is firm. */
        @JvmField val confirmed: Boolean
    )

    /** Parses and validates the whole file; any invalid entry rejects it. */
    @JvmStatic
    fun parse(json: String): List<Event> {
        require(json.length <= MAX_BYTES) { "Events file too large" }
        val root = JSONObject(json)
        require(root.getInt("schema") == SCHEMA) { "Unsupported schema" }
        val rows = root.getJSONArray("events")
        val seen = HashSet<String>()
        val events = ArrayList<Event>()
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            val id = row.getString("id")
            require(ID.matches(id) && id.length <= 60 && seen.add(id)) { "Bad or duplicate id: $id" }
            val month = row.getInt("month")
            val day = row.getInt("day")
            val days = row.optInt("days", 1)
            require(month in 0..12 && day in 0..30) { "Bad Hijri date in $id" }
            require(days in 1..10 && (day == 0 && days == 1 || day in 1..30 && day + days - 1 <= 30)) { "Bad length in $id" }
            val set = row.getString("set")
            require(set in SETS) { "Unknown set in $id" }
            val nameEn = row.getString("name_en").trim()
            require(nameEn.isNotEmpty() && nameEn.length <= 120) { "Bad name in $id" }
            events.add(Event(id, month, day, set, nameEn,
                row.optString("name_ur", "").trim().ifEmpty { null },
                row.optString("note_en", "").trim().ifEmpty { null },
                row.optBoolean("eve", false), days))
        }
        return events
    }

    /**
     * The civil date of [event] in the given Hijri month, or null when the
     * month has no such day (a 30th in a 29-day month).
     */
    @JvmStatic
    fun dateIn(event: Event, hijriYear: Int, hijriMonth: Int, confirmedStarts: Map<String, LocalDate>): LocalDate? {
        require(event.month == 0 || event.month == hijriMonth) { "Event is not in month $hijriMonth" }
        val start = ReminderPlanner.effectiveMonth(hijriYear, hijriMonth, confirmedStarts).start
        val nextYear = if (hijriMonth == 12) hijriYear + 1 else hijriYear
        val nextMonth = if (hijriMonth == 12) 1 else hijriMonth + 1
        val lastDay = ReminderPlanner.effectiveMonth(nextYear, nextMonth, confirmedStarts).start.minusDays(1)
        val date = if (event.day == 0) lastDay else start.plusDays((event.day - 1).toLong())
        return if (date.isAfter(lastDay)) null else date
    }

    /**
     * Observances in the chosen sets that start on or after [today], earliest
     * first. Ties keep the file's order.
     */
    @JvmStatic
    fun upcoming(events: List<Event>, today: LocalDate, confirmedStarts: Map<String, LocalDate>,
                 enabledSets: Set<String>, limit: Int): List<Occurrence> {
        val approximateHijriYear = today.year - 579
        val found = ArrayList<Occurrence>()
        for (year in maxOf(1, approximateHijriYear - 1)..approximateHijriYear + 2) {
            for (month in 1..12) {
                val shown = ReminderPlanner.effectiveMonth(year, month, confirmedStarts)
                val confirmed = shown.basis == LunarEngine.Basis.LIVE_REPORTED_CONFIRMED
                for (event in events) {
                    if (event.set !in enabledSets || (event.month != 0 && event.month != month)) continue
                    val date = dateIn(event, year, month, confirmedStarts) ?: continue
                    if (!date.isBefore(today)) found.add(Occurrence(event, shown, date, confirmed))
                }
            }
        }
        val ordered = found.sortedBy { it.date }
        return if (ordered.size > limit) ordered.subList(0, limit) else ordered
    }
}
