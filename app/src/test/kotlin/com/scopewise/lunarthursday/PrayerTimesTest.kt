package com.scopewise.lunarthursday

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reference values were taken from api.aladhan.com on 13 Nov 2026 (method 1
 * with Hanafi Asr, and method 0), as an independent implementation. Each time
 * must agree within two minutes.
 */
class PrayerTimesTest {
    private fun minutes(t: LocalTime?) = t!!.hour * 60 + t.minute
    private fun near(label: String, expected: String, actual: LocalTime?) {
        assertNotNull(label, actual)
        val gap = Math.abs(minutes(LocalTime.parse(expected)) - minutes(actual))
        assertTrue("$label expected $expected but was $actual", gap <= 2)
    }
    private fun check(date: String, lat: Double, lng: Double, method: PrayerTimes.Method, expected: List<String>) {
        val day = PrayerTimes.compute(LocalDate.parse(date), lat, lng, method)
        val label = "$date $method"
        near("$label fajr", expected[0], day.fajr)
        near("$label sunrise", expected[1], day.sunrise)
        near("$label dhuhr", expected[2], day.dhuhr)
        near("$label asr", expected[3], day.asr)
        near("$label sunset", expected[4], day.sunset)
        near("$label maghrib", expected[5], day.maghrib)
        near("$label isha", expected[6], day.isha)
    }

    @Test fun lahoreInNovember() {
        check("2026-11-13", 31.5204, 74.3587, PrayerTimes.Method.KARACHI_HANAFI,
            listOf("05:05", "06:28", "11:47", "15:28", "17:05", "17:05", "18:29"))
        check("2026-11-13", 31.5204, 74.3587, PrayerTimes.Method.JAFARI,
            listOf("05:14", "06:28", "11:47", "14:45", "17:05", "17:21", "18:09"))
    }

    @Test fun karachiAtMidsummer() {
        check("2027-06-21", 24.8607, 67.0011, PrayerTimes.Method.KARACHI_HANAFI,
            listOf("04:14", "05:43", "12:34", "17:16", "19:24", "19:24", "20:53"))
        check("2027-06-21", 24.8607, 67.0011, PrayerTimes.Method.JAFARI,
            listOf("04:25", "05:43", "12:34", "15:55", "19:24", "19:40", "20:32"))
    }

    @Test fun gilgitInJanuary() {
        check("2027-01-15", 35.9208, 74.3144, PrayerTimes.Method.KARACHI_HANAFI,
            listOf("05:42", "07:13", "12:12", "15:33", "17:12", "17:12", "18:42"))
        check("2027-01-15", 35.9208, 74.3144, PrayerTimes.Method.JAFARI,
            listOf("05:53", "07:13", "12:12", "14:52", "17:12", "17:29", "18:21"))
    }

    @Test fun timesAreInOrderEveryDayOfTheYearForEveryCity() {
        val cities = listOf(24.8607 to 67.0011, 31.5204 to 74.3587, 33.6844 to 73.0479, 34.0151 to 71.5249,
            30.1798 to 66.9750, 30.1575 to 71.5249, 31.4504 to 73.1350, 25.3960 to 68.3578, 35.9208 to 74.3144)
        for ((lat, lng) in cities) for (method in PrayerTimes.Method.values()) {
            var date = LocalDate.parse("2027-01-01")
            repeat(365) {
                val day = PrayerTimes.compute(date, lat, lng, method)
                val order = listOf(day.fajr, day.sunrise, day.dhuhr, day.asr, day.sunset, day.maghrib, day.isha).map { minutes(it) }
                for (i in 1 until order.size) assertTrue("$date $lat $method $order", order[i] >= order[i - 1])
                if (method == PrayerTimes.Method.KARACHI_HANAFI) assertEquals(day.sunset, day.maghrib)
                else assertTrue(minutes(day.maghrib) - minutes(day.sunset) in 10..25)
                date = date.plusDays(1)
            }
        }
    }

    @Test fun sunsetAgreesWithTheExistingSunsetClock() {
        var date = LocalDate.parse("2027-01-01")
        repeat(73) {
            val old = SunsetMath.sunsetPakistan(date, 31.5204, 74.3587)
            val new = PrayerTimes.compute(date, 31.5204, 74.3587, PrayerTimes.Method.KARACHI_HANAFI).sunset
            assertTrue("$date $old $new", Math.abs(minutes(old) - minutes(new)) <= 2)
            date = date.plusDays(5)
        }
    }

    @Test fun methodIdsRoundTripAndUnknownFallsBack() {
        for (m in PrayerTimes.Method.values()) assertEquals(m, PrayerTimes.Method.fromId(m.id))
        assertEquals(PrayerTimes.Method.KARACHI_HANAFI, PrayerTimes.Method.fromId(null))
        assertEquals(PrayerTimes.Method.KARACHI_HANAFI, PrayerTimes.Method.fromId("nonsense"))
    }
}
