package com.scopewise.lunarthursday;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** User preferences, permission guidance, and the release update manager. No date editing. */
public final class SettingsActivity extends LocalizedActivity {
    private static final int NOTIFICATION_REQUEST = 85;
    private static final int WEATHER_LOCATION_REQUEST = 86;
    private SharedPreferences p;
    private int primary;
    private int onPrimary;
    private int surface;
    private int onSurface;
    private int onSurfaceVariant;
    private int primaryContainer;
    private int surfaceVariant;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        DynamicColors.applyToActivityIfAvailable(this);
        p = ReminderStore.prefs(this);
        render();
        if (getIntent().getBooleanExtra("open_language_dialog", false)) {
            getIntent().removeExtra("open_language_dialog");
            new android.os.Handler(android.os.Looper.getMainLooper()).post(this::chooseLanguage);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (p != null) render();
        AppUpdateInstaller.resumeIfPermitted(this);
    }

    private void render() {
        primary = col(androidx.appcompat.R.attr.colorPrimary, 0xFF136F53);
        onPrimary = col(com.google.android.material.R.attr.colorOnPrimary, 0xFFFFFFFF);
        surface = col(com.google.android.material.R.attr.colorSurface, 0xFFFFFFFF);
        onSurface = col(com.google.android.material.R.attr.colorOnSurface, 0xFF1B211E);
        onSurfaceVariant = col(com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF414943);
        primaryContainer = col(com.google.android.material.R.attr.colorPrimaryContainer, 0xFFBCEAD3);
        surfaceVariant = col(com.google.android.material.R.attr.colorSurfaceVariant, 0xFFE1E8E2);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(surface);
        LinearLayout body = column();
        body.setPadding(dp(20), dp(32), dp(20), dp(44));
        scroll.addView(body);
        setContentView(scroll);

        add(body, slimAction("←", "Back", this::finish), 0);
        add(body, text("Settings", 28, true, onSurface), 9);
        add(body, slimAction("🌐", "App language", this::chooseLanguage), 10);
        add(body, text("Choose the language used for the app screens and reminders.", 12, false, onSurfaceVariant), 5);
        add(body, text("Alert delivery, permissions and app updates", 14, false, onSurfaceVariant), 4);

        LinearLayout alert = panel();
        add(alert, text("How reminders reach you", 19, true, onSurface), 0);
        add(alert, slimAction("⏰", getString(R.string.hijri_reminder_time)
                + " · " + UiText.reminderTimeText(this) + " PKT",
                this::chooseReminderTime), 9);
        add(alert, text(getString(R.string.hijri_time_note), 12, false, onSurfaceVariant), 5);
        boolean ringing = p.getBoolean(ReminderStore.ALARM_MODE, false);
        toggle(alert, "Ring audible alarm", "Use a ringing alarm with Dismiss and Snooze, instead of a simple notification.",
                ringing, value -> {
                    p.edit().putBoolean(ReminderStore.ALARM_MODE, value).apply();
                    render();
                });
        if (ringing) {
            toggle(alert, "Play alarm sound", getString(R.string.tone_sound_help),
                    p.getBoolean(ReminderStore.ALARM_SOUND, true), value ->
                            p.edit().putBoolean(ReminderStore.ALARM_SOUND, value).apply());
            String selectedTone = AlarmTonePlayer.selection(this);
            int toneName = AlarmTonePolicy.SUKOON.equals(selectedTone) ? R.string.tone_sukoon
                    : AlarmTonePolicy.NIDA.equals(selectedTone) ? R.string.tone_nida
                    : AlarmTonePolicy.PHONE.equals(selectedTone) ? R.string.tone_phone : R.string.tone_noor;
            add(alert, slimAction("♪", getString(R.string.tone_heading) + " · " + getString(toneName),
                    () -> startActivity(new Intent(this, AlarmToneActivity.class))), 9);
            toggle(alert, "Vibrate", "Vibration follows the device's vibration policy.",
                    p.getBoolean(ReminderStore.ALARM_VIBRATE, true), value ->
                            p.edit().putBoolean(ReminderStore.ALARM_VIBRATE, value).apply());
            add(alert, slimAction("🔔", "Test ringing alarm", () -> {
                if (!ReminderNotifications.permitted(this)) { askNotifications(); return; }
                if (!AlarmRingingService.startTest(this)) toast("Alarm service unavailable on this device");
            }), 16);
            if (Build.VERSION.SDK_INT >= 34 && !AlarmRingingService.canUseFullScreen(this)) {
                add(alert, text("Full-screen alarm display is not allowed. Android may show a heads-up alert instead. Alarm sound can still play when permitted.",
                        13, false, onSurfaceVariant), 12);
                add(alert, button("Allow full-screen alarm display", false, this::askFullScreen), 8);
            }
        }
        add(alert, text(getString(R.string.hijri_first_thursday_note),
                13, false, onSurfaceVariant), 16);
        add(body, card(alert), 22);

        LinearLayout observances = panel();
        add(observances, text(getString(R.string.events_settings_title), 19, true, onSurface), 0);
        add(observances, text(getString(R.string.events_settings_help), 13, false, onSurfaceVariant), 6);
        toggle(observances, getString(R.string.events_set_shared), getString(R.string.events_set_shared_help),
                EventsStore.isShown(this, IslamicEvents.SET_SHARED),
                value -> EventsStore.setShown(this, IslamicEvents.SET_SHARED, value));
        toggle(observances, getString(R.string.events_set_a), getString(R.string.events_set_a_help),
                EventsStore.isShown(this, IslamicEvents.SET_A),
                value -> EventsStore.setShown(this, IslamicEvents.SET_A, value));
        toggle(observances, getString(R.string.events_set_b), getString(R.string.events_set_b_help),
                EventsStore.isShown(this, IslamicEvents.SET_B),
                value -> EventsStore.setShown(this, IslamicEvents.SET_B, value));
        toggle(observances, getString(R.string.events_remind), getString(R.string.events_remind_help),
                EventsStore.remindersOn(this), value -> EventsStore.setRemindersOn(this, value));
        add(body, card(observances), 16);

        LinearLayout extras = panel();
        add(extras, text(getString(R.string.extra_title), 19, true, onSurface), 0);
        toggle(extras, getString(R.string.extra_prayer_toggle), getString(R.string.extra_prayer_help),
                ExtraReminderScheduler.isOn(this, ExtraReminderScheduler.PRAYER_ALERTS),
                value -> ExtraReminderScheduler.setOn(this, ExtraReminderScheduler.PRAYER_ALERTS, value));
        toggle(extras, getString(R.string.extra_thursday_toggle), getString(R.string.extra_thursday_help),
                ExtraReminderScheduler.isOn(this, ExtraReminderScheduler.THURSDAY_EVENING),
                value -> ExtraReminderScheduler.setOn(this, ExtraReminderScheduler.THURSDAY_EVENING, value));
        toggle(extras, getString(R.string.extra_friday_toggle), getString(R.string.extra_friday_help),
                ExtraReminderScheduler.isOn(this, ExtraReminderScheduler.FRIDAY),
                value -> ExtraReminderScheduler.setOn(this, ExtraReminderScheduler.FRIDAY, value));
        add(body, card(extras), 16);

        LinearLayout announcement = panel();
        add(announcement, text(getString(R.string.announcement_title), 19, true, onSurface), 0);
        toggle(announcement, getString(R.string.announcement_toggle), getString(R.string.announcement_help),
                AnnouncementSync.isOn(this), value -> AnnouncementSync.setOn(this, value));
        add(body, card(announcement), 16);

        LinearLayout permissions = panel();
        add(permissions, text("Android permissions", 19, true, onSurface), 0);
        add(permissions, text(ReminderNotifications.permitted(this) ? "Notifications: allowed" : "Notifications: permission needed",
                14, false, onSurfaceVariant), 10);
        if (!ReminderNotifications.permitted(this)) add(permissions, button("Allow notifications", false, this::askNotifications), 10);
        add(permissions, text(ReminderScheduler.exactAlarmsAllowed(this) ? "Precise alarm scheduling: available"
                        : "Precise alarm scheduling: not available; reminder may be delayed",
                14, false, onSurfaceVariant), 10);
        if (!ReminderScheduler.exactAlarmsAllowed(this)) add(permissions, button("Allow exact alarms", false, this::askExactAlarms), 10);
        add(body, card(permissions), 16);

        LinearLayout hijriTiming = panel();
        add(hijriTiming, text(getString(R.string.hijri_clock_section),
                19, true, onSurface), 0);
        int cityIndex = HijriClock.selectedCity(this);
        String[] cityLabels = HijriClock.cityLabels(this);
        String cityName = cityIndex < cityLabels.length ? cityLabels[cityIndex] : cityLabels[0];
        add(hijriTiming, slimAction("🌇", getString(R.string.hijri_select_city)
                + " · " + cityName, this::chooseHijriCity), 10);
        add(hijriTiming, text(getString(R.string.hijri_clock_explanation),
                12, false, onSurfaceVariant), 6);
        add(hijriTiming, slimAction("🕌", getString(R.string.prayer_method_title) + " · "
                + getString(PrayerStore.method(this) == PrayerTimes.Method.JAFARI
                        ? R.string.prayer_method_jafari : R.string.prayer_method_karachi),
                this::choosePrayerMethod), 12);
        add(hijriTiming, text(getString(R.string.prayer_method_help), 12, false, onSurfaceVariant), 6);
        add(body, card(hijriTiming), 16);

        LinearLayout scenes = panel();
        add(scenes, text("🌤 Sky and local weather", 19, true, onSurface), 0);
        add(scenes, text("The moon and mosque respond to your device time even without location access.",
                13, false, onSurfaceVariant), 8);
        toggle(scenes, "Weather-based sky effects", "Optional: ask for approximate location only while you use the app. Weather data by Open-Meteo (CC BY 4.0).",
                WeatherManager.enabled(this), enabled -> {
                    if (!enabled) { WeatherManager.disable(this); render(); return; }
                    if (WeatherManager.permissionGranted(this)) {
                        p.edit().putBoolean(WeatherManager.ENABLED, true).apply();
                        WeatherManager.refresh(this, true, this::render);
                        render();
                    } else {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, WEATHER_LOCATION_REQUEST);
                    }
                });
        if (WeatherManager.enabled(this)) {
            WeatherManager.Scene scene = WeatherManager.current(this);
            add(scenes, text(scene == null ? "Weather not available yet. A time-aware scene is still displayed."
                    : scene.description(this), 13, false, onSurfaceVariant), 7);
            add(scenes, slimAction("↻", "Refresh weather now", () -> {
                WeatherManager.refresh(this, true, this::render);
            }), 8);
        }
        add(scenes, text("Approximate coordinates are rounded before weather requests and never saved. Disabling this setting clears cached conditions. No background-location permission is requested.",
                12, false, onSurfaceVariant), 8);
        add(body, card(scenes), 16);

        LinearLayout updates = panel();
        add(updates, text("App updates", 19, true, onSurface), 0);
        add(updates, text(UiText.tr(this, "Installed version: ") + UpdateManager.installed(this), 14, false, onSurfaceVariant), 10);
        toggle(updates, "Check automatically", "Checks GitHub when you open the app, normally at most twice per day.",
                p.getBoolean(UpdateManager.AUTO_CHECK, true), value -> {
                    p.edit().putBoolean(UpdateManager.AUTO_CHECK, value).apply();
                    UpdateCheckScheduler.ensure(this);
                });
        toggle(updates, "Background update notifications", "Android checks GitHub roughly daily, even when this app is closed; you decide when to download and install.",
                p.getBoolean(UpdateManager.BACKGROUND_CHECK, true), value -> {
                    p.edit().putBoolean(UpdateManager.BACKGROUND_CHECK, value).apply();
                    UpdateCheckScheduler.ensure(this);
                });
        if (UpdateManager.isReleaseBuild(this)) {
            add(updates, text("Stable release signing active. Incompatible debug-signed previews are excluded from update checks.",
                    12, false, onSurfaceVariant), 10);
        } else {
            toggle(updates, "Include preview versions", "Preview APKs use debug signing and may require reinstalling.",
                    p.getBoolean(UpdateManager.INCLUDE_PREVIEWS, true), value -> {
                        p.edit().putBoolean(UpdateManager.INCLUDE_PREVIEWS, value).apply();
                        UpdateManager.check(this, true, (ok, message) -> { toast(message); render(); });
                        render();
                    });
        }
        long last = UpdateManager.lastChecked(this);
        String label = last <= 0 ? "Never checked"
                : Instant.ofEpochMilli(last).atZone(LunarEngine.PAKISTAN)
                        .format(DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", AppLanguage.locale(this))) + " PKT";
        add(updates, text(UiText.tr(this, "Last checked: ") + label, 12, false, onSurfaceVariant), 10);
        UpdateManager.Result available = UpdateManager.available(this);
        if (available == null) {
            add(updates, text(UiText.tr(this, "No newer release detected") + (last <= 0 ? UiText.tr(this, " (not yet checked)") : ""),
                    14, true, onSurface), 8);
        } else {
            add(updates, text(UiText.tr(this, "Update available: ") + VersionPolicy.display(available.tag)
                            + (available.preview ? UiText.tr(this, " (preview)") : ""), 16, true, primary), 12);
            add(updates, slimAction("⬇", "Download and install update", () -> AppUpdateInstaller.prompt(this, available)), 8);
        }
        add(updates, slimAction("↻", UpdateManager.isChecking() ? "Checking GitHub..." : "Check for updates now", () -> {
            if (UpdateManager.isChecking()) { toast("Update check already running"); return; }
            UpdateManager.check(this, true, (ok, message) -> { toast(message); render(); });
            toast("Checking GitHub releases");
        }), 12);
        add(updates, text("Verified APKs download inside the app. Android confirms installation and may first require permission to install from this app. Permanent signing allows later stable releases to update in place.",
                12, false, onSurfaceVariant), 12);
        add(body, card(updates), 16);
    }

    /** Tappable icon row; keeps the screen scannable without large button blocks. */
    private LinearLayout slimAction(String emblem, String title, Runnable operation) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(7), dp(8), dp(7), dp(8));
        TextView symbol = text(emblem, 22, false, primary);
        symbol.setGravity(android.view.Gravity.CENTER);
        row.addView(symbol, new LinearLayout.LayoutParams(dp(44), dp(40)));
        TextView name = text(title, 15, true, onSurface);
        row.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(text("›", 22, false, onSurfaceVariant));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> operation.run());
        return row;
    }

    private interface Changed { void set(boolean enabled); }
    private void toggle(LinearLayout parent, String title, String help, boolean checked, Changed changed) {
        MaterialSwitch control = new MaterialSwitch(this);
        control.setText(UiText.tr(this, title));
        control.setTextColor(onSurface);
        control.setTextSize(16);
        control.setChecked(checked);
        add(parent, control, 14);
        add(parent, text(help, 12, false, onSurfaceVariant), 0);
        control.setOnCheckedChangeListener((button, value) -> changed.set(value));
    }

    private void chooseReminderTime() {
        LocalTime current = ReminderStore.reminderTime(this);
        new android.app.TimePickerDialog(this, (picker, hour, minute) -> {
            if (ReminderStore.setReminderTime(this, hour, minute)) {
                ReminderScheduler.sync(this); // Cancel old time, reschedule first Thursday.
                DateSyncScheduler.ensure(this); // Update the pre-reminder date refresh window.
                render();
            } else {
                toast("Unable to save reminder time");
            }
        }, current.getHour(), current.getMinute(),
                android.text.format.DateFormat.is24HourFormat(this)).show();
    }

    private void choosePrayerMethod() {
        PrayerTimes.Method[] methods = {PrayerTimes.Method.KARACHI_HANAFI, PrayerTimes.Method.JAFARI};
        String[] names = {getString(R.string.prayer_method_karachi), getString(R.string.prayer_method_jafari)};
        int current = PrayerStore.method(this) == PrayerTimes.Method.JAFARI ? 1 : 0;
        new android.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.prayer_method_title))
                .setSingleChoiceItems(names, current, (dialog, index) -> {
                    PrayerStore.setMethod(this, methods[index]);
                    ExtraReminderScheduler.sync(this); // Prayer alerts follow the method.
                    dialog.dismiss();
                    render();
                })
                .setNegativeButton(UiText.tr(this, "Close"), null)
                .show();
    }

    private void chooseHijriCity() {
        String[] names = HijriClock.cityLabels(this);
        new android.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.hijri_select_city))
                .setSingleChoiceItems(names, HijriClock.selectedCity(this), (dialog, index) -> {
                    HijriClock.selectCity(this, index);
                    ExtraReminderScheduler.sync(this); // Prayer alerts follow the city.
                    DateSyncScheduler.ensure(this); // Recalculate month-end Maghrib check times.
                    dialog.dismiss();
                    render();
                })
                .setNegativeButton(UiText.tr(this, "Close"), null)
                .show();
    }

    private void chooseLanguage() {
        String[] names = new String[]{
                UiText.tr(this, "Follow device language"), "English", "اردو"
        };
        String current = AppLanguage.choice(this);
        int selected = "ur".equals(current) ? 2 : "en".equals(current) ? 1 : 0;
        new android.app.AlertDialog.Builder(this)
                .setTitle(UiText.tr(this, "App language"))
                .setSingleChoiceItems(names, selected, (dialog, index) -> {
                    String code = index == 2 ? "ur" : index == 1 ? "en" : "";
                    dialog.dismiss();
                    if (!code.equals(AppLanguage.choice(this))) {
                        AppLanguage.select(this, code);
                        Intent restart = new Intent(this, MainActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(restart);
                        finish();
                    }
                })
                .setNegativeButton(UiText.tr(this, "Close"), null)
                .show();
    }

    private void askNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && !ReminderNotifications.permitted(this)) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQUEST);
        } else {
            try { startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())); }
            catch (RuntimeException error) { toast("Open notification settings in Android"); }
        }
    }

    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(code, permissions, grants);
        if (code == NOTIFICATION_REQUEST) render();
        if (code == WEATHER_LOCATION_REQUEST) {
            if (WeatherManager.permissionGranted(this)) {
                p.edit().putBoolean(WeatherManager.ENABLED, true).apply();
                WeatherManager.refresh(this, true, this::render);
            } else {
                WeatherManager.disable(this);
                toast("Approximate location denied; time-aware sky remains available");
            }
            render();
        }
    }

    private void askExactAlarms() {
        if (Build.VERSION.SDK_INT < 31) return;
        try { startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .setData(Uri.parse("package:" + getPackageName()))); }
        catch (RuntimeException unavailable) { toast("Open Android Settings > Alarms & reminders"); }
    }

    private void askFullScreen() {
        if (Build.VERSION.SDK_INT < 34) return;
        try { startActivity(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                .setData(Uri.parse("package:" + getPackageName()))); }
        catch (RuntimeException unavailable) { toast("Full-screen alarm access can be managed in Android settings"); }
    }

    private void open(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (RuntimeException ex) { toast("A browser is needed to open the release"); }
    }

    private int col(int attr, int fallback) { return MaterialColors.getColor(this, attr, fallback); }
    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }
    private LinearLayout panel() {
        LinearLayout p = column();
        p.setPadding(dp(18), dp(18), dp(18), dp(18));
        return p;
    }
    private MaterialCardView card(LinearLayout content) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(24));
        card.setCardElevation(0f);
        card.setCardBackgroundColor(surfaceVariant);
        card.addView(content);
        return card;
    }
    private TextView text(String value, int size, boolean bold, int color) {
        TextView v = new TextView(this);
        v.setText(UiText.tr(this, value));
        v.setTextSize(size);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }
    private MaterialButton button(String value, boolean primaryButton, Runnable run) {
        MaterialButton button = new MaterialButton(this);
        button.setText(UiText.tr(this, value));
        button.setAllCaps(false);
        button.setMinHeight(dp(48));
        button.setCornerRadius(dp(18));
        button.setBackgroundTintList(ColorStateList.valueOf(primaryButton ? primary : primaryContainer));
        button.setTextColor(primaryButton ? onPrimary : onSurface);
        button.setOnClickListener(v -> run.run());
        return button;
    }
    private void add(LinearLayout parent, View view, int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(margin);
        parent.addView(view, params);
    }
    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + .5f); }
    private void toast(String text) { Toast.makeText(this, UiText.tr(this, text), Toast.LENGTH_LONG).show(); }
}
