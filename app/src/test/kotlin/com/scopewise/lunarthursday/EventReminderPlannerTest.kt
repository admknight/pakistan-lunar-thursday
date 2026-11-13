package com.scopewise.lunarthursday

import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventReminderPlannerTest {
    private fun d(iso: String) = LocalDate.parse(iso)
    private val eleven = LocalTime.of(11, 0)
    private val shared = setOf(IslamicEvents.SET_SHARED)
    private val all = setOf(IslamicEvents.SET_SHARED, IslamicEvents.SET_A, IslamicEvents.SET_B)

    private val bundled: List<IslamicEvents.Event> by lazy {
        val file = listOf(File("src/main/assets/events.json"), File("app/src/main/assets/events.json")).first { it.isFile }
        IslamicEvents.parse(file.readText(Charsets.UTF_8))
    }
    private val defaults: Set<String> by lazy { bundled.filter { it.remindDefault }.map { it.id }.toSet() }

    private fun next(now: String, sets: Set<String> = shared, ids: Set<String> = defaults,
                     confirmed: Map<String, LocalDate> = emptyMap()) =
        EventReminderPlanner.next(bundled, Instant.parse(now), confirmed, sets, ids, eleven)

    @Test fun everyEventHasAnUrduNameAndOnlyTheMonthlyFastIsOffByDefault() {
        for (e in bundled) assertTrue(e.id, !e.nameUr.isNullOrBlank())
        assertEquals(listOf("ayyam-al-beedh"), bundled.filter { !it.remindDefault }.map { it.id })
    }

    @Test fun announcesOnTheDayAtTheChosenTime() {
        // Forecast: Ashura 1449 is Tue 15 Jun 2027.
        val batch = next("2027-06-10T00:00:00Z")!!
        assertEquals(d("2027-06-15"), batch.notifyDate)
        assertEquals(listOf("ashura"), batch.items.map { it.event.id })
        assertEquals(Instant.parse("2027-06-15T06:00:00Z"), batch.triggerAt(eleven))
        // Still ahead one minute before; gone at the reminder time.
        assertEquals(d("2027-06-15"), next("2027-06-15T05:59:00Z")!!.notifyDate)
        assertTrue(next("2027-06-15T06:00:00Z")!!.notifyDate.isAfter(d("2027-06-15")))
    }

    @Test fun nightBeforeObservancesAreAnnouncedADayEarlier() {
        // Forecast: Sha'ban 1448 starts Sun 10 Jan 2027, so the 15th is Sun 24 Jan and its night is Sat 23 Jan.
        val batch = next("2027-01-20T00:00:00Z")!!
        assertEquals(listOf("shab-e-barat"), batch.items.map { it.event.id })
        assertEquals(d("2027-01-23"), batch.notifyDate)
        assertEquals(d("2027-01-24"), batch.items[0].date)
    }

    @Test fun eventsOnTheSameDayShareOneBatch() {
        // 18 Dhu al-Hijjah has one entry in set a and one in set b.
        val batch = next("2027-05-24T00:00:00Z", sets = setOf(IslamicEvents.SET_A, IslamicEvents.SET_B))!!
        assertEquals(setOf("ghadeer", "usman-shahadat"), batch.items.map { it.event.id }.toSet())
        // With only one set chosen, only that entry is announced.
        val onlyB = next("2027-05-24T00:00:00Z", sets = setOf(IslamicEvents.SET_B))!!
        assertEquals(listOf("usman-shahadat"), onlyB.items.map { it.event.id })
    }

    @Test fun mutedEventsAndUnchosenSetsAreSkipped() {
        val withoutAshura = next("2027-06-10T00:00:00Z", ids = defaults - "ashura")!!
        assertFalse(withoutAshura.items.any { it.event.id == "ashura" })
        assertTrue(withoutAshura.notifyDate.isAfter(d("2027-06-15")))
        assertNull(next("2027-06-10T00:00:00Z", sets = emptySet()))
        assertNull(next("2027-06-10T00:00:00Z", ids = emptySet()))
    }

    @Test fun monthlyFastIsAnnouncedOnItsFirstDayOnlyWhenTurnedOn() {
        // Jumada al-Thani 1448 starts 12 Nov 2026 (confirmed), so the 13th is Tue 24 Nov.
        val confirmed = mapOf("1448-6" to d("2026-11-12"))
        val off = next("2026-11-13T12:00:00Z", confirmed = confirmed)!!
        assertFalse(off.items.any { it.event.id == "ayyam-al-beedh" })
        val on = next("2026-11-13T12:00:00Z", ids = defaults + "ayyam-al-beedh", confirmed = confirmed)!!
        assertEquals(d("2026-11-24"), on.notifyDate)
        assertEquals(listOf("ayyam-al-beedh"), on.items.map { it.event.id })
        assertTrue(on.items[0].confirmed)
    }

    @Test fun aCorrectedMonthStartMovesTheAnnouncement() {
        val moved = next("2027-06-10T00:00:00Z", confirmed = mapOf("1449-1" to d("2027-06-07")))!!
        assertEquals(d("2027-06-16"), moved.notifyDate)
        assertTrue(moved.items[0].confirmed)
    }

    @Test fun onDateListsWhatIsAnnouncedThatDay() {
        val same = EventReminderPlanner.onDate(bundled, d("2027-01-23"), emptyMap(), all, defaults)
        assertTrue(same.any { it.event.id == "shab-e-barat" })
        // The set-a entry for 15 Sha'ban is a daytime observance, so it is announced on the 24th.
        assertFalse(same.any { it.event.id == "mahdi-wiladat" })
        assertTrue(EventReminderPlanner.onDate(bundled, d("2027-01-24"), emptyMap(), all, defaults)
            .any { it.event.id == "mahdi-wiladat" })
    }
}
