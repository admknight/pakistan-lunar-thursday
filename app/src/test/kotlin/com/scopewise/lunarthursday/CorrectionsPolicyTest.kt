package com.scopewise.lunarthursday

import java.io.File
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CorrectionsPolicyTest {
    private fun d(iso: String) = LocalDate.parse(iso)
    /** Fri 13 Nov 2026, 09:00 PKT. */
    private val now = Instant.parse("2026-11-13T04:00:00Z")

    private fun row(year: Int, month: Int, firstDay: String,
                    sources: String = "\"https://a.example/x\",\"https://b.example/y\"") =
        "{\"hijri_year\":$year,\"hijri_month\":$month,\"first_day\":\"$firstDay\",\"sources\":[$sources]}"

    private fun file(vararg rows: String, schema: Int = 1) =
        "{\"schema\":$schema,\"months\":[${rows.joinToString(",")}]}"

    private fun keys(json: String?, stored: Map<String, LocalDate> = emptyMap(), at: Instant = now) =
        CorrectionsPolicy.accept(json, stored, at).map { "${it.monthKey}=${it.firstDay}" }

    @Test fun acceptsAConfirmedStartThatMatchesTheForecast() {
        assertEquals(listOf("1448-6=2026-11-12"), keys(file(row(1448, 6, "2026-11-12"))))
    }

    @Test fun acceptsAOneDayDifferenceFromTheForecast() {
        assertEquals(listOf("1448-6=2026-11-13"), keys(file(row(1448, 6, "2026-11-13"))))
    }

    @Test fun rejectsAStartFarFromTheForecastWhenNoNeighbourIsConfirmed() {
        assertTrue(keys(file(row(1448, 6, "2026-11-05"))).isEmpty())
    }

    @Test fun withAConfirmedPreviousMonthOnlyTwentyNineOrThirtyDaysFit() {
        val stored = mapOf("1448-6" to d("2026-11-12"))
        val at = Instant.parse("2026-12-12T04:00:00Z")
        assertEquals(listOf("1448-7=2026-12-11"), keys(file(row(1448, 7, "2026-12-11")), stored, at))
        assertEquals(listOf("1448-7=2026-12-12"), keys(file(row(1448, 7, "2026-12-12")), stored, at))
        // 28 days: within 3 days of the forecast, but impossible after a confirmed 12 Nov start.
        assertTrue(keys(file(row(1448, 7, "2026-12-10")), stored, at).isEmpty())
        assertTrue(keys(file(row(1448, 7, "2026-12-13")), stored, at).isEmpty())
    }

    @Test fun rowsInTheSameFileChainOnEachOther() {
        val at = Instant.parse("2026-12-12T04:00:00Z")
        val good = file(row(1448, 7, "2026-12-12"), row(1448, 6, "2026-11-12"))
        assertEquals(listOf("1448-6=2026-11-12", "1448-7=2026-12-12"), keys(good, at = at))
        val broken = file(row(1448, 6, "2026-11-12"), row(1448, 7, "2026-12-10"))
        assertEquals(listOf("1448-6=2026-11-12"), keys(broken, at = at))
    }

    @Test fun aCorrectionReplacesAStoredValue() {
        val stored = mapOf("1448-6" to d("2026-11-13"))
        assertEquals(listOf("1448-6=2026-11-12"), keys(file(row(1448, 6, "2026-11-12")), stored))
    }

    @Test fun announcementsAreAtMostTwoDaysAhead() {
        // Announced Tue 10 Nov evening for Thu 12 Nov: two days ahead is allowed.
        val tuesdayEvening = Instant.parse("2026-11-10T15:00:00Z")
        assertEquals(listOf("1448-6=2026-11-12"), keys(file(row(1448, 6, "2026-11-12")), at = tuesdayEvening))
        val monday = Instant.parse("2026-11-09T15:00:00Z")
        assertTrue(keys(file(row(1448, 6, "2026-11-12")), at = monday).isEmpty())
    }

    @Test fun oldMonthsAreIgnored() {
        val muchLater = Instant.parse("2027-06-01T04:00:00Z")
        assertTrue(keys(file(row(1448, 6, "2026-11-12")), at = muchLater).isEmpty())
    }

    @Test fun needsTwoIndependentHttpsSources() {
        val one = "\"https://a.example/x\""
        val sameSite = "\"https://www.a.example/x\",\"https://a.example/y\""
        val notHttps = "\"http://a.example/x\",\"https://b.example/y\""
        for (sources in listOf(one, sameSite, notHttps, "")) {
            assertTrue(sources, keys(file(row(1448, 6, "2026-11-12", sources))).isEmpty())
        }
    }

    @Test fun oneBadRowRejectsTheWholeFile() {
        // A malformed file is not trusted in part.
        val bad = file(row(1448, 6, "2026-11-12"), row(1448, 13, "2026-12-11"))
        assertTrue(keys(bad).isEmpty())
        val duplicate = file(row(1448, 6, "2026-11-12"), row(1448, 6, "2026-11-13"))
        assertTrue(keys(duplicate).isEmpty())
    }

    @Test fun rejectsMalformedInput() {
        val oversized = " ".repeat(CorrectionsPolicy.MAX_BYTES + 1)
        for (text in listOf(null, "", "not json", "[]", "{}", file(schema = 2),
                file(row(1448, 6, "12-11-2026")), "{\"schema\":1}", oversized)) {
            assertTrue(keys(text).isEmpty())
        }
    }

    @Test fun emptyMonthListIsFine() {
        assertTrue(keys(file()).isEmpty())
        assertEquals(0, CorrectionsPolicy.wellFormedRowCount(file()))
    }

    /** Guards the committed file: it must parse, and its newest row must fit the calendar. */
    @Test fun committedFileIsValid() {
        val candidates = listOf(File("../data/month-starts.json"), File("data/month-starts.json"))
        val committed = candidates.firstOrNull { it.isFile }
            ?: throw AssertionError("data/month-starts.json not found from ${File(".").absolutePath}")
        val text = committed.readText(Charsets.UTF_8)
        assertTrue(text.length <= CorrectionsPolicy.MAX_BYTES)
        assertTrue(CorrectionsPolicy.wellFormedRowCount(text) >= 1)
        // On the day after the Jumada al-Thani 1448 announcement took effect, that row is accepted.
        assertTrue("1448-6=2026-11-12" in keys(text))
    }
}
