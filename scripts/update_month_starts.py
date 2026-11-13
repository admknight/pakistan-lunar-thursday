#!/usr/bin/env python3
"""Fill data/month-starts.json from news reports of the Central Ruet-e-Hilal
Committee's announcement.

A Hijri month is 29 or 30 days, so after a confirmed start S the next month
begins on S+29 (moon sighted on the evening of the 29th) or S+30 (not sighted).
This script only has to answer that yes/no question. It reads Google News
headlines, keeps those that clearly report Pakistan's decision for the right
month, and writes an entry only when at least two different publishers agree
and none disagrees. Otherwise it writes nothing and the app keeps its forecast.

This is a project-maintained record, not an official government source.
"""
import argparse
import datetime as dt
import json
import os
import re
import sys
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from email.utils import parsedate_to_datetime

PKT = dt.timezone(dt.timedelta(hours=5))
DATA_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "data", "month-starts.json")
MIN_PUBLISHERS = 2
# The decision is announced after sunset on the 29th; reports follow within hours.
WINDOW_OPENS = dt.time(16, 30)
WINDOW_DAYS = 3
GIVE_UP_DAYS = 35

MONTHS = {
    1: ("Muharram", r"\bmuharram\b|\bmoharram\b", ["Muharram"]),
    2: ("Safar", r"\bsafar\b", ["Safar"]),
    3: ("Rabi al-Awwal", r"\brabi\w*\s+(?:ul\s+|al\s+)?aww?al\b", ["Rabi ul Awwal", "Rabi al-Awwal"]),
    4: ("Rabi al-Thani", r"\brabi\w*\s+(?:ul\s+|us\s+|al\s+|uth\s+)?(?:sani|saani|thani|akhir\w*)\b",
        ["Rabi us Sani", "Rabi al-Thani"]),
    5: ("Jumada al-Awwal", r"\bj[au]mad[ai]\w*\s+(?:ul\s+|al\s+)?(?:aww?al|ula|oola)\b",
        ["Jamadi ul Awwal", "Jumada al-Awwal"]),
    6: ("Jumada al-Thani", r"\bj[au]mad[ai]\w*\s+(?:ul\s+|us\s+|al\s+|uth\s+)?(?:sani|saani|thani|akhir\w*)\b",
        ["Jamadi us Sani", "Jumada al-Thani"]),
    7: ("Rajab", r"\brajab\b", ["Rajab"]),
    8: ("Sha'ban", r"\bsha\s?a?baa?n\b", ["Shaban", "Sha'ban"]),
    9: ("Ramadan", r"\bramadh?an\b|\bramazan\b|\bramzan\b", ["Ramazan", "Ramadan"]),
    10: ("Shawwal", r"\bshaww?al\b|\beid\s?ul\s?fitr\b|\beid\s+al\s+fitr\b", ["Shawwal", "Eid ul Fitr"]),
    11: ("Dhu al-Qa'dah", r"\b(?:zil|zul|zi|zee|dhul|dhu\s+al|dhu\s+l)\s*q[aiu]{1,3}\s?d\w*\b",
         ["Zilqad", "Dhul Qadah"]),
    12: ("Dhu al-Hijjah", r"\b(?:zil|zul|zi|dhul|dhu\s+al|dhu\s+l)\s*h[aiu]j\w*\b|\beid\s?ul\s?a[dz]ha\b",
         ["Zilhaj", "Dhul Hijjah"]),
}

NOT_SIGHTED = re.compile(r"\bnot\s+(?:been\s+)?(?:sighted|seen|visible)\b|\bno\s+sighting\b|\bunsighted\b")
SIGHTED = re.compile(r"\bsighted\b|\bseen\b")
# Forecasts, meeting notices and questions are not announcements.
NOT_AN_ANNOUNCEMENT = re.compile(
    r"\b(?:likely|unlikely|expected|expects?|predict\w*|forecast\w*|may|might|could|chances?|probab\w*|"
    r"possib\w*|to\s+meet|meets?|meeting|underway|convene\w*|when\s+is|will\s+be\s+sighted|to\s+be\s+sighted|"
    r"suparco|pmd|met\s+office|astronom\w*|research\s+council|updates?|live)\b")
ELSEWHERE = re.compile(
    r"\b(?:saudi|ksa|uae|emirates|gulf|india|indian|bangladesh|indonesia|malaysia|brunei|turkey|turkiye|"
    r"iran|iraq|oman|qatar|kuwait|bahrain|egypt|morocco|uk|britain|usa|america|canada|australia|"
    r"afghanistan|japan|singapore|sri\s+lanka|new\s+zealand|nepal|maldives|arab|"
    r"peshawar|khyber|kp|popalzai|qasim|unofficial|zonal)\b")
# A single city's report is not the national decision.
ONE_CITY = re.compile(r"\b(?:lahore|karachi|quetta|islamabad|rawalpindi|multan|hyderabad|faisalabad|gilgit)\b")
MOON_WORD = re.compile(r"\b(?:moon|crescent|chand|hilal)\b")
WEEKDAYS = ["monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"]
PAKISTANI_PUBLISHERS = {
    "dawn.com", "tribune.com.pk", "geo.tv", "arynews.tv", "dunyanews.tv", "app.com.pk", "pakobserver.net",
    "thenews.com.pk", "samaa.tv", "nation.com.pk", "brecorder.com", "radio.gov.pk", "dailytimes.com.pk",
    "propakistani.pk", "bolnews.com", "24newshd.tv", "en.dailypakistan.com.pk", "pakistantoday.com.pk",
    "aaj.tv", "dailyausaf.com", "bloompakistan.com", "humnews.pk", "samaaenglish.tv", "thefridaytimes.com",
    "express.pk", "92newshd.tv", "abbtakk.tv", "ptv.com.pk", "thenews.pk",
}
GREGORIAN = {m: i + 1 for i, names in enumerate([
    ("jan", "january"), ("feb", "february"), ("mar", "march"), ("apr", "april"), ("may",),
    ("jun", "june"), ("jul", "july"), ("aug", "august"), ("sep", "sept", "september"),
    ("oct", "october"), ("nov", "november"), ("dec", "december")]) for m in names}
_MONTH_WORD = "|".join(sorted(GREGORIAN, key=len, reverse=True))
DATE_A = re.compile(r"\b(%s)\s+(\d{1,2})\b" % _MONTH_WORD)
DATE_B = re.compile(r"\b(\d{1,2})\s+(%s)\b" % _MONTH_WORD)


def normalise(text):
    return re.sub(r"\s+", " ", re.sub(r"[^a-z0-9]+", " ", text.lower())).strip()


def host_of(url):
    host = (urllib.parse.urlparse(url or "").hostname or "").lower()
    return host[4:] if host.startswith("www.") else host


def dates_in(text, near):
    """Day-and-month mentions in a headline, resolved to the year closest to `near`."""
    found = set()
    pairs = [(GREGORIAN[m], int(d)) for m, d in DATE_A.findall(text)]
    pairs += [(GREGORIAN[m], int(d)) for d, m in DATE_B.findall(text)]
    for month, day in pairs:
        best = None
        for year in (near.year - 1, near.year, near.year + 1):
            try:
                candidate = dt.date(year, month, day)
            except ValueError:
                continue
            if best is None or abs((candidate - near).days) < abs((best - near).days):
                best = candidate
        if best:
            found.add(best)
    return found


def month_named_with_moon(text):
    """The Hijri month a headline's moon report is about (the name nearest before 'moon')."""
    moon = MOON_WORD.search(text)
    best, best_gap = None, None
    for number, (_, pattern, _) in MONTHS.items():
        for match in re.finditer(pattern, text):
            gap = abs((moon.start() if moon else len(text)) - match.end())
            if best_gap is None or gap < best_gap:
                best, best_gap = number, gap
    return best


def classify(title, publisher_url, hijri_month, if_sighted, if_not_sighted):
    """Return (vote, dated) for one headline, or (None, False).

    vote is 'sighted' or 'not_sighted'. dated is True when the headline itself
    names the resulting first day by date or weekday.
    """
    if "?" in title:
        return None, False
    text = normalise(title.rsplit(" - ", 1)[0])
    if month_named_with_moon(text) != hijri_month:
        return None, False
    if NOT_AN_ANNOUNCEMENT.search(text) or ELSEWHERE.search(text):
        return None, False
    publisher = host_of(publisher_url)
    names_pakistan = "pakistan" in text
    if not names_pakistan and (publisher not in PAKISTANI_PUBLISHERS or ONE_CITY.search(text)):
        return None, False
    if NOT_SIGHTED.search(text):
        vote, own, other = "not_sighted", if_not_sighted, if_sighted
    elif SIGHTED.search(text):
        vote, own, other = "sighted", if_sighted, if_not_sighted
    else:
        return None, False
    dates = dates_in(text, if_sighted)
    days = {name for name in WEEKDAYS if re.search(r"\b%s\b" % name, text)}
    own_day, other_day = WEEKDAYS[own.weekday()], WEEKDAYS[other.weekday()]
    # A headline that names the other candidate day contradicts its own verdict.
    if other in dates or (other_day in days and own_day not in days):
        return None, False
    return vote, (own in dates or own_day in days)


def is_date_only(published):
    """Google News gives older items a bare date: midnight US Pacific, 07:00 or 08:00 UTC."""
    utc = published.astimezone(dt.timezone.utc)
    return (utc.minute, utc.second) == (0, 0) and utc.hour in (7, 8)


def in_time(vote, dated, published, evening_of_29th):
    """Was this headline published when it can only refer to the evening of the 29th?"""
    local = published.astimezone(PKT)
    opens = dt.datetime.combine(evening_of_29th, WINDOW_OPENS, PKT)
    if local.date() == evening_of_29th:
        return is_date_only(published) or local >= opens
    if local < opens or local > opens + dt.timedelta(days=WINDOW_DAYS):
        return False
    if vote == "not_sighted":
        return True
    # "Sighted" the next day could mean the evening of the 30th, when the moon is
    # always visible. Accept it only before dawn, or when it names the first day.
    before_dawn = (not is_date_only(published)
                   and local < dt.datetime.combine(evening_of_29th + dt.timedelta(days=1), dt.time(4, 0), PKT))
    return before_dawn or dated


def decide(items, hijri_month, evening_of_29th):
    """items: dicts with title, publisher_url, published (aware datetime).

    Returns (outcome, evidence): outcome is 'sighted', 'not_sighted',
    'conflict' or 'unknown'.
    """
    if_sighted = evening_of_29th + dt.timedelta(days=1)
    if_not_sighted = evening_of_29th + dt.timedelta(days=2)
    votes = {"sighted": {}, "not_sighted": {}}
    for item in items:
        published = item.get("published")
        publisher = host_of(item.get("publisher_url"))
        if published is None or not publisher:
            continue
        vote, dated = classify(item["title"], item.get("publisher_url"), hijri_month, if_sighted, if_not_sighted)
        if vote and in_time(vote, dated, published, evening_of_29th):
            votes[vote].setdefault(publisher, item)
    yes, no = votes["sighted"], votes["not_sighted"]
    if yes and no:
        return "conflict", list(yes.values()) + list(no.values())
    for outcome, group in (("sighted", yes), ("not_sighted", no)):
        known = [publisher for publisher in group if publisher in PAKISTANI_PUBLISHERS]
        if len(group) >= MIN_PUBLISHERS and known:
            return outcome, list(group.values())
    return "unknown", list(yes.values()) + list(no.values())


def fetch_headlines(hijri_month, evening_of_29th):
    items, seen = [], set()
    # Bound the search to the days around the decision so older and later
    # months do not crowd out the reports that matter.
    span = "after:%s before:%s" % ((evening_of_29th - dt.timedelta(days=1)).isoformat(),
                                   (evening_of_29th + dt.timedelta(days=WINDOW_DAYS + 2)).isoformat())
    queries = ["Ruet-e-Hilal moon Pakistan " + span, "moon sighted Pakistan " + span]
    for name in MONTHS[hijri_month][2]:
        queries.append("%s moon sighted Pakistan %s" % (name, span))
        queries.append("%s moon not sighted Pakistan %s" % (name, span))
    for query in queries:
        url = ("https://news.google.com/rss/search?q=%s&hl=en-PK&gl=PK&ceid=PK:en"
               % urllib.parse.quote(query))
        try:
            request = urllib.request.Request(url, headers={"User-Agent": "HilalThursday-corrections/1.0"})
            with urllib.request.urlopen(request, timeout=25) as response:
                root = ET.fromstring(response.read(2_000_000))
        except Exception as error:  # one failed query must not stop the others
            print("query failed: %s (%s)" % (query, error), file=sys.stderr)
            continue
        for node in root.iter("item"):
            title = (node.findtext("title") or "").strip()
            source = node.find("source")
            publisher_url = source.get("url") if source is not None else ""
            try:
                published = parsedate_to_datetime(node.findtext("pubDate") or "")
            except (TypeError, ValueError):
                continue
            if published.tzinfo is None:
                published = published.replace(tzinfo=dt.timezone.utc)
            key = (host_of(publisher_url), title)
            if title and key not in seen:
                seen.add(key)
                items.append({"title": title, "publisher_url": publisher_url, "published": published})
    return items


def next_month(year, month):
    return (year + 1, 1) if month == 12 else (year, month + 1)


def latest_entry(document):
    rows = document.get("months", [])
    if not rows:
        return None
    return max(rows, key=lambda row: row["first_day"])


def write_output(**values):
    path = os.environ.get("GITHUB_OUTPUT")
    lines = ["%s=%s" % (k, v) for k, v in values.items()]
    if path:
        with open(path, "a", encoding="utf-8") as handle:
            handle.write("\n".join(lines) + "\n")
    print(" ".join(lines))


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true", help="report the result, change nothing")
    parser.add_argument("--now", help="ISO time to act as (testing)")
    parser.add_argument("--anchor", help="backtest from this entry, e.g. 1448-5=2026-10-13")
    parser.add_argument("--force", action="store_true", help="search even outside the usual hours")
    args = parser.parse_args(argv)

    now = dt.datetime.fromisoformat(args.now) if args.now else dt.datetime.now(PKT)
    if now.tzinfo is None:
        now = now.replace(tzinfo=PKT)
    now = now.astimezone(PKT)

    with open(DATA_FILE, encoding="utf-8") as handle:
        document = json.load(handle)
    if args.anchor:
        key, first_day = args.anchor.split("=")
        year, month = (int(part) for part in key.split("-"))
        anchor = {"hijri_year": year, "hijri_month": month, "first_day": first_day}
    else:
        anchor = latest_entry(document)
    if anchor is None:
        write_output(changed="false", attention="true", summary="No confirmed month to start from")
        return 0

    start = dt.date.fromisoformat(anchor["first_day"])
    evening = start + dt.timedelta(days=28)
    year, month = next_month(anchor["hijri_year"], anchor["hijri_month"])
    label = "%s %d" % (MONTHS[month][0], year)
    opens = dt.datetime.combine(evening, WINDOW_OPENS, PKT)

    if now < opens:
        write_output(changed="false", attention="false",
                     summary="%s: decision not due until %s" % (label, opens.isoformat()))
        return 0
    overdue = now >= dt.datetime.combine(evening + dt.timedelta(days=1), dt.time(6, 0), PKT)
    if now > opens + dt.timedelta(days=GIVE_UP_DAYS):
        write_output(changed="false", attention="true",
                     summary="%s: no confirmation found for over %d days; edit the file by hand" % (label, GIVE_UP_DAYS))
        return 0
    # GitHub starts only a fraction of scheduled runs, so every run that does
    # start searches until the month is confirmed or the give-up limit passes.
    outcome, evidence = decide(fetch_headlines(month, evening), month, evening)
    for item in evidence:
        print("  evidence: %s | %s | %s" % (item["published"].astimezone(PKT).isoformat(),
                                             host_of(item["publisher_url"]), item["title"]))
    if outcome not in ("sighted", "not_sighted"):
        write_output(changed="false", attention=str(overdue).lower(),
                     summary="%s: %s (%d usable headlines)" % (label, outcome, len(evidence)))
        return 0

    first_day = evening + dt.timedelta(days=1 if outcome == "sighted" else 2)
    entry = {
        "hijri_year": year,
        "hijri_month": month,
        "first_day": first_day.isoformat(),
        "sighted_on_29th": outcome == "sighted",
        "sources": sorted({"https://%s/" % host_of(item["publisher_url"]) for item in evidence}),
        "evidence": [{"publisher": host_of(item["publisher_url"]), "title": item["title"],
                      "published": item["published"].astimezone(PKT).isoformat()} for item in evidence],
    }
    summary = "%s starts %s (moon %s on %s; %d publishers)" % (
        label, first_day.isoformat(), "sighted" if outcome == "sighted" else "not sighted",
        evening.isoformat(), len(entry["sources"]))
    if args.dry_run or args.anchor:
        print(json.dumps(entry, indent=2, ensure_ascii=False))
        write_output(changed="false", attention="false", summary="DRY RUN: " + summary)
        return 0
    document["months"].append(entry)
    with open(DATA_FILE, "w", encoding="utf-8") as handle:
        json.dump(document, handle, indent=2, ensure_ascii=False)
        handle.write("\n")
    write_output(changed="true", attention="false", summary=summary)
    return 0


if __name__ == "__main__":
    sys.exit(main())
