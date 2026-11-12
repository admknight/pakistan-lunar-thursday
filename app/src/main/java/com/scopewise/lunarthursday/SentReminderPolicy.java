package com.scopewise.lunarthursday;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Duplicate guard for the monthly reminder. A reminder counts as already sent
 * only for the same Hijri month AND the same Pakistan civil date. If a later
 * source correction moves that month's first Thursday to a different date,
 * the corrected Thursday must still notify.
 */
final class SentReminderPolicy {
    private static final ZoneId PKT = ZoneId.of("Asia/Karachi");
    private static final char SEPARATOR = '@';

    private SentReminderPolicy() { }

    /** Stored marker, e.g. "1448-6@2026-11-12". */
    static String token(String monthKey, long triggerMillis) {
        if (monthKey == null || monthKey.isEmpty() || triggerMillis <= 0)
            throw new IllegalArgumentException("Month key and trigger required");
        return monthKey + SEPARATOR + dateOf(triggerMillis);
    }

    /**
     * Markers saved before v1.9.2 hold only the month key and no date; they keep
     * their original month-wide meaning because the sent date is unknown.
     */
    static boolean alreadySent(String stored, String monthKey, long triggerMillis) {
        if (stored == null || stored.isEmpty() || monthKey == null || monthKey.isEmpty()
                || triggerMillis <= 0) return false;
        if (stored.indexOf(SEPARATOR) < 0) return stored.equals(monthKey);
        return stored.equals(token(monthKey, triggerMillis));
    }

    private static LocalDate dateOf(long triggerMillis) {
        return Instant.ofEpochMilli(triggerMillis).atZone(PKT).toLocalDate();
    }
}
