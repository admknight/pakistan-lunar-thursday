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
        Map<String, Integer> KEYS = new HashMap<>();
        KEYS.put("☾  HILAL THURSDAY", R.string.tr_23639ea3a);
        KEYS.put("Hilal Thursday", R.string.tr_9193319ab);
        KEYS.put("Hilal Thursday alarm", R.string.tr_ee6dcaadb);
        KEYS.put("Hilal Thursday alarm was missed", R.string.tr_d5beb0e19);
        KEYS.put("Hilal Thursday test notification", R.string.tr_317ebbade);
        KEYS.put("First Hilal Thursday reminder", R.string.tr_3f0043a99);
        KEYS.put("First Lunar Thursday", R.string.tr_8f2742ccf);
        KEYS.put("Lunar Thursday", R.string.tr_aea4a1df5);
        KEYS.put("First Lunar Thursday reminder", R.string.tr_c0346dc3c);
        KEYS.put("Lunar Thursday alarm was missed", R.string.tr_c49feebf8);
        KEYS.put("Lunar Thursday test notification", R.string.tr_a7ac68052);
        KEYS.put("Lunar Thursday alarm", R.string.tr_44e6395f1);
        KEYS.put("Your next lunar Thursday, at a glance", R.string.tr_00a83a63b);
        KEYS.put("Today in Pakistan: ", R.string.tr_01f44bc97);
        KEYS.put("Monthly reminder • 11:00 AM PKT", R.string.tr_9bb43362e);
        KEYS.put("Settings  ·  alarm & updates", R.string.tr_e0ef1d33f);
        KEYS.put("About  ·  developer & surprises", R.string.tr_06352ba4d);
        KEYS.put("NEXT REMINDER", R.string.tr_8e053a60d);
        KEYS.put("11:00 AM  ·  Pakistan Standard Time", R.string.tr_8c0bda2f7);
        KEYS.put("Tap for the month-start date and source", R.string.tr_c45a5fc04);
        KEYS.put("UPDATE AVAILABLE", R.string.tr_8d46cd4f3);
        KEYS.put("Version ", R.string.tr_03283ea97);
        KEYS.put(" · Preview", R.string.tr_82e4c1c51);
        KEYS.put("Download on GitHub", R.string.tr_ba9728b14);
        KEYS.put("Monthly reminders", R.string.tr_89b23b0f8);
        KEYS.put("Allow notifications", R.string.tr_3ad3c4eb8);
        KEYS.put("Allow precise alarm scheduling", R.string.tr_dbca0d249);
        KEYS.put("Alert mode: ", R.string.tr_164ab02db);
        KEYS.put("Ringing alarm", R.string.tr_cafd11c28);
        KEYS.put("Standard notification", R.string.tr_301587d50);
        KEYS.put("Upcoming lunar months", R.string.tr_1ef5187f3);
        KEYS.put("PMD forecast · not confirmed", R.string.tr_39d9ad26e);
        KEYS.put("Third-party feed reports confirmation", R.string.tr_553dce768);
        KEYS.put("Calendar estimate · not confirmed", R.string.tr_062079591);
        KEYS.put("  •  Tap for details", R.string.tr_b8468f15a);
        KEYS.put("Automatic date updates", R.string.tr_9b5824f8b);
        KEYS.put("No manual date changes. Pakistan Hijri-date feed checks are scheduled about every six hours, subject to Android background limits.", R.string.tr_8c7b688f9);
        KEYS.put("Only explicitly confirmed dates from the third-party provider are cached as reported. Otherwise PMD forecasts or calendar estimates are used, which may differ from Pakistan's official sighting.", R.string.tr_3f1373ff2);
        KEYS.put("Last successful feed check: ", R.string.tr_1b36cb9e2);
        KEYS.put("Not checked online yet", R.string.tr_d1191e3eb);
        KEYS.put("Online date data may be stale. Forecasts are shown until a new check succeeds.", R.string.tr_f58fbf254);
        KEYS.put("Request a date refresh", R.string.tr_97677a3d8);
        KEYS.put("Refresh requested; Android will run it when network is available", R.string.tr_e6dca6297);
        KEYS.put("Enable reminders to allow date checks", R.string.tr_de6925be8);
        KEYS.put("Notifications and updates", R.string.tr_24731f717);
        KEYS.put("Send a test notification", R.string.tr_75c42cd1d);
        KEYS.put("Check notifications", R.string.tr_af9c36846);
        KEYS.put("Open app settings", R.string.tr_6f0a71443);
        KEYS.put("View Pakistan PMD calendar", R.string.tr_c97c92d50);
        KEYS.put("Automatic scheduling and date checks require no login or monthly adjustments.", R.string.tr_78f7af007);
        KEYS.put("Scheduled time has arrived", R.string.tr_ce34430d9);
        KEYS.put("%1$d days, %2$d hours remaining", R.string.tr_3f02015c4);
        KEYS.put("%1$d hours, %2$d minutes remaining", R.string.tr_b1b598c15);
        KEYS.put("%1$d minutes remaining", R.string.tr_17fd4b2db);
        KEYS.put("Lunar month details", R.string.tr_e6e3d3f37);
        KEYS.put("Month: ", R.string.tr_09e0cb995);
        KEYS.put("First civil day (Pakistan): ", R.string.tr_62748ca83);
        KEYS.put("First Thursday: ", R.string.tr_a3be44b12);
        KEYS.put("Reminder: 11:00 AM PKT", R.string.tr_2693fa2db);
        KEYS.put("Source: ", R.string.tr_f59a85c50);
        KEYS.put("Forecasts and third-party reports may differ from the official Ruet-e-Hilal decision.", R.string.tr_196d2da5d);
        KEYS.put("Close", R.string.tr_bbfa773e5);
        KEYS.put("Reminders are paused.", R.string.tr_2ab2b2db2);
        KEYS.put("Allow notifications to receive reminders or ringing alarms.", R.string.tr_004a77be9);
        KEYS.put("Notifications are blocked in Android settings.", R.string.tr_d2dadf8d6);
        KEYS.put("Enabled, but Android may delay delivery without precise alarm access.", R.string.tr_24c60376d);
        KEYS.put("Ringing alarm is selected. Sound, vibration and Snooze are managed in Settings.", R.string.tr_52b98b6ac);
        KEYS.put("Notifications are scheduled automatically in Pakistan time.", R.string.tr_f90b49d82);
        KEYS.put("Enable Alarms & reminders in Android settings", R.string.tr_10eae0ce7);
        KEYS.put("A browser is required", R.string.tr_f2a40225e);
        KEYS.put("Moonlight unlocked", R.string.tr_8e665e894);
        KEYS.put("🌙 Moonlight unlocked", R.string.tr_d4d62ebda);
        KEYS.put("Seven taps, seven skies. Your next Thursday is written in the stars — but the date still comes from the calendar, not the stars.", R.string.tr_4905881fe);
        KEYS.put("Lovely", R.string.tr_2b791f512);
        KEYS.put("Settings", R.string.tr_c7f73bb54);
        KEYS.put("←  Back", R.string.tr_f9c72b159);
        KEYS.put("Alert delivery, permissions and app updates", R.string.tr_d48d6f64c);
        KEYS.put("How reminders reach you", R.string.tr_38fd47dcf);
        KEYS.put("Ring audible alarm", R.string.tr_31177076d);
        KEYS.put("Use a ringing alarm with Dismiss and Snooze, instead of a simple notification.", R.string.tr_f6909a1b7);
        KEYS.put("Play alarm sound", R.string.tr_9ee8e160d);
        KEYS.put("Use your phone's default alarm tone, for up to two minutes.", R.string.tr_bbb6663d7);
        KEYS.put("Vibrate", R.string.tr_9c1391509);
        KEYS.put("Vibration follows the device's vibration policy.", R.string.tr_12b461e73);
        KEYS.put("Test ringing alarm", R.string.tr_f93b9817b);
        KEYS.put("Alarm service unavailable on this device", R.string.tr_ba95d7d71);
        KEYS.put("Full-screen alarm display is not allowed. Android may show a heads-up alert instead. Alarm sound can still play when permitted.", R.string.tr_e324b814d);
        KEYS.put("Allow full-screen alarm display", R.string.tr_895bbef9c);
        KEYS.put("Reminder time remains 11:00 AM Pakistan time. Lunar dates are updated automatically; there is no manual calendar correction.", R.string.tr_c1ebcee85);
        KEYS.put("Android permissions", R.string.tr_b76bd04ef);
        KEYS.put("Notifications: allowed", R.string.tr_61a4aaf1c);
        KEYS.put("Notifications: permission needed", R.string.tr_00fedff2f);
        KEYS.put("Precise alarm scheduling: available", R.string.tr_4d80c320a);
        KEYS.put("Precise alarm scheduling: not available; reminder may be delayed", R.string.tr_a62ad68a8);
        KEYS.put("Allow exact alarms", R.string.tr_d5d9126e0);
        KEYS.put("App updates", R.string.tr_16213f85d);
        KEYS.put("Installed version: ", R.string.tr_8cda36e31);
        KEYS.put("Check automatically", R.string.tr_7cd229d28);
        KEYS.put("Checks GitHub when you open the app, normally at most twice per day.", R.string.tr_2bf4fb109);
        KEYS.put("Stable release signing active. Incompatible debug-signed previews are excluded from update checks.", R.string.tr_34f30058d);
        KEYS.put("Include preview versions", R.string.tr_0b6504c95);
        KEYS.put("Preview APKs use debug signing and may require reinstalling.", R.string.tr_d9a529667);
        KEYS.put("Last checked: ", R.string.tr_92cd2c60f);
        KEYS.put("Never checked", R.string.tr_e738e9bad);
        KEYS.put("No newer release detected", R.string.tr_fc6624bd9);
        KEYS.put(" (not yet checked)", R.string.tr_5c620cc0f);
        KEYS.put("Update available: ", R.string.tr_b968826a0);
        KEYS.put(" (preview)", R.string.tr_6c52dbbbd);
        KEYS.put("Download update on GitHub", R.string.tr_ea60c65e6);
        KEYS.put("Checking GitHub...", R.string.tr_3605d841a);
        KEYS.put("Check for updates now", R.string.tr_cca542e49);
        KEYS.put("Update check already running", R.string.tr_a8cfdf4e5);
        KEYS.put("Checking GitHub releases", R.string.tr_0005bdb96);
        KEYS.put("Android always asks before installing APK updates. Moving from older debug-signed preview builds to a signed release requires uninstalling the preview first; future releases with the same signing key can update in place.", R.string.tr_c4f1ae264);
        KEYS.put("A browser is needed to open the release", R.string.tr_752280813);
        KEYS.put("Open notification settings in Android", R.string.tr_d0125779a);
        KEYS.put("Open Android Settings > Alarms & reminders", R.string.tr_b979c6881);
        KEYS.put("Full-screen alarm access can be managed in Android settings", R.string.tr_b54c2a250);
        KEYS.put("About Hilal Thursday", R.string.tr_e9fae6216);
        KEYS.put("About / Developer", R.string.tr_5803fbc4e);
        KEYS.put("A quieter way to remember the rhythm of Pakistan's lunar months.", R.string.tr_0047ae94e);
        KEYS.put("Made for Pakistan, open to everyone", R.string.tr_24b907f39);
        KEYS.put("Developer / maintainer: admknight", R.string.tr_b4ab1852a);
        KEYS.put("An open-source project. Contributions and issue reports are welcome on GitHub.", R.string.tr_ba6fed22b);
        KEYS.put("Source code and issues", R.string.tr_03366c77b);
        KEYS.put("Download releases", R.string.tr_1aaa27f9a);
        KEYS.put("How reminders work", R.string.tr_f68ab1a99);
        KEYS.put("The reminder is scheduled for 11:00 AM Pakistan Standard Time on the first Thursday on or after each new Hijri month's first civil day.", R.string.tr_5d63b73c0);
        KEYS.put("Official Pakistani moon sighting is determined by the Central Ruet-e-Hilal Committee. The app uses third-party reported dates when available and PMD forecasts or calculated estimates otherwise. It does not claim a government-approved date feed.", R.string.tr_6ad863e01);
        KEYS.put("Privacy", R.string.tr_cf01481f6);
        KEYS.put("No account, ads, location request, or app analytics. The phone checks a public Hijri-date feed and GitHub's Releases API; those services can see the connecting IP address.", R.string.tr_ebd783954);
        KEYS.put("Credits and transparency", R.string.tr_d235418b3);
        KEYS.put("Pakistan Meteorological Department (forecast references), public third-party Hijri-date service, Android Material 3 / Material You, and community contributors.", R.string.tr_a4d7b4050);
        KEYS.put("Open project license (MIT)", R.string.tr_367b81c35);
        KEYS.put("  ·  tap for details", R.string.tr_5f91c2744);
        KEYS.put("Made with care under the same moon.", R.string.tr_64e319269);
        KEYS.put("✦ Constellation unlocked ✦", R.string.tr_4d999ebe9);
        KEYS.put("The night sky has no update button, but your reminder app does.", R.string.tr_d871233bf);
        KEYS.put("Back to Earth", R.string.tr_c94f17a39);
        KEYS.put("🌟 Five stars", R.string.tr_26ab89cd8);
        KEYS.put("A tiny secret from the developer: some dates need patience, but a gentle reminder can still brighten the day.", R.string.tr_3f40f4683);
        KEYS.put("Shine on", R.string.tr_7d2f163e8);
        KEYS.put("Dismiss alarm", R.string.tr_7d5b577f6);
        KEYS.put("Snooze 10 minutes", R.string.tr_f36436329);
        KEYS.put("Test alarm", R.string.tr_30e977abc);
        KEYS.put("11:00 AM Pakistan time", R.string.tr_59feb6cf1);
        KEYS.put("Dismiss", R.string.tr_70afe9eff);
        KEYS.put("Snooze 10 min", R.string.tr_3f5595d00);
        KEYS.put("Ringing lunar alarms", R.string.tr_625a5311e);
        KEYS.put("Optional ringing alarm. Sound and vibration are controlled inside the app.", R.string.tr_a3111f6e0);
        KEYS.put("this lunar month", R.string.tr_6eb499e3e);
        KEYS.put("Live date provider reports confirmation.", R.string.tr_7df0179ba);
        KEYS.put("Calendar estimate; official sighting may differ.", R.string.tr_0bb2128a2);
        KEYS.put("Notifications are enabled. Monthly reminders target 11:00 AM PKT.", R.string.tr_5ca78f773);
        KEYS.put("One reminder on the first Thursday of each lunar month in Pakistan time", R.string.tr_18e0e1489);
        KEYS.put("The ringing alarm stopped after two minutes. Open the app to review your reminder.", R.string.tr_e49e6ce0b);
        KEYS.put("Update check already in progress", R.string.tr_4ce8b7133);
        KEYS.put("No newer compatible release found", R.string.tr_133278b92);
        KEYS.put("New version available: ", R.string.tr_b493ac133);
        KEYS.put("Could not check GitHub. Check your connection or try later.", R.string.tr_6f0c4c4f3);
        KEYS.put("Language", R.string.tr_89b86ab0e);
        KEYS.put("App language", R.string.tr_01e1787a9);
        KEYS.put("Choose the language used for the app screens and reminders.", R.string.tr_c264249ca);
        KEYS.put("Follow device language", R.string.tr_fe390c37a);
        KEYS.put("English", R.string.tr_649df08a4);
        KEYS.put("Urdu", R.string.tr_412e31909);
        KEYS.put("اردو", R.string.tr_482f6e668);
        KEYS.put("Language changed", R.string.tr_e05882ae7);
        KEYS.put("11:00 AM PKT", R.string.tr_a049d931d);
        KEYS.put("PMD published forecast - not a confirmed sighting", R.string.tr_f49025ed2);
        KEYS.put("Live provider reports confirmation (not a government API)", R.string.tr_1f044de6c);
        KEYS.put("Calculated estimate - NOT a Pakistan sighting confirmation", R.string.tr_c45d4724f);
        KEYS.put(" 11:00 AM Pakistan time. ", R.string.tr_2740d8755);
        KEYS.put(" at 11:00 AM Pakistan time. ", R.string.tr_14d624084);
        KEYS.put(": the ringing alarm stopped after two minutes. Open the app to review your reminder.", R.string.tr_2b2c8ff92);
        KEYS.put(" · 11:00 AM Pakistan time", R.string.tr_30abb2248);
        KEYS.put("       ✦           ·\n  ·       ☾         ✧\n       ✧       ✦\n\nThe night sky has no update button, but your reminder app does.", R.string.tr_28efa34c4);
        KEYS.put("A tiny secret from the developer: some dates need patience, but a gentle reminder can still brighten the day.\n\n🌙  ✦  ✧  ✦  🌙", R.string.tr_8cc4c465e);
        UiText.KEYS = Collections.unmodifiableMap(KEYS);
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
