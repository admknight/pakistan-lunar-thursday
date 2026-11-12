package com.scopewise.lunarthursday;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Runs with a plain JDK: javac -d build/test ... && java -cp build/test ...EngineTest */
public final class EngineTest {
    private static int passed = 0;
    private static void check(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
        passed++;
    }
    private static void date(LocalDate actual, String expected) {
        check(actual.equals(LocalDate.parse(expected)), "Expected " + expected + " got " + actual);
    }

    public static void main(String[] args) {
        // 2026 & 2027 PMD source row transcription verification.
        check(LunarEngine.publishedStarts().size() == 25, "25 forecast rows expected");
        date(LunarEngine.month(1448,6,Collections.emptyMap()).start,"2026-11-12");
        date(LunarEngine.month(1448,7,Collections.emptyMap()).start,"2026-12-11");
        date(LunarEngine.month(1448,8,Collections.emptyMap()).start,"2027-01-10");
        date(LunarEngine.month(1448,9,Collections.emptyMap()).start,"2027-02-08");
        date(LunarEngine.month(1449,8,Collections.emptyMap()).start,"2027-12-30");

        // Every starting weekday reaches the first valid Thursday within 0..6 days.
        LocalDate thursday = LocalDate.parse("2026-11-12");
        for (int days = 0; days < 7; days++) {
            LocalDate first = thursday.plusDays(days);
            LocalDate actual = LunarEngine.firstThursday(first);
            check(actual.getDayOfWeek() == DayOfWeek.THURSDAY, "Must be Thursday");
            check(!actual.isBefore(first) && actual.isBefore(first.plusDays(7)), "Must be first Thursday");
        }
        date(LunarEngine.firstThursday(LocalDate.parse("2026-12-11")),"2026-12-17");
        date(LunarEngine.firstThursday(LocalDate.parse("2027-12-30")),"2027-12-30");

        // A reported confirmed start date moves the scheduled event automatically.
        Map<String, LocalDate> override = new HashMap<>();
        override.put("1448-7", LocalDate.parse("2026-12-12"));
        LunarEngine.Month corrected = LunarEngine.month(1448,7,override);
        check(corrected.basis == LunarEngine.Basis.LIVE_REPORTED_CONFIRMED, "Live reported confirmation marker");
        date(corrected.start, "2026-12-12");
        date(corrected.thursday(), "2026-12-17");

        // At 18:41 PKT on Nov 12, the 11 AM Nov 12 occurrence has passed.
        Instant now = Instant.parse("2026-11-12T13:41:00Z");
        List<LunarEngine.Month> upcoming = LunarEngine.upcoming(now, Collections.emptyMap(),5);
        check(upcoming.size() == 5, "Five dates");
        date(upcoming.get(0).thursday(), "2026-12-17");
        date(upcoming.get(1).thursday(), "2027-01-14");
        date(upcoming.get(2).thursday(), "2027-02-11");
        for (int i = 1; i < upcoming.size(); i++) {
            check(upcoming.get(i-1).remindAt().isBefore(upcoming.get(i).remindAt()), "Ascending schedule");
            check(upcoming.get(i).thursday().getDayOfWeek() == DayOfWeek.THURSDAY, "Thursday only");
        }
        check(upcoming.get(0).remindAt().equals(Instant.parse("2026-12-17T06:00:00Z")), "11 PKT = 06 UTC");

        // Starts beyond the dated PMD tables are estimates, not implicitly 'official'.
        check(LunarEngine.month(1450,1,Collections.emptyMap()).basis == LunarEngine.Basis.CALCULATED_ESTIMATE,
              "Unknown year cannot be marked PMD or confirmed");
        date(LunarEngine.estimatedFirstDay(1448,1),"2026-06-17");
        check(LunarEngine.upcoming(now, Collections.emptyMap(), 0).isEmpty(), "Zero schedule count");

        // A first-day correction across Wednesday/Thursday must change the week.
        override.put("1448-7", LocalDate.parse("2026-12-10"));
        date(LunarEngine.month(1448,7,override).thursday(), "2026-12-10");
        // 10:59 AM in Pakistan on the month start still qualifies for same-day alert.
        List<LunarEngine.Month> beforeEleven = LunarEngine.upcoming(
                Instant.parse("2026-11-12T05:59:00Z"), Collections.emptyMap(), 1);
        date(beforeEleven.get(0).thursday(), "2026-11-12");
        List<LunarEngine.Month> afterEleven = LunarEngine.upcoming(
                Instant.parse("2026-11-12T06:01:00Z"), Collections.emptyMap(), 1);
        date(afterEleven.get(0).thursday(), "2026-12-17");
        // All PMD months remain ordered, non-duplicated and first Thursday only.
        java.util.Set<LocalDate> unique = new java.util.HashSet<>();
        for (Map.Entry<String, LocalDate> row : LunarEngine.publishedStarts().entrySet()) {
            LocalDate first = row.getValue();
            LocalDate thu = LunarEngine.firstThursday(first);
            check(thu.getDayOfWeek() == DayOfWeek.THURSDAY, "Thursday for " + row.getKey());
            check(thu.toEpochDay() - first.toEpochDay() < 7, "Within one week");
            check(unique.add(thu), "Monthly events must be distinct");
        }
        // Time-zone stable: the required instant is 06:00 UTC for 11:00 PKT.
        check(LunarEngine.month(1449,8,Collections.emptyMap()).remindAt()
                .equals(Instant.parse("2027-12-30T06:00:00Z")), "Time-zone fixed");
        System.out.println("EngineTest PASS: " + passed + " assertions");
    }
}
