package com.scopewise.lunarthursday;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
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
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Automatic lunar reminder dashboard, Material You, with Settings and update indicator. */
public final class MainActivity extends LocalizedActivity {
    private static final int REQUEST_NOTIFICATIONS = 44;
    private static final DateTimeFormatter DATE_LONG = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_SHORT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private SharedPreferences preferences;
    private int primary;
    private int onPrimary;
    private int surface;
    private int onSurface;
    private int surfaceVariant;
    private int onSurfaceVariant;
    private int primaryContainer;
    private int onPrimaryContainer;
    private final Handler ticker = new Handler(Looper.getMainLooper());
    private TextView countdownView;
    private Instant countdownTarget;
    private int moonTaps;
    private final Runnable tickerTask = new Runnable() {
        @Override public void run() {
            if (countdownView == null || countdownTarget == null || isFinishing()) return;
            countdownView.setText(countdownText(countdownTarget));
            ticker.postDelayed(this, 60_000L);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
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
        if (preferences == null) return;
        ReminderScheduler.sync(this);
        DateSyncScheduler.ensure(this);
        showScreen();
        UpdateManager.check(this, false, (success, message) -> showScreen());
    }

    @Override protected void onPause() {
        ticker.removeCallbacks(tickerTask);
        super.onPause();
    }

    private void applyPalette() {
        primary = color(androidx.appcompat.R.attr.colorPrimary, 0xFF136F53);
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
        List<LunarEngine.Month> upcoming = LunarEngine.upcoming(Instant.now(),
                ReminderStore.confirmedStarts(this), 6);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(surface);
        LinearLayout body = panel();
        body.setPadding(dp(20), dp(20), dp(20), dp(36));
        scroll.addView(body);
        if (Build.VERSION.SDK_INT >= 30) {
            scroll.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                body.setPadding(dp(20), dp(20) + bars.top, dp(20), dp(36) + bars.bottom);
                return insets;
            });
        } else {
            scroll.setFitsSystemWindows(true);
        }
        setContentView(scroll);

        TextView moonHeader = label("☾  HILAL THURSDAY", 14, true, primary);
        add(body, moonHeader, 0);
        moonHeader.setOnClickListener(v -> {
            if (++moonTaps >= 7) {
                moonTaps = 0;
                new AlertDialog.Builder(this).setTitle(UiText.tr(this, "🌙 Moonlight unlocked"))
                        .setMessage(UiText.tr(this, "Seven taps, seven skies. Your next Thursday is written in the stars — but the date still comes from the calendar, not the stars."))
                        .setPositiveButton(UiText.tr(this, "Lovely"), null).show();
            }
        });
        add(body, label("Your next lunar Thursday, at a glance", 26, true, onSurface), 8);
        add(body, label(UiText.tr(this, "Today in Pakistan: ") + UiText.date(this, LocalDate.now(LunarEngine.PAKISTAN), true), 13, false, onSurfaceVariant), 8);
        add(body, label("Monthly reminder • 11:00 AM PKT", 14, false, onSurfaceVariant), 8);
        add(body, button("Settings  ·  alarm & updates", false,
                () -> startActivity(new Intent(this, SettingsActivity.class))), 12);
        add(body, button("About  ·  developer & surprises", false,
                () -> startActivity(new Intent(this, AboutActivity.class))), 8);

        if (!upcoming.isEmpty()) {
            LunarEngine.Month next = upcoming.get(0);
            LinearLayout hero = panel();
            add(hero, label("NEXT REMINDER", 12, true, onPrimaryContainer), 0);
            countdownTarget = next.remindAt();
            countdownView = label(countdownText(countdownTarget), 19, true, onPrimaryContainer);
            add(hero, countdownView, 10);
            ticker.removeCallbacks(tickerTask);
            ticker.postDelayed(tickerTask, 60_000L);
            add(hero, label(UiText.date(this, next.thursday(), true), 24, true, onPrimaryContainer), 10);
            add(hero, label("11:00 AM  ·  Pakistan Standard Time", 16, true, onPrimaryContainer), 8);
            add(hero, label(UiText.month(this, next) + "  ·  " + sourceLabel(next.basis), 14, false, onPrimaryContainer), 12);
            add(hero, label("Tap for the month-start date and source", 12, false, onPrimaryContainer), 8);
            MaterialCardView heroCard = card(hero, primaryContainer);
            heroCard.setOnClickListener(v -> showMonthDetails(next));
            add(body, heroCard, 20);
        }

        UpdateManager.Result release = UpdateManager.available(this);
        if (release != null) {
            LinearLayout available = panel();
            add(available, label("UPDATE AVAILABLE", 12, true, onPrimaryContainer), 0);
            add(available, label(UiText.tr(this, "Version ") + VersionPolicy.display(release.tag)
                    + (release.preview ? UiText.tr(this, " · Preview") : ""), 17, true, onPrimaryContainer), 8);
            add(available, button("Download on GitHub", true, () -> open(release.url)), 10);
            add(body, card(available, primaryContainer), 14);
        }

        LinearLayout state = panel();
        MaterialSwitch toggle = new MaterialSwitch(this);
        toggle.setText("Monthly reminders");
        toggle.setTextColor(onSurface);
        toggle.setTypeface(null, Typeface.BOLD);
        toggle.setTextSize(16);
        toggle.setChecked(enabled);
        add(state, toggle, 0);
        add(state, label(statusMessage(enabled), 13, false, onSurfaceVariant), 8);
        add(state, label(UiText.tr(this, "Alert mode: ") + UiText.tr(this, preferences.getBoolean(ReminderStore.ALARM_MODE, false)
                ? "Ringing alarm" : "Standard notification"), 13, true, onSurface), 8);
        if (enabled && !ReminderNotifications.permitted(this)) {
            add(state, button("Allow notifications", true, this::requestNotificationPermission), 10);
        }
        if (enabled && !ReminderScheduler.exactAlarmsAllowed(this)) {
            add(state, button("Allow precise alarm scheduling", false, this::requestExactAlarmAccess), 10);
        }
        add(body, card(state, surfaceVariant), 16);
        toggle.setOnCheckedChangeListener((control, checked) -> {
            preferences.edit().putBoolean(ReminderStore.ENABLED, checked).apply();
            ReminderScheduler.sync(this);
            DateSyncScheduler.ensure(this);
            if (checked) requestNotificationPermission();
            else SnoozeReceiver.clear(this);
            showScreen();
        });

        add(body, label("Upcoming lunar months", 19, true, onSurface), 24);
        LinearLayout list = panel();
        for (int i = 0; i < Math.min(5, upcoming.size()); i++) {
            LunarEngine.Month item = upcoming.get(i);
            LinearLayout itemView = panel();
            itemView.setPadding(dp(2), dp(7), dp(2), dp(7));
            add(itemView, label(UiText.date(this, item.thursday(), false) + "  ·  " + UiText.month(this, item),
                    15, true, onSurface), 0);
            add(itemView, label(sourceLabel(item.basis) + UiText.tr(this, "  •  Tap for details"),
                    12, false, onSurfaceVariant), 5);
            itemView.setOnClickListener(v -> showMonthDetails(item));
            add(list, itemView, i == 0 ? 0 : 6);
        }
        add(body, card(list, surfaceVariant), 10);

        add(body, label("Automatic date updates", 19, true, onSurface), 24);
        LinearLayout info = panel();
        add(info, label("No manual date changes. Pakistan Hijri-date feed checks are scheduled about every six hours, subject to Android background limits.",
                14, false, onSurface), 0);
        add(info, label("Only explicitly confirmed dates from the third-party provider are cached as reported. Otherwise PMD forecasts or calendar estimates are used, which may differ from Pakistan's official sighting.",
                13, false, onSurfaceVariant), 12);
        long checked = preferences.getLong(ReminderStore.LAST_REFRESH_SUCCESS, 0L);
        String when = checked <= 0 ? "Not checked online yet"
                : Instant.ofEpochMilli(checked).atZone(LunarEngine.PAKISTAN)
                         .format(DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", AppLanguage.locale(this))) + " PKT";
        add(info, label(UiText.tr(this, "Last successful feed check: ") + when, 12, true, onSurfaceVariant), 12);
        long age = checked > 0 && System.currentTimeMillis() >= checked
                ? System.currentTimeMillis() - checked : 0L;
        if (checked == 0 || age > 36L * 60 * 60 * 1000) {
            add(info, label("Online date data may be stale. Forecasts are shown until a new check succeeds.",
                    12, true, onSurfaceVariant), 9);
        }
        add(info, button("Request a date refresh", false, () -> {
            if (DateSyncScheduler.requestNow(this)) toast("Refresh requested; Android will run it when network is available");
            else toast("Enable reminders to allow date checks");
        }), 12);
        add(body, card(info, surfaceVariant), 10);

        add(body, label("Notifications and updates", 19, true, onSurface), 24);
        add(body, button("Send a test notification", false, () -> {
            if (!ReminderNotifications.permitted(this)) requestNotificationPermission();
            else { ReminderNotifications.test(this); toast("Check notifications"); }
        }), 8);
        add(body, button("Open app settings", false,
                () -> startActivity(new Intent(this, SettingsActivity.class))), 8);
        add(body, button("View Pakistan PMD calendar", false,
                () -> open("https://weather.gov.pk/cdpc/lunar-calendar")), 8);
        add(body, label("Automatic scheduling and date checks require no login or monthly adjustments.",
                12, false, onSurfaceVariant), 20);
    }

    private String countdownText(Instant target) {
        long millis = Math.max(0L, Duration.between(Instant.now(), target).toMillis());
        long minutes = (millis + 59_999L) / 60_000L;
        long days = minutes / (24L * 60L);
        long hours = (minutes % (24L * 60L)) / 60L;
        if (minutes == 0L) return UiText.tr(this, "Scheduled time has arrived");
        if (days > 0) return UiText.fmt(this, "%1$d days, %2$d hours remaining", days, hours);
        if (hours > 0) return UiText.fmt(this, "%1$d hours, %2$d minutes remaining", hours, minutes % 60L);
        return UiText.fmt(this, "%1$d minutes remaining", minutes);
    }

    private void showMonthDetails(LunarEngine.Month item) {
        String description = UiText.tr(this, "Month: ") + UiText.month(this, item)
                + "\n" + UiText.tr(this, "First civil day (Pakistan): ") + UiText.date(this, item.start, true)
                + "\n" + UiText.tr(this, "First Thursday: ") + UiText.date(this, item.thursday(), true)
                + "\n" + UiText.tr(this, "Reminder: 11:00 AM PKT")
                + "\n\n" + UiText.tr(this, "Source: ") + UiText.tr(this, LunarEngine.basisDescription(item.basis))
                + "\n\n" + UiText.tr(this, "Forecasts and third-party reports may differ from the official Ruet-e-Hilal decision.");
        new AlertDialog.Builder(this).setTitle(UiText.tr(this, "Lunar month details"))
                .setMessage(description).setPositiveButton(UiText.tr(this, "Close"), null).show();
    }

    private String sourceLabel(LunarEngine.Basis b) {
        if (b == LunarEngine.Basis.PMD_FORECAST) return UiText.tr(this, "PMD forecast · not confirmed");
        if (b == LunarEngine.Basis.LIVE_REPORTED_CONFIRMED) return UiText.tr(this, "Third-party feed reports confirmation");
        return UiText.tr(this, "Calendar estimate · not confirmed");
    }

    private String statusMessage(boolean enabled) {
        if (!enabled) return "Reminders are paused.";
        if (!ReminderNotifications.permitted(this)) return "Allow notifications to receive reminders or ringing alarms.";
        NotificationManager n = getSystemService(NotificationManager.class);
        if (n != null && !n.areNotificationsEnabled()) return "Notifications are blocked in Android settings.";
        if (!ReminderScheduler.exactAlarmsAllowed(this)) return "Enabled, but Android may delay delivery without precise alarm access.";
        if (preferences.getBoolean(ReminderStore.ALARM_MODE, false)) {
            return "Ringing alarm is selected. Sound, vibration and Snooze are managed in Settings.";
        }
        return "Notifications are scheduled automatically in Pakistan time.";
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && !ReminderNotifications.permitted(this)) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
    }

    @Override public void onRequestPermissionsResult(int code, String[] names, int[] grants) {
        super.onRequestPermissionsResult(code, names, grants);
        if (code == REQUEST_NOTIFICATIONS) showScreen();
    }

    private void requestExactAlarmAccess() {
        if (Build.VERSION.SDK_INT < 31) return;
        try { startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .setData(Uri.parse("package:" + getPackageName()))); }
        catch (RuntimeException denied) { toast("Enable Alarms & reminders in Android settings"); }
    }

    private void open(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (RuntimeException noBrowser) { toast("A browser is required"); }
    }

    private int color(int attribute, int fallback) { return MaterialColors.getColor(this, attribute, fallback); }
    private TextView label(String title, int sizeSp, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(UiText.tr(this, title));
        t.setTextSize(sizeSp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private LinearLayout panel() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(18), dp(18), dp(18), dp(18));
        return l;
    }
    private MaterialCardView card(LinearLayout inner, int color) {
        MaterialCardView c = new MaterialCardView(this);
        c.setRadius(dp(24));
        c.setCardElevation(0f);
        c.setCardBackgroundColor(color);
        c.addView(inner);
        return c;
    }
    private MaterialButton button(String title, boolean prominent, Runnable action) {
        MaterialButton b = new MaterialButton(this);
        b.setText(UiText.tr(this, title));
        b.setAllCaps(false);
        b.setCornerRadius(dp(18));
        b.setMinHeight(dp(48));
        b.setBackgroundTintList(ColorStateList.valueOf(prominent ? primary : primaryContainer));
        b.setTextColor(prominent ? onPrimary : onPrimaryContainer);
        b.setOnClickListener(v -> action.run());
        return b;
    }
    private void add(LinearLayout parent, View child, int marginDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(marginDp);
        parent.addView(child, p);
    }
    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + .5f); }
    private void toast(String message) { Toast.makeText(this, UiText.tr(this, message), Toast.LENGTH_LONG).show(); }
}
