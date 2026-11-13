package com.scopewise.lunarthursday

import android.content.Context
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Consistent, resource-backed English/Urdu UI strings and Islamic month display. */
object UiText {
    private val KEYS: Map<String, Int> = hashMapOf(
        "☾  HILAL THURSDAY" to R.string.tr_23639ea3a,
        "Hilal Thursday" to R.string.tr_9193319ab,
        "Hilal Thursday alarm" to R.string.tr_ee6dcaadb,
        "Hilal Thursday alarm was missed" to R.string.tr_d5beb0e19,
        "Hilal Thursday test notification" to R.string.tr_317ebbade,
        "First Hilal Thursday reminder" to R.string.tr_3f0043a99,
        "First Lunar Thursday" to R.string.tr_8f2742ccf,
        "Lunar Thursday" to R.string.tr_aea4a1df5,
        "First Lunar Thursday reminder" to R.string.tr_c0346dc3c,
        "Lunar Thursday alarm was missed" to R.string.tr_c49feebf8,
        "Lunar Thursday test notification" to R.string.tr_a7ac68052,
        "Lunar Thursday alarm" to R.string.tr_44e6395f1,
        "Your next lunar Thursday, at a glance" to R.string.tr_00a83a63b,
        "Today in Pakistan: " to R.string.tr_01f44bc97,
        "Monthly reminder • 11:00 AM PKT" to R.string.tr_9bb43362e,
        "Settings  ·  alarm & updates" to R.string.tr_e0ef1d33f,
        "About  ·  developer & surprises" to R.string.tr_06352ba4d,
        "NEXT REMINDER" to R.string.tr_8e053a60d,
        "11:00 AM  ·  Pakistan Standard Time" to R.string.tr_8c0bda2f7,
        "Tap for the month-start date and source" to R.string.tr_c45a5fc04,
        "UPDATE AVAILABLE" to R.string.tr_8d46cd4f3,
        "Version " to R.string.tr_03283ea97,
        " · Preview" to R.string.tr_82e4c1c51,
        "Download on GitHub" to R.string.tr_ba9728b14,
        "Monthly reminders" to R.string.tr_89b23b0f8,
        "Allow notifications" to R.string.tr_3ad3c4eb8,
        "Allow precise alarm scheduling" to R.string.tr_dbca0d249,
        "Alert mode: " to R.string.tr_164ab02db,
        "Ringing alarm" to R.string.tr_cafd11c28,
        "Standard notification" to R.string.tr_301587d50,
        "Upcoming lunar months" to R.string.tr_1ef5187f3,
        "PMD forecast · not confirmed" to R.string.tr_39d9ad26e,
        "Third-party feed reports confirmation" to R.string.tr_553dce768,
        "Calendar estimate · not confirmed" to R.string.tr_062079591,
        "  •  Tap for details" to R.string.tr_b8468f15a,
        "Automatic date updates" to R.string.tr_9b5824f8b,
        "No manual date changes. Pakistan Hijri-date feed checks are scheduled about every six hours, subject to Android background limits." to R.string.tr_8c7b688f9,
        "Only explicitly confirmed dates from the third-party provider are cached as reported. Otherwise PMD forecasts or calendar estimates are used, which may differ from Pakistan's official sighting." to R.string.tr_3f1373ff2,
        "Last successful feed check: " to R.string.tr_1b36cb9e2,
        "Not checked online yet" to R.string.tr_d1191e3eb,
        "Online date data may be stale. Forecasts are shown until a new check succeeds." to R.string.tr_f58fbf254,
        "Request a date refresh" to R.string.tr_97677a3d8,
        "Refresh requested; Android will run it when network is available" to R.string.tr_e6dca6297,
        "Enable reminders to allow date checks" to R.string.tr_de6925be8,
        "Notifications and updates" to R.string.tr_24731f717,
        "Send a test notification" to R.string.tr_75c42cd1d,
        "Check notifications" to R.string.tr_af9c36846,
        "Open app settings" to R.string.tr_6f0a71443,
        "View Pakistan PMD calendar" to R.string.tr_c97c92d50,
        "Automatic scheduling and date checks require no login or monthly adjustments." to R.string.tr_78f7af007,
        "Scheduled time has arrived" to R.string.tr_ce34430d9,
        "%1\$d days, %2\$d hours remaining" to R.string.tr_3f02015c4,
        "%1\$d hours, %2\$d minutes remaining" to R.string.tr_b1b598c15,
        "%1\$d minutes remaining" to R.string.tr_17fd4b2db,
        "Lunar month details" to R.string.tr_e6e3d3f37,
        "Month: " to R.string.tr_09e0cb995,
        "First civil day (Pakistan): " to R.string.tr_62748ca83,
        "First Thursday: " to R.string.tr_a3be44b12,
        "Reminder: 11:00 AM PKT" to R.string.tr_2693fa2db,
        "Source: " to R.string.tr_f59a85c50,
        "Forecasts and third-party reports may differ from the official Ruet-e-Hilal decision." to R.string.tr_196d2da5d,
        "Close" to R.string.tr_bbfa773e5,
        "Reminders are paused." to R.string.tr_2ab2b2db2,
        "Allow notifications to receive reminders or ringing alarms." to R.string.tr_004a77be9,
        "Notifications are blocked in Android settings." to R.string.tr_d2dadf8d6,
        "Enabled, but Android may delay delivery without precise alarm access." to R.string.tr_24c60376d,
        "Ringing alarm is selected. Sound, vibration and Snooze are managed in Settings." to R.string.tr_52b98b6ac,
        "Notifications are scheduled automatically in Pakistan time." to R.string.tr_f90b49d82,
        "Enable Alarms & reminders in Android settings" to R.string.tr_10eae0ce7,
        "A browser is required" to R.string.tr_f2a40225e,
        "Moonlight unlocked" to R.string.tr_8e665e894,
        "🌙 Moonlight unlocked" to R.string.tr_d4d62ebda,
        "Seven taps, seven skies. Your next Thursday is written in the stars — but the date still comes from the calendar, not the stars." to R.string.tr_4905881fe,
        "Lovely" to R.string.tr_2b791f512,
        "Settings" to R.string.tr_c7f73bb54,
        "←  Back" to R.string.tr_f9c72b159,
        "Alert delivery, permissions and app updates" to R.string.tr_d48d6f64c,
        "How reminders reach you" to R.string.tr_38fd47dcf,
        "Ring audible alarm" to R.string.tr_31177076d,
        "Use a ringing alarm with Dismiss and Snooze, instead of a simple notification." to R.string.tr_f6909a1b7,
        "Play alarm sound" to R.string.tr_9ee8e160d,
        "Use your phone's default alarm tone, for up to two minutes." to R.string.tr_bbb6663d7,
        "Vibrate" to R.string.tr_9c1391509,
        "Vibration follows the device's vibration policy." to R.string.tr_12b461e73,
        "Test ringing alarm" to R.string.tr_f93b9817b,
        "Alarm service unavailable on this device" to R.string.tr_ba95d7d71,
        "Full-screen alarm display is not allowed. Android may show a heads-up alert instead. Alarm sound can still play when permitted." to R.string.tr_e324b814d,
        "Allow full-screen alarm display" to R.string.tr_895bbef9c,
        "Reminder time remains 11:00 AM Pakistan time. Lunar dates are updated automatically; there is no manual calendar correction." to R.string.tr_c1ebcee85,
        "Android permissions" to R.string.tr_b76bd04ef,
        "Notifications: allowed" to R.string.tr_61a4aaf1c,
        "Notifications: permission needed" to R.string.tr_00fedff2f,
        "Precise alarm scheduling: available" to R.string.tr_4d80c320a,
        "Precise alarm scheduling: not available; reminder may be delayed" to R.string.tr_a62ad68a8,
        "Allow exact alarms" to R.string.tr_d5d9126e0,
        "App updates" to R.string.tr_16213f85d,
        "Installed version: " to R.string.tr_8cda36e31,
        "Check automatically" to R.string.tr_7cd229d28,
        "Checks GitHub when you open the app, normally at most twice per day." to R.string.tr_2bf4fb109,
        "Stable release signing active. Incompatible debug-signed previews are excluded from update checks." to R.string.tr_34f30058d,
        "Include preview versions" to R.string.tr_0b6504c95,
        "Preview APKs use debug signing and may require reinstalling." to R.string.tr_d9a529667,
        "Last checked: " to R.string.tr_92cd2c60f,
        "Never checked" to R.string.tr_e738e9bad,
        "No newer release detected" to R.string.tr_fc6624bd9,
        " (not yet checked)" to R.string.tr_5c620cc0f,
        "Update available: " to R.string.tr_b968826a0,
        " (preview)" to R.string.tr_6c52dbbbd,
        "Download update on GitHub" to R.string.tr_ea60c65e6,
        "Checking GitHub..." to R.string.tr_3605d841a,
        "Check for updates now" to R.string.tr_cca542e49,
        "Update check already running" to R.string.tr_a8cfdf4e5,
        "Checking GitHub releases" to R.string.tr_0005bdb96,
        "Android always asks before installing APK updates. Moving from older debug-signed preview builds to a signed release requires uninstalling the preview first; future releases with the same signing key can update in place." to R.string.tr_c4f1ae264,
        "A browser is needed to open the release" to R.string.tr_752280813,
        "Open notification settings in Android" to R.string.tr_d0125779a,
        "Open Android Settings > Alarms & reminders" to R.string.tr_b979c6881,
        "Full-screen alarm access can be managed in Android settings" to R.string.tr_b54c2a250,
        "About Hilal Thursday" to R.string.tr_e9fae6216,
        "About / Developer" to R.string.tr_5803fbc4e,
        "A quieter way to remember the rhythm of Pakistan's lunar months." to R.string.tr_0047ae94e,
        "Made for Pakistan, open to everyone" to R.string.tr_24b907f39,
        "Developer / maintainer: admknight" to R.string.tr_b4ab1852a,
        "An open-source project. Contributions and issue reports are welcome on GitHub." to R.string.tr_ba6fed22b,
        "Source code and issues" to R.string.tr_03366c77b,
        "Download releases" to R.string.tr_1aaa27f9a,
        "How reminders work" to R.string.tr_f68ab1a99,
        "The reminder is scheduled for 11:00 AM Pakistan Standard Time on the first Thursday on or after each new Hijri month's first civil day." to R.string.tr_5d63b73c0,
        "Official Pakistani moon sighting is determined by the Central Ruet-e-Hilal Committee. The app uses third-party reported dates when available and PMD forecasts or calculated estimates otherwise. It does not claim a government-approved date feed." to R.string.tr_6ad863e01,
        "Privacy" to R.string.tr_cf01481f6,
        "No account, ads, location request, or app analytics. The phone checks a public Hijri-date feed and GitHub's Releases API; those services can see the connecting IP address." to R.string.tr_ebd783954,
        "Credits and transparency" to R.string.tr_d235418b3,
        "Pakistan Meteorological Department (forecast references), public third-party Hijri-date service, Android Material 3 / Material You, and community contributors." to R.string.tr_a4d7b4050,
        "Open project license (MIT)" to R.string.tr_367b81c35,
        "  ·  tap for details" to R.string.tr_5f91c2744,
        "Made with care under the same moon." to R.string.tr_64e319269,
        "✦ Constellation unlocked ✦" to R.string.tr_4d999ebe9,
        "The night sky has no update button, but your reminder app does." to R.string.tr_d871233bf,
        "Back to Earth" to R.string.tr_c94f17a39,
        "🌟 Five stars" to R.string.tr_26ab89cd8,
        "A tiny secret from the developer: some dates need patience, but a gentle reminder can still brighten the day." to R.string.tr_3f40f4683,
        "Shine on" to R.string.tr_7d2f163e8,
        "Dismiss alarm" to R.string.tr_7d5b577f6,
        "Snooze 10 minutes" to R.string.tr_f36436329,
        "Test alarm" to R.string.tr_30e977abc,
        "11:00 AM Pakistan time" to R.string.tr_59feb6cf1,
        "Dismiss" to R.string.tr_70afe9eff,
        "Snooze 10 min" to R.string.tr_3f5595d00,
        "Ringing lunar alarms" to R.string.tr_625a5311e,
        "Optional ringing alarm. Sound and vibration are controlled inside the app." to R.string.tr_a3111f6e0,
        "this lunar month" to R.string.tr_6eb499e3e,
        "Live date provider reports confirmation." to R.string.tr_7df0179ba,
        "Calendar estimate; official sighting may differ." to R.string.tr_0bb2128a2,
        "Notifications are enabled. Monthly reminders target 11:00 AM PKT." to R.string.tr_5ca78f773,
        "One reminder on the first Thursday of each lunar month in Pakistan time" to R.string.tr_18e0e1489,
        "The ringing alarm stopped after two minutes. Open the app to review your reminder." to R.string.tr_e49e6ce0b,
        "Update check already in progress" to R.string.tr_4ce8b7133,
        "No newer compatible release found" to R.string.tr_133278b92,
        "New version available: " to R.string.tr_b493ac133,
        "Could not check GitHub. Check your connection or try later." to R.string.tr_6f0c4c4f3,
        "Language" to R.string.tr_89b86ab0e,
        "App language" to R.string.tr_01e1787a9,
        "Choose the language used for the app screens and reminders." to R.string.tr_c264249ca,
        "Follow device language" to R.string.tr_fe390c37a,
        "English" to R.string.tr_649df08a4,
        "Urdu" to R.string.tr_412e31909,
        "اردو" to R.string.tr_482f6e668,
        "Language changed" to R.string.tr_e05882ae7,
        "11:00 AM PKT" to R.string.tr_a049d931d,
        "PMD published forecast - not a confirmed sighting" to R.string.tr_f49025ed2,
        "Live provider reports confirmation (not a government API)" to R.string.tr_1f044de6c,
        "Calculated estimate - NOT a Pakistan sighting confirmation" to R.string.tr_c45d4724f,
        " 11:00 AM Pakistan time. " to R.string.tr_2740d8755,
        " at 11:00 AM Pakistan time. " to R.string.tr_14d624084,
        ": the ringing alarm stopped after two minutes. Open the app to review your reminder." to R.string.tr_2b2c8ff92,
        " · 11:00 AM Pakistan time" to R.string.tr_30abb2248,
        "       ✦           ·\n  ·       ☾         ✧\n       ✧       ✦\n\nThe night sky has no update button, but your reminder app does." to R.string.tr_28efa34c4,
        "A tiny secret from the developer: some dates need patience, but a gentle reminder can still brighten the day.\n\n🌙  ✦  ✧  ✦  🌙" to R.string.tr_8cc4c465e,
        "More options" to R.string.tr_bc79cdffb,
        "Home" to R.string.tr_3a7869538,
        "Calendar" to R.string.tr_d5d0a30b5,
        "Updates" to R.string.tr_22e2bada8,
        "About" to R.string.tr_4efca0d10,
        "Days" to R.string.tr_e08c0aa8f,
        "Hours" to R.string.tr_21e849293,
        "Minutes" to R.string.tr_4f846a84e,
        "Reminders enabled" to R.string.tr_7e7ca678f,
        "Reminders paused" to R.string.tr_14cd8cc8f,
        "Review alert permissions" to R.string.tr_0146bbb12,
        "An Android permission may delay reminders" to R.string.tr_5d0c1e332,
        "Update available" to R.string.tr_ff8b555d8,
        "See all  ›" to R.string.tr_c19c8e067,
        "Dates are calculated automatically and may be estimated." to R.string.tr_e7976b5a5,
        "First Thursday · 11:00 AM PKT" to R.string.tr_5c6a715d0,
        "Calendar source information" to R.string.tr_6fe626b4e,
        "Forecasts and calculations are not official confirmations" to R.string.tr_12e1ccc8e,
        "Updates & calendar" to R.string.tr_c14de2388,
        "App releases and automatic Pakistan lunar-date checks" to R.string.tr_9a840afff,
        "APP VERSION" to R.string.tr_896159511,
        "AUTOMATIC HIJRI DATE CHECKS" to R.string.tr_d8c998b38,
        "Dates are updated automatically. Third-party reports, forecasts and calculated estimates can differ from Pakistan's official sighting decision." to R.string.tr_9e90cc8da,
        "Back" to R.string.tr_76900f1bf,
        "🌙 A gentle reminder, every lunar month." to R.string.tr_826773931,
        "11:00 AM Pakistan time · English & Urdu" to R.string.tr_5948efcd3,
        "👨‍💻  Developer" to R.string.tr_6675c7c21,
        "No account, ads or location tracking" to R.string.tr_e9f5537f0,
        "Calendar sources & accuracy" to R.string.tr_2625b494a,
        "How Islamic month dates are determined" to R.string.tr_b2c4cd11d,
        "What's new" to R.string.tr_135e912e8,
        "View the latest public release notes" to R.string.tr_e72a29ae1,
        "Open source & feedback" to R.string.tr_0865ed3f4,
        "Code, issues and contributions" to R.string.tr_808f656e2,
        "MIT License" to R.string.tr_abb2896dc,
        "Read the project license" to R.string.tr_e135e38ac,
        "✨ Made with care under the same moon." to R.string.tr_68a5ff1d6,
        "🌤 Sky and local weather" to R.string.tr17_2961a9a599,
        "The moon and mosque respond to your device time even without location access." to R.string.tr17_c2d5693fc0,
        "Weather-based sky effects" to R.string.tr17_4118f05e26,
        "Optional: ask for approximate location only while you use the app. Weather data by Open-Meteo (CC BY 4.0)." to R.string.tr17_a3cf787004,
        "Weather not available yet. A time-aware scene is still displayed." to R.string.tr17_d64589d0a3,
        "Refresh weather now" to R.string.tr17_da9a0efccf,
        "Approximate coordinates are rounded before weather requests and never saved. Disabling this setting clears cached conditions. No background-location permission is requested." to R.string.tr17_3aef63aaaf,
        "Weather unavailable · time-aware sky shown" to R.string.tr17_a65f32e69d,
        "Allow approximate location in Settings" to R.string.tr17_d976168caa,
        "Clear" to R.string.tr17_719ea396ad,
        "Cloudy" to R.string.tr17_44961bce9c,
        "Foggy" to R.string.tr17_88957d8137,
        "Rainy" to R.string.tr17_cb2afce13b,
        "Snowy" to R.string.tr17_9889a6718c,
        "Thunderstorm" to R.string.tr17_320002f337,
        "Background update notifications" to R.string.tr17_63f21863d0,
        "Android checks GitHub roughly daily, even when this app is closed; you decide when to download and install." to R.string.tr17_2cc18c2fb0,
        "Download and install update" to R.string.tr17_6cf8845e00,
        "Update Hilal Thursday" to R.string.tr17_3e0bf8d605,
        "Not now" to R.string.tr17_e457149073,
        "Download & install" to R.string.tr17_7db0bddc82,
        "Installing update" to R.string.tr17_439f5b6308,
        "Downloading securely…" to R.string.tr17_359f771d8f,
        "Downloading APK…" to R.string.tr17_0a82591963,
        "Cancel" to R.string.tr17_77dfd2135f,
        "Android installation permission" to R.string.tr17_dd26aca1cd,
        "Open Android settings" to R.string.tr17_610e342dea,
        "Later" to R.string.tr17_56e2f5d363,
        "Approximate location denied; time-aware sky remains available" to R.string.tr17_f8b8cfcfff,
        "Hilal Thursday update available" to R.string.tr17_1e434c4650,
        "No browser is needed." to R.string.tr17_d15c2b37c7,
        "Weather: Open-Meteo" to R.string.tr17_3b425ad7cf,
        "Newer signed update" to R.string.tr17_74c7f34de9,
        "Optional weather location · no ads or accounts" to R.string.tr17_6f266286a2,
        "Weather visuals and in-app updates" to R.string.tr17_717c781cff,
        "No account, ads, or analytics. Location is requested only when you enable weather visuals, and only while the app is in use. Approximate coordinates are rounded, sent to Open-Meteo, and never stored. The Hijri-date feed and GitHub release checks also contact external services. Each provider can see the connecting IP address." to R.string.tr17_3e3ca4d087,
        "Hilal Thursday v1.7 adds optional location-based weather scenery, a time-aware moon-and-mosque illustration, background update alerts and secure APK downloads inside the app. Android always confirms installation. Weather data: Open-Meteo (CC BY 4.0)." to R.string.tr17_3f090a48a3,
        "Download version " to R.string.tr17_912d5a7d18,
        " securely inside this app? The APK is checked against its SHA-256 digest and the installed signing certificate before Android confirms installation." to R.string.tr17_72c55f8abb,
        "Allow Hilal Thursday to install app updates in Android settings. Return to this app to continue. No web browser is needed." to R.string.tr17_a5e7a0d079,
        "Previously downloaded APK is no longer compatible" to R.string.tr17_6f2bb9125b,
        "Verified APKs download inside the app. Android confirms installation and may first require permission to install from this app. Permanent signing allows later stable releases to update in place." to R.string.tr17_adff75831c,
        " is ready. Tap to download inside this app." to R.string.tr17_4236381988
    )

    @JvmStatic
    fun tr(context: Context, english: String?): String {
        val key = KEYS[english] ?: return english ?: ""
        return AppLanguage.wrap(context).getString(key)
    }

    @JvmStatic
    fun fmt(context: Context, template: String, vararg values: Any?): String =
        String.format(AppLanguage.locale(context), tr(context, template), *values)

    /** Always marks the displayed day as provisional, even for third-party reports. */
    @JvmStatic
    fun expectedHijriDate(context: Context, month: LunarEngine.Month, civilDate: LocalDate): String {
        val day = String.format(AppLanguage.locale(context), "%d", month.hijriDay(civilDate))
        return AppLanguage.wrap(context).getString(R.string.expected_hijri_date, day, month(context, month))
    }

    /** Today's Hijri-date label is neutral; the source is qualified separately. */
    @JvmStatic
    fun currentHijriDate(context: Context, value: LunarEngine.HijriDate): String {
        val day = String.format(AppLanguage.locale(context), "%d", value.day)
        return AppLanguage.wrap(context).getString(R.string.hijri_current_date, day, month(context, value.month))
    }

    @JvmStatic
    fun reminderTimeText(context: Context): String =
        DateTimeFormatter.ofPattern("h:mm a", AppLanguage.locale(context))
            .format(ReminderStore.reminderTime(context))

    @JvmStatic
    fun hijriClockStatus(context: Context, clock: HijriClock.Result): String {
        val approximateSunset = clock.approximateSunset
            ?: return AppLanguage.wrap(context).getString(R.string.hijri_civil_fallback)
        val sunset = DateTimeFormatter.ofPattern("h:mm a", AppLanguage.locale(context)).format(approximateSunset)
        return AppLanguage.wrap(context).getString(R.string.hijri_sunset_status, sunset, clock.locationName)
    }

    @JvmStatic
    fun date(context: Context, value: LocalDate, longFormat: Boolean): String {
        val format = if (longFormat) "EEEE, d MMMM yyyy" else "d MMM yyyy"
        return DateTimeFormatter.ofPattern(format, AppLanguage.locale(context)).format(value)
    }

    @JvmStatic
    fun dateTime(context: Context, moment: Instant): String =
        DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", AppLanguage.locale(context))
            .format(moment.atZone(LunarEngine.PAKISTAN))

    @JvmStatic
    fun month(context: Context, month: LunarEngine.Month): String {
        if (!AppLanguage.isUrdu(context)) return month.label()
        val names = AppLanguage.wrap(context).resources.getStringArray(R.array.hijri_months)
        if (month.hijriMonth < 1 || month.hijriMonth > names.size) return month.label()
        return names[month.hijriMonth - 1] + " " + month.hijriYear + "ھ"
    }

    /** Convert persisted English month labels for notifications without rewriting the cache. */
    @JvmStatic
    fun monthLabel(context: Context, label: String?): String {
        if (label == null) return tr(context, "this lunar month")
        if (!AppLanguage.isUrdu(context)) return label
        val names = AppLanguage.wrap(context).resources.getStringArray(R.array.hijri_months)
        for (i in 1..names.size) {
            val month = LunarEngine.monthName(i)
            if (label.startsWith("$month ") && label.endsWith(" AH")) {
                return names[i - 1] + " " + label.substring(month.length + 1, label.length - 3) + "ھ"
            }
        }
        return tr(context, label)
    }
}
