package com.scopewise.lunarthursday

import java.io.File
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IslamicEventsTest {
    private fun d(iso: String) = LocalDate.parse(iso)
    private val all = setOf(IslamicEvents.SET_SHARED, IslamicEvents.SET_A, IslamicEvents.SET_B)

    private val bundled: List<IslamicEvents.Event> by lazy {
        val file = listOf(File("src/main/assets/events.json"), File("app/src/main/assets/events.json"))
            .firstOrNull { it.isFile } ?: throw AssertionError("events.json not found from ${File(".").absolutePath}")
        IslamicEvents.parse(file.readText(Charsets.UTF_8))
    }
    private fun event(id: String) = bundled.single { it.id == id }

    private fun file(vararg rows: String) = "{\"schema\":1,\"events\":[${rows.joinToString(",")}]}"
    private fun row(id: String = "x", month: Int = 1, day: Int = 1, set: String = "shared",
                    name: String = "Name", extra: String = "") =
        "{\"id\":\"$id\",\"month\":$month,\"day\":$day,\"set\":\"$set\",\"name_en\":\"$name\"$extra}"

    @Test fun bundledFileIsValidAndCoversAllThreeSets() {
        assertEquals(50, bundled.size)
        assertEquals(all, bundled.map { it.set }.toSet())
        assertTrue(bundled.count { it.set == IslamicEvents.SET_SHARED } >= 10)
        for (e in bundled) assertTrue(e.id, e.nameEn.isNotBlank())
    }

    @Test fun datesFollowTheMonthStartsTheRemindersUse() {
        // Forecast: Muharram 1449 starts Sun 6 Jun 2027.
        assertEquals(d("2027-06-06"), IslamicEvents.dateIn(event("new-year"), 1449, 1, emptyMap()))
        assertEquals(d("2027-06-15"), IslamicEvents.dateIn(event("ashura"), 1449, 1, emptyMap()))
        // A confirmed start a day later moves every date in that month with it.
        val confirmed = mapOf("1449-1" to d("2027-06-07"))
        assertEquals(d("2027-06-16"), IslamicEvents.dateIn(event("ashura"), 1449, 1, confirmed))
    }

    @Test fun lastDayOfTheMonthIsTheDayBeforeTheNextMonthStarts() {
        // Forecast: Rabi al-Awwal 1449 starts Wed 4 Aug 2027, so Safar ends Tue 3 Aug.
        assertEquals(d("2027-08-03"), IslamicEvents.dateIn(event("ali-raza-shahadat"), 1449, 2, emptyMap()))
        val confirmed = mapOf("1449-3" to d("2027-08-05"))
        assertEquals(d("2027-08-04"), IslamicEvents.dateIn(event("ali-raza-shahadat"), 1449, 2, confirmed))
    }

    @Test fun aThirtiethDoesNotExistInATwentyNineDayMonth() {
        val thirtieth = IslamicEvents.parse(file(row(day = 30)))[0]
        // Forecast: Muharram 1449 6 Jun to Safar 6 Jul 2027 is 30 days; Safar to 4 Aug is 29.
        assertEquals(d("2027-07-05"), IslamicEvents.dateIn(thirtieth, 1449, 1, emptyMap()))
        val inSafar = IslamicEvents.parse(file(row(month = 2, day = 30)))[0]
        assertNull(IslamicEvents.dateIn(inSafar, 1449, 2, emptyMap()))
    }

    @Test fun upcomingIsOrderedFilteredAndStartsToday() {
        val today = d("2027-06-15") // Ashura 1449 on the forecast
        val shared = IslamicEvents.upcoming(bundled, today, emptyMap(), setOf(IslamicEvents.SET_SHARED), 4)
        assertEquals("ashura", shared[0].event.id)
        assertEquals(today, shared[0].date)
        assertEquals("ayyam-al-beedh", shared[1].event.id)
        assertEquals(d("2027-06-18"), shared[1].date)
        for (o in shared) assertEquals(IslamicEvents.SET_SHARED, o.event.set)
        for (i in 1 until shared.size) assertFalse(shared[i].date.isBefore(shared[i - 1].date))
        // Adding a set adds its events; nothing from another set leaks in.
        val withA = IslamicEvents.upcoming(bundled, today, emptyMap(), setOf(IslamicEvents.SET_SHARED, IslamicEvents.SET_A), 60)
        assertTrue(withA.any { it.event.id == "arbaeen" })
        assertFalse(withA.any { it.event.set == IslamicEvents.SET_B })
        assertTrue(IslamicEvents.upcoming(bundled, today, emptyMap(), emptySet(), 10).isEmpty())
    }

    @Test fun monthlyEventRepeatsEveryMonth() {
        val monthly = IslamicEvents.upcoming(bundled.filter { it.month == 0 }, d("2026-11-13"), emptyMap(), all, 3)
        // Jumada al-Thani 1448 starts 12 Nov 2026, Rajab 11 Dec, Sha'ban 10 Jan (forecast).
        assertEquals(listOf(d("2026-11-24"), d("2026-12-23"), d("2027-01-22")), monthly.map { it.date })
        assertEquals(3, monthly[0].event.days)
    }

    @Test fun confirmedFlagFollowsTheMonth() {
        val confirmed = mapOf("1448-6" to d("2026-11-12"))
        val next = IslamicEvents.upcoming(bundled, d("2026-11-13"), confirmed, all, 60)
        assertTrue(next.first { it.month.key() == "1448-6" }.confirmed)
        assertFalse(next.first { it.month.key() == "1448-7" }.confirmed)
    }

    @Test fun rejectsInvalidFiles() {
        val bad = listOf(
            "not json", "{}", "{\"schema\":2,\"events\":[]}",
            file(row(month = 13)), file(row(day = 31)), file(row(month = -1)), file(row(set = "c")),
            file(row(id = "Bad Id")), file(row(name = " ")), file(row(), row()),
            file(row(day = 0, extra = ",\"days\":2")), file(row(day = 29, extra = ",\"days\":3")),
            file(row(extra = ",\"days\":0")))
        for (text in bad) {
            try {
                IslamicEvents.parse(text)
                throw AssertionError("Accepted: $text")
            } catch (expected: Exception) {
                if (expected is AssertionError) throw expected
            }
        }
        assertTrue(IslamicEvents.parse(file()).isEmpty())
        val full = IslamicEvents.parse(file(row(extra = ",\"name_ur\":\"نام\",\"note_en\":\"n\",\"eve\":true,\"days\":2")))[0]
        assertEquals("نام", full.nameUr)
        assertTrue(full.eve)
        assertEquals(2, full.days)
    }
}
