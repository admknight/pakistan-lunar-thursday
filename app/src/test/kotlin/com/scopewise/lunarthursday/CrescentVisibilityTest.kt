package com.scopewise.lunarthursday

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrescentVisibilityTest {
    /** Values from an independent prototype of the same formulas, to catch porting slips. */
    @Test fun matchesThePrototype() {
        assertEquals(2.43, CrescentVisibility.estimate(LocalDate.parse("2026-11-10")).value, 0.15)
        assertEquals(8.43, CrescentVisibility.estimate(LocalDate.parse("2026-12-10")).value, 0.15)
        assertEquals(-0.51, CrescentVisibility.estimate(LocalDate.parse("2027-01-08")).value, 0.15)
        assertEquals(-5.6, CrescentVisibility.estimate(LocalDate.parse("2027-03-08")).value, 0.15)
    }

    /** The real decision on 10 Nov 2026: not sighted, and the estimate was not 'likely'. */
    @Test fun doesNotCallTheNovember2026EveningLikely() {
        assertTrue(CrescentVisibility.estimate(LocalDate.parse("2026-11-10")).outlook != CrescentVisibility.Outlook.LIKELY)
    }

    /**
     * Against the PMD forecast table: wherever this estimate says 'unlikely' the
     * forecast month is 30 days, and it agrees with the forecast in at least
     * 19 of the 22 usable months when 'likely' is read as a 29-day month.
     */
    @Test fun broadlyAgreesWithThePmdForecast() {
        val starts = LunarEngine.publishedStarts().entries.sortedBy { it.value }
        var usable = 0
        var agree = 0
        for (i in 1 until starts.size) {
            val length = ChronoUnit.DAYS.between(starts[i - 1].value, starts[i].value)
            if (length != 29L && length != 30L) continue   // the table's one impossible entry
            usable++
            val estimate = CrescentVisibility.estimate(starts[i - 1].value.plusDays(28))
            if (estimate.outlook == CrescentVisibility.Outlook.UNLIKELY) {
                assertEquals("${starts[i].key} unlikely but forecast 29 days", 30L, length)
            }
            if ((estimate.outlook == CrescentVisibility.Outlook.LIKELY) == (length == 29L)) agree++
        }
        assertEquals(22, usable)
        assertTrue("agreement $agree of $usable", agree >= 19)
    }

    @Test fun outlookFollowsTheThresholds() {
        var date = LocalDate.parse("2027-01-01")
        repeat(120) {
            val e = CrescentVisibility.estimate(date)
            val expected = when {
                e.value >= CrescentVisibility.LIKELY_FROM -> CrescentVisibility.Outlook.LIKELY
                e.value < CrescentVisibility.UNLIKELY_BELOW -> CrescentVisibility.Outlook.UNLIKELY
                else -> CrescentVisibility.Outlook.BORDERLINE
            }
            assertEquals(expected, e.outlook)
            date = date.plusDays(1)
        }
    }
}
