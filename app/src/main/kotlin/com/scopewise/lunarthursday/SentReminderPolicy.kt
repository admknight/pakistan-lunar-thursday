package com.scopewise.lunarthursday

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Duplicate guard for the monthly reminder. A reminder counts as already sent
 * only for the same Hijri month AND the same Pakistan civil date. If a later
 * source correction moves that month's first Thursday to a different date,
 * the corrected Thursday must still notify.
 */
object SentReminderPolicy {
    private val PKT: ZoneId = ZoneId.of("Asia/Karachi")
    private const val SEPARATOR = '@'

    /** Stored marker, e.g. "1448-6@2026-11-12". */
    @JvmStatic
    fun token(monthKey: String?, triggerMillis: Long): String {
        if (monthKey.isNullOrEmpty() || triggerMillis <= 0) {
            throw IllegalArgumentException("Month key and trigger required")
        }
        return monthKey + SEPARATOR + dateOf(triggerMillis)
    }

    /**
     * Markers saved before v1.9.2 hold only the month key and no date; they keep
     * their original month-wide meaning because the sent date is unknown.
     */
    @JvmStatic
    fun alreadySent(stored: String?, monthKey: String?, triggerMillis: Long): Boolean {
        if (stored.isNullOrEmpty() || monthKey.isNullOrEmpty() || triggerMillis <= 0) return false
        if (stored.indexOf(SEPARATOR) < 0) return stored == monthKey
        return stored == token(monthKey, triggerMillis)
    }

    private fun dateOf(triggerMillis: Long): LocalDate =
        Instant.ofEpochMilli(triggerMillis).atZone(PKT).toLocalDate()
}
