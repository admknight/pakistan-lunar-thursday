package com.scopewise.lunarthursday

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Mirrors scripts/tests/test_update_month_starts.py so the app and the workflow agree. */
class AnnouncementReaderTest {
    private val yes = LocalDate.parse("2026-11-11")
    private val no = LocalDate.parse("2026-11-12")
    private val evening = LocalDate.parse("2026-11-10")
    private val NOT = "Jamadi-us-Sani moon not sighted in Pakistan"
    private val YES = "Jamadi-us-Sani moon sighted in Pakistan"

    private fun vote(title: String, publisher: String = "example.org", month: Int = 6,
                     ifYes: LocalDate = yes, ifNo: LocalDate = no) =
        AnnouncementReader.classify(title, publisher, month, ifYes, ifNo).first

    private fun item(title: String, publisher: String, at: String): AnnouncementReader.Headline {
        val instant = if (at.endsWith("Z") || at.contains("+")) OffsetDateTime.parse(at).toInstant()
        else OffsetDateTime.parse("$at+05:00").toInstant()
        return AnnouncementReader.Headline(title, publisher, instant)
    }
    private fun outcome(vararg items: AnnouncementReader.Headline) =
        AnnouncementReader.decide(items.toList(), 6, evening)

    @Test fun realAnnouncementHeadlines() {
        for ((title, publisher) in listOf(
            "Jamadi-ul-Sani Moon Not Sighted in Pakistan; First Day on Nov. 12 - Pakistan Observer" to "pakobserver.net",
            "Jumada-ul-Sani moon not sighted in Pakistan - 24 News HD" to "24newshd.tv",
            "Jumada al-Thani moon not sighted in Pakistan; month to begin on November 12 - Daily Pakistan" to "en.dailypakistan.com.pk")) {
            assertEquals(title, AnnouncementReader.Outcome.NOT_SIGHTED, vote(title, publisher))
        }
    }

    @Test fun monthSpellings() {
        val cases = mapOf(
            1 to "Muharram moon not sighted in Pakistan, Ashura to fall on June 26",
            2 to "Moon sighted in Pakistan; 1st Safar on July 16",
            3 to "Rabiul Awwal moon not sighted in Pakistan", 4 to "Rabi-us-Sani moon not sighted in Pakistan",
            5 to "Jumadi-ul-Awwal 1448 AH Moon Sighted in Pakistan", 6 to "Jamadi ul Sani moon not sighted in Pakistan",
            7 to "Rajab moon sighted in Pakistan", 8 to "Sha'ban moon not sighted in Pakistan",
            9 to "Ramazan moon sighted in Pakistan", 10 to "Shawwal moon not sighted in Pakistan",
            11 to "Zil-Qad moon sighted in Pakistan", 12 to "Zilhaj moon sighted in Pakistan")
        for ((month, title) in cases) assertNotNull(title, vote(title, month = month))
        assertNotNull(vote("Dhul Qadah moon sighted in Pakistan", month = 11))
        assertNotNull(vote("Dhul Hijjah moon sighted in Pakistan", month = 12))
        assertNull(vote("Dhul Hijjah moon sighted in Pakistan", month = 11))
        assertNull(vote("Jamadi ul Awwal moon sighted in Pakistan", month = 6))
        assertNull(vote("Rabi ul Awwal moon sighted in Pakistan", month = 4))
    }

    @Test fun theMonthNextToMoonWins() {
        val title = "Only four months left until Ramadan as the Jumada al-Awwal moon is sighted in Pakistan"
        assertNull(vote(title, month = 9))
        assertEquals(AnnouncementReader.Outcome.SIGHTED, vote(title, month = 5))
    }

    @Test fun forecastsMeetingsQuestionsAndOtherPlacesAreIgnored() {
        for (title in listOf(
            "Jamadi-us-Sani moon likely to be sighted in Pakistan on Nov 10",
            "Jamadi-us-Sani moon expected to be sighted in Pakistan",
            "Ruet-e-Hilal Committee to meet for Jamadi-us-Sani moon sighting in Pakistan",
            "Will Jamadi-us-Sani moon be sighted in Pakistan tomorrow?",
            "PMD says Jamadi-us-Sani moon not sighted possibility in Pakistan",
            "Live: Jamadi-us-Sani moon sighted updates Pakistan",
            "Pakistan Announces Date for Start of Jamadi-us-Sani",
            "Jamadi-us-Sani moon sighted in Saudi Arabia, UAE",
            "Jamadi-us-Sani moon sighted in Pakistan, India, Bangladesh",
            "Jamadi-us-Sani moon sighted in Peshawar, Pakistan",
            "Zonal committee says Jamadi-us-Sani moon not sighted in Pakistan",
            "Unofficial committee: Jamadi-us-Sani moon sighted in Pakistan",
            "Jamadi-us-Sani moon not sighted in Karachi")) {
            assertNull(title, vote(title, "geo.tv"))
        }
    }

    @Test fun unknownPublisherMustNamePakistan() {
        assertNull(vote("Jamadi-us-Sani moon not sighted", "blog.example.org"))
        assertEquals(AnnouncementReader.Outcome.NOT_SIGHTED, vote("Jamadi-us-Sani moon not sighted", "dawn.com"))
        assertEquals(AnnouncementReader.Outcome.NOT_SIGHTED, vote(NOT, "blog.example.org"))
    }

    @Test fun headlineNamingTheOtherDayIsDropped() {
        assertNull(vote("$YES, first day on Nov 12"))
        assertNull(vote("$NOT, month begins November 11"))
        assertNull(vote("Eidul Fitr to be celebrated on Saturday as Shawwal moon sighted in Pakistan", "dunyanews.tv",
            month = 10, ifYes = LocalDate.parse("2026-03-20"), ifNo = LocalDate.parse("2026-03-21")))
        assertEquals(AnnouncementReader.Outcome.NOT_SIGHTED, vote(
            "Muharram moon not sighted in Pakistan, Ashura to fall on June 26", "dawn.com",
            month = 1, ifYes = LocalDate.parse("2026-06-16"), ifNo = LocalDate.parse("2026-06-17")))
    }

    @Test fun twoPublishersMustAgreeAndOneMustBeKnown() {
        assertEquals(AnnouncementReader.Outcome.NOT_SIGHTED, outcome(
            item(NOT, "dawn.com", "2026-11-10T19:00:00"), item(NOT, "geo.tv", "2026-11-10T19:30:00")))
        assertEquals(AnnouncementReader.Outcome.SIGHTED, outcome(
            item(YES, "dawn.com", "2026-11-10T19:00:00"), item(YES, "geo.tv", "2026-11-10T19:30:00")))
        assertEquals(AnnouncementReader.Outcome.UNKNOWN, outcome(
            item(NOT, "dawn.com", "2026-11-10T19:00:00"), item(NOT, "dawn.com", "2026-11-10T20:00:00")))
        assertEquals(AnnouncementReader.Outcome.UNKNOWN, outcome(
            item(NOT, "a.example.org", "2026-11-10T19:00:00"), item(NOT, "b.example.org", "2026-11-10T19:30:00")))
        assertEquals(AnnouncementReader.Outcome.UNKNOWN, outcome())
        assertEquals(AnnouncementReader.Outcome.CONFLICT, outcome(
            item(NOT, "dawn.com", "2026-11-10T19:00:00"), item(NOT, "geo.tv", "2026-11-10T19:30:00"),
            item(YES, "samaa.tv", "2026-11-10T19:40:00")))
    }

    @Test fun timingRules() {
        // Before the evening: ignored.
        assertEquals(AnnouncementReader.Outcome.UNKNOWN, outcome(
            item(NOT, "dawn.com", "2026-11-10T15:00:00"), item(NOT, "geo.tv", "2026-11-09T19:30:00")))
        // Late "not sighted" reports still count, within three days.
        assertEquals(AnnouncementReader.Outcome.NOT_SIGHTED, outcome(
            item(NOT, "dawn.com", "2026-11-11T09:00:00"), item(NOT, "geo.tv", "2026-11-12T10:00:00")))
        assertEquals(AnnouncementReader.Outcome.UNKNOWN, outcome(
            item(NOT, "dawn.com", "2026-11-14T09:00:00"), item(NOT, "geo.tv", "2026-11-14T10:00:00")))
        // Next-evening "sighted" could be about the 30th unless it names the day.
        assertEquals(AnnouncementReader.Outcome.UNKNOWN, outcome(
            item(YES, "dawn.com", "2026-11-11T19:00:00"), item(YES, "geo.tv", "2026-11-11T19:30:00")))
        val dated = "$YES, first day on Nov 11"
        assertEquals(AnnouncementReader.Outcome.SIGHTED, outcome(
            item(dated, "dawn.com", "2026-11-11T09:00:00"), item(dated, "geo.tv", "2026-11-11T10:00:00")))
        assertEquals(AnnouncementReader.Outcome.SIGHTED, outcome(
            item(YES, "dawn.com", "2026-11-11T00:30:00"), item(YES, "geo.tv", "2026-11-10T23:30:00")))
        // Date-only stamps (07:00 or 08:00 UTC) on the 29th count; on the next day "sighted" does not.
        assertEquals(AnnouncementReader.Outcome.NOT_SIGHTED, outcome(
            item(NOT, "dawn.com", "2026-11-10T07:00:00Z"), item(NOT, "geo.tv", "2026-11-10T08:00:00Z")))
        assertEquals(AnnouncementReader.Outcome.UNKNOWN, outcome(
            item(YES, "dawn.com", "2026-11-11T07:00:00Z"), item(YES, "geo.tv", "2026-11-11T07:00:00Z")))
    }

    @Test fun parsesAGoogleNewsFeed() {
        val xml = """<?xml version="1.0" encoding="UTF-8"?><rss version="2.0"><channel><title>x</title>
            <item><title>Jumada al-Thani moon not sighted in Pakistan - Daily Ausaf</title>
              <pubDate>Tue, 10 Nov 2026 13:51:22 GMT</pubDate><source url="https://dailyausaf.com">Daily Ausaf</source></item>
            <item><title>Jumada-ul-Sani moon not sighted in Pakistan - 24 News HD</title>
              <pubDate>Tue, 10 Nov 2026 15:02:45 GMT</pubDate><source url="https://www.24newshd.tv">24 News HD</source></item>
            <item><title>No source here</title><pubDate>Tue, 10 Nov 2026 15:02:45 GMT</pubDate></item>
            <item><title>Bad date</title><pubDate>yesterday</pubDate><source url="https://dawn.com">Dawn</source></item>
            </channel></rss>"""
        val items = AnnouncementReader.parseRss(xml)
        assertEquals(listOf("dailyausaf.com", "24newshd.tv"), items.map { it.publisherHost })
        assertEquals(Instant.parse("2026-11-10T13:51:22Z"), items[0].published)
        assertEquals(AnnouncementReader.Outcome.NOT_SIGHTED, AnnouncementReader.decide(items, 6, evening))
        for (bad in listOf(null, "", "not xml", "<rss><channel></channel></rss>")) {
            assertTrue(AnnouncementReader.parseRss(bad).isEmpty())
        }
    }

    @Test fun queryUrlsStayOnGoogleNewsAndBoundTheDates() {
        val urls = AnnouncementReader.queryUrls(7, LocalDate.parse("2026-12-10"))
        assertEquals(4, urls.size)
        for (url in urls) {
            assertTrue(url.startsWith("https://news.google.com/rss/search?q="))
            assertTrue(url.contains("after%3A2026-12-09") && url.contains("before%3A2026-12-15"))
        }
        assertTrue(urls.any { it.contains("Rajab") })
    }
}
