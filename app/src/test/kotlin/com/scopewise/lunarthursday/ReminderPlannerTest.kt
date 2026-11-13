package com.scopewise.lunarthursday

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPlannerTest {
    private fun d(iso: String) = LocalDate.parse(iso)
    private val eleven = LocalTime.of(11, 0)
    private val jt1448 = mapOf("1448-6" to d("2026-11-12"))

    private fun shape(year: Int, month: Int, confirmed: Map<String, LocalDate>) =
        ReminderPlanner.occurrences(year, month, confirmed).map { "${it.thursday} ${it.kind}" }

    @Test fun confirmedMonthHasOneConfirmedReminder() {
        assertEquals(listOf("2026-11-12 CONFIRMED"), shape(1448, 6, jt1448))
    }

    /** With 12 Nov confirmed, Rajab starts Fri 11 or Sat 12 Dec: one Thursday either way. */
    @Test fun certainMonthHasOneExpectedReminder() {
        assertEquals(listOf("2026-12-17 EXPECTED"), shape(1448, 7, jt1448))
    }

    /** The 29th falls on a Wednesday: forecast says Friday start, a sighting would make Thursday the 1st. */
    @Test fun sightingDependentMonthAddsAPossibleNoticeOnTheOtherThursday() {
        val confirmed = mapOf("1448-10" to d("2027-03-10"))
        assertEquals(listOf("2027-04-08 POSSIBLE", "2027-04-15 EXPECTED"), shape(1448, 11, confirmed))
        val possible = ReminderPlanner.occurrences(1448, 11, confirmed).first()
        assertEquals(d("2027-04-15"), possible.expectedThursday)
        // Once the start is confirmed either way, only one reminder remains.
        assertEquals(listOf("2027-04-08 CONFIRMED"), shape(1448, 11, confirmed + ("1448-11" to d("2027-04-08"))))
        assertEquals(listOf("2027-04-15 CONFIRMED"), shape(1448, 11, confirmed + ("1448-11" to d("2027-04-09"))))
    }

    /** No confirmations at all: the forecast Thursday keeps its normal reminder. */
    @Test fun withoutConfirmationsTheForecastThursdayIsAlwaysExpected() {
        for ((key, start) in LunarEngine.publishedStarts()) {
            val (y, m) = key.split("-").map { it.toInt() }
            val all = ReminderPlanner.occurrences(y, m, emptyMap())
            val expected = all.single { it.kind == ReminderPlanner.Kind.EXPECTED }
            assertEquals(key, LunarEngine.firstThursday(start), expected.thursday)
            assertTrue(key, all.size <= 2)
            for (o in all) assertEquals(DayOfWeek.THURSDAY, o.thursday.dayOfWeek)
        }
        // Forecast Friday start: the Thursday before it is the possible one.
        assertEquals(listOf("2026-12-10 POSSIBLE", "2026-12-17 EXPECTED"), shape(1448, 7, emptyMap()))
        // Forecast Thursday start: the following Thursday is the possible one.
        assertEquals(listOf("2026-11-12 EXPECTED", "2026-11-19 POSSIBLE"), shape(1448, 6, emptyMap()))
    }

    @Test fun nextSkipsWhatHasPassedAndKeepsTheChosenTime() {
        val beforeReminder = Instant.parse("2026-12-17T05:59:00Z") // 10:59 PKT
        val next = ReminderPlanner.next(beforeReminder, jt1448, eleven)!!
        assertEquals("1448-7", next.month.key())
        assertEquals(Instant.parse("2026-12-17T06:00:00Z"), next.triggerAt(eleven))
        val after = ReminderPlanner.next(Instant.parse("2026-12-17T06:00:00Z"), jt1448, eleven)!!
        assertEquals("1448-8", after.month.key())
        // A later reminder time on the same Thursday is still ahead.
        val evening = ReminderPlanner.next(Instant.parse("2026-12-17T06:00:00Z"), jt1448, LocalTime.of(20, 0))!!
        assertEquals("1448-7", evening.month.key())
    }

    @Test fun nextReturnsThePossibleNoticeWhenItComesFirst() {
        val confirmed = mapOf("1448-10" to d("2027-03-10"))
        val first = ReminderPlanner.next(Instant.parse("2027-04-01T00:00:00Z"), confirmed, eleven)!!
        assertEquals(ReminderPlanner.Kind.POSSIBLE, first.kind)
        assertEquals(d("2027-04-08"), first.thursday)
        // After it has passed unconfirmed, the expected Thursday is next.
        val second = ReminderPlanner.next(first.triggerAt(eleven), confirmed, eleven)!!
        assertEquals(ReminderPlanner.Kind.EXPECTED, second.kind)
        assertEquals(d("2027-04-15"), second.thursday)
        // A late correction that moves the month to the later Thursday still reminds there.
        val corrected = confirmed + ("1448-11" to d("2027-04-09"))
        val third = ReminderPlanner.next(first.triggerAt(eleven), corrected, eleven)!!
        assertEquals("2027-04-15 CONFIRMED", "${third.thursday} ${third.kind}")
    }

    /** With no confirmations, the planner's expected reminders are exactly the old engine's. */
    @Test fun matchesTheExistingEngineWhenIgnoringPossibleNotices() {
        var now = Instant.parse("2026-01-01T00:00:00Z")
        repeat(30) {
            val old = LunarEngine.upcoming(now, emptyMap(), 1, eleven)[0]
            var planned = ReminderPlanner.next(now, emptyMap(), eleven)
            while (planned!!.kind == ReminderPlanner.Kind.POSSIBLE) {
                planned = ReminderPlanner.next(planned.triggerAt(eleven), emptyMap(), eleven)
            }
            assertNotNull(planned)
            assertEquals(old.key(), planned.month.key())
            assertEquals(old.remindAt(eleven), planned.triggerAt(eleven))
            now = old.remindAt(eleven)
        }
    }
}
