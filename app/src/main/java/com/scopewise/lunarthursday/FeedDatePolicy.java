package com.scopewise.lunarthursday;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Strict, dependency-free gate for observations from a third-party Pakistan
 * Hijri-date feed. The feed itself is NOT a government-issued API.
 * A forecast / estimated feed row must never be marked confirmed.
 */
public final class FeedDatePolicy {
    public static final class Observation {
        public final String monthKey;
        public final LocalDate firstDay;

        private Observation(String monthKey, LocalDate firstDay) {
            this.monthKey = monthKey;
            this.firstDay = firstDay;
        }
    }

    private FeedDatePolicy() { }

    public static Observation evaluate(String gregorianDate, int hijriDay,
            int hijriMonth, int hijriYear, String method, String timezone,
            String generatedAt, Instant now) {
        if (!"Asia/Karachi".equals(timezone)) return null;
        if (method == null || !method.startsWith("pakistan_ruet_e_hilal_")) return null;
        boolean reportedConfirmed = method.equals("pakistan_ruet_e_hilal_confirmed")
                || method.equals("pakistan_ruet_e_hilal_announced")
                || method.equals("pakistan_ruet_e_hilal_official");
        if (!reportedConfirmed) return null;
        if (hijriDay < 1 || hijriDay > 30 || hijriMonth < 1 || hijriMonth > 12
                || hijriYear < 1400 || hijriYear > 1600) return null;

        try {
            LocalDate date = LocalDate.parse(gregorianDate);
            OffsetDateTime stamp = OffsetDateTime.parse(generatedAt);
            long ageSeconds = now.getEpochSecond() - stamp.toInstant().getEpochSecond();
            if (ageSeconds < -15 * 60L || ageSeconds > 48 * 60 * 60L) return null;
            if (!stamp.toInstant().atZone(LunarEngine.PAKISTAN).toLocalDate().equals(date))
                return null;
            LocalDate start = date.minusDays(hijriDay - 1L);
            LocalDate pakistanNow = now.atZone(LunarEngine.PAKISTAN).toLocalDate();
            if (start.isAfter(pakistanNow)) return null;
            LocalDate baseline = LunarEngine.month(hijriYear, hijriMonth,
                    java.util.Collections.emptyMap()).start;
            // A wildly inconsistent remote record should not replace a calendar.
            if (Math.abs(ChronoUnit.DAYS.between(baseline, start)) > 3) return null;
            return new Observation(hijriYear + "-" + hijriMonth, start);
        } catch (RuntimeException invalidFeedData) {
            return null;
        }
    }
}
