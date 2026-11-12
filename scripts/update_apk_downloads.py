#!/usr/bin/env python3
"""Sum GitHub release APK download_count across stable AND preview releases.

No tracking code, database, pageview counting, or unique-user estimates.
Uses only the official public GitHub REST API. Deleted releases cannot be counted.
"""
import argparse
import datetime
import html
import json
import os
from pathlib import Path
import urllib.error
import urllib.request

REPO = "admknight/pakistan-lunar-thursday"
API = f"https://api.github.com/repos/{REPO}/releases"
JSON_PATH = Path("docs/stats/apk-downloads.json")
SVG_PATH = Path("docs/stats/apk-downloads.svg")

def summarize(releases):
    total = stable = preview = asset_count = release_count = 0
    for release in releases:
        if release.get("draft"):
            continue
        release_count += 1
        for asset in release.get("assets", []):
            name = str(asset.get("name", ""))
            if not name.lower().endswith(".apk") or asset.get("state", "uploaded") != "uploaded":
                continue
            count = asset.get("download_count")
            if type(count) is not int or count < 0:
                raise ValueError(f"Invalid download_count on {name!r}: {count!r}")
            total += count
            asset_count += 1
            if release.get("prerelease"):
                preview += count
            else:
                stable += count
    return dict(apk_downloads=total, stable_downloads=stable,
                preview_downloads=preview, counted_apk_assets=asset_count,
                published_releases=release_count)

def fetch_releases():
    headers = {"Accept": "application/vnd.github+json",
               "X-GitHub-Api-Version": "2022-11-28",
               "User-Agent": "HilalThursday-APK-Counter"}
    token = os.getenv("GH_TOKEN") or os.getenv("GITHUB_TOKEN")
    if token:
        headers["Authorization"] = "Bearer " + token
    all_releases = []
    for page in range(1, 101):
        request = urllib.request.Request(f"{API}?per_page=100&page={page}", headers=headers)
        with urllib.request.urlopen(request, timeout=35) as response:
            chunk = json.load(response)
        if not isinstance(chunk, list):
            raise RuntimeError("GitHub Releases API did not return a list")
        all_releases.extend(chunk)
        if len(chunk) < 100:
            return all_releases
    raise RuntimeError("Exceeded 10,000 releases; refusing to undercount")

def svg_badge(total):
    number = f"{total:,}"
    left = 151
    right = max(48, len(number) * 10 + 24)
    width = left + right
    title = html.escape(
        f"{number} cumulative published GitHub APK downloads, including stable and preview releases; not unique installs"
    )
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="26" '
        f'viewBox="0 0 {width} 26" role="img" aria-label="APK downloads: {number}">'
        f'<title>{title}</title><defs><clipPath id="clip"><rect width="{width}" height="26" rx="6"/></clipPath></defs>'
        f'<g clip-path="url(#clip)"><rect width="{left}" height="26" fill="#103C43"/>'
        f'<rect x="{left}" width="{right}" height="26" fill="#168568"/></g>'
        f'<g font-family="DejaVu Sans,Verdana,sans-serif" font-size="12" '
        f'font-weight="600" text-anchor="middle" fill="#fff">'
        f'<text x="{left/2}" y="17">All APK downloads</text>'
        f'<text x="{left+right/2}" y="17">{number}</text></g></svg>\n'
    )

def run():
    summary = summarize(fetch_releases())
    payload = {
        "schema_version": 1, "repository": REPO, **summary,
        "metric": "Sum of download_count for .apk release assets on all currently published GitHub releases; includes stable and preview",
        "not_unique_users": True,
        "source_api": API,
    }
    if JSON_PATH.exists():
        previous = json.loads(JSON_PATH.read_text(encoding="utf-8"))
        unchanged = all(previous.get(k) == v for k, v in payload.items())
        if unchanged:
            print("Cumulative APK count unchanged:", summary["apk_downloads"])
            return
    payload["last_changed_utc"] = datetime.datetime.now(datetime.timezone.utc).replace(microsecond=0).isoformat()
    JSON_PATH.parent.mkdir(parents=True, exist_ok=True)
    JSON_PATH.write_text(json.dumps(payload, indent=2, ensure_ascii=False, sort_keys=True) + "\n", encoding="utf-8")
    SVG_PATH.write_text(svg_badge(summary["apk_downloads"]), encoding="utf-8")
    print("Cumulative APK download count:", summary)

def self_test():
    fixture = [
        {"prerelease": False, "assets": [
            {"name": "HilalThursday-v1.8.apk", "download_count": 6, "state": "uploaded"},
            {"name": "HilalThursday-v1.8.apk.sha256", "download_count": 80, "state": "uploaded"},
            {"name": "source.zip", "download_count": 40, "state": "uploaded"}]},
        {"prerelease": True, "assets": [{"name": "app-debug.APK", "download_count": 3, "state": "uploaded"}]},
        {"draft": True, "assets": [{"name": "draft.apk", "download_count": 90}]}
    ]
    assert summarize(fixture) == {"apk_downloads": 9, "stable_downloads": 6,
        "preview_downloads": 3, "counted_apk_assets": 2, "published_releases": 2}
    assert ">9</text>" in svg_badge(9)
    try:
        summarize([{"assets": [{"name": "bad.apk", "download_count": -1}]}])
    except ValueError:
        pass
    else:
        raise AssertionError("Bad counters must fail closed")
    print("APK-only cumulative counter self-test PASS")

if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("--self-test", action="store_true")
    args = p.parse_args()
    self_test() if args.self_test else run()
