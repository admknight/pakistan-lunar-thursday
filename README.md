# Hilal Thursday — ہلال جمعرات

**Hilal Thursday**, formerly *Lunar Thursday*, is an open-source, bilingual English/Urdu Android reminder for the **first Thursday on or after the first civil day of every new Islamic month in Pakistan**, at **11:00 AM Pakistan Standard Time (Asia/Karachi)**.

![Build Android APK](https://github.com/admknight/pakistan-lunar-thursday/actions/workflows/build-apk.yml/badge.svg)

## Download / install

Open **[Releases](https://github.com/admknight/pakistan-lunar-thursday/releases)** to download publicly available APKs. Preview releases contain `app-debug.apk` (temporary debug signing). The [permanent signing setup](docs/SIGNING.md) is operational (first stable release: v1.4.0). The [Publish permanently signed APK workflow](https://github.com/admknight/pakistan-lunar-thursday/actions/workflows/publish-stable.yml) publishes a **normal GitHub Release** with `HilalThursday-vX.Y.Z.apk` and its SHA-256 checksum. For diagnostics, [Actions → Build Android APK](https://github.com/admknight/pakistan-lunar-thursday/actions/workflows/build-apk.yml) retains temporary artifacts.

The GitHub Actions build produces a **debug-signed test APK**, not a production-signed stable APK. Each cloud runner may use a different debug signing key. Therefore, installing a newer build can require uninstalling the old one first. Do not treat this as a stable release-signing solution. Android 13+ requires the one-time **Notifications** permission; Android 12+ may also require **Alarms & reminders** permission for closer-to-exact delivery. No monthly date entry is necessary.

## Features

- A one-time automatically scheduled reminder at **11 AM Pakistan time** on the first Thursday of each new Islamic month.
- Automated date checks using HTTPS and Android `JobScheduler`, nominally every six hours and subject to Android background execution limits.
- Cache of dates the third-party provider explicitly labels as confirmed, never treating its estimates as confirmations.
- PMD published **2026–2027 forecast dates** used as fallback, then a clearly labeled *calculated approximation* for other years.
- Rescheduling on device reboot, clock or timezone changes, app update, and exact-alarm permission changes.
- Material 3 interface, light/dark theme, **Material You dynamic wallpaper color** on Android 12+.
- **English / اردو bilingual interface**, with a Settings language selector (device language / English / Urdu), Urdu calendar month labels, translated reminder text, and right-to-left layout. Android 13+ users can also change the app language in system settings.
- **Rebranded Hilal Thursday icon:** teal-and-gold crescent with a highlighted calendar day; adaptive icon for shaped launchers plus monochrome themed-icon variant on supported Android devices.
- **Optional ringing alarm** in Settings: repeats the system alarm sound/vibration for up to two minutes; **Dismiss** and **Snooze 10 minutes** controls; a missed-alert notification remains after timeout. The original notification-only mode stays the default.
- **Settings page** with alarm preferences, exact-alarm/full-screen permission guidance, and automatic updates that require no manual lunar-date adjustments.
- **In-app update manager:** Checks the project's GitHub Releases API when the app opens, with optional daily background update notifications. Download progress and SHA-256 digest, Android package-ID, version, and signing-certificate verification all occur inside Hilal Thursday; **no browser or GitHub page is opened to update**. Android still controls and confirms APK installation, and may first request approval to install from this app. Installation is never silent. Stable builds exclude incompatible debug-signed previews.
- **Interactive home dashboard** with live countdown in Pakistan time, tap-to-explore upcoming month cards, a reminder status / alert-mode overview, and one-tap date refresh requests (queued through Android JobScheduler).
- **About / Developer page** for `admknight`, open-source attribution, privacy, calendar-source transparency, and small hidden easter eggs.
- Next reminder, upcoming months, source quality, last successful feed check, and a test-notification button.
- **No accounts, no ads, no tracking, and no manually entered lunar dates.** Optional location permission is used **only after the user enables weather visuals**; the app never requests background or precise location.

## Ringing alarm and updates

Open **Settings** to enable **Ring audible alarm** if a normal notification is easy to miss. You can toggle sound and vibration independently and test the alarm from Settings. Alarm sound/vibration stop automatically after two minutes even if you don't dismiss; Snooze schedules another attempt ten minutes later. Android notification, exact-alarm, foreground service, and (on some devices) full-screen intent permissions affect availability and timing. Do Not Disturb, battery restrictions, device power-off, and OEM behavior can suppress or delay ringing. This path has been built in CI, **not tested on a physical phone**.

The **update manager** checks the public GitHub Releases API when the app opens, at most every twelve hours after a successful check. An optional daily Android JobScheduler task can notify you about newer signed stable releases while the app is closed; Android may defer the check for battery/network reasons. A selected update is downloaded **inside the app**, with progress, maximum-size checks, GitHub release-asset SHA-256 digest verification, package identity, version upgrade, and matching installed APK signer verification. It then invokes Android's built-in installer to obtain explicit user approval. Android 8+ may require a one-time **Allow from this source** system permission. No GitHub browser or web page is involved in updating; no APK is silently installed. Debug preview builds cannot replace a permanently signed stable APK.

### Preview builds versus permanently signed public releases

The automatic CI workflow generates a **debug signing key per runner/build**, so its builds are correctly tagged **preview**. **GitHub pre-releases are still public** and provide direct APK downloads. A long-lived release key and a separate certificate-verified release workflow have now been prepared. The maintainer must add two encrypted GitHub Actions secrets once before that workflow can publish a normally labeled public release. See [Private signing setup](docs/SIGNING.md); **the private signing material is never part of this public repository**. First-time migration from any debug-signed APK to the permanently signed APK requires uninstalling the debug APK. After that, updates using the same signer and a higher version code can install in place. App signing is not a substitute for actual device testing. The stable build excludes incompatible debug-signed preview updates.


## Dashboard and navigation (v1.6.0)

The main screen now has a compact app header with a three-dot **More options** menu, a decorative crescent-and-mosque night-sky card, three live countdown tiles (days/hours/minutes), a small reminder-status row and the next upcoming months. A bottom navigation bar switches between **Home**, **Calendar** (upcoming Hijri months), **Updates** (release/date checks), and **About**. The design is built with native Android views and Material 3 dynamic colors; the previous promotional concept was not a screenshot and is not claimed to be reproduced pixel-for-pixel.

The **About / Developer** screen uses concise icon-and-text rows for privacy, calendar accuracy, recent releases, project feedback and the MIT license rather than full-width GitHub buttons. Three optional hidden interactions remain: tap the home logo/title seven times, long-press the About icon, and tap the About version area five times. None of the easter eggs changes alarms, dates, or update settings.

Android accessibility labels, English/Urdu string resources, and right-to-left layout behavior are retained. The reminders still fire at **11:00 AM Pakistan time**; month start accuracy remains subject to the independent data-source limitations below. The graphical layout has not yet been visually exercised on a physical Android device.

## Living sky and optional local weather (v1.7.0)

The home illustration reacts to the device's current time of day (morning, daylight, sunset, night), **even offline and without location access**. In **Settings → Sky and local weather**, opt in to weather effects. Android will ask for **approximate location** while the app is in use. Hilal Thursday retrieves a one-time coarse location, rounds it to two decimal places, and retrieves current temperature, WMO weather code, and daylight status from Open-Meteo. Clouds, rain, fog, snow, and storms change only the decorative scene; **weather data is not a hazard alert or official lunar-sighting source**. No continuous GPS polling or background location access is used. Cached conditions expire after three hours, and weather refreshes are throttled to about 45 minutes. When permission, location, connectivity, or weather data is unavailable, the app falls back to the time-of-day sky.

Weather data attribution: [Open-Meteo](https://open-meteo.com/) · [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). Open-Meteo's free hosted endpoint is for non-commercial use; maintainers must review licensing before any commercialization.

## Bilingual app language and icon

Go to **Settings → App language** to choose **Follow device language**, **English**, or **اردو**. The language selection is retained locally on Android 8–12; on Android 13+ it uses Android's native per-app language system. Islamic month labels and on-screen dates follow the selected interface language. The app's reminder timing stays fixed at **11:00 AM Pakistan Standard Time**, regardless of the phone's locale or time zone.

The updated app icon uses native Android vector foreground/background layers and a separate monochrome layer for Material You themed icons. The published package name `com.scopewise.lunarthursday` and GitHub repository URL are intentionally unchanged to preserve upgrade compatibility, app data, and existing links. Branding is not a claim of government association or official sighting confirmation.

## Important accuracy limitations

A third-party data feed at `https://islamicdatetoday.pk/data/today.json` is used for estimated and explicitly reported-confirmed dates; it is **not** an official government announcement API. The authoritative moon-sighting decision in Pakistan is made by the Central Ruet-e-Hilal Committee. The feed previously returned a value of `pakistan_ruet_e_hilal_estimated`, which the app correctly does **not** treat as confirmation. There is no verified official machine-readable announcement API integrated into this project.

**Automatic operation does not mean guaranteed official accuracy.** If the feed is late, wrong, offline, or never reports a confirmation, the reminder will use a forecast/estimate; this may move the alert to the wrong Thursday. Android power management can defer a background refresh or notification, especially with permission restrictions, a switched-off device or force-stop. The UI always labels the calendar basis.

## Toolchain

- **Android Gradle Plugin:** 9.4.1 (stable)
- **Gradle:** 9.6.0
- **Material Components for Android:** 1.14.0 (Material 3)
- **Java:** 17
- **compileSdk:** 36; **targetSdk:** 35; **minSdk:** 26
- **Application ID:** `com.scopewise.lunarthursday`

The GitHub workflow installs the matching Android SDK platform/build tools, runs 180 pure-Java scheduling/feed-policy/version/weather-code assertions, builds the APK and attaches its SHA-256 hash. It runs on pushes to `main`, pull requests and manual dispatch. Successful pushes to `main` also publish a **versioned GitHub pre-release** if that version has not already been published. Increment `versionName` when publishing a new preview; rebuilding the same version does not silently replace public assets. This project deliberately uses the stable Java Material Components SDK rather than introducing Kotlin/Compose into an already tested Java-only notification engine.

## Local build

Open the repository in recent Android Studio with JDK 17, or use Gradle 9.6.0 with Android SDK platform 36:

```bash
gradle --no-daemon :app:assembleDebug
```

The debug build appears at `app/build/outputs/apk/debug/app-debug.apk`.

Portable scheduling tests (no Android SDK required):

```bash
mkdir -p build/portable-tests
javac -d build/portable-tests \
  app/src/main/java/com/scopewise/lunarthursday/LunarEngine.java \
  app/src/main/java/com/scopewise/lunarthursday/FeedDatePolicy.java \
  app/src/main/java/com/scopewise/lunarthursday/VersionPolicy.java \
  test/java/com/scopewise/lunarthursday/EngineTest.java \
  test/java/com/scopewise/lunarthursday/FeedDatePolicyTest.java \
  test/java/com/scopewise/lunarthursday/VersionPolicyTest.java
java -cp build/portable-tests com.scopewise.lunarthursday.EngineTest
java -cp build/portable-tests com.scopewise.lunarthursday.FeedDatePolicyTest
java -cp build/portable-tests com.scopewise.lunarthursday.VersionPolicyTest
```

## Privacy

The app contacts the independent Hijri-date feed and GitHub's public Releases API, whose providers may observe the connecting IP address. **Weather visuals are strictly opt-in:** after enabling them, Android requests only approximate foreground location; coordinates are rounded to two decimal places (approximately 1 km) and sent by HTTPS to [Open-Meteo](https://open-meteo.com/). Raw location coordinates are not saved, and turning the feature off clears cached weather. No background or precise location permission is requested. Weather conditions are cached locally for up to three hours. Open-Meteo forecast data is supplied under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/) and its free-hosted API is intended for non-commercial use. The app sends no account credentials or tracking identifiers. Existing reminder settings and month-start observations remain local.

## Attribution / references

- [Pakistan Meteorological Department — lunar calendar](https://weather.gov.pk/cdpc/lunar-calendar)
- [PMD 2026 expected lunar starts](https://cdpc.pmd.gov.pk/probable2026.pdf)
- [PMD 2027 expected lunar starts](https://weather.gov.pk/storage/uploads/cdpc/lunar_calendar/pdf/probable%202027.pdf)
- [Third-party Pakistan Hijri-date feed](https://islamicdatetoday.pk/data/today.json)
- [Android notification alarm guidance](https://developer.android.com/develop/background-work/services/alarms)
- [Material 3 dynamic color](https://developer.android.com/develop/ui/views/theming/dynamic-colors)
- [Android Gradle Plugin compatibility](https://developer.android.com/build/releases/about-agp)

## License

MIT — see [LICENSE](LICENSE).

**Build evidence:** Pure-Java tests and static XML checks can be run without Android SDK. Successful GitHub CI builds establish APK compilation, not real-device notification timing or official announcement accuracy.
