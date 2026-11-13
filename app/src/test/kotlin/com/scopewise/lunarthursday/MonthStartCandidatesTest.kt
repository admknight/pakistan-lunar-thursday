package com.scopewise.lunarthursday

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthStartCandidatesTest {
    private fun d(iso: String) = LocalDate.parse(iso)

    @Test fun confirmedMonthHasOneStartAndOneThursday() {
        val r = MonthStartCandidates.forMonth(1448, 6, mapOf("1448-6" to d("2026-11-12")))
        assertEquals(MonthStartCandidates.Basis.CONFIRMED, r.basis)
        assertEquals(listOf(d("2026-11-12")), r.possibleStarts)
        assertTrue(r.isFirstThursdayCertain)
        assertEquals(d("2026-11-12"), r.earliestThursday)
    }

    /** Jumada al-Thani 1448 confirmed Thu 12 Nov 2026: Rajab starts Fri 11 or Sat 12 Dec. */
    @Test fun rajab1448IsCertainWhateverTheSighting() {
        val r = MonthStartCandidates.forMonth(1448, 7, mapOf("1448-6" to d("2026-11-12")))
        assertEquals(MonthStartCandidates.Basis.FROM_CONFIRMED_EARLIER_MONTH, r.basis)
        assertEquals(listOf(d("2026-12-11"), d("2026-12-12")), r.possibleStarts)
        assertTrue(r.isFirstThursdayCertain)
        assertEquals(d("2026-12-17"), r.earliestThursday)
        assertEquals(d("2026-12-17"), r.latestThursday)
    }

    /** The only uncertain case: the 29th is a Wednesday, so a sighting makes Thursday the 1st. */
    @Test fun wednesdayTwentyNinthMakesFirstThursdayDependOnSighting() {
        // Previous month confirmed on Wed 10 Mar 2027; its 29th is Wed 7 Apr 2027.
        val r = MonthStartCandidates.forMonth(1448, 11, mapOf("1448-10" to d("2027-03-10")))
        assertEquals(listOf(d("2027-04-08"), d("2027-04-09")), r.possibleStarts)
        assertEquals(DayOfWeek.THURSDAY, r.possibleStarts[0].dayOfWeek)
        assertFalse(r.isFirstThursdayCertain)
        assertEquals(d("2027-04-08"), r.earliestThursday)
        assertEquals(d("2027-04-15"), r.latestThursday)
    }

    /** With the previous start confirmed, uncertainty exists exactly when start+29 is a Thursday. */
    @Test fun uncertainOnlyWhenEarlierCandidateIsThursday() {
        var anchor = d("2026-01-01")
        repeat(400) {
            val r = MonthStartCandidates.forMonth(1448, 7, mapOf("1448-6" to anchor))
            val earlierIsThursday = anchor.plusDays(29).dayOfWeek == DayOfWeek.THURSDAY
            assertEquals("anchor $anchor", !earlierIsThursday, r.isFirstThursdayCertain)
            for (t in r.possibleThursdays) assertEquals(DayOfWeek.THURSDAY, t.dayOfWeek)
            anchor = anchor.plusDays(1)
        }
    }

    @Test fun olderConfirmationWidensTheRange() {
        // Two months back: 58 to 60 days after the confirmed start.
        val two = MonthStartCandidates.forMonth(1448, 8, mapOf("1448-6" to d("2026-11-12")))
        assertEquals(listOf(d("2027-01-09"), d("2027-01-10"), d("2027-01-11")), two.possibleStarts)
        assertEquals(MonthStartCandidates.Basis.FROM_CONFIRMED_EARLIER_MONTH, two.basis)
        // Three months back: 87 to 90 days.
        val three = MonthStartCandidates.forMonth(1448, 9, mapOf("1448-6" to d("2026-11-12")))
        assertEquals(4, three.possibleStarts.size)
        assertEquals(d("2027-02-07"), three.possibleStarts.first())
        assertEquals(d("2027-02-10"), three.possibleStarts.last())
    }

    @Test fun nearestConfirmationWins() {
        val r = MonthStartCandidates.forMonth(1448, 8,
            mapOf("1448-6" to d("2026-11-12"), "1448-7" to d("2026-12-12")))
        assertEquals(listOf(d("2027-01-10"), d("2027-01-11")), r.possibleStarts)
    }

    @Test fun lookbackCrossesTheHijriYear() {
        val r = MonthStartCandidates.forMonth(1449, 1, mapOf("1448-12" to d("2027-05-08")))
        assertEquals(listOf(d("2027-06-06"), d("2027-06-07")), r.possibleStarts)
    }

    @Test fun withoutRecentConfirmationUsesForecastPlusMinusOneDay() {
        // PMD forecast for Rajab 1448 is Fri 11 Dec 2026.
        val none = MonthStartCandidates.forMonth(1448, 7, emptyMap())
        assertEquals(MonthStartCandidates.Basis.FORECAST_ONLY, none.basis)
        assertEquals(listOf(d("2026-12-10"), d("2026-12-11"), d("2026-12-12")), none.possibleStarts)
        assertFalse(none.isFirstThursdayCertain)
        assertEquals(listOf(d("2026-12-10"), d("2026-12-17")), none.possibleThursdays)
        // A confirmation four months back is too old to anchor on.
        val stale = MonthStartCandidates.forMonth(1448, 7, mapOf("1448-3" to d("2026-08-15")))
        assertEquals(MonthStartCandidates.Basis.FORECAST_ONLY, stale.basis)
    }

    @Test fun everyForecastMonthStaysWithinItsCandidates() {
        for ((key, start) in LunarEngine.publishedStarts()) {
            val (y, m) = key.split("-").map { it.toInt() }
            val r = MonthStartCandidates.forMonth(y, m, emptyMap())
            assertTrue(key, start in r.possibleStarts)
            assertTrue(key, LunarEngine.firstThursday(start) in r.possibleThursdays)
            assertTrue(key, r.possibleThursdays.size <= 2)
        }
    }

    @Test fun rejectsInvalidMonth() {
        for ((y, m) in listOf(1448 to 0, 1448 to 13, 0 to 5)) {
            try {
                MonthStartCandidates.forMonth(y, m, emptyMap())
                throw AssertionError("Accepted $y-$m")
            } catch (expected: IllegalArgumentException) { }
        }
    }
}
