package com.scopewise.lunarthursday

import java.io.StringReader
import java.net.URI
import java.net.URLEncoder
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.abs
import org.w3c.dom.Element
import org.xml.sax.InputSource

/**
 * Reads news headlines about the Central Ruet-e-Hilal Committee's decision.
 *
 * After a confirmed month start the next month begins 29 or 30 days later, so
 * the only question is whether the moon was sighted on the evening of the
 * 29th. A result is given only when at least two publishers (one a known
 * Pakistani outlet) agree and none disagrees. This mirrors
 * scripts/update_month_starts.py and is not an official source.
 */
object AnnouncementReader {
    enum class Outcome { SIGHTED, NOT_SIGHTED, CONFLICT, UNKNOWN }

    class Headline(
        @JvmField val title: String,
        @JvmField val publisherHost: String,
        @JvmField val published: Instant
    )

    const val MIN_PUBLISHERS = 2
    const val WINDOW_DAYS = 3L
    @JvmField val WINDOW_OPENS: LocalTime = LocalTime.of(16, 30)
    private const val MAX_FEED_CHARS = 2_000_000

    private val MONTH_PATTERNS: Map<Int, Regex> = mapOf(
        1 to Regex("""\bmuharram\b|\bmoharram\b"""),
        2 to Regex("""\bsafar\b"""),
        3 to Regex("""\brabi\w*\s+(?:ul\s+|al\s+)?aww?al\b"""),
        4 to Regex("""\brabi\w*\s+(?:ul\s+|us\s+|al\s+|uth\s+)?(?:sani|saani|thani|akhir\w*)\b"""),
        5 to Regex("""\bj[au]mad[ai]\w*\s+(?:ul\s+|al\s+)?(?:aww?al|ula|oola)\b"""),
        6 to Regex("""\bj[au]mad[ai]\w*\s+(?:ul\s+|us\s+|al\s+|uth\s+)?(?:sani|saani|thani|akhir\w*)\b"""),
        7 to Regex("""\brajab\b"""),
        8 to Regex("""\bsha\s?a?baa?n\b"""),
        9 to Regex("""\bramadh?an\b|\bramazan\b|\bramzan\b"""),
        10 to Regex("""\bshaww?al\b|\beid\s?ul\s?fitr\b|\beid\s+al\s+fitr\b"""),
        11 to Regex("""\b(?:zil|zul|zi|zee|dhul|dhu\s+al|dhu\s+l)\s*q[aiu]{1,3}\s?d\w*\b"""),
        12 to Regex("""\b(?:zil|zul|zi|dhul|dhu\s+al|dhu\s+l)\s*h[aiu]j\w*\b|\beid\s?ul\s?a[dz]ha\b"""))
    private val QUERY_NAMES: Map<Int, List<String>> = mapOf(
        1 to listOf("Muharram"), 2 to listOf("Safar"), 3 to listOf("Rabi ul Awwal"), 4 to listOf("Rabi us Sani"),
        5 to listOf("Jamadi ul Awwal"), 6 to listOf("Jamadi us Sani"), 7 to listOf("Rajab"), 8 to listOf("Shaban"),
        9 to listOf("Ramazan"), 10 to listOf("Shawwal"), 11 to listOf("Zilqad"), 12 to listOf("Zilhaj"))

    private val NOT_SIGHTED = Regex("""\bnot\s+(?:been\s+)?(?:sighted|seen|visible)\b|\bno\s+sighting\b|\bunsighted\b""")
    private val SIGHTED = Regex("""\bsighted\b|\bseen\b""")
    private val NOT_AN_ANNOUNCEMENT = Regex(
        """\b(?:likely|unlikely|expected|expects?|predict\w*|forecast\w*|may|might|could|chances?|probab\w*|""" +
        """possib\w*|to\s+meet|meets?|meeting|underway|convene\w*|when\s+is|will\s+be\s+sighted|to\s+be\s+sighted|""" +
        """suparco|pmd|met\s+office|astronom\w*|research\s+council|updates?|live)\b""")
    private val ELSEWHERE = Regex(
        """\b(?:saudi|ksa|uae|emirates|gulf|india|indian|bangladesh|indonesia|malaysia|brunei|turkey|turkiye|""" +
        """iran|iraq|oman|qatar|kuwait|bahrain|egypt|morocco|uk|britain|usa|america|canada|australia|""" +
        """afghanistan|japan|singapore|sri\s+lanka|new\s+zealand|nepal|maldives|arab|""" +
        """peshawar|khyber|kp|popalzai|qasim|unofficial|zonal)\b""")
    private val ONE_CITY = Regex("""\b(?:lahore|karachi|quetta|islamabad|rawalpindi|multan|hyderabad|faisalabad|gilgit)\b""")
    private val MOON_WORD = Regex("""\b(?:moon|crescent|chand|hilal)\b""")
    private val PAKISTANI_PUBLISHERS = setOf(
        "dawn.com", "tribune.com.pk", "geo.tv", "arynews.tv", "dunyanews.tv", "app.com.pk", "pakobserver.net",
        "thenews.com.pk", "samaa.tv", "nation.com.pk", "brecorder.com", "radio.gov.pk", "dailytimes.com.pk",
        "propakistani.pk", "bolnews.com", "24newshd.tv", "en.dailypakistan.com.pk", "pakistantoday.com.pk",
        "aaj.tv", "dailyausaf.com", "bloompakistan.com", "humnews.pk", "samaaenglish.tv", "thefridaytimes.com",
        "express.pk", "92newshd.tv", "abbtakk.tv", "ptv.com.pk", "thenews.pk")
    private val GREGORIAN: Map<String, Int> = mapOf(
        "jan" to 1, "january" to 1, "feb" to 2, "february" to 2, "mar" to 3, "march" to 3, "apr" to 4, "april" to 4,
        "may" to 5, "jun" to 6, "june" to 6, "jul" to 7, "july" to 7, "aug" to 8, "august" to 8,
        "sep" to 9, "sept" to 9, "september" to 9, "oct" to 10, "october" to 10, "nov" to 11, "november" to 11,
        "dec" to 12, "december" to 12)
    private val MONTH_WORD = GREGORIAN.keys.sortedByDescending { it.length }.joinToString("|")
    private val DATE_A = Regex("""\b($MONTH_WORD)\s+(\d{1,2})\b""")
    private val DATE_B = Regex("""\b(\d{1,2})\s+($MONTH_WORD)\b""")
    private val NON_ALNUM = Regex("[^a-z0-9]+")

    @JvmStatic
    fun normalise(text: String): String = NON_ALNUM.replace(text.lowercase(), " ").trim()

    @JvmStatic
    fun hostOf(url: String?): String {
        val host = try { URI(url ?: "").host } catch (invalid: Exception) { null } ?: return ""
        return host.lowercase().removePrefix("www.")
    }

    private fun datesIn(text: String, near: LocalDate): Set<LocalDate> {
        val pairs = DATE_A.findAll(text).map { GREGORIAN.getValue(it.groupValues[1]) to it.groupValues[2].toInt() } +
            DATE_B.findAll(text).map { GREGORIAN.getValue(it.groupValues[2]) to it.groupValues[1].toInt() }
        val found = HashSet<LocalDate>()
        for ((month, day) in pairs) {
            var best: LocalDate? = null
            for (year in near.year - 1..near.year + 1) {
                val candidate = try { LocalDate.of(year, month, day) } catch (invalid: Exception) { continue }
                if (best == null || abs(ChronoUnit.DAYS.between(candidate, near)) < abs(ChronoUnit.DAYS.between(best, near))) {
                    best = candidate
                }
            }
            best?.let { found.add(it) }
        }
        return found
    }

    /** The Hijri month a headline's moon report is about: the name nearest the word 'moon'. */
    private fun monthNamedWithMoon(text: String): Int? {
        val moonAt = MOON_WORD.find(text)?.range?.first ?: text.length
        var best: Int? = null
        var bestGap = Int.MAX_VALUE
        for ((number, pattern) in MONTH_PATTERNS) {
            for (match in pattern.findAll(text)) {
                val gap = abs(moonAt - (match.range.last + 1))
                if (gap < bestGap) { best = number; bestGap = gap }
            }
        }
        return best
    }

    /**
     * One headline's verdict. first: SIGHTED, NOT_SIGHTED or null. second: true
     * when the headline itself names the resulting first day.
     */
    @JvmStatic
    fun classify(title: String, publisherHost: String, hijriMonth: Int,
                 ifSighted: LocalDate, ifNotSighted: LocalDate): Pair<Outcome?, Boolean> {
        val none = Pair<Outcome?, Boolean>(null, false)
        if ('?' in title) return none
        val text = normalise(title.substringBeforeLast(" - "))
        if (monthNamedWithMoon(text) != hijriMonth) return none
        if (NOT_AN_ANNOUNCEMENT.containsMatchIn(text) || ELSEWHERE.containsMatchIn(text)) return none
        val namesPakistan = "pakistan" in text
        if (!namesPakistan && (publisherHost !in PAKISTANI_PUBLISHERS || ONE_CITY.containsMatchIn(text))) return none
        val vote: Outcome
        val own: LocalDate
        val other: LocalDate
        if (NOT_SIGHTED.containsMatchIn(text)) {
            vote = Outcome.NOT_SIGHTED; own = ifNotSighted; other = ifSighted
        } else if (SIGHTED.containsMatchIn(text)) {
            vote = Outcome.SIGHTED; own = ifSighted; other = ifNotSighted
        } else {
            return none
        }
        val dates = datesIn(text, ifSighted)
        val days = DayOfWeek.values().filter { Regex("\\b${it.name.lowercase()}\\b").containsMatchIn(text) }.toSet()
        // A headline that names the other candidate day contradicts its own verdict.
        if (other in dates || (other.dayOfWeek in days && own.dayOfWeek !in days)) return none
        return Pair(vote, own in dates || own.dayOfWeek in days)
    }

    /** Google News gives older items a bare date: midnight US Pacific, 07:00 or 08:00 UTC. */
    private fun isDateOnly(published: Instant): Boolean {
        val utc = published.atZone(ZoneOffset.UTC)
        return utc.minute == 0 && utc.second == 0 && (utc.hour == 7 || utc.hour == 8)
    }

    private fun inTime(vote: Outcome, dated: Boolean, published: Instant, eveningOf29th: LocalDate): Boolean {
        val local = published.atZone(LunarEngine.PAKISTAN)
        val opens = ZonedDateTime.of(eveningOf29th, WINDOW_OPENS, LunarEngine.PAKISTAN)
        if (local.toLocalDate() == eveningOf29th) return isDateOnly(published) || !local.isBefore(opens)
        if (local.isBefore(opens) || local.isAfter(opens.plusDays(WINDOW_DAYS))) return false
        if (vote == Outcome.NOT_SIGHTED) return true
        // "Sighted" the next day could mean the evening of the 30th, when the moon is
        // always visible. Accept it only before dawn, or when it names the first day.
        val beforeDawn = !isDateOnly(published) &&
            local.isBefore(ZonedDateTime.of(eveningOf29th.plusDays(1), LocalTime.of(4, 0), LunarEngine.PAKISTAN))
        return beforeDawn || dated
    }

    @JvmStatic
    fun decide(items: List<Headline>, hijriMonth: Int, eveningOf29th: LocalDate): Outcome {
        val ifSighted = eveningOf29th.plusDays(1)
        val ifNotSighted = eveningOf29th.plusDays(2)
        val yes = HashSet<String>()
        val no = HashSet<String>()
        for (item in items) {
            if (item.publisherHost.isEmpty()) continue
            val (vote, dated) = classify(item.title, item.publisherHost, hijriMonth, ifSighted, ifNotSighted)
            if (vote == null || !inTime(vote, dated, item.published, eveningOf29th)) continue
            (if (vote == Outcome.SIGHTED) yes else no).add(item.publisherHost)
        }
        if (yes.isNotEmpty() && no.isNotEmpty()) return Outcome.CONFLICT
        if (yes.size >= MIN_PUBLISHERS && yes.any { it in PAKISTANI_PUBLISHERS }) return Outcome.SIGHTED
        if (no.size >= MIN_PUBLISHERS && no.any { it in PAKISTANI_PUBLISHERS }) return Outcome.NOT_SIGHTED
        return Outcome.UNKNOWN
    }

    /** Search addresses for the days around the decision, most specific last. */
    @JvmStatic
    fun queryUrls(hijriMonth: Int, eveningOf29th: LocalDate): List<String> {
        val span = "after:${eveningOf29th.minusDays(1)} before:${eveningOf29th.plusDays(WINDOW_DAYS + 2)}"
        val queries = ArrayList<String>()
        queries.add("Ruet-e-Hilal moon Pakistan $span")
        queries.add("moon sighted Pakistan $span")
        for (name in QUERY_NAMES.getValue(hijriMonth)) {
            queries.add("$name moon sighted Pakistan $span")
            queries.add("$name moon not sighted Pakistan $span")
        }
        return queries.map {
            "https://news.google.com/rss/search?q=" + URLEncoder.encode(it, "UTF-8") + "&hl=en-PK&gl=PK&ceid=PK:en"
        }
    }

    /** Items of one RSS feed. Anything unreadable is skipped; a broken feed gives an empty list. */
    @JvmStatic
    fun parseRss(xml: String?): List<Headline> {
        if (xml == null || xml.length > MAX_FEED_CHARS) return emptyList()
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            try { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) } catch (ignored: Exception) { }
            try { factory.isExpandEntityReferences = false } catch (ignored: Exception) { }
            val builder = factory.newDocumentBuilder()
            builder.setErrorHandler(object : org.xml.sax.helpers.DefaultHandler() { })
            val document = builder.parse(InputSource(StringReader(xml)))
            val nodes = document.getElementsByTagName("item")
            val result = ArrayList<Headline>()
            for (i in 0 until nodes.length) {
                val item = nodes.item(i) as? Element ?: continue
                val title = item.getElementsByTagName("title").item(0)?.textContent?.trim().orEmpty()
                val source = item.getElementsByTagName("source").item(0) as? Element
                val host = hostOf(source?.getAttribute("url"))
                val stamp = item.getElementsByTagName("pubDate").item(0)?.textContent?.trim().orEmpty()
                val published = try {
                    ZonedDateTime.parse(stamp, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
                } catch (invalid: Exception) { continue }
                if (title.isNotEmpty() && host.isNotEmpty()) result.add(Headline(title, host, published))
            }
            result
        } catch (invalid: Exception) {
            emptyList()
        }
    }
}
