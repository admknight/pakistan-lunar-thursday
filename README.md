# Lunar Thursday — Pakistan

A small, **fully automatic, best-effort** Android reminder for the **first Thursday on or after the first civil day of every new Islamic month in Pakistan**, at **11:00 AM Pakistan Standard Time (Asia/Karachi)**.

![Build Android APK](https://github.com/admknight/pakistan-lunar-thursday/actions/workflows/build-apk.yml/badge.svg)

## Download / install

Open **[Releases](https://github.com/admknight/pakistan-lunar-thursday/releases)** and select the latest **Preview** release. Under **Assets**, download `app-debug.apk` directly and install it on Android 8.0 or later. You can also download `app-debug.apk.sha256` to verify its checksum. For build-by-build debugging, [Actions → Build Android APK](https://github.com/admknight/pakistan-lunar-thursday/actions/workflows/build-apk.yml) retains the temporary `LunarThursday-debug-apk` artifact.

The GitHub Actions build produces a **debug-signed test APK**, not a production-signed stable APK. Each cloud runner may use a different debug signing key. Therefore, installing a newer build can require uninstalling the old one first. Do not treat this as a stable release-signing solution. Android 13+ requires the one-time **Notifications** permission; Android 12+ may also require **Alarms & reminders** permission for closer-to-exact delivery. No monthly date entry is necessary.

## Features

- A one-time automatically scheduled reminder at **11 AM Pakistan time** on the first Thursday of each new Islamic month.
- Automated date checks using HTTPS and Android `JobScheduler`, nominally every six hours and subject to Android background execution limits.
- Cache of dates the third-party provider explicitly labels as confirmed, never treating its estimates as confirmations.
- PMD published **2026–2027 forecast dates** used as fallback, then a clearly labeled *calculated approximation* for other years.
- Rescheduling on device reboot, clock or timezone changes, app update, and exact-alarm permission changes.
- Material 3 interface, light/dark theme, **Material You dynamic wallpaper color** on Android 12+.
- **Optional ringing alarm** in Settings: repeats the system alarm sound/vibration for up to two minutes; **Dismiss** and **Snooze 10 minutes** controls; a missed-alert notification remains after timeout. The original notification-only mode stays the default.
- **Settings page** with alarm preferences, exact-alarm/full-screen permission guidance, and automatic updates that require no manual lunar-date adjustments.
- **In-app update indicator** on the home screen, with a GitHub release checker on app open (about twice daily) and a manual **Check now** option. You can include or exclude preview releases. APK installation always requires user approval.
- Next reminder, upcoming months, source quality, last successful feed check, and a test-notification button.
- **No accounts, no ads, no location permission, no tracking and no manually entered dates.**

## Ringing alarm and updates

Open **Settings** to enable **Ring audible alarm** if a normal notification is easy to miss. You can toggle sound and vibration independently and test the alarm from Settings. Alarm sound/vibration stop automatically after two minutes even if you don't dismiss; Snooze schedules another attempt ten minutes later. Android notification, exact-alarm, foreground service, and (on some devices) full-screen intent permissions affect availability and timing. Do Not Disturb, battery restrictions, device power-off, and OEM behavior can suppress or delay ringing. This path has been built in CI, **not tested on a physical phone**.

The **update manager** checks the public GitHub Releases API automatically when the app is opened, at most every twelve hours after a successful check. It reports a newer matching release and opens its APK download URL in your browser. It cannot silently install updates. With **Include preview versions** disabled, it will hide preview releases. It requests no new accounts, location, or tracking permissions.

### Why releases are currently previews

The existing workflow generates a **new debug signing key per runner/build**. An APK signed by a different key usually cannot update an installed copy without uninstalling it first. To ship a real stable release and seamless updates, the project needs a **persistent release signing keystore managed securely outside this public repository** and Android device testing. No private keystore is stored in the repository or embedded in GitHub Actions; until that is configured, builds are explicitly tagged **preview**. GitHub preview releases are public and retain downloadable APK assets; the Actions artifacts are temporary.

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
