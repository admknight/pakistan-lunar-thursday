package com.scopewise.lunarthursday

import java.time.Instant
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtraReminderPlannerTest {
    private val eleven = LocalTime.of(11, 0)
    private fun options(prayer: Boolean = false, thursday: Boolean = false, friday: Boolean = false,
                        city: Boolean = true, method: PrayerTimes.Method = PrayerTimes.Method.KARACHI_HANAFI) =
        ExtraReminderPlanner.Options(prayer, thursday, friday,
            if (city) 31.5204 else null, if (city) 74.3587 else null, method, eleven)
    private fun next(now: String, o: ExtraReminderPlanner.Options) = ExtraReminderPlanner.next(Instant.parse(now), o)

    @Test fun nothingIsPlannedWhenEverythingIsOff() {
        assertNull(next("2026-11-13T00:00:00Z", options()))
    }

    /** Lahore, Fri 13 Nov 2026 (reference times: Fajr 05:05, Dhuhr 11:47, Asr 15:28, Maghrib 17:05, Isha 18:29). */
    @Test fun prayerAlertsFollowTheDayInOrder() {
        val seen = ArrayList<ExtraReminderPlanner.Kind>()
        var now = Instant.parse("2026-11-12T19:30:00Z") // 00:30 PKT Friday
        repeat(6) {
            val n = ExtraReminderPlanner.next(now, options(prayer = true))!!
            seen.add(n.kind)
            now = n.at
        }
        assertEquals(listOf(ExtraReminderPlanner.Kind.FAJR, ExtraReminderPlanner.Kind.DHUHR, ExtraReminderPlanner.Kind.ASR,
            ExtraReminderPlanner.Kind.MAGHRIB, ExtraReminderPlanner.Kind.ISHA, ExtraReminderPlanner.Kind.FAJR), seen)
        val fajr = next("2026-11-12T19:30:00Z", options(prayer = true))!!
        assertTrue(Math.abs(fajr.localTime.toSecondOfDay() - LocalTime.of(5, 5).toSecondOfDay()) <= 120)
    }

    @Test fun prayerAlertsNeedACity() {
        assertNull(next("2026-11-13T00:00:00Z", options(prayer = true, city = false)))
    }

    @Test fun jafariMaghribIsLaterThanSunset() {
        val karachi = next("2026-11-13T11:00:00Z", options(prayer = true))!!                 // after Asr
        val jafari = next("2026-11-13T11:00:00Z", options(prayer = true, method = PrayerTimes.Method.JAFARI))!!
        assertEquals(ExtraReminderPlanner.Kind.MAGHRIB, karachi.kind)
        assertEquals(ExtraReminderPlanner.Kind.MAGHRIB, jafari.kind)
        assertTrue(jafari.at.isAfter(karachi.at))
    }

    @Test fun thursdayEveningUsesMaghribOrAFixedTimeWithoutACity() {
        // Fri 13 Nov 2026: the next Thursday is 19 Nov.
        val withCity = next("2026-11-13T00:00:00Z", options(thursday = true))!!
        assertEquals(ExtraReminderPlanner.Kind.THURSDAY_EVENING, withCity.kind)
        assertEquals("2026-11-19", withCity.at.atZone(LunarEngine.PAKISTAN).toLocalDate().toString())
        assertTrue(withCity.localTime.hour == 17)
        val withoutCity = next("2026-11-13T00:00:00Z", options(thursday = true, city = false))!!
        assertEquals(ExtraReminderPlanner.EVENING_WITHOUT_CITY, withoutCity.localTime)
    }

    @Test fun fridayUsesTheChosenReminderTime() {
        val before = next("2026-11-13T05:00:00Z", options(friday = true))!!   // Fri 10:00 PKT
        assertEquals(Instant.parse("2026-11-13T06:00:00Z"), before.at)
        val after = next("2026-11-13T06:00:00Z", options(friday = true))!!    // exactly 11:00: next week
        assertEquals(Instant.parse("2026-11-20T06:00:00Z"), after.at)
    }

    @Test fun theEarliestOfAllEnabledRemindersWins() {
        val n = next("2026-11-13T05:00:00Z", options(prayer = true, thursday = true, friday = true))!!
        assertEquals(ExtraReminderPlanner.Kind.FRIDAY, n.kind)   // 11:00, before Dhuhr at 11:47
    }
}
