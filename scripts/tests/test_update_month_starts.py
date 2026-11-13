import datetime as dt
import os
import sys
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
import update_month_starts as u  # noqa: E402


def item(title, publisher, when):
    published = dt.datetime.fromisoformat(when)
    if published.tzinfo is None:
        published = published.replace(tzinfo=u.PKT)
    return {"title": title, "publisher_url": "https://" + publisher, "published": published}


class Classify(unittest.TestCase):
    # Jumada al-Thani 1448: 29th was Tue 10 Nov 2026.
    yes, no = dt.date(2026, 11, 11), dt.date(2026, 11, 12)

    def vote(self, title, publisher="www.example.org", month=6, yes=None, no=None):
        return u.classify(title, "https://" + publisher, month, yes or self.yes, no or self.no)[0]

    def test_real_announcement_headlines(self):
        for title, publisher in [
            ("Jamadi-ul-Sani Moon Not Sighted in Pakistan; First Day on Nov. 12 - Pakistan Observer", "pakobserver.net"),
            ("Jumada-ul-Sani moon not sighted in Pakistan - 24 News HD", "www.24newshd.tv"),
            ("Jumada al-Thani moon not sighted in Pakistan; month to begin on November 12 - Daily Pakistan",
             "en.dailypakistan.com.pk"),
        ]:
            self.assertEqual("not_sighted", self.vote(title, publisher), title)

    def test_month_spellings(self):
        cases = {
            1: "Muharram moon not sighted in Pakistan, Ashura to fall on June 26",
            2: "Moon sighted in Pakistan; 1st Safar on July 16",
            3: "Rabiul Awwal moon not sighted in Pakistan",
            4: "Rabi-us-Sani moon not sighted in Pakistan",
            5: "Jumadi-ul-Awwal 1448 AH Moon Sighted in Pakistan",
            6: "Jamadi ul Sani moon not sighted in Pakistan",
            7: "Rajab moon sighted in Pakistan",
            8: "Sha'ban moon not sighted in Pakistan",
            9: "Ramazan moon sighted in Pakistan",
            10: "Shawwal moon not sighted in Pakistan",
            11: "Zil-Qad moon sighted in Pakistan",
            12: "Zilhaj moon sighted in Pakistan",
        }
        for month, title in cases.items():
            self.assertIsNotNone(self.vote(title, month=month), title)
        self.assertIsNotNone(self.vote("Dhul Qadah moon sighted in Pakistan", month=11))
        self.assertIsNotNone(self.vote("Dhul Hijjah moon sighted in Pakistan", month=12))
        self.assertIsNone(self.vote("Dhul Hijjah moon sighted in Pakistan", month=11))
        self.assertIsNone(self.vote("Jamadi ul Awwal moon sighted in Pakistan", month=6))
        self.assertIsNone(self.vote("Rabi ul Awwal moon sighted in Pakistan", month=4))

    def test_the_month_next_to_moon_wins(self):
        title = "Only four months left until Ramadan as the Jumada al-Awwal moon is sighted in Pakistan"
        self.assertIsNone(self.vote(title, month=9))
        self.assertEqual("sighted", self.vote(title, month=5))

    def test_forecasts_meetings_and_questions_are_ignored(self):
        for title in [
            "Jamadi-us-Sani moon likely to be sighted in Pakistan on Nov 10",
            "Jamadi-us-Sani moon expected to be sighted in Pakistan",
            "Ruet-e-Hilal Committee to meet for Jamadi-us-Sani moon sighting in Pakistan",
            "Will Jamadi-us-Sani moon be sighted in Pakistan tomorrow?",
            "PMD says Jamadi-us-Sani moon not sighted possibility in Pakistan",
            "Live: Jamadi-us-Sani moon sighted updates Pakistan",
            "Pakistan Announces Date for Start of Jamadi-us-Sani",
        ]:
            self.assertIsNone(self.vote(title, "geo.tv"), title)

    def test_other_countries_and_local_reports_are_ignored(self):
        for title in [
            "Jamadi-us-Sani moon sighted in Saudi Arabia, UAE",
            "Jamadi-us-Sani moon sighted in Pakistan, India, Bangladesh",
            "Jamadi-us-Sani moon sighted in Peshawar, Pakistan",
            "Zonal committee says Jamadi-us-Sani moon not sighted in Pakistan",
            "Unofficial committee: Jamadi-us-Sani moon sighted in Pakistan",
        ]:
            self.assertIsNone(self.vote(title, "geo.tv"), title)
        # A city-only report counts for nothing, even from a known publisher.
        self.assertIsNone(self.vote("Jamadi-us-Sani moon not sighted in Karachi", "geo.tv"))

    def test_unknown_publisher_must_name_pakistan(self):
        self.assertIsNone(self.vote("Jamadi-us-Sani moon not sighted", "blog.example.org"))
        self.assertEqual("not_sighted", self.vote("Jamadi-us-Sani moon not sighted", "www.dawn.com"))
        self.assertEqual("not_sighted", self.vote("Jamadi-us-Sani moon not sighted in Pakistan", "blog.example.org"))

    def test_headline_naming_the_other_day_is_dropped(self):
        self.assertIsNone(self.vote("Jamadi-us-Sani moon sighted in Pakistan, first day on Nov 12"))
        self.assertIsNone(self.vote("Jamadi-us-Sani moon not sighted in Pakistan, month begins November 11"))
        # 29 Ramadan 1447 was Thu 19 Mar 2026; a 'sighted' headline naming Saturday is about the 30th.
        self.assertIsNone(self.vote(
            "Eidul Fitr to be celebrated on Saturday as Shawwal moon sighted in Pakistan", "dunyanews.tv",
            month=10, yes=dt.date(2026, 3, 20), no=dt.date(2026, 3, 21)))
        # Other dates in the headline (Ashura) do not matter.
        self.assertEqual("not_sighted", self.vote(
            "Muharram moon not sighted in Pakistan, Ashura to fall on June 26", "dawn.com",
            month=1, yes=dt.date(2026, 6, 16), no=dt.date(2026, 6, 17)))

    def test_dated_flag(self):
        self.assertTrue(u.classify("Jamadi-us-Sani moon not sighted in Pakistan; First Day on Nov. 12",
                                   "https://dawn.com", 6, self.yes, self.no)[1])
        self.assertTrue(u.classify("Jamadi-us-Sani moon sighted in Pakistan, first day on Wednesday",
                                   "https://dawn.com", 6, self.yes, self.no)[1])
        self.assertFalse(u.classify("Jamadi-us-Sani moon sighted in Pakistan",
                                    "https://dawn.com", 6, self.yes, self.no)[1])


class Decide(unittest.TestCase):
    evening = dt.date(2026, 11, 10)
    NOT = "Jamadi-us-Sani moon not sighted in Pakistan"
    YES = "Jamadi-us-Sani moon sighted in Pakistan"

    def outcome(self, *items):
        return u.decide(list(items), 6, self.evening)[0]

    def test_two_publishers_agreeing(self):
        self.assertEqual("not_sighted", self.outcome(
            item(self.NOT, "dawn.com", "2026-11-10T19:00:00"), item(self.NOT, "geo.tv", "2026-11-10T19:30:00")))
        self.assertEqual("sighted", self.outcome(
            item(self.YES, "dawn.com", "2026-11-10T19:00:00"), item(self.YES, "geo.tv", "2026-11-10T19:30:00")))

    def test_one_publisher_is_not_enough(self):
        self.assertEqual("unknown", self.outcome(
            item(self.NOT, "dawn.com", "2026-11-10T19:00:00"), item(self.NOT, "www.dawn.com", "2026-11-10T20:00:00")))
        self.assertEqual("unknown", self.outcome())

    def test_needs_one_known_publisher(self):
        self.assertEqual("unknown", self.outcome(
            item(self.NOT, "a.example.org", "2026-11-10T19:00:00"), item(self.NOT, "b.example.org", "2026-11-10T19:30:00")))

    def test_disagreement_writes_nothing(self):
        self.assertEqual("conflict", self.outcome(
            item(self.NOT, "dawn.com", "2026-11-10T19:00:00"), item(self.NOT, "geo.tv", "2026-11-10T19:30:00"),
            item(self.YES, "samaa.tv", "2026-11-10T19:40:00")))

    def test_reports_before_the_evening_are_ignored(self):
        self.assertEqual("unknown", self.outcome(
            item(self.NOT, "dawn.com", "2026-11-10T15:00:00"), item(self.NOT, "geo.tv", "2026-11-09T19:30:00")))

    def test_late_not_sighted_reports_still_count(self):
        self.assertEqual("not_sighted", self.outcome(
            item(self.NOT, "dawn.com", "2026-11-11T09:00:00"), item(self.NOT, "geo.tv", "2026-11-12T10:00:00")))
        self.assertEqual("unknown", self.outcome(
            item(self.NOT, "dawn.com", "2026-11-14T09:00:00"), item(self.NOT, "geo.tv", "2026-11-14T10:00:00")))

    def test_next_day_sighted_needs_the_date(self):
        # Published the next evening, undated: could be about the 30th.
        self.assertEqual("unknown", self.outcome(
            item(self.YES, "dawn.com", "2026-11-11T19:00:00"), item(self.YES, "geo.tv", "2026-11-11T19:30:00")))
        dated = self.YES + ", first day on Nov 11"
        self.assertEqual("sighted", self.outcome(
            item(dated, "dawn.com", "2026-11-11T09:00:00"), item(dated, "geo.tv", "2026-11-11T10:00:00")))
        # Shortly after midnight is still the same announcement.
        self.assertEqual("sighted", self.outcome(
            item(self.YES, "dawn.com", "2026-11-11T00:30:00"), item(self.YES, "geo.tv", "2026-11-10T23:30:00")))

    def test_date_only_stamps_on_the_29th_count(self):
        # Google News shows older items as 07:00 or 08:00 UTC (12:00 or 13:00 PKT).
        self.assertEqual("not_sighted", self.outcome(
            item(self.NOT, "dawn.com", "2026-11-10T07:00:00+00:00"), item(self.NOT, "geo.tv", "2026-11-10T08:00:00+00:00")))
        self.assertEqual("unknown", self.outcome(
            item(self.YES, "dawn.com", "2026-11-11T07:00:00+00:00"), item(self.YES, "geo.tv", "2026-11-11T07:00:00+00:00")))


class Helpers(unittest.TestCase):
    def test_next_month(self):
        self.assertEqual((1448, 7), u.next_month(1448, 6))
        self.assertEqual((1449, 1), u.next_month(1448, 12))

    def test_dates_in(self):
        near = dt.date(2026, 12, 30)
        self.assertEqual({dt.date(2027, 1, 2)}, u.dates_in(u.normalise("first day on Jan. 2"), near))
        self.assertEqual({dt.date(2026, 11, 12)}, u.dates_in(u.normalise("12 November"), dt.date(2026, 11, 11)))
        self.assertEqual(set(), u.dates_in(u.normalise("no date here"), near))

    def test_host_of(self):
        self.assertEqual("dawn.com", u.host_of("https://www.dawn.com/news/1"))
        self.assertEqual("", u.host_of(""))


if __name__ == "__main__":
    unittest.main()
