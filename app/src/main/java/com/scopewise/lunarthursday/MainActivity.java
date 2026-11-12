package com.scopewise.lunarthursday;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
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
import java.util.List;
import java.util.Locale;

/** Read-only monthly schedule; month-start corrections are always automatic. */
public final class MainActivity extends Activity {
    private static final int REQUEST_NOTIFICATIONS = 44;
    private static final DateTimeFormatter DATE_LONG = DateTimeFormatter.ofPattern(
            "EEEE, d MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_SHORT = DateTimeFormatter.ofPattern(
            "d MMM yyyy", Locale.ENGLISH);
    private SharedPreferences preferences;
    private int primary;
    private int onPrimary;
    private int surface;
    private int onSurface;
    private int surfaceVariant;
    private int onSurfaceVariant;
    private int primaryContainer;
    private int onPrimaryContainer;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        // Wallpaper-based Material You palette on Android 12+; Material 3 DayNight elsewhere.
        DynamicColors.applyToActivityIfAvailable(this);
        preferences = ReminderStore.prefs(this);
        if (!preferences.contains(ReminderStore.ENABLED)) {
            preferences.edit().putBoolean(ReminderStore.ENABLED, true).apply();
        }
        ReminderNotifications.ensureChannel(this);
        ReminderScheduler.sync(this);
        DateSyncScheduler.ensure(this);
        if (Build.VERSION.SDK_INT >= 33) requestNotificationPermission();
        showScreen();
    }

    @Override protected void onResume() {
        super.onResume();
        if (preferences != null) {
            ReminderScheduler.sync(this);
            DateSyncScheduler.ensure(this);
            showScreen();
        }
    }

    private void applyPalette() {
        primary = color(com.google.android.material.R.attr.colorPrimary, 0xFF136F53);
        onPrimary = color(com.google.android.material.R.attr.colorOnPrimary, 0xFFFFFFFF);
        surface = color(com.google.android.material.R.attr.colorSurface, 0xFFFFFFFF);
        onSurface = color(com.google.android.material.R.attr.colorOnSurface, 0xFF1B211E);
        surfaceVariant = color(com.google.android.material.R.attr.colorSurfaceVariant, 0xFFE1E8E2);
        onSurfaceVariant = color(com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF414943);
        primaryContainer = color(com.google.android.material.R.attr.colorPrimaryContainer, 0xFFBCEAD3);
        onPrimaryContainer = color(com.google.android.material.R.attr.colorOnPrimaryContainer, 0xFF002114);
    }

    private void showScreen() {
        applyPalette();
        boolean enabled = preferences.getBoolean(ReminderStore.ENABLED, false);
        List<LunarEngine.Month> upcoming = LunarEngine.upcoming(
                Instant.now(), ReminderStore.confirmedStarts(this), 6);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(surface);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(20), dp(20), dp(20), dp(36));
        scroll.addView(body);
        if (Build.VERSION.SDK_INT >= 30) {
            scroll.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                body.setPadding(dp(20), dp(20) + bars.top,
                        dp(20), dp(36) + bars.bottom);
                return insets;
            });
        } else {
            scroll.setFitsSystemWindows(true);
        }
        setContentView(scroll);

        add(body, label("☾  LUNAR THURSDAY", 14, true, primary), 0);
        add(body, label("A little reminder, every new lunar month", 26, true, onSurface), 8);
        add(body, label("Pakistan • First Thursday • 11:00 AM PKT", 14,
                false, onSurfaceVariant), 8);

        if (!upcoming.isEmpty()) {
            LunarEngine.Month next = upcoming.get(0);
            LinearLayout hero = panel(primaryContainer);
            add(hero, label("NEXT REMINDER", 12, true, onPrimaryContainer), 0);
            add(hero, label(next.thursday().format(DATE_LONG), 24, true,
                    onPrimaryContainer), 12);
            add(hero, label("11:00 AM  ·  Asia/Karachi", 16, true,
                    onPrimaryContainer), 8);
            add(hero, label(next.label(), 16, false, onPrimaryContainer), 12);
            add(hero, label(LunarEngine.basisDescription(next.basis), 12,
                    false, onPrimaryContainer), 8);
            add(body, card(hero, primaryContainer), 22);
        }

        LinearLayout state = panel(surfaceVariant);
        MaterialSwitch toggle = new MaterialSwitch(this);
        toggle.setText("Monthly reminders");
        toggle.setTextSize(16);
        toggle.setTextColor(onSurface);
        toggle.setTypeface(null, Typeface.BOLD);
        toggle.setChecked(enabled);
        state.addView(toggle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        add(state, label(statusMessage(enabled), 13, false, onSurfaceVariant), 8);
        if (enabled && !ReminderNotifications.permitted(this)) {
            add(state, button("Allow notifications", true,
                    this::requestNotificationPermission), 12);
        }
        if (enabled && !ReminderScheduler.exactAlarmsAllowed(this)) {
            add(state, button("Enable precise alarm access", false,
                    this::requestExactAlarmAccess), 10);
        }
        add(body, card(state, surfaceVariant), 16);
        toggle.setOnCheckedChangeListener((control, checked) -> {
            preferences.edit().putBoolean(ReminderStore.ENABLED, checked).apply();
            ReminderScheduler.sync(this);
            DateSyncScheduler.ensure(this);
            if (checked) requestNotificationPermission();
            showScreen();
        });

        add(body, sectionTitle("Upcoming months"), 24);
        LinearLayout list = panel(surfaceVariant);
        for (int i = 0; i < Math.min(5, upcoming.size()); i++) {
            LunarEngine.Month item = upcoming.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            add(row, label(item.thursday().format(DATE_SHORT) + "  ·  " + item.label(),
                    15, true, onSurface), 0);
            add(row, label(sourceLabel(item.basis), 12, false, onSurfaceVariant), 5);
            add(list, row, i == 0 ? 0 : 14);
        }
        add(body, card(list, surfaceVariant), 10);

        add(body, sectionTitle("Automatic Pakistan date updates"), 24);
        LinearLayout info = panel(surfaceVariant);
        add(info, label("No manual dates or corrections. The app checks a public Pakistan Hijri-date feed periodically, normally every six hours, subject to Android background limits.",
                14, false, onSurface), 0);
        add(info, label("Confirmed dates are accepted only when explicitly reported by the third-party feed. Otherwise, published PMD forecasts (2026–27) or calculated estimates are used. A forecast is not an official moon-sighting decision.",
                13, false, onSurfaceVariant), 12);
        long lastCheck = preferences.getLong(ReminderStore.LAST_REFRESH_SUCCESS, 0L);
        String checked = lastCheck == 0L ? "Not yet checked online"
                : Instant.ofEpochMilli(lastCheck).atZone(LunarEngine.PAKISTAN)
                        .format(DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH))
                        + " PKT";
        add(info, label("Last successful feed check: " + checked,
                12, true, onSurfaceVariant), 12);
        add(info, label("The service is independent of Pakistan's official Ruet-e-Hilal announcement. Network failures, incorrect source data, battery restrictions, or disabled permissions may affect reminder dates or timing.",
                12, false, onSurfaceVariant), 10);
        add(body, card(info, surfaceVariant), 10);

        add(body, sectionTitle("Notification check"), 24);
        add(body, button("Send a test notification", false, () -> {
            if (!ReminderNotifications.permitted(this)) {
                requestNotificationPermission();
            } else {
                ReminderNotifications.test(this);
                toast("Check your notifications");
            }
        }), 10);
        add(body, button("View PMD lunar calendar", false,
                () -> openSite("https://weather.gov.pk/cdpc/lunar-calendar")), 8);
        add(body, label("No account, location permission or manual monthly setup is required.",
                12, false, onSurfaceVariant), 20);
    }

    private String sourceLabel(LunarEngine.Basis basis) {
        if (basis == LunarEngine.Basis.PMD_FORECAST) return "PMD forecast · not confirmed";
        if (basis == LunarEngine.Basis.LIVE_REPORTED_CONFIRMED) return "Third-party feed reports confirmation";
        return "Calendar estimate · not confirmed";
    }

    private String statusMessage(boolean enabled) {
        if (!enabled) return "Reminders are paused.";
        if (!ReminderNotifications.permitted(this)) return "Allow notifications to receive monthly reminders.";
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null && !nm.areNotificationsEnabled()) return "Notifications are blocked in Android settings.";
        if (!ReminderScheduler.exactAlarmsAllowed(this)) {
            return "Enabled. Android may delay the alert beyond 11:00 AM without precise alarm access.";
        }
        return "Enabled. Reminders schedule automatically in Pakistan time.";
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && !ReminderNotifications.permitted(this)) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATIONS);
        }
    }

    @Override public void onRequestPermissionsResult(int code, String[] names, int[] grants) {
        super.onRequestPermissionsResult(code, names, grants);
        if (code == REQUEST_NOTIFICATIONS) showScreen();
    }

    private void requestExactAlarmAccess() {
        if (Build.VERSION.SDK_INT < 31) return;
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (RuntimeException unavailable) {
            toast("Open Android settings > Special app access > Alarms & reminders");
        }
    }

    private void openSite(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (RuntimeException noBrowser) { toast("A browser is required to open this page"); }
    }

    private int color(int attribute, int fallback) {
        return MaterialColors.getColor(this, attribute, fallback);
    }

    private TextView sectionTitle(String title) {
        return label(title, 19, true, onSurface);
    }

    private TextView label(String value, int sizeSp, boolean strong, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        if (strong) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LinearLayout panel(int color) {
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(18), dp(18), dp(18), dp(18));
        return inner;
    }

    private MaterialCardView card(LinearLayout child, int color) {
        MaterialCardView view = new MaterialCardView(this);
        view.setRadius(dp(24));
        view.setCardElevation(0f);
        view.setCardBackgroundColor(color);
        view.addView(child);
        return view;
    }

    private MaterialButton button(String label, boolean prominent, Runnable action) {
        MaterialButton button = new MaterialButton(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setCornerRadius(dp(18));
        button.setMinHeight(dp(48));
        button.setBackgroundTintList(ColorStateList.valueOf(prominent ? primary : primaryContainer));
        button.setTextColor(prominent ? onPrimary : onPrimaryContainer);
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private void add(LinearLayout parent, View child, int topDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(topDp);
        parent.addView(child, params);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
