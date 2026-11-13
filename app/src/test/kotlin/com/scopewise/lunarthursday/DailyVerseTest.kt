package com.scopewise.lunarthursday

import java.io.File
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyVerseTest {
    private val text: String by lazy {
        listOf(File("src/main/assets/verses.json"), File("app/src/main/assets/verses.json"))
            .first { it.isFile }.readText(Charsets.UTF_8)
    }
    private val library by lazy { DailyVerse.parse(text) }

    @Test fun bundledFileIsCompleteAndCarriesItsNotice() {
        assertEquals(31, library.verses.size)
        for (v in library.verses) {
            assertTrue(v.reference(), v.arabic.isNotBlank())
            for (id in DailyVerse.TRANSLATIONS) assertTrue(v.reference() + id, v.urdu(id).isNotBlank())
        }
        assertEquals(DailyVerse.TRANSLATIONS.toSet(), library.translators.keys)
        assertTrue(library.notice.contains("Tanzil") && library.notice.contains("non-commercial"))
    }

    /** The file records a fingerprint of every text line when it is built from Tanzil; an edit breaks it. */
    @Test fun textMatchesTheFingerprintRecordedAtBuildTime() {
        val recorded = org.json.JSONObject(text).getString("text_sha256")
        val lines = library.verses.map { it.arabic } +
            library.verses.flatMap { v -> DailyVerse.TRANSLATIONS.map { v.urdu(it) } }
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(lines.joinToString("\n").toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        assertEquals(recorded, digest)
    }

    @Test fun oneVersePerDayCyclingInOrder() {
        val start = LocalDate.parse("2026-11-14")
        val first = DailyVerse.forDate(library, start)
        assertEquals(first.reference(), DailyVerse.forDate(library, start).reference())
        assertNotEquals(first.reference(), DailyVerse.forDate(library, start.plusDays(1)).reference())
        assertEquals(first.reference(), DailyVerse.forDate(library, start.plusDays(31)).reference())
        val seen = (0 until 31).map { DailyVerse.forDate(library, start.plusDays(it.toLong())).reference() }.toSet()
        assertEquals(31, seen.size)
        // Dates before 1970 still map into the list.
        assertTrue(DailyVerse.forDate(library, LocalDate.parse("1960-01-01")).arabic.isNotBlank())
    }

    @Test fun translationChoiceFallsBackSafely() {
        val verse = library.verses[0]
        assertEquals(verse.urdu("jalandhry"), verse.urdu(null))
        assertEquals(verse.urdu("jalandhry"), verse.urdu("unknown"))
        assertNotEquals(verse.urdu("jalandhry"), verse.urdu("najafi"))
        assertEquals("jalandhry", DailyVerse.normaliseTranslation(null))
        assertEquals("jawadi", DailyVerse.normaliseTranslation("jawadi"))
    }

    @Test fun rejectsIncompleteFiles() {
        val names = "\"translations\":{\"jalandhry\":\"a\",\"najafi\":\"b\",\"jawadi\":\"c\"}"
        fun verse(ur: String) = "{\"surah\":1,\"ayah\":2,\"arabic\":\"x\",\"ur\":$ur}"
        val full = "{\"jalandhry\":\"a\",\"najafi\":\"b\",\"jawadi\":\"c\"}"
        val bad = listOf(
            "{}", "{\"schema\":2,\"notice\":\"n\",$names,\"verses\":[${verse(full)}]}",
            "{\"schema\":1,\"notice\":\"n\",$names,\"verses\":[]}",
            "{\"schema\":1,\"notice\":\"n\",$names,\"verses\":[${verse("{\"jalandhry\":\"a\",\"najafi\":\"b\"}")}]}",
            "{\"schema\":1,\"notice\":\"n\",$names,\"verses\":[${verse(full)},${verse(full)}]}",
            "{\"schema\":1,\"notice\":\"n\",$names,\"verses\":[{\"surah\":115,\"ayah\":1,\"arabic\":\"x\",\"ur\":$full}]}")
        for (json in bad) {
            try { DailyVerse.parse(json); throw AssertionError("Accepted: $json") }
            catch (expected: Exception) { if (expected is AssertionError) throw expected }
        }
        assertEquals(1, DailyVerse.parse("{\"schema\":1,\"notice\":\"n\",$names,\"verses\":[${verse(full)}]}").verses.size)
    }
}
