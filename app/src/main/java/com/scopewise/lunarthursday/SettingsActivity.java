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
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** User preferences, permission guidance, and the release update manager. No date editing. */
public final class SettingsActivity extends Activity {
    private static final int NOTIFICATION_REQUEST = 85;
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
    }

    @Override protected void onResume() {
        super.onResume();
        if (p != null) render();
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

        add(body, button("←  Back", false, this::finish), 0);
        add(body, text("Settings", 28, true, onSurface), 18);
        add(body, text("Alert delivery, permissions and app updates", 14, false, onSurfaceVariant), 4);

        LinearLayout alert = panel();
        add(alert, text("How reminders reach you", 19, true, onSurface), 0);
        boolean ringing = p.getBoolean(ReminderStore.ALARM_MODE, false);
        toggle(alert, "Ring audible alarm", "Use a ringing alarm with Dismiss and Snooze, instead of a simple notification.",
                ringing, value -> {
                    p.edit().putBoolean(ReminderStore.ALARM_MODE, value).apply();
                    render();
                });
        if (ringing) {
            toggle(alert, "Play alarm sound", "Use your phone's default alarm tone, for up to two minutes.",
                    p.getBoolean(ReminderStore.ALARM_SOUND, true), value ->
                            p.edit().putBoolean(ReminderStore.ALARM_SOUND, value).apply());
            toggle(alert, "Vibrate", "Vibration follows the device's vibration policy.",
                    p.getBoolean(ReminderStore.ALARM_VIBRATE, true), value ->
                            p.edit().putBoolean(ReminderStore.ALARM_VIBRATE, value).apply());
            add(alert, button("Test ringing alarm", true, () -> {
                if (!ReminderNotifications.permitted(this)) { askNotifications(); return; }
                if (!AlarmRingingService.startTest(this)) toast("Alarm service unavailable on this device");
            }), 16);
            if (Build.VERSION.SDK_INT >= 34 && !AlarmRingingService.canUseFullScreen(this)) {
                add(alert, text("Full-screen alarm display is not allowed. Android may show a heads-up alert instead. Alarm sound can still play when permitted.",
                        13, false, onSurfaceVariant), 12);
                add(alert, button("Allow full-screen alarm display", false, this::askFullScreen), 8);
            }
        }
        add(alert, text("Reminder time remains 11:00 AM Pakistan time. Lunar dates are updated automatically; there is no manual calendar correction.",
                13, false, onSurfaceVariant), 16);
        add(body, card(alert), 22);

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

        LinearLayout updates = panel();
        add(updates, text("App updates", 19, true, onSurface), 0);
        add(updates, text("Installed version: " + UpdateManager.installed(this), 14, false, onSurfaceVariant), 10);
        toggle(updates, "Check automatically", "Checks GitHub when you open the app, normally at most twice per day.",
                p.getBoolean(UpdateManager.AUTO_CHECK, true), value ->
                        p.edit().putBoolean(UpdateManager.AUTO_CHECK, value).apply());
        toggle(updates, "Include preview versions", "Preview APKs may use different debug signing keys; an update may require reinstalling.",
                p.getBoolean(UpdateManager.INCLUDE_PREVIEWS, true), value -> {
                    p.edit().putBoolean(UpdateManager.INCLUDE_PREVIEWS, value).apply();
                    UpdateManager.check(this, true, (ok, message) -> { toast(message); render(); });
                    render();
                });
        long last = UpdateManager.lastChecked(this);
        String label = last <= 0 ? "Never checked"
                : Instant.ofEpochMilli(last).atZone(LunarEngine.PAKISTAN)
                        .format(DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH)) + " PKT";
        add(updates, text("Last checked: " + label, 12, false, onSurfaceVariant), 10);
        UpdateManager.Result available = UpdateManager.available(this);
        if (available == null) {
            add(updates, text("No newer release detected" + (last <= 0 ? " (not yet checked)" : ""),
                    14, true, onSurface), 8);
        } else {
            add(updates, text("Update available: " + VersionPolicy.display(available.tag)
                            + (available.preview ? " (preview)" : ""), 16, true, primary), 12);
            add(updates, button("Download update on GitHub", true, () -> open(available.url)), 8);
        }
        add(updates, button(UpdateManager.isChecking() ? "Checking GitHub..." : "Check for updates now", false, () -> {
            if (UpdateManager.isChecking()) { toast("Update check already running"); return; }
            UpdateManager.check(this, true, (ok, message) -> { toast(message); render(); });
            toast("Checking GitHub releases");
        }), 12);
        add(updates, text("Android requires your confirmation to install APK updates. Preview builds are debug-signed and may not install over previous builds.",
                12, false, onSurfaceVariant), 12);
        add(body, card(updates), 16);
    }

    private interface Changed { void set(boolean enabled); }
    private void toggle(LinearLayout parent, String title, String help, boolean checked, Changed changed) {
        MaterialSwitch control = new MaterialSwitch(this);
        control.setText(title);
        control.setTextColor(onSurface);
        control.setTextSize(16);
        control.setChecked(checked);
        add(parent, control, 14);
        add(parent, text(help, 12, false, onSurfaceVariant), 0);
        control.setOnCheckedChangeListener((button, value) -> changed.set(value));
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
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }
    private MaterialButton button(String value, boolean primaryButton, Runnable run) {
        MaterialButton button = new MaterialButton(this);
        button.setText(value);
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
    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_LONG).show(); }
}
