package com.scopewise.lunarthursday

import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import org.json.JSONObject

/**
 * Strict gate for the project's own corrections file (data/month-starts.json),
 * which lists Hijri month starts confirmed from reports of the Central
 * Ruet-e-Hilal Committee's announcement. The file is maintained in this
 * repository; it is not a government API. Anything that does not fit the
 * calendar is dropped and the app keeps its forecast.
 */
object CorrectionsPolicy {
    const val SCHEMA = 1
    const val MAX_BYTES = 64 * 1024
    const val MIN_SOURCES = 2
    /** An announcement is made at most two civil days before the month starts. */
    private const val MAX_DAYS_AHEAD = 2L
    /** Older months no longer affect any upcoming reminder. */
    private const val MAX_DAYS_BACK = 130L
    /** Same tolerance the feed gate uses when no confirmed neighbour exists. */
    private const val MAX_BASELINE_DRIFT_DAYS = 3L

    class Entry internal constructor(
        @JvmField val monthKey: String,
        @JvmField val firstDay: LocalDate
    )

    /**
     * Returns the entries that may be stored as confirmed, oldest first.
     * [storedConfirmed] is what the app already holds; a month is checked
     * against its confirmed predecessor (29 or 30 days earlier) when one is
     * known, otherwise against the forecast.
     */
    @JvmStatic
    fun accept(json: String?, storedConfirmed: Map<String, LocalDate>, now: Instant): List<Entry> {
        if (json == null || json.length > MAX_BYTES) return emptyList()
        val today = now.atZone(LunarEngine.PAKISTAN).toLocalDate()
        val parsed = try { parse(json) } catch (invalid: Exception) { return emptyList() }

        val known = HashMap(storedConfirmed)
        val accepted = ArrayList<Entry>()
        for (row in parsed.sortedBy { it.firstDay }) {
            if (row.firstDay.isAfter(today.plusDays(MAX_DAYS_AHEAD))) continue
            if (row.firstDay.isBefore(today.minusDays(MAX_DAYS_BACK))) continue
            if (!fitsCalendar(row, known)) continue
            val key = "${row.year}-${row.month}"
            known[key] = row.firstDay
            accepted.add(Entry(key, row.firstDay))
        }
        return accepted
    }

    /** Structural check used by tests on the committed file: every row must be usable. */
    @JvmStatic
    fun wellFormedRowCount(json: String): Int = parse(json).size

    private class Row(val year: Int, val month: Int, val firstDay: LocalDate)

    private fun parse(json: String): List<Row> {
        val root = JSONObject(json)
        require(root.getInt("schema") == SCHEMA) { "Unsupported schema" }
        val months = root.getJSONArray("months")
        val rows = ArrayList<Row>()
        val seen = HashSet<String>()
        for (i in 0 until months.length()) {
            val item = months.getJSONObject(i)
            val year = item.getInt("hijri_year")
            val month = item.getInt("hijri_month")
            require(year in 1400..1600 && month in 1..12) { "Invalid Hijri month" }
            require(seen.add("$year-$month")) { "Duplicate month $year-$month" }
            val firstDay = LocalDate.parse(item.getString("first_day"))
            val sources = item.getJSONArray("sources")
            val distinctHosts = HashSet<String>()
            for (s in 0 until sources.length()) {
                val url = java.net.URI(sources.getString(s))
                require(url.scheme == "https" && !url.host.isNullOrEmpty()) { "Source must be https" }
                distinctHosts.add(url.host.lowercase().removePrefix("www."))
            }
            require(distinctHosts.size >= MIN_SOURCES) { "Needs $MIN_SOURCES independent sources" }
            rows.add(Row(year, month, firstDay))
        }
        return rows
    }

    private fun fitsCalendar(row: Row, known: Map<String, LocalDate>): Boolean {
        val previousKey = if (row.month == 1) "${row.year - 1}-12" else "${row.year}-${row.month - 1}"
        val previous = known[previousKey]
        if (previous != null) {
            val length = ChronoUnit.DAYS.between(previous, row.firstDay)
            return length == 29L || length == 30L
        }
        val baseline = LunarEngine.month(row.year, row.month, emptyMap()).start
        return abs(ChronoUnit.DAYS.between(baseline, row.firstDay)) <= MAX_BASELINE_DRIFT_DAYS
    }
}
