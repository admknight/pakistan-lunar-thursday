package com.scopewise.lunarthursday;

import android.content.Context;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

/** Consistent, resource-backed English/Urdu UI strings and Islamic month display. */
final class UiText {
    private static final Map<String, Integer> KEYS;
    static {
        Map<String, Integer> entries = new HashMap<>();
        entries.put("☾  HILAL THURSDAY", R.string.tr_23639ea3a);
        entries.put("Hilal Thursday", R.string.tr_9193319ab);
        entries.put("Hilal Thursday alarm", R.string.tr_ee6dcaadb);
        entries.put("Hilal Thursday alarm was missed", R.string.tr_d5beb0e19);
        entries.put("Hilal Thursday test notification", R.string.tr_317ebbade);
        entries.put("First Hilal Thursday reminder", R.string.tr_3f0043a99);
        entries.put("First Lunar Thursday", R.string.tr_8f2742ccf);
        entries.put("Lunar Thursday", R.string.tr_aea4a1df5);
        entries.put("First Lunar Thursday reminder", R.string.tr_c0346dc3c);
        entries.put("Lunar Thursday alarm was missed", R.string.tr_c49feebf8);
        entries.put("Lunar Thursday test notification", R.string.tr_a7ac68052);
        entries.put("Lunar Thursday alarm", R.string.tr_44e6395f1);
        entries.put("Your next lunar Thursday, at a glance", R.string.tr_00a83a63b);
        entries.put("Today in Pakistan: ", R.string.tr_01f44bc97);
        entries.put("Monthly reminder • 11:00 AM PKT", R.string.tr_9bb43362e);
        entries.put("Settings  ·  alarm & updates", R.string.tr_e0ef1d33f);
        entries.put("About  ·  developer & surprises", R.string.tr_06352ba4d);
        entries.put("NEXT REMINDER", R.string.tr_8e053a60d);
        entries.put("11:00 AM  ·  Pakistan Standard Time", R.string.tr_8c0bda2f7);
        entries.put("Tap for the month-start date and source", R.string.tr_c45a5fc04);
        entries.put("UPDATE AVAILABLE", R.string.tr_8d46cd4f3);
        entries.put("Version ", R.string.tr_03283ea97);
        entries.put(" · Preview", R.string.tr_82e4c1c51);
        entries.put("Download on GitHub", R.string.tr_ba9728b14);
        entries.put("Monthly reminders", R.string.tr_89b23b0f8);
        entries.put("Allow notifications", R.string.tr_3ad3c4eb8);
        entries.put("Allow precise alarm scheduling", R.string.tr_dbca0d249);
        entries.put("Alert mode: ", R.string.tr_164ab02db);
        entries.put("Ringing alarm", R.string.tr_cafd11c28);
        entries.put("Standard notification", R.string.tr_301587d50);
        entries.put("Upcoming lunar months", R.string.tr_1ef5187f3);
        entries.put("PMD forecast · not confirmed", R.string.tr_39d9ad26e);
        entries.put("Third-party feed reports confirmation", R.string.tr_553dce768);
        entries.put("Calendar estimate · not confirmed", R.string.tr_062079591);
        entries.put("  •  Tap for details", R.string.tr_b8468f15a);
        entries.put("Automatic date updates", R.string.tr_9b5824f8b);
        entries.put("No manual date changes. Pakistan Hijri-date feed checks are scheduled about every six hours, subject to Android background limits.", R.string.tr_8c7b688f9);
        entries.put("Only explicitly confirmed dates from the third-party provider are cached as reported. Otherwise PMD forecasts or calendar estimates are used, which may differ from Pakistan's official sighting.", R.string.tr_3f1373ff2);
        entries.put("Last successful feed check: ", R.string.tr_1b36cb9e2);
        entries.put("Not checked online yet", R.string.tr_d1191e3eb);
        entries.put("Online date data may be stale. Forecasts are shown until a new check succeeds.", R.string.tr_f58fbf254);
        entries.put("Request a date refresh", R.string.tr_97677a3d8);
        entries.put("Refresh requested; Android will run it when network is available", R.string.tr_e6dca6297);
        entries.put("Enable reminders to allow date checks", R.string.tr_de6925be8);
        entries.put("Notifications and updates", R.string.tr_24731f717);
        entries.put("Send a test notification", R.string.tr_75c42cd1d);
        entries.put("Check notifications", R.string.tr_af9c36846);
        entries.put("Open app settings", R.string.tr_6f0a71443);
        entries.put("View Pakistan PMD calendar", R.string.tr_c97c92d50);
        entries.put("Automatic scheduling and date checks require no login or monthly adjustments.", R.string.tr_78f7af007);
        entries.put("Scheduled time has arrived", R.string.tr_ce34430d9);
        entries.put("%1$d days, %2$d hours remaining", R.string.tr_3f02015c4);
        entries.put("%1$d hours, %2$d minutes remaining", R.string.tr_b1b598c15);
        entries.put("%1$d minutes remaining", R.string.tr_17fd4b2db);
        entries.put("Lunar month details", R.string.tr_e6e3d3f37);
        entries.put("Month: ", R.string.tr_09e0cb995);
        entries.put("First civil day (Pakistan): ", R.string.tr_62748ca83);
        entries.put("First Thursday: ", R.string.tr_a3be44b12);
        entries.put("Reminder: 11:00 AM PKT", R.string.tr_2693fa2db);
        entries.put("Source: ", R.string.tr_f59a85c50);
        entries.put("Forecasts and third-party reports may differ from the official Ruet-e-Hilal decision.", R.string.tr_196d2da5d);
        entries.put("Close", R.string.tr_bbfa773e5);
        entries.put("Reminders are paused.", R.string.tr_2ab2b2db2);
        entries.put("Allow notifications to receive reminders or ringing alarms.", R.string.tr_004a77be9);
        entries.put("Notifications are blocked in Android settings.", R.string.tr_d2dadf8d6);
        entries.put("Enabled, but Android may delay delivery without precise alarm access.", R.string.tr_24c60376d);
        entries.put("Ringing alarm is selected. Sound, vibration and Snooze are managed in Settings.", R.string.tr_52b98b6ac);
        entries.put("Notifications are scheduled automatically in Pakistan time.", R.string.tr_f90b49d82);
        entries.put("Enable Alarms & reminders in Android settings", R.string.tr_10eae0ce7);
        entries.put("A browser is required", R.string.tr_f2a40225e);
        entries.put("Moonlight unlocked", R.string.tr_8e665e894);
        entries.put("🌙 Moonlight unlocked", R.string.tr_d4d62ebda);
        entries.put("Seven taps, seven skies. Your next Thursday is written in the stars — but the date still comes from the calendar, not the stars.", R.string.tr_4905881fe);
        entries.put("Lovely", R.string.tr_2b791f512);
        entries.put("Settings", R.string.tr_c7f73bb54);
        entries.put("←  Back", R.string.tr_f9c72b159);
        entries.put("Alert delivery, permissions and app updates", R.string.tr_d48d6f64c);
        entries.put("How reminders reach you", R.string.tr_38fd47dcf);
        entries.put("Ring audible alarm", R.string.tr_31177076d);
        entries.put("Use a ringing alarm with Dismiss and Snooze, instead of a simple notification.", R.string.tr_f6909a1b7);
        entries.put("Play alarm sound", R.string.tr_9ee8e160d);
        entries.put("Use your phone's default alarm tone, for up to two minutes.", R.string.tr_bbb6663d7);
        entries.put("Vibrate", R.string.tr_9c1391509);
        entries.put("Vibration follows the device's vibration policy.", R.string.tr_12b461e73);
        entries.put("Test ringing alarm", R.string.tr_f93b9817b);
        entries.put("Alarm service unavailable on this device", R.string.tr_ba95d7d71);
        entries.put("Full-screen alarm display is not allowed. Android may show a heads-up alert instead. Alarm sound can still play when permitted.", R.string.tr_e324b814d);
        entries.put("Allow full-screen alarm display", R.string.tr_895bbef9c);
        entries.put("Reminder time remains 11:00 AM Pakistan time. Lunar dates are updated automatically; there is no manual calendar correction.", R.string.tr_c1ebcee85);
        entries.put("Android permissions", R.string.tr_b76bd04ef);
        entries.put("Notifications: allowed", R.string.tr_61a4aaf1c);
        entries.put("Notifications: permission needed", R.string.tr_00fedff2f);
        entries.put("Precise alarm scheduling: available", R.string.tr_4d80c320a);
        entries.put("Precise alarm scheduling: not available; reminder may be delayed", R.string.tr_a62ad68a8);
        entries.put("Allow exact alarms", R.string.tr_d5d9126e0);
        entries.put("App updates", R.string.tr_16213f85d);
        entries.put("Installed version: ", R.string.tr_8cda36e31);
        entries.put("Check automatically", R.string.tr_7cd229d28);
        entries.put("Checks GitHub when you open the app, normally at most twice per day.", R.string.tr_2bf4fb109);
        entries.put("Stable release signing active. Incompatible debug-signed previews are excluded from update checks.", R.string.tr_34f30058d);
        entries.put("Include preview versions", R.string.tr_0b6504c95);
        entries.put("Preview APKs use debug signing and may require reinstalling.", R.string.tr_d9a529667);
        entries.put("Last checked: ", R.string.tr_92cd2c60f);
        entries.put("Never checked", R.string.tr_e738e9bad);
        entries.put("No newer release detected", R.string.tr_fc6624bd9);
        entries.put(" (not yet checked)", R.string.tr_5c620cc0f);
        entries.put("Update available: ", R.string.tr_b968826a0);
        entries.put(" (preview)", R.string.tr_6c52dbbbd);
        entries.put("Download update on GitHub", R.string.tr_ea60c65e6);
        entries.put("Checking GitHub...", R.string.tr_3605d841a);
        entries.put("Check for updates now", R.string.tr_cca542e49);
        entries.put("Update check already running", R.string.tr_a8cfdf4e5);
        entries.put("Checking GitHub releases", R.string.tr_0005bdb96);
        entries.put("Android always asks before installing APK updates. Moving from older debug-signed preview builds to a signed release requires uninstalling the preview first; future releases with the same signing key can update in place.", R.string.tr_c4f1ae264);
        entries.put("A browser is needed to open the release", R.string.tr_752280813);
        entries.put("Open notification settings in Android", R.string.tr_d0125779a);
        entries.put("Open Android Settings > Alarms & reminders", R.string.tr_b979c6881);
        entries.put("Full-screen alarm access can be managed in Android settings", R.string.tr_b54c2a250);
        entries.put("About Hilal Thursday", R.string.tr_e9fae6216);
        entries.put("About / Developer", R.string.tr_5803fbc4e);
        entries.put("A quieter way to remember the rhythm of Pakistan's lunar months.", R.string.tr_0047ae94e);
        entries.put("Made for Pakistan, open to everyone", R.string.tr_24b907f39);
        entries.put("Developer / maintainer: admknight", R.string.tr_b4ab1852a);
        entries.put("An open-source project. Contributions and issue reports are welcome on GitHub.", R.string.tr_ba6fed22b);
        entries.put("Source code and issues", R.string.tr_03366c77b);
        entries.put("Download releases", R.string.tr_1aaa27f9a);
        entries.put("How reminders work", R.string.tr_f68ab1a99);
        entries.put("The reminder is scheduled for 11:00 AM Pakistan Standard Time on the first Thursday on or after each new Hijri month's first civil day.", R.string.tr_5d63b73c0);
        entries.put("Official Pakistani moon sighting is determined by the Central Ruet-e-Hilal Committee. The app uses third-party reported dates when available and PMD forecasts or calculated estimates otherwise. It does not claim a government-approved date feed.", R.string.tr_6ad863e01);
        entries.put("Privacy", R.string.tr_cf01481f6);
        entries.put("No account, ads, location request, or app analytics. The phone checks a public Hijri-date feed and GitHub's Releases API; those services can see the connecting IP address.", R.string.tr_ebd783954);
        entries.put("Credits and transparency", R.string.tr_d235418b3);
        entries.put("Pakistan Meteorological Department (forecast references), public third-party Hijri-date service, Android Material 3 / Material You, and community contributors.", R.string.tr_a4d7b4050);
        entries.put("Open project license (MIT)", R.string.tr_367b81c35);
        entries.put("  ·  tap for details", R.string.tr_5f91c2744);
        entries.put("Made with care under the same moon.", R.string.tr_64e319269);
        entries.put("✦ Constellation unlocked ✦", R.string.tr_4d999ebe9);
        entries.put("The night sky has no update button, but your reminder app does.", R.string.tr_d871233bf);
        entries.put("Back to Earth", R.string.tr_c94f17a39);
        entries.put("🌟 Five stars", R.string.tr_26ab89cd8);
        entries.put("A tiny secret from the developer: some dates need patience, but a gentle reminder can still brighten the day.", R.string.tr_3f40f4683);
        entries.put("Shine on", R.string.tr_7d2f163e8);
        entries.put("Dismiss alarm", R.string.tr_7d5b577f6);
        entries.put("Snooze 10 minutes", R.string.tr_f36436329);
        entries.put("Test alarm", R.string.tr_30e977abc);
        entries.put("11:00 AM Pakistan time", R.string.tr_59feb6cf1);
        entries.put("Dismiss", R.string.tr_70afe9eff);
        entries.put("Snooze 10 min", R.string.tr_3f5595d00);
        entries.put("Ringing lunar alarms", R.string.tr_625a5311e);
        entries.put("Optional ringing alarm. Sound and vibration are controlled inside the app.", R.string.tr_a3111f6e0);
        entries.put("this lunar month", R.string.tr_6eb499e3e);
        entries.put("Live date provider reports confirmation.", R.string.tr_7df0179ba);
        entries.put("Calendar estimate; official sighting may differ.", R.string.tr_0bb2128a2);
        entries.put("Notifications are enabled. Monthly reminders target 11:00 AM PKT.", R.string.tr_5ca78f773);
        entries.put("One reminder on the first Thursday of each lunar month in Pakistan time", R.string.tr_18e0e1489);
        entries.put("The ringing alarm stopped after two minutes. Open the app to review your reminder.", R.string.tr_e49e6ce0b);
        entries.put("Update check already in progress", R.string.tr_4ce8b7133);
        entries.put("No newer compatible release found", R.string.tr_133278b92);
        entries.put("New version available: ", R.string.tr_b493ac133);
        entries.put("Could not check GitHub. Check your connection or try later.", R.string.tr_6f0c4c4f3);
        entries.put("Language", R.string.tr_89b86ab0e);
        entries.put("App language", R.string.tr_01e1787a9);
        entries.put("Choose the language used for the app screens and reminders.", R.string.tr_c264249ca);
        entries.put("Follow device language", R.string.tr_fe390c37a);
        entries.put("English", R.string.tr_649df08a4);
        entries.put("Urdu", R.string.tr_412e31909);
        entries.put("اردو", R.string.tr_482f6e668);
        entries.put("Language changed", R.string.tr_e05882ae7);
        entries.put("11:00 AM PKT", R.string.tr_a049d931d);
        entries.put("PMD published forecast - not a confirmed sighting", R.string.tr_f49025ed2);
        entries.put("Live provider reports confirmation (not a government API)", R.string.tr_1f044de6c);
        entries.put("Calculated estimate - NOT a Pakistan sighting confirmation", R.string.tr_c45d4724f);
        entries.put(" 11:00 AM Pakistan time. ", R.string.tr_2740d8755);
        entries.put(" at 11:00 AM Pakistan time. ", R.string.tr_14d624084);
        entries.put(": the ringing alarm stopped after two minutes. Open the app to review your reminder.", R.string.tr_2b2c8ff92);
        entries.put(" · 11:00 AM Pakistan time", R.string.tr_30abb2248);
        entries.put("       ✦           ·\n  ·       ☾         ✧\n       ✧       ✦\n\nThe night sky has no update button, but your reminder app does.", R.string.tr_28efa34c4);
        entries.put("A tiny secret from the developer: some dates need patience, but a gentle reminder can still brighten the day.\n\n🌙  ✦  ✧  ✦  🌙", R.string.tr_8cc4c465e);
        entries.put("More options", R.string.tr_bc79cdffb);
        entries.put("Home", R.string.tr_3a7869538);
        entries.put("Calendar", R.string.tr_d5d0a30b5);
        entries.put("Updates", R.string.tr_22e2bada8);
        entries.put("About", R.string.tr_4efca0d10);
        entries.put("Days", R.string.tr_e08c0aa8f);
        entries.put("Hours", R.string.tr_21e849293);
        entries.put("Minutes", R.string.tr_4f846a84e);
        entries.put("Reminders enabled", R.string.tr_7e7ca678f);
        entries.put("Reminders paused", R.string.tr_14cd8cc8f);
        entries.put("Review alert permissions", R.string.tr_0146bbb12);
        entries.put("An Android permission may delay reminders", R.string.tr_5d0c1e332);
        entries.put("Update available", R.string.tr_ff8b555d8);
        entries.put("See all  ›", R.string.tr_c19c8e067);
        entries.put("Dates are calculated automatically and may be estimated.", R.string.tr_e7976b5a5);
        entries.put("First Thursday · 11:00 AM PKT", R.string.tr_5c6a715d0);
        entries.put("Calendar source information", R.string.tr_6fe626b4e);
        entries.put("Forecasts and calculations are not official confirmations", R.string.tr_12e1ccc8e);
        entries.put("Updates & calendar", R.string.tr_c14de2388);
        entries.put("App releases and automatic Pakistan lunar-date checks", R.string.tr_9a840afff);
        entries.put("APP VERSION", R.string.tr_896159511);
        entries.put("AUTOMATIC HIJRI DATE CHECKS", R.string.tr_d8c998b38);
        entries.put("Dates are updated automatically. Third-party reports, forecasts and calculated estimates can differ from Pakistan's official sighting decision.", R.string.tr_9e90cc8da);
        entries.put("Back", R.string.tr_76900f1bf);
        entries.put("🌙 A gentle reminder, every lunar month.", R.string.tr_826773931);
        entries.put("11:00 AM Pakistan time · English & Urdu", R.string.tr_5948efcd3);
        entries.put("👨‍💻  Developer", R.string.tr_6675c7c21);
        entries.put("No account, ads or location tracking", R.string.tr_e9f5537f0);
        entries.put("Calendar sources & accuracy", R.string.tr_2625b494a);
        entries.put("How Islamic month dates are determined", R.string.tr_b2c4cd11d);
        entries.put("What's new", R.string.tr_135e912e8);
        entries.put("View the latest public release notes", R.string.tr_e72a29ae1);
        entries.put("Open source & feedback", R.string.tr_0865ed3f4);
        entries.put("Code, issues and contributions", R.string.tr_808f656e2);
        entries.put("MIT License", R.string.tr_abb2896dc);
        entries.put("Read the project license", R.string.tr_e135e38ac);
        entries.put("✨ Made with care under the same moon.", R.string.tr_68a5ff1d6);
        entries.put("🌤 Sky and local weather", R.string.tr17_2961a9a599);
        entries.put("The moon and mosque respond to your device time even without location access.", R.string.tr17_c2d5693fc0);
        entries.put("Weather-based sky effects", R.string.tr17_4118f05e26);
        entries.put("Optional: ask for approximate location only while you use the app. Weather data by Open-Meteo (CC BY 4.0).", R.string.tr17_a3cf787004);
        entries.put("Weather not available yet. A time-aware scene is still displayed.", R.string.tr17_d64589d0a3);
        entries.put("Refresh weather now", R.string.tr17_da9a0efccf);
        entries.put("Approximate coordinates are rounded before weather requests and never saved. Disabling this setting clears cached conditions. No background-location permission is requested.", R.string.tr17_3aef63aaaf);
        entries.put("Weather unavailable · time-aware sky shown", R.string.tr17_a65f32e69d);
        entries.put("Allow approximate location in Settings", R.string.tr17_d976168caa);
        entries.put("Clear", R.string.tr17_719ea396ad);
        entries.put("Cloudy", R.string.tr17_44961bce9c);
        entries.put("Foggy", R.string.tr17_88957d8137);
        entries.put("Rainy", R.string.tr17_cb2afce13b);
        entries.put("Snowy", R.string.tr17_9889a6718c);
        entries.put("Thunderstorm", R.string.tr17_320002f337);
        entries.put("Background update notifications", R.string.tr17_63f21863d0);
        entries.put("Android checks GitHub roughly daily, even when this app is closed; you decide when to download and install.", R.string.tr17_2cc18c2fb0);
        entries.put("Download and install update", R.string.tr17_6cf8845e00);
        entries.put("Update Hilal Thursday", R.string.tr17_3e0bf8d605);
        entries.put("Not now", R.string.tr17_e457149073);
        entries.put("Download & install", R.string.tr17_7db0bddc82);
        entries.put("Installing update", R.string.tr17_439f5b6308);
        entries.put("Downloading securely…", R.string.tr17_359f771d8f);
        entries.put("Downloading APK…", R.string.tr17_0a82591963);
        entries.put("Cancel", R.string.tr17_77dfd2135f);
        entries.put("Android installation permission", R.string.tr17_dd26aca1cd);
        entries.put("Open Android settings", R.string.tr17_610e342dea);
        entries.put("Later", R.string.tr17_56e2f5d363);
        entries.put("Approximate location denied; time-aware sky remains available", R.string.tr17_f8b8cfcfff);
        entries.put("Hilal Thursday update available", R.string.tr17_1e434c4650);
        entries.put("No browser is needed.", R.string.tr17_d15c2b37c7);
        entries.put("Weather: Open-Meteo", R.string.tr17_3b425ad7cf);
        entries.put("Newer signed update", R.string.tr17_74c7f34de9);
        entries.put("Optional weather location · no ads or accounts", R.string.tr17_6f266286a2);
        entries.put("Weather visuals and in-app updates", R.string.tr17_717c781cff);
        entries.put("No account, ads, or analytics. Location is requested only when you enable weather visuals, and only while the app is in use. Approximate coordinates are rounded, sent to Open-Meteo, and never stored. The Hijri-date feed and GitHub release checks also contact external services. Each provider can see the connecting IP address.", R.string.tr17_3e3ca4d087);
        entries.put("Hilal Thursday v1.7 adds optional location-based weather scenery, a time-aware moon-and-mosque illustration, background update alerts and secure APK downloads inside the app. Android always confirms installation. Weather data: Open-Meteo (CC BY 4.0).", R.string.tr17_3f090a48a3);
        entries.put("Download version ", R.string.tr17_912d5a7d18);
        entries.put(" securely inside this app? The APK is checked against its SHA-256 digest and the installed signing certificate before Android confirms installation.", R.string.tr17_72c55f8abb);
        entries.put("Allow Hilal Thursday to install app updates in Android settings. Return to this app to continue. No web browser is needed.", R.string.tr17_a5e7a0d079);
        entries.put("Previously downloaded APK is no longer compatible", R.string.tr17_6f2bb9125b);
        entries.put("Verified APKs download inside the app. Android confirms installation and may first require permission to install from this app. Permanent signing allows later stable releases to update in place.", R.string.tr17_adff75831c);
        entries.put(" is ready. Tap to download inside this app.", R.string.tr17_4236381988);
        KEYS = Collections.unmodifiableMap(entries);
    }
    private UiText() { }

    static String tr(Context context, String english) {
        Integer key = KEYS.get(english);
        if (key == null) return english;
        return AppLanguage.wrap(context).getString(key);
    }

    static String fmt(Context context, String template, Object... values) {
        return String.format(AppLanguage.locale(context), tr(context, template), values);
    }

    /** Always marks the displayed day as provisional, even for third-party reports. */
    static String expectedHijriDate(Context context, LunarEngine.Month month, LocalDate civilDate) {
        String day = String.format(AppLanguage.locale(context), "%d", month.hijriDay(civilDate));
        return AppLanguage.wrap(context).getString(
                R.string.expected_hijri_date, day, month(context, month));
    }

    static String date(Context context, LocalDate value, boolean longFormat) {
        String format = longFormat ? "EEEE, d MMMM yyyy" : "d MMM yyyy";
        return DateTimeFormatter.ofPattern(format, AppLanguage.locale(context)).format(value);
    }

    static String dateTime(Context context, java.time.Instant when) {
        return DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", AppLanguage.locale(context))
                .format(when.atZone(LunarEngine.PAKISTAN));
    }

    static String month(Context context, LunarEngine.Month month) {
        if (!AppLanguage.isUrdu(context)) return month.label();
        String[] names = AppLanguage.wrap(context).getResources().getStringArray(R.array.hijri_months);
        if (month.hijriMonth < 1 || month.hijriMonth > names.length) return month.label();
        return names[month.hijriMonth - 1] + " " + month.hijriYear + "ھ";
    }

    /** Convert persisted English month labels for notifications without rewriting the cache. */
    static String monthLabel(Context context, String label) {
        if (label == null) return tr(context, "this lunar month");
        if (!AppLanguage.isUrdu(context)) return label;
        String[] names = AppLanguage.wrap(context).getResources().getStringArray(R.array.hijri_months);
        for (int i = 1; i <= names.length; i++) {
            String month = LunarEngine.monthName(i);
            if (label.startsWith(month + " ") && label.endsWith(" AH")) {
                return names[i - 1] + " " + label.substring(month.length() + 1, label.length() - 3) + "ھ";
            }
        }
        return tr(context, label);
    }
}
