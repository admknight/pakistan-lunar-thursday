package com.scopewise.lunarthursday;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.MaterialColors;

/** Identity, transparency and three preserved opt-in easter eggs. */
public final class AboutActivity extends LocalizedActivity {
    private static final String PROJECT = "https://github.com/admknight/pakistan-lunar-thursday";
    private int primary, surface, surfaceVariant, primaryContainer, onSurface, secondary, onContainer;
    private int versionTaps;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DynamicColors.applyToActivityIfAvailable(this);
        primary = color(androidx.appcompat.R.attr.colorPrimary, 0xFF136F53);
        surface = color(com.google.android.material.R.attr.colorSurface, 0xFFFFFFFF);
        surfaceVariant = color(com.google.android.material.R.attr.colorSurfaceVariant, 0xFFEAF1EB);
        primaryContainer = color(com.google.android.material.R.attr.colorPrimaryContainer, 0xFFD5F0DD);
        onSurface = color(com.google.android.material.R.attr.colorOnSurface, 0xFF192D26);
        secondary = color(com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF4D6459);
        onContainer = color(com.google.android.material.R.attr.colorOnPrimaryContainer, 0xFF002B1A);
        render();
    }

    private void render() {
        LinearLayout root = column();
        root.setBackgroundColor(surface);
        if (Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(0, bars.top, 0, bars.bottom);
                return insets;
            });
        } else root.setFitsSystemWindows(true);
        setContentView(root);

        LinearLayout toolbar = row();
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(14), 0, dp(18), 0);
        ImageView back = new ImageView(this);
        back.setImageResource(R.drawable.ic_ui_back);
        back.setColorFilter(primary);
        if (AppLanguage.isUrdu(this)) back.setScaleX(-1f);
        back.setPadding(dp(11), dp(11), dp(11), dp(11));
        back.setContentDescription(UiText.tr(this, "Back"));
        back.setOnClickListener(v -> finish());
        toolbar.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView heading = text("About / Developer", 19, true, onSurface);
        toolbar.addView(heading);
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(62)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout body = column();
        body.setPadding(dp(18), dp(9), dp(18), dp(36));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.mipmap.ic_launcher);
        logo.setContentDescription(UiText.tr(this, "Hilal Thursday"));
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(108), dp(108));
        logoParams.gravity = Gravity.CENTER_HORIZONTAL;
        body.addView(logo, logoParams);
        logo.setOnLongClickListener(v -> { constellation(); return true; });

        TextView name = text(getString(R.string.app_name), 28, true, onSurface);
        name.setGravity(Gravity.CENTER);
        add(body, name, 10);
        TextView tagline = text("🌙 A gentle reminder, every lunar month.", 15, false, secondary);
        tagline.setGravity(Gravity.CENTER);
        add(body, tagline, 6);
        TextView detail = text("11:00 AM Pakistan time · English & Urdu", 12, false, secondary);
        detail.setGravity(Gravity.CENTER);
        add(body, detail, 7);

        LinearLayout developer = column();
        developer.setPadding(dp(17), dp(17), dp(17), dp(17));
        add(developer, text("👨‍💻  Developer", 16, true, onContainer), 0);
        add(developer, text("admknight", 21, true, onContainer), 7);
        add(developer, text("Made for Pakistan, open to everyone", 13, false, onContainer), 5);
        add(body, card(developer, primaryContainer), 22);

        LinearLayout list = column();
        list.setPadding(dp(13), dp(6), dp(13), dp(6));
        list.addView(linkRow("🔒", "Privacy", "Optional weather location · no ads or accounts",
                () -> showInfo("Privacy", "No account, ads, or analytics. Location is requested only when you enable weather visuals, and only while the app is in use. Approximate coordinates are rounded, sent to Open-Meteo, and never stored. The Hijri-date feed and GitHub release checks also contact external services. Each provider can see the connecting IP address.")));
        divider(list);
        list.addView(linkRow("📅", "Calendar sources & accuracy", "How Islamic month dates are determined",
                () -> showInfo("Calendar sources & accuracy", "Official Pakistani moon sighting is determined by the Central Ruet-e-Hilal Committee. The app uses third-party reported dates when available and PMD forecasts or calculated estimates otherwise. It does not claim a government-approved date feed.")));
        divider(list);
        list.addView(linkRow("✨", "What's new", "Weather visuals and in-app updates",
                () -> showInfo("What's new", "Hilal Thursday v1.7 adds optional location-based weather scenery, a time-aware moon-and-mosque illustration, background update alerts and secure APK downloads inside the app. Android always confirms installation. Weather data: Open-Meteo (CC BY 4.0).")));
        divider(list);
        list.addView(linkRow("⌘", "Open source & feedback", "Code, issues and contributions",
                () -> open(PROJECT)));
        divider(list);
        list.addView(linkRow("📜", "MIT License", "Read the project license",
                () -> open(PROJECT + "/blob/main/LICENSE")));
        add(body, card(list, surfaceVariant), 16);

        LinearLayout version = column();
        version.setGravity(Gravity.CENTER);
        TextView versionText = text(UiText.tr(this, "Version ") + UpdateManager.installed(this), 15, true, secondary);
        versionText.setGravity(Gravity.CENTER);
        add(version, versionText, 0);
        add(version, text("✨ Made with care under the same moon.", 12, false, secondary), 9);
        version.setOnClickListener(v -> {
            if (++versionTaps == 5) {
                versionTaps = 0;
                new AlertDialog.Builder(this).setTitle(UiText.tr(this, "🌟 Five stars"))
                        .setMessage(UiText.tr(this, "A tiny secret from the developer: some dates need patience, but a gentle reminder can still brighten the day.\n\n🌙  ✦  ✧  ✦  🌙"))
                        .setPositiveButton(UiText.tr(this, "Shine on"), null).show();
            }
        });
        add(body, version, 25);
    }

    private void constellation() {
        new AlertDialog.Builder(this).setTitle(UiText.tr(this, "✦ Constellation unlocked ✦"))
                .setMessage(UiText.tr(this, "       ✦           ·\n  ·       ☾         ✧\n       ✧       ✦\n\nThe night sky has no update button, but your reminder app does."))
                .setPositiveButton(UiText.tr(this, "Back to Earth"), null).show();
    }

    private LinearLayout linkRow(String glyph, String title, String subtitle, Runnable action) {
        LinearLayout row = row();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(3), dp(13), dp(3), dp(13));
        TextView emoji = text(glyph, 21, false, primary);
        emoji.setGravity(Gravity.CENTER);
        row.addView(emoji, new LinearLayout.LayoutParams(dp(45), dp(45)));
        LinearLayout words = column();
        words.addView(text(title, 15, true, onSurface));
        words.addView(text(subtitle, 12, false, secondary));
        row.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(text(AppLanguage.isUrdu(this) ? "‹" : "›", 24, false, secondary));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> action.run());
        row.setContentDescription(UiText.tr(this, title));
        return row;
    }

    private void divider(LinearLayout parent) {
        View line = new View(this);
        line.setBackgroundColor((secondary & 0x00ffffff) | 0x22000000);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(1));
        p.setMarginStart(dp(48));
        parent.addView(line, p);
    }

    private void showInfo(String title, String detail) {
        new AlertDialog.Builder(this).setTitle(UiText.tr(this, title))
                .setMessage(UiText.tr(this, detail))
                .setPositiveButton(UiText.tr(this, "Close"), null).show();
    }

    private void open(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (RuntimeException e) {
            Toast.makeText(this, UiText.tr(this, "A browser is required"), Toast.LENGTH_LONG).show();
        }
    }

    private LinearLayout column() { LinearLayout v = new LinearLayout(this); v.setOrientation(1); return v; }
    private LinearLayout row() { LinearLayout v = new LinearLayout(this); v.setOrientation(0); return v; }
    private TextView text(String value, int size, boolean strong, int tint) {
        TextView t = new TextView(this);
        t.setText(UiText.tr(this, value));
        t.setTextSize(size);
        t.setTextColor(tint);
        if (strong) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private MaterialCardView card(View view, int fill) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardElevation(0);
        card.setRadius(dp(23));
        card.setCardBackgroundColor(fill);
        card.addView(view);
        return card;
    }
    private int color(int attr, int fallback) { return MaterialColors.getColor(this, attr, fallback); }
    private int dp(int n) { return (int) (getResources().getDisplayMetrics().density * n + 0.5f); }
    private void add(LinearLayout parent, View child, int topDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(topDp);
        parent.addView(child, p);
    }
}
