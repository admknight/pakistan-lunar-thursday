#!/usr/bin/env python3
"""Build app/src/main/assets/verses.json from Tanzil downloads.

The verse text is never typed or edited here: each line is copied verbatim
from the Tanzil files, which must be downloaded first (see --help). Only the
short list of references below is project-chosen.

  Quran text:   https://tanzil.net/download/  (Uthmani, text with aya numbers)
  Translations: https://tanzil.net/trans/     (ur.jalandhry, ur.najafi, ur.jawadi)
"""
import argparse
import hashlib
import json
import os

# Well-known verses that read as a complete thought: supplications, reminders and
# statements of mercy, patience, gratitude and good conduct. No legal rulings.
# A verse whose Arabic or any translation is too long for the card is left out
# when the file is built (see MAX_ARABIC and MAX_TRANSLATION).
REFERENCES = [
    "1:2", "1:5", "1:6", "2:43", "2:45", "2:110", "2:115", "2:127", "2:128", "2:152",
    "2:153", "2:155", "2:156", "2:163", "2:186", "2:195", "2:201", "2:238", "2:245", "2:250",
    "2:255", "2:261", "2:263", "2:269", "2:274", "2:277", "2:285", "2:286", "3:8", "3:9",
    "3:16", "3:18", "3:26", "3:27", "3:31", "3:53", "3:92", "3:102", "3:103", "3:104",
    "3:133", "3:134", "3:139", "3:147", "3:159", "3:160", "3:173", "3:185", "3:190", "3:191",
    "3:192", "3:193", "3:194", "4:58", "4:86", "4:110", "5:8", "5:35", "5:83", "6:17",
    "6:54", "6:59", "6:160", "6:162", "7:23", "7:31", "7:55", "7:56", "7:180", "7:199",
    "7:204", "7:205", "8:2", "8:46", "9:51", "9:71", "9:119", "9:128", "9:129", "10:57",
    "10:62", "10:85", "10:107", "11:6", "11:114", "11:115", "12:86", "12:87", "12:101", "13:11",
    "13:28", "13:29", "14:7", "14:34", "14:38", "14:40", "14:41", "15:9", "15:49", "15:98",
    "15:99", "16:18", "16:53", "16:90", "16:96", "16:97", "16:125", "16:127", "16:128", "17:9",
    "17:23", "17:24", "17:36", "17:37", "17:53", "17:70", "17:80", "17:81", "17:82", "18:10",
    "18:46", "18:109", "18:110", "19:96", "20:8", "20:14", "20:25", "20:26", "20:114", "20:132",
    "21:35", "21:83", "21:87", "21:88", "21:89", "21:107", "22:77", "23:1", "23:29", "23:97",
    "23:109", "23:115", "23:116", "23:118", "24:35", "25:58", "25:63", "25:65", "25:74", "26:80",
    "26:83", "26:84", "26:85", "27:19", "27:62", "28:16", "28:24", "28:77", "28:88", "29:2",
    "29:45", "29:57", "29:69", "30:21", "30:60", "31:17", "31:18", "31:19", "32:16", "33:21",
    "33:41", "33:42", "33:43", "33:56", "33:70", "33:71", "35:2", "35:15", "36:82", "37:180",
    "37:181", "37:182", "39:10", "39:53", "40:44", "40:60", "41:30", "41:33", "41:34", "42:19",
    "42:25", "42:36", "42:43", "46:13", "46:15", "47:7", "49:10", "49:12", "49:13", "50:16",
    "51:55", "51:56", "51:58", "53:39", "55:13", "55:26", "55:27", "55:60", "57:3", "57:4",
    "57:21", "58:11", "59:10", "59:18", "59:21", "59:22", "59:23", "59:24", "60:4", "60:5",
    "63:9", "64:11", "64:13", "64:16", "66:8", "67:1", "67:2", "67:13", "67:14", "68:4",
    "70:5", "71:10", "71:28", "73:8", "73:9", "76:8", "76:9", "87:14", "87:16", "87:17",
    "89:27", "89:28", "91:9", "91:10", "93:4", "93:5", "93:9", "93:10", "93:11", "94:5",
    "94:6", "94:7", "94:8", "95:4", "97:1", "97:3", "99:7", "99:8", "108:1", "110:3",
]
MAX_ARABIC = 430
MAX_TRANSLATION = 560
TRANSLATIONS = {
    "jalandhry": ("ur.jalandhry.txt", "Fateh Muhammad Jalandhry"),
    "najafi": ("ur.najafi.txt", "Muhammad Hussain Najafi"),
    "jawadi": ("ur.jawadi.txt", "Syed Zeeshan Haider Jawadi"),
}


def load(path):
    lines = {}
    with open(path, encoding="utf-8") as handle:
        for raw in handle:
            line = raw.rstrip("\n").rstrip("\r")
            if not line or line.startswith("#"):
                continue
            surah, ayah, text = line.split("|", 2)
            lines["%s:%s" % (surah, ayah)] = text
    return lines


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("folder", help="folder holding quran.txt and the three ur.*.txt files from Tanzil")
    args = parser.parse_args()
    quran = load(os.path.join(args.folder, "quran.txt"))
    translations = {key: load(os.path.join(args.folder, name)) for key, (name, _) in TRANSLATIONS.items()}
    verses = []
    for reference in REFERENCES:
        surah, ayah = (int(part) for part in reference.split(":"))
        longest = max(len(translations[key][reference]) for key in TRANSLATIONS)
        if len(quran[reference]) > MAX_ARABIC or longest > MAX_TRANSLATION:
            print("left out as too long for the card: %s" % reference)
            continue
        verses.append({
            "surah": surah, "ayah": ayah, "arabic": quran[reference],
            "ur": {key: translations[key][reference] for key in TRANSLATIONS},
        })
    document = {
        "schema": 1,
        "notice": ("Quran text: Tanzil Quran Text (Uthmani, Version 1.1), Copyright (C) 2007-2026 Tanzil Project, "
                   "License: Creative Commons Attribution 3.0, copied verbatim; changing it is not allowed. "
                   "Source and updates: https://tanzil.net/ . Urdu translations from https://tanzil.net/trans/ , "
                   "provided there for non-commercial purposes only; any other use needs the translator's or "
                   "publisher's permission. These texts are not covered by this project's MIT license."),
        "translations": {key: name for key, (_, name) in TRANSLATIONS.items()},
        # Fingerprint of every text line, so an accidental edit of this file is caught by the tests.
        "text_sha256": hashlib.sha256("\n".join(
            [v["arabic"] for v in verses] + [v["ur"][key] for v in verses for key in TRANSLATIONS]
        ).encode("utf-8")).hexdigest(),
        "verses": verses,
    }
    out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets", "verses.json")
    with open(out, "w", encoding="utf-8") as handle:
        json.dump(document, handle, indent=2, ensure_ascii=False)
        handle.write("\n")
    print("wrote %d verses" % len(verses))


if __name__ == "__main__":
    main()
