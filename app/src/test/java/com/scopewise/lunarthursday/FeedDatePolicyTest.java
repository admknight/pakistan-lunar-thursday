package com.scopewise.lunarthursday;

import java.time.Instant;
import java.time.LocalDate;

public final class FeedDatePolicyTest {
    private static int passed;
    private static final Instant NOW = Instant.parse("2026-11-12T13:40:00Z");

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        passed++;
    }

    private static FeedDatePolicy.Observation decide(String date, int day, int month,
            int year, String method, String zone, String stamp, Instant now) {
        return FeedDatePolicy.evaluate(date, day, month, year, method, zone, stamp, now);
    }

    public static void main(String[] args) {
        String stamp = "2026-11-12T18:38:45+05:00";
        FeedDatePolicy.Observation estimated = decide("2026-11-12", 1, 6, 1448,
                "pakistan_ruet_e_hilal_estimated", "Asia/Karachi", stamp, NOW);
        check(estimated == null, "Estimated feed may not be labelled confirmed");
        FeedDatePolicy.Observation confirmed = decide("2026-11-12", 1, 6, 1448,
                "pakistan_ruet_e_hilal_confirmed", "Asia/Karachi", stamp, NOW);
        check(confirmed != null, "Confirmed feed accepted");
        check(confirmed.monthKey.equals("1448-6"), "Month identity");
        check(confirmed.firstDay.equals(LocalDate.parse("2026-11-12")), "Confirmed first day");
        check(decide("2026-11-12", 1, 6, 1448, "pakistan_ruet_e_hilal_announced",
                "Asia/Karachi", stamp, NOW) != null, "Announced alias");
        check(decide("2026-11-12", 1, 6, 1448, "pakistan_ruet_e_hilal_official",
                "Asia/Karachi", stamp, NOW) != null, "Official alias");
        check(decide("2026-11-12", 1, 6, 1448, "pakistan_ruet_e_hilal",
                "Asia/Karachi", stamp, NOW) == null, "Ambiguous method rejected");
        check(decide("2026-11-12", 1, 6, 1448, "pakistan_ruet_e_hilal_calculated",
                "Asia/Karachi", stamp, NOW) == null, "Calculated method rejected");
        check(decide("2026-11-12", 0, 6, 1448, "pakistan_ruet_e_hilal_confirmed",
                "Asia/Karachi", stamp, NOW) == null, "Day zero rejected");
        check(decide("2026-11-12", 31, 6, 1448, "pakistan_ruet_e_hilal_confirmed",
                "Asia/Karachi", stamp, NOW) == null, "Day 31 rejected");
        check(decide("2026-11-12", 1, 0, 1448, "pakistan_ruet_e_hilal_confirmed",
                "Asia/Karachi", stamp, NOW) == null, "Month zero rejected");
        check(decide("2026-11-12", 1, 6, 1448, "pakistan_ruet_e_hilal_confirmed",
                "Asia/Dubai", stamp, NOW) == null, "Wrong timezone rejected");
        check(decide("2026-11-10", 1, 6, 1448, "pakistan_ruet_e_hilal_confirmed",
                "Asia/Karachi", stamp, NOW) == null, "Mismatch between generation date and record");
        check(decide("2026-11-12", 1, 6, 1448, "pakistan_ruet_e_hilal_confirmed",
                "Asia/Karachi", "invalid", NOW) == null, "Invalid generated-at rejected");
        check(decide("2026-11-12", 1, 6, 1448, "pakistan_ruet_e_hilal_confirmed",
                "Asia/Karachi", "2026-11-09T18:38:45+05:00", NOW) == null,
                "Stale observation rejected");
        check(decide("2026-11-12", 1, 6, 1448, "pakistan_ruet_e_hilal_confirmed",
                "Asia/Karachi", "2026-11-13T18:38:45+05:00", NOW) == null,
                "Future observation rejected");
        check(decide("2026-11-12", 1, 6, 1448, "pakistan_ruet_e_hilal_confirmed",
                "Asia/Karachi", stamp, Instant.parse("2026-11-09T12:00:00Z")) == null,
                "Observation cannot be issued after current moment");
        // A later-day confirmed observation infers the same first day.
        FeedDatePolicy.Observation later = decide("2026-11-15", 4, 6, 1448,
                "pakistan_ruet_e_hilal_confirmed", "Asia/Karachi",
                "2026-11-15T10:00:00+05:00", Instant.parse("2026-11-15T06:00:00Z"));
        check(later != null && later.firstDay.equals(confirmed.firstDay), "Later days infer start");
        // A remote row 1 day away from the published forecast is acceptable.
        FeedDatePolicy.Observation shifted = decide("2026-12-12", 1, 7, 1448,
                "pakistan_ruet_e_hilal_confirmed", "Asia/Karachi",
                "2026-12-12T10:00:00+05:00", Instant.parse("2026-12-12T06:00:00Z"));
        check(shifted != null && shifted.firstDay.equals(LocalDate.parse("2026-12-12")),
                "One-day difference allowed");
        check(decide("2026-12-15", 1, 7, 1448, "pakistan_ruet_e_hilal_confirmed",
                "Asia/Karachi", "2026-12-15T10:00:00+05:00",
                Instant.parse("2026-12-15T06:00:00Z")) == null,
                "Four-day difference from source rejected");
        System.out.println("FeedDatePolicyTest PASS: " + passed + " assertions");
    }
}
