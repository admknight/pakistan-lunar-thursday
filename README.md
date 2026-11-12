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
- **In-app update indicator** on the home screen, with a GitHub release checker on app open (about twice daily) and a manual **Check now** option. Debug builds can opt into preview update checks; permanently signed release builds exclude incompatible debug-signed preview APKs. APK installation always requires user approval.
- **Interactive home dashboard** with live countdown in Pakistan time, tap-to-explore upcoming month cards, a reminder status / alert-mode overview, and one-tap date refresh requests (queued through Android JobScheduler).
- **About / Developer page** for `admknight`, open-source attribution, privacy, calendar-source transparency, and small hidden easter eggs.
- Next reminder, upcoming months, source quality, last successful feed check, and a test-notification button.
- **No accounts, no ads, no location permission, no tracking and no manually entered dates.**

## Ringing alarm and updates

Open **Settings** to enable **Ring audible alarm** if a normal notification is easy to miss. You can toggle sound and vibration independently and test the alarm from Settings. Alarm sound/vibration stop automatically after two minutes even if you don't dismiss; Snooze schedules another attempt ten minutes later. Android notification, exact-alarm, foreground service, and (on some devices) full-screen intent permissions affect availability and timing. Do Not Disturb, battery restrictions, device power-off, and OEM behavior can suppress or delay ringing. This path has been built in CI, **not tested on a physical phone**.

The **update manager** checks the public GitHub Releases API automatically when the app is opened, at most every twelve hours after a successful check. It reports a newer matching release and opens its APK download URL in your browser. It cannot silently install updates. With **Include preview versions** disabled, it will hide preview releases. It requests no new accounts, location, or tracking permissions.

### Preview builds versus permanently signed public releases

The automatic CI workflow generates a **debug signing key per runner/build**, so its builds are correctly tagged **preview**. **GitHub pre-releases are still public** and provide direct APK downloads. A long-lived release key and a separate certificate-verified release workflow have now been prepared. The maintainer must add two encrypted GitHub Actions secrets once before that workflow can publish a normally labeled public release. See [Private signing setup](docs/SIGNING.md); **the private signing material is never part of this public repository**. First-time migration from any debug-signed APK to the permanently signed APK requires uninstalling the debug APK. After that, updates using the same signer and a higher version code can install in place. App signing is not a substitute for actual device testing. The stable build excludes incompatible debug-signed preview updates.


## Dashboard and navigation (v1.6.0)

The main screen now has a compact app header with a three-dot **More options** menu, a decorative crescent-and-mosque night-sky card, three live countdown tiles (days/hours/minutes), a small reminder-status row and the next upcoming months. A bottom navigation bar switches between **Home**, **Calendar** (upcoming Hijri months), **Updates** (release/date checks), and **About**. The design is built with native Android views and Material 3 dynamic colors; the previous promotional concept was not a screenshot and is not claimed to be reproduced pixel-for-pixel.

The **About / Developer** screen uses concise icon-and-text rows for privacy, calendar accuracy, recent releases, project feedback and the MIT license rather than full-width GitHub buttons. Three optional hidden interactions remain: tap the home logo/title seven times, long-press the About icon, and tap the About version area five times. None of the easter eggs changes alarms, dates, or update settings.

Android accessibility labels, English/Urdu string resources, and right-to-left layout behavior are retained. The reminders still fire at **11:00 AM Pakistan time**; month start accuracy remains subject to the independent data-source limitations below. The graphical layout has not yet been visually exercised on a physical Android device.

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

The GitHub workflow installs the matching Android SDK platform/build tools, runs 160 pure-Java scheduling/feed-policy/version assertions, builds the APK and attaches its SHA-256 hash. It runs on pushes to `main`, pull requests and manual dispatch. Successful pushes to `main` also publish a **versioned GitHub pre-release** if that version has not already been published. Increment `versionName` when publishing a new preview; rebuilding the same version does not silently replace public assets. This project deliberately uses the stable Java Material Components SDK rather than introducing Kotlin/Compose into an already tested Java-only notification engine.

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

The app sends HTTPS requests to the external third-party date feed and to GitHub's public Releases API; those providers may observe the connecting IP address. The app itself sends no user profile, account credentials, location, or device identifiers. Data such as enabled state, last date check, and month-start observations is stored locally on the phone.

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
