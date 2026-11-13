package com.scopewise.lunarthursday

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetContentTest {
    private val confirmed = mapOf("1448-6" to LocalDate.parse("2026-11-12"))

    @Test fun countsWholePakistanDays() {
        // Fri 13 Nov 2026, 17:00 PKT: Rajab's first Thursday is 17 Dec.
        val next = WidgetContent.next(Instant.parse("2026-11-13T12:00:00Z"), confirmed)!!
        assertEquals("1448-7", next.month.key())
        assertEquals(LocalDate.parse("2026-12-17"), next.thursday)
        assertEquals(34, next.daysLeft)
        assertEquals(null, next.otherThursday)
    }

    @Test fun staysOnTodayUntilThePakistanDayEnds() {
        val morning = WidgetContent.next(Instant.parse("2026-12-16T19:30:00Z"), confirmed)!! // 00:30 PKT Thu
        assertEquals(0, morning.daysLeft)
        val lateEvening = WidgetContent.next(Instant.parse("2026-12-17T18:59:00Z"), confirmed)!! // 23:59 PKT Thu
        assertEquals("1448-7", lateEvening.month.key())
        assertEquals(0, lateEvening.daysLeft)
        val afterMidnight = WidgetContent.next(Instant.parse("2026-12-17T19:00:00Z"), confirmed)!! // 00:00 PKT Fri
        assertEquals("1448-8", afterMidnight.month.key())
        val dayBefore = WidgetContent.next(Instant.parse("2026-12-16T10:00:00Z"), confirmed)!!
        assertEquals(1, dayBefore.daysLeft)
    }

    @Test fun carriesTheOtherThursdayForSightingDependentMonths() {
        val next = WidgetContent.next(Instant.parse("2027-04-09T05:00:00Z"),
            mapOf("1448-10" to LocalDate.parse("2027-03-10")))!!
        assertEquals(LocalDate.parse("2027-04-15"), next.thursday)
        assertEquals(LocalDate.parse("2027-04-08"), next.otherThursday)
    }
}
