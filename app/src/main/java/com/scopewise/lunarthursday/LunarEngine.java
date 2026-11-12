package com.scopewise.lunarthursday;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure-Java scheduler. Month starts in the embedded 2026-2027 PMD tables are
 * FORECASTS, not announcements from the Central Ruet-e-Hilal Committee.
 * For dates not covered by PMD data, the tabular Islamic calendar is used only
 * as an APPROXIMATION. A background HTTPS check may replace a month start
 * only when the Pakistan date provider explicitly reports confirmation.
 */
public final class LunarEngine {
    public static final ZoneId PAKISTAN = ZoneId.of("Asia/Karachi");
    public static final LocalTime REMINDER_TIME = LocalTime.of(11, 0);

    public enum Basis { PMD_FORECAST, CALCULATED_ESTIMATE, LIVE_REPORTED_CONFIRMED }

    public static final class Month {
        public final int hijriYear;
        public final int hijriMonth;
        public final LocalDate start;
        public final Basis basis;

        Month(int year, int month, LocalDate start, Basis basis) {
            this.hijriYear = year;
            this.hijriMonth = month;
            this.start = start;
            this.basis = basis;
        }

        public String key() { return hijriYear + "-" + hijriMonth; }
        public String label() { return monthName(hijriMonth) + " " + hijriYear + " AH"; }
        public LocalDate thursday() { return firstThursday(start); }

        /** Expected Hijri day on a Pakistan civil date, relative to this month start. */
        public int hijriDay(LocalDate civilDate) {
            long day = ChronoUnit.DAYS.between(start, civilDate) + 1L;
            if (day < 1 || day > 30) {
                throw new IllegalArgumentException("Civil date outside expected Hijri month");
            }
            return (int) day;
        }

        public Instant remindAt() { return remindAt(REMINDER_TIME); }

        /** PKT wall time chosen by the user; calendar day remains first Thursday. */
        public Instant remindAt(LocalTime chosenTime) {
            if (chosenTime == null) throw new IllegalArgumentException("Reminder time required");
            return ZonedDateTime.of(thursday(), chosenTime, PAKISTAN).toInstant();
        }
    }

    private static final Map<String, LocalDate> PMD_FIRST_DAYS;
    static {
        Map<String, LocalDate> m = new HashMap<>();
        // Pakistan Meteorological Department 2026 expected first days:
        // https://cdpc.pmd.gov.pk/probable2026.pdf (03 Oct 2025)
        put(m,1447, 8,"2026-01-21");
        put(m,1447, 9,"2026-02-19");
        put(m,1447,10,"2026-03-21");
        put(m,1447,11,"2026-04-21");
        put(m,1447,12,"2026-05-18");
        put(m,1448, 1,"2026-06-17");
        put(m,1448, 2,"2026-07-16");
        put(m,1448, 3,"2026-08-15");
        put(m,1448, 4,"2026-09-14");
        put(m,1448, 5,"2026-10-13");
        put(m,1448, 6,"2026-11-12");
        put(m,1448, 7,"2026-12-11");
        // Pakistan Meteorological Department 2027 expected first days:
        // https://weather.gov.pk/storage/uploads/cdpc/lunar_calendar/pdf/probable%202027.pdf
        // Published 30 Oct 2026
        put(m,1448, 8,"2027-01-10");
        put(m,1448, 9,"2027-02-08");
        put(m,1448,10,"2027-03-10");
        put(m,1448,11,"2027-04-09");
        put(m,1448,12,"2027-05-08");
        put(m,1449, 1,"2027-06-06");
        put(m,1449, 2,"2027-07-06");
        put(m,1449, 3,"2027-08-04");
        put(m,1449, 4,"2027-09-03");
        put(m,1449, 5,"2027-10-03");
        put(m,1449, 6,"2027-11-01");
        put(m,1449, 7,"2027-11-30");
        put(m,1449, 8,"2027-12-30");
        PMD_FIRST_DAYS = Collections.unmodifiableMap(m);
    }

    private static void put(Map<String, LocalDate> map, int year, int month, String iso) {
        map.put(year + "-" + month, LocalDate.parse(iso));
    }

    private LunarEngine() { }

    public static LocalDate firstThursday(LocalDate monthStart) {
        int days = (DayOfWeek.THURSDAY.getValue() - monthStart.getDayOfWeek().getValue() + 7) % 7;
        return monthStart.plusDays(days);
    }

    /** JDN -> Gregorian LocalDate; arithmetic/tabular estimate, not sighting. */
    public static LocalDate estimatedFirstDay(int hijriYear, int hijriMonth) {
        if (hijriYear < 1 || hijriMonth < 1 || hijriMonth > 12) {
            throw new IllegalArgumentException("Invalid Hijri year/month");
        }
        long monthOffset = (long) Math.ceil(29.5 * (hijriMonth - 1));
        long jdn = 1 + monthOffset + (hijriYear - 1L) * 354
                + Math.floorDiv(3L + 11L * hijriYear, 30L) + 1948439L;
        return LocalDate.ofEpochDay(jdn - 2440588L);
    }

    public static Month month(int year, int month, Map<String, LocalDate> confirmedStarts) {
        String key = year + "-" + month;
        LocalDate confirmed = confirmedStarts.get(key);
        LocalDate pmd = PMD_FIRST_DAYS.get(key);
        if (confirmed != null) return new Month(year, month, confirmed, Basis.LIVE_REPORTED_CONFIRMED);
        if (pmd != null) return new Month(year, month, pmd, Basis.PMD_FORECAST);
        return new Month(year, month, estimatedFirstDay(year, month), Basis.CALCULATED_ESTIMATE);
    }

    /** A provisional Hijri civil date sourced from the same month-start data as reminders. */
    public static final class HijriDate {
        public final Month month;
        public final int day;

        private HijriDate(Month month, int day) {
            this.month = month;
            this.day = day;
        }
    }

    /**
     * Find the Hijri date expected on a Pakistan civil day. Use the most recent
     * source-selected month start within 30 days, never a second conversion API.
     * Return null if no defensible start exists; do not display day 31+.
     */
    public static HijriDate expectedOnDate(LocalDate civilDate,
                                           Map<String, LocalDate> confirmedStarts) {
        if (civilDate == null) throw new IllegalArgumentException("Civil date is required");
        int approximateYear = civilDate.getYear() - 579;
        Month best = null;
        for (int year = Math.max(1, approximateYear - 5); year <= approximateYear + 7; year++) {
            for (int m = 1; m <= 12; m++) {
                Month candidate = month(year, m, confirmedStarts);
                long elapsed = ChronoUnit.DAYS.between(candidate.start, civilDate);
                if (elapsed >= 0 && elapsed < 30
                        && (best == null || candidate.start.isAfter(best.start))) {
                    best = candidate;
                }
            }
        }
        return best == null ? null : new HijriDate(best, best.hijriDay(civilDate));
    }

    /** Upcoming one-time reminder occurrences, sorted by Pakistan local date. */
    public static List<Month> upcoming(Instant now, Map<String, LocalDate> confirmedStarts, int limit) {
        return upcoming(now, confirmedStarts, limit, REMINDER_TIME);
    }

    /** Scheduling at a user-selected Pakistan wall time, never a different date. */
    public static List<Month> upcoming(Instant now, Map<String, LocalDate> confirmedStarts,
                                       int limit, LocalTime chosenTime) {
        if (chosenTime == null) throw new IllegalArgumentException("Reminder time required");
        if (limit < 1) return Collections.emptyList();
        int gregorianYear = now.atZone(PAKISTAN).getYear();
        // Bounding the search to nearby Hijri years prevents pathological work.
        int estimatedHijriYear = gregorianYear - 579; // broad center; filtered by date below
        List<Month> months = new ArrayList<>();
        for (int year = Math.max(1, estimatedHijriYear - 5); year <= estimatedHijriYear + 7; year++) {
            for (int mon = 1; mon <= 12; mon++) {
                Month candidate = month(year, mon, confirmedStarts);
                if (candidate.remindAt(chosenTime).isAfter(now)) months.add(candidate);
            }
        }
        months.sort(Comparator.comparing(x -> x.remindAt(chosenTime)));
        if (months.size() <= limit) return months;
        return new ArrayList<>(months.subList(0, limit));
    }

    public static String monthName(int month) {
        switch (month) {
            case 1: return "Muharram";
            case 2: return "Safar";
            case 3: return "Rabi al-Awwal";
            case 4: return "Rabi al-Thani";
            case 5: return "Jumada al-Awwal";
            case 6: return "Jumada al-Thani";
            case 7: return "Rajab";
            case 8: return "Sha'ban";
            case 9: return "Ramadan";
            case 10: return "Shawwal";
            case 11: return "Dhu al-Qa'dah";
            case 12: return "Dhu al-Hijjah";
            default: throw new IllegalArgumentException("Invalid Hijri month");
        }
    }

    public static String basisDescription(Basis basis) {
        switch (basis) {
            case PMD_FORECAST: return "PMD published forecast - not a confirmed sighting";
            case LIVE_REPORTED_CONFIRMED: return "Live provider reports confirmation (not a government API)";
            default: return "Calculated estimate - NOT a Pakistan sighting confirmation";
        }
    }

    public static Map<String, LocalDate> publishedStarts() { return PMD_FIRST_DAYS; }
}
