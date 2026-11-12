package com.scopewise.lunarthursday;

import android.Manifest;
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
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.MaterialColors;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Compact dashboard with three-dot navigation and persistent bottom tabs. */
public final class MainActivity extends LocalizedActivity {
    private static final int REQUEST_NOTIFICATIONS = 44;
    private static final int TAB_HOME = 0;
    private static final int TAB_MONTHS = 1;
    private static final int TAB_UPDATES = 2;

    private SharedPreferences preferences;
    private int activeTab = TAB_HOME;
    private int surface, surfaceVariant, onSurface, secondary, primary;
    private int primaryContainer, onPrimaryContainer;
    private int monthTaps;
    private Instant countdownTarget;
    private TextView daysValue, hoursValue, minutesValue;
    private final Handler ticker = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (isFinishing() || isDestroyed() || activeTab != TAB_HOME) return;
            updateCounters();
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
        if (state != null) activeTab = state.getInt("dashboard_tab", TAB_HOME);
        else activeTab = getIntent().getIntExtra("open_tab", TAB_HOME);
        ReminderNotifications.ensureChannel(this);
        ReminderScheduler.sync(this);
        DateSyncScheduler.ensure(this);
        UpdateCheckScheduler.ensure(this);
        if (Build.VERSION.SDK_INT >= 33) requestNotificationPermission();
        render();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putInt("dashboard_tab", activeTab);
        super.onSaveInstanceState(out);
    }

    @Override protected void onResume() {
        super.onResume();
        if (preferences == null) return;
        ReminderScheduler.sync(this);
        DateSyncScheduler.ensure(this);
        UpdateCheckScheduler.ensure(this);
        render();
        AppUpdateInstaller.resumeIfPermitted(this);
        WeatherManager.refresh(this, false, () -> {
            if (!isFinishing() && !isDestroyed()) render();
        });
        UpdateManager.check(this, false, (ok, message) -> {
            if (!isFinishing() && !isDestroyed()) render();
        });
    }

    @Override protected void onPause() {
        ticker.removeCallbacks(tick);
        super.onPause();
    }

    private void palette() {
        surface = color(com.google.android.material.R.attr.colorSurface, 0xFFFAFCF8);
        surfaceVariant = color(com.google.android.material.R.attr.colorSurfaceVariant, 0xFFE9F1EB);
        onSurface = color(com.google.android.material.R.attr.colorOnSurface, 0xFF1C2A25);
        secondary = color(com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF4D6258);
        primary = color(androidx.appcompat.R.attr.colorPrimary, 0xFF087F55);
        primaryContainer = color(com.google.android.material.R.attr.colorPrimaryContainer, 0xFFD9F4DF);
        onPrimaryContainer = color(com.google.android.material.R.attr.colorOnPrimaryContainer, 0xFF0D3826);
    }

    private void render() {
        palette();
        ticker.removeCallbacks(tick);
        daysValue = hoursValue = minutesValue = null;
        countdownTarget = null;
        LinearLayout root = column();
        root.setBackgroundColor(surface);
        if (Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout());
                v.setPadding(0, bars.top, 0, bars.bottom);
                return insets;
            });
        } else {
            root.setFitsSystemWindows(true);
        }
        setContentView(root);
        root.addView(toolbar(), new LinearLayout.LayoutParams(-1, dp(64)));

        ScrollView scroller = new ScrollView(this);
        scroller.setFillViewport(true);
        scroller.setClipToPadding(false);
        LinearLayout body = column();
        body.setPadding(dp(18), dp(12), dp(18), dp(24));
        scroller.addView(body);
        root.addView(scroller, new LinearLayout.LayoutParams(-1, 0, 1));
        if (activeTab == TAB_MONTHS) renderMonths(body);
        else if (activeTab == TAB_UPDATES) renderUpdates(body);
        else renderHome(body);
        root.addView(bottomNavigation(), new LinearLayout.LayoutParams(-1, dp(74)));
    }

    private LinearLayout toolbar() {
        LinearLayout bar = row();
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(18), 0, dp(12), 0);
        ImageView appIcon = new ImageView(this);
        appIcon.setImageResource(R.mipmap.ic_launcher);
        appIcon.setContentDescription(UiText.tr(this, "Hilal Thursday"));
        bar.addView(appIcon, new LinearLayout.LayoutParams(dp(43), dp(43)));
        LinearLayout heading = column();
        heading.setPadding(dp(12), 0, 0, 0);
        TextView appName = text("Hilal Thursday", 18, true, onSurface);
        TextView subtitle = text("ہلال جمعرات", 12, false, secondary);
        heading.addView(appName);
        heading.addView(subtitle);
        bar.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        View.OnClickListener moonEgg = v -> {
            if (++monthTaps >= 7) {
                monthTaps = 0;
                new AlertDialog.Builder(this)
                        .setTitle(UiText.tr(this, "🌙 Moonlight unlocked"))
                        .setMessage(UiText.tr(this, "Seven taps, seven skies. Your next Thursday is written in the stars — but the date still comes from the calendar, not the stars."))
                        .setPositiveButton(UiText.tr(this, "Lovely"), null).show();
            }
        };
        appIcon.setOnClickListener(moonEgg);
        heading.setOnClickListener(moonEgg);
        ImageView menu = icon(R.drawable.ic_ui_more, UiText.tr(this, "More options"), primary, dp(44));
        menu.setOnClickListener(this::openMenu);
        bar.addView(menu, new LinearLayout.LayoutParams(dp(46), dp(46)));
        return bar;
    }

    private void openMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, UiText.tr(this, "Settings"));
        popup.getMenu().add(0, 2, 1, UiText.tr(this, "App language"));
        popup.getMenu().add(0, 3, 2, UiText.tr(this, "Test ringing alarm"));
        popup.getMenu().add(0, 4, 3, UiText.tr(this, "Check for updates now"));
        popup.getMenu().add(0, 5, 4, UiText.tr(this, "About / Developer"));
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: case 2:
                    openSettings(item.getItemId() == 2); break;
                case 3:
                    if (!ReminderNotifications.permitted(this)) requestNotificationPermission();
                    else if (!AlarmRingingService.startTest(this)) toast("Alarm service unavailable on this device");
                    break;
                case 4: checkUpdates(); break;
                case 5: startActivity(new Intent(this, AboutActivity.class)); break;
                default: return false;
            }
            return true;
        });
        popup.show();
    }

    private LinearLayout bottomNavigation() {
        LinearLayout nav = row();
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setPadding(dp(7), dp(4), dp(7), dp(4));
        nav.setBackgroundColor(surfaceVariant);
        nav.addView(tab(R.drawable.ic_ui_home, "Home", activeTab == TAB_HOME, () -> switchTab(TAB_HOME)),
                new LinearLayout.LayoutParams(0, -1, 1));
        nav.addView(tab(R.drawable.ic_ui_calendar, "Calendar", activeTab == TAB_MONTHS, () -> switchTab(TAB_MONTHS)),
                new LinearLayout.LayoutParams(0, -1, 1));
        nav.addView(tab(R.drawable.ic_ui_refresh, "Updates", activeTab == TAB_UPDATES, () -> switchTab(TAB_UPDATES)),
                new LinearLayout.LayoutParams(0, -1, 1));
        nav.addView(tab(R.drawable.ic_ui_info, "About", false,
                        () -> startActivity(new Intent(this, AboutActivity.class))),
                new LinearLayout.LayoutParams(0, -1, 1));
        return nav;
    }

    private LinearLayout tab(int art, String title, boolean selected, Runnable click) {
        LinearLayout item = column();
        item.setGravity(Gravity.CENTER);
        item.setContentDescription(UiText.tr(this, title));
        item.setClickable(true);
        item.setFocusable(true);
        ImageView img = icon(art, title, selected ? primary : secondary, dp(24));
        if (selected) {
            LinearLayout pill = row();
            pill.setGravity(Gravity.CENTER);
            pill.setBackground(round(primaryContainer, 20));
            pill.addView(img, new LinearLayout.LayoutParams(dp(22), dp(22)));
            item.addView(pill, new LinearLayout.LayoutParams(dp(55), dp(34)));
        } else {
            item.addView(img, new LinearLayout.LayoutParams(dp(24), dp(34)));
        }
        TextView name = text(title, 11, selected, selected ? primary : secondary);
        name.setGravity(Gravity.CENTER);
        item.addView(name);
        item.setOnClickListener(v -> click.run());
        return item;
    }

    private void switchTab(int next) {
        if (activeTab == next) return;
        activeTab = next;
        render();
    }

    private void renderHome(LinearLayout body) {
        List<LunarEngine.Month> upcoming = months(6);
        TextView welcome = text("Your next lunar Thursday, at a glance", 25, true, onSurface);
        add(body, welcome, 0);
        add(body, text(UiText.tr(this, "Today in Pakistan: ")
                + UiText.date(this, LocalDate.now(LunarEngine.PAKISTAN), true), 13, false, secondary), 6);

        if (!upcoming.isEmpty()) {
            LunarEngine.Month next = upcoming.get(0);
            MaterialCardView card = card(primaryContainer, 27);
            LinearLayout hero = column();
            card.addView(hero);
            MoonSkyView sky = new MoonSkyView(this);
            sky.setWeather(WeatherManager.current(this));
            hero.addView(sky, new LinearLayout.LayoutParams(-1, dp(151)));
            LinearLayout details = column();
            details.setPadding(dp(17), dp(12), dp(17), dp(19));
            add(details, text("NEXT REMINDER", 12, true, onPrimaryContainer), 0);
            add(details, text(UiText.month(this, next), 22, true, onPrimaryContainer), 6);
            add(details, text(UiText.date(this, next.thursday(), true), 16, true, onPrimaryContainer), 7);
            add(details, text("11:00 AM  ·  Pakistan Standard Time", 13, false, onPrimaryContainer), 3);
            LinearLayout countdown = row();
            countdown.setGravity(Gravity.CENTER);
            daysValue = addCounter(countdown, "Days");
            hoursValue = addCounter(countdown, "Hours");
            minutesValue = addCounter(countdown, "Minutes");
            add(details, countdown, 15);
            add(details, text(sourceLabel(next.basis), 12, false, onPrimaryContainer), 12);
            WeatherManager.Scene live = WeatherManager.current(this);
            if (live != null) {
                add(details, text("🌤 " + live.description(this) + "  ·  Weather: Open-Meteo", 12, false, onPrimaryContainer), 7);
            } else if (WeatherManager.enabled(this)) {
                add(details, text("🌤 " + (WeatherManager.permissionGranted(this)
                     ? "Weather unavailable · time-aware sky shown" : "Allow approximate location in Settings"),
                     12, false, onPrimaryContainer), 7);
            }
            add(details, text("Tap for the month-start date and source", 11, false, onPrimaryContainer), 6);
            hero.addView(details);
            card.setOnClickListener(v -> showMonthDetails(next));
            add(body, card, 17);
            countdownTarget = next.remindAt();
            updateCounters();
            ticker.postDelayed(tick, 60_000L);
        }

        LinearLayout status = compactSurfaceRow("🔔", preferences.getBoolean(ReminderStore.ENABLED, false)
                        ? "Reminders enabled" : "Reminders paused",
                UiText.tr(this, preferences.getBoolean(ReminderStore.ALARM_MODE, false)
                        ? "Ringing alarm" : "Standard notification"),
                () -> openSettings(false));
        add(body, card(status, surfaceVariant, 21), 14);
        if (!ReminderNotifications.permitted(this) || !ReminderScheduler.exactAlarmsAllowed(this)) {
            add(body, compactSurfaceRow("⚠", "Review alert permissions",
                    "An Android permission may delay reminders", () -> openSettings(false)), 9);
        }
        UpdateManager.Result release = UpdateManager.available(this);
        if (release != null) {
            add(body, card(compactSurfaceRow("✨", "Update available",
                    VersionPolicy.display(release.tag), () -> switchTab(TAB_UPDATES)), primaryContainer, 20), 12);
        }

        LinearLayout subhead = row();
        subhead.setGravity(Gravity.CENTER_VERTICAL);
        subhead.addView(weightedText("Upcoming lunar months", 18, true, onSurface));
        TextView see = text("See all  ›", 13, true, primary);
        see.setPadding(dp(8), dp(12), 0, dp(12));
        see.setOnClickListener(v -> switchTab(TAB_MONTHS));
        subhead.addView(see);
        add(body, subhead, 17);
        for (int i = 1; i < Math.min(3, upcoming.size()); i++) {
            LunarEngine.Month item = upcoming.get(i);
            add(body, card(monthRow(item), surfaceVariant, 18), 8);
        }
        if (upcoming.size() <= 1) {
            add(body, text("Dates are calculated automatically and may be estimated.", 12, false, secondary), 9);
        }
        add(body, text("Forecasts and third-party reports may differ from the official Ruet-e-Hilal decision.",
                12, false, secondary), 18);
    }

    private TextView addCounter(LinearLayout parent, String unit) {
        LinearLayout tile = column();
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(4), dp(8), dp(4), dp(8));
        tile.setBackground(round(surface, 16));
        TextView value = text("0", 24, true, onSurface); // Readable against Material surface tile
        value.setGravity(Gravity.CENTER);
        tile.addView(value);
        TextView title = text(unit, 12, false, secondary);
        title.setGravity(Gravity.CENTER);
        tile.addView(title);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(76), 1);
        lp.setMarginEnd(dp(7));
        parent.addView(tile, lp);
        return value;
    }

    private void updateCounters() {
        if (daysValue == null || hoursValue == null || minutesValue == null || countdownTarget == null) return;
        long min = Math.max(0L, (Duration.between(Instant.now(), countdownTarget).toMillis() + 59_999L) / 60_000L);
        daysValue.setText(String.format(AppLanguage.locale(this), "%d", min / 1_440L));
        hoursValue.setText(String.format(AppLanguage.locale(this), "%d", (min % 1_440L) / 60L));
        minutesValue.setText(String.format(AppLanguage.locale(this), "%d", min % 60L));
    }

    private void renderMonths(LinearLayout body) {
        add(body, text("Upcoming lunar months", 26, true, onSurface), 2);
        add(body, text("First Thursday · 11:00 AM PKT", 13, false, secondary), 7);
        List<LunarEngine.Month> next = months(7);
        for (int i = 0; i < next.size(); i++) {
            add(body, card(monthRow(next.get(i)), i == 0 ? primaryContainer : surfaceVariant, 19), i == 0 ? 17 : 11);
        }
        add(body, card(compactSurfaceRow("ⓘ", "Calendar source information",
                "Forecasts and calculations are not official confirmations",
                this::calendarDisclaimer), surfaceVariant, 19), 18);
    }

    private LinearLayout monthRow(LunarEngine.Month item) {
        LinearLayout row = row();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(13), dp(12), dp(12), dp(12));
        TextView glyph = text("☾", 29, true, primary);
        glyph.setGravity(Gravity.CENTER);
        glyph.setBackground(round(primaryContainer, 18));
        row.addView(glyph, new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout details = column();
        details.setPadding(dp(11), 0, 0, 0);
        details.addView(text(UiText.month(this, item), 15, true, onSurface));
        details.addView(text(UiText.date(this, item.thursday(), false), 13, false, secondary));
        details.addView(text(sourceLabel(item.basis), 11, false, secondary));
        row.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(text(chevron(), 27, false, secondary));
        row.setOnClickListener(v -> showMonthDetails(item));
        row.setContentDescription(UiText.month(this, item) + ", " + UiText.date(this, item.thursday(), true));
        return row;
    }

    private void renderUpdates(LinearLayout body) {
        add(body, text("Updates & calendar", 26, true, onSurface), 2);
        add(body, text("App releases and automatic Pakistan lunar-date checks", 13, false, secondary), 6);
        LinearLayout releaseCard = column();
        releaseCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        releaseCard.addView(text("APP VERSION", 12, true, secondary));
        add(releaseCard, text(UiText.tr(this, "Version ") + UpdateManager.installed(this), 19, true, onSurface), 6);
        UpdateManager.Result result = UpdateManager.available(this);
        if (result == null) add(releaseCard, text("No newer release detected", 13, false, secondary), 9);
        else add(releaseCard, compactSurfaceRow("✨", "Update available",
                VersionPolicy.display(result.tag), () -> AppUpdateInstaller.prompt(this, result)), 9);
        add(releaseCard, actionRow(R.drawable.ic_ui_refresh, "Check for updates now", this::checkUpdates), 12);
        add(body, card(releaseCard, surfaceVariant, 23), 18);

        LinearLayout feed = column();
        feed.setPadding(dp(16), dp(14), dp(16), dp(14));
        add(feed, text("AUTOMATIC HIJRI DATE CHECKS", 12, true, secondary), 0);
        long checked = preferences.getLong(ReminderStore.LAST_REFRESH_SUCCESS, 0L);
        String when = checked <= 0 ? UiText.tr(this, "Not checked online yet")
                : UiText.dateTime(this, Instant.ofEpochMilli(checked)) + " PKT";
        add(feed, text(UiText.tr(this, "Last successful feed check: ") + when, 14, false, onSurface), 9);
        add(feed, actionRow(R.drawable.ic_ui_refresh, "Request a date refresh", () -> {
            if (DateSyncScheduler.requestNow(this)) toast("Refresh requested; Android will run it when network is available");
            else toast("Enable reminders to allow date checks");
        }), 10);
        add(feed, actionRow(R.drawable.ic_ui_calendar, "View Pakistan PMD calendar",
                () -> open("https://weather.gov.pk/cdpc/lunar-calendar")), 6);
        add(body, card(feed, surfaceVariant, 23), 14);
        add(body, text("Dates are updated automatically. Third-party reports, forecasts and calculated estimates can differ from Pakistan's official sighting decision.",
                13, false, secondary), 15);
    }

    private View actionRow(int iconId, String title, Runnable action) {
        LinearLayout row = row();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(2), dp(10), 0, dp(10));
        row.addView(icon(iconId, title, primary, dp(23)), new LinearLayout.LayoutParams(dp(30), dp(30)));
        row.addView(weightedText(title, 14, true, onSurface));
        row.addView(text(chevron(), 24, false, secondary));
        row.setOnClickListener(v -> action.run());
        return row;
    }

    private LinearLayout compactSurfaceRow(String emblem, String heading, String sub, Runnable action) {
        LinearLayout row = row();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(13), dp(12), dp(13), dp(12));
        TextView mark = text(emblem, 21, false, primary);
        mark.setGravity(Gravity.CENTER);
        row.addView(mark, new LinearLayout.LayoutParams(dp(40), dp(44)));
        LinearLayout details = column();
        details.addView(text(heading, 15, true, onSurface));
        details.addView(text(sub, 12, false, secondary));
        row.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(text(chevron(), 24, false, secondary));
        row.setOnClickListener(v -> action.run());
        return row;
    }

    private void openSettings(boolean language) {
        Intent intent = new Intent(this, SettingsActivity.class);
        if (language) intent.putExtra("open_language_dialog", true);
        startActivity(intent);
    }

    private void checkUpdates() {
        if (UpdateManager.isChecking()) { toast("Update check already running"); return; }
        UpdateManager.check(this, true, (ok, message) -> {
            toast(message);
            if (!isFinishing() && !isDestroyed()) render();
        });
        toast("Checking GitHub releases");
    }

    private void showMonthDetails(LunarEngine.Month item) {
        String message = UiText.tr(this, "Month: ") + UiText.month(this, item)
                + "\n" + UiText.tr(this, "First civil day (Pakistan): ") + UiText.date(this, item.start, true)
                + "\n" + UiText.tr(this, "First Thursday: ") + UiText.date(this, item.thursday(), true)
                + "\n" + UiText.tr(this, "Reminder: 11:00 AM PKT")
                + "\n\n" + UiText.tr(this, "Source: ") + LunarEngine.basisDescription(item.basis)
                + "\n\n" + UiText.tr(this, "Forecasts and third-party reports may differ from the official Ruet-e-Hilal decision.");
        new AlertDialog.Builder(this).setTitle(UiText.tr(this, "Lunar month details"))
                .setMessage(message).setPositiveButton(UiText.tr(this, "Close"), null).show();
    }

    private void calendarDisclaimer() {
        new AlertDialog.Builder(this).setTitle(UiText.tr(this, "Calendar source information"))
                .setMessage(UiText.tr(this, "Official Pakistani moon sighting is determined by the Central Ruet-e-Hilal Committee. The app uses third-party reported dates when available and PMD forecasts or calculated estimates otherwise. It does not claim a government-approved date feed."))
                .setPositiveButton(UiText.tr(this, "Close"), null).show();
    }

    private String sourceLabel(LunarEngine.Basis basis) {
        if (basis == LunarEngine.Basis.LIVE_REPORTED_CONFIRMED) return UiText.tr(this, "Third-party feed reports confirmation");
        if (basis == LunarEngine.Basis.PMD_FORECAST) return UiText.tr(this, "PMD forecast · not confirmed");
        return UiText.tr(this, "Calendar estimate · not confirmed");
    }

    private List<LunarEngine.Month> months(int n) {
        return LunarEngine.upcoming(Instant.now(), ReminderStore.confirmedStarts(this), n);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && !ReminderNotifications.permitted(this))
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
    }

    @Override public void onRequestPermissionsResult(int code, String[] names, int[] grants) {
        super.onRequestPermissionsResult(code, names, grants);
        if (code == REQUEST_NOTIFICATIONS) render();
    }

    private void open(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (RuntimeException ex) { toast("A browser is required"); }
    }

    private String chevron() { return AppLanguage.isUrdu(this) ? "‹" : "›"; }

    private void toast(String s) { Toast.makeText(this, UiText.tr(this, s), Toast.LENGTH_LONG).show(); }
    private int dp(int n) { return (int) (n * getResources().getDisplayMetrics().density + .5f); }
    private int color(int attr, int fallback) { return MaterialColors.getColor(this, attr, fallback); }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(1); return l; }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(0); return l; }

    private TextView text(String value, int size, boolean strong, int tint) {
        TextView t = new TextView(this);
        t.setText(UiText.tr(this, value));
        t.setTextSize(size);
        t.setTextColor(tint);
        if (strong) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private TextView weightedText(String value, int size, boolean strong, int tint) {
        TextView t = text(value, size, strong, tint);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        return t;
    }

    private ImageView icon(int drawable, String label, int tint, int size) {
        ImageView img = new ImageView(this);
        img.setImageResource(drawable);
        img.setColorFilter(tint);
        img.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        img.setContentDescription(UiText.tr(this, label));
        img.setPadding(dp(4), dp(4), dp(4), dp(4));
        return img;
    }

    private MaterialCardView card(int color, int radiusDp) {
        MaterialCardView c = new MaterialCardView(this);
        c.setCardElevation(0);
        c.setRadius(dp(radiusDp));
        c.setCardBackgroundColor(color);
        return c;
    }
    private MaterialCardView card(View child, int color, int radiusDp) {
        MaterialCardView c = card(color, radiusDp);
        c.addView(child);
        return c;
    }
    private android.graphics.drawable.GradientDrawable round(int color, int radiusDp) {
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }
    private void add(LinearLayout parent, View child, int topDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(topDp);
        parent.addView(child, p);
    }
}
