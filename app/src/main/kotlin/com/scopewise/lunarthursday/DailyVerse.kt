package com.scopewise.lunarthursday

import java.time.LocalDate
import org.json.JSONObject

/**
 * The bundled short verses for the daily card. The Arabic text and the Urdu
 * translations are copied verbatim from Tanzil by scripts/build_verses.py and
 * are never altered here. They are not covered by this project's MIT license.
 */
object DailyVerse {
    const val SCHEMA = 1
    @JvmField val TRANSLATIONS: List<String> = listOf("jalandhry", "najafi", "jawadi")

    class Verse internal constructor(
        @JvmField val surah: Int,
        @JvmField val ayah: Int,
        @JvmField val arabic: String,
        private val urdu: Map<String, String>
    ) {
        /** The chosen translation, or the first one when the id is unknown. */
        fun urdu(translationId: String?): String = urdu[translationId] ?: urdu.getValue(TRANSLATIONS[0])
        fun reference(): String = "$surah:$ayah"
    }

    class Library internal constructor(
        @JvmField val verses: List<Verse>,
        /** Translation id to translator's name. */
        @JvmField val translators: Map<String, String>,
        @JvmField val notice: String
    )

    /** Parses the whole file; any incomplete verse rejects it. */
    @JvmStatic
    fun parse(json: String): Library {
        val root = JSONObject(json)
        require(root.getInt("schema") == SCHEMA) { "Unsupported schema" }
        val names = root.getJSONObject("translations")
        val translators = TRANSLATIONS.associateWith { names.getString(it) }
        val rows = root.getJSONArray("verses")
        val verses = ArrayList<Verse>()
        val seen = HashSet<String>()
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            val surah = row.getInt("surah")
            val ayah = row.getInt("ayah")
            require(surah in 1..114 && ayah in 1..286 && seen.add("$surah:$ayah")) { "Bad reference $surah:$ayah" }
            val arabic = row.getString("arabic")
            require(arabic.isNotBlank()) { "Missing text for $surah:$ayah" }
            val urdu = row.getJSONObject("ur")
            val all = TRANSLATIONS.associateWith { urdu.getString(it).also { text ->
                require(text.isNotBlank()) { "Missing translation for $surah:$ayah" } } }
            verses.add(Verse(surah, ayah, arabic, all))
        }
        require(verses.isNotEmpty()) { "No verses" }
        return Library(verses, translators, root.getString("notice"))
    }

    /** The same verse for everyone on a given day, cycling through the list in order. */
    @JvmStatic
    fun forDate(library: Library, date: LocalDate): Verse =
        library.verses[Math.floorMod(date.toEpochDay(), library.verses.size.toLong()).toInt()]

    @JvmStatic
    fun normaliseTranslation(id: String?): String = if (id in TRANSLATIONS) id!! else TRANSLATIONS[0]
}
