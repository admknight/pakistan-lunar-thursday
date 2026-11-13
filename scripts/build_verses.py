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

# Short, self-contained verses. No rulings, nothing that needs its context.
REFERENCES = [
    "2:45", "2:152", "2:153", "2:186", "3:8", "3:139", "6:162", "10:62", "13:28", "14:7",
    "16:128", "17:80", "18:10", "21:107", "23:118", "29:69", "33:41", "33:56", "39:53", "40:60",
    "49:10", "49:13", "50:16", "55:13", "55:60", "87:14", "93:5", "94:5", "94:6", "99:7", "99:8",
]
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
