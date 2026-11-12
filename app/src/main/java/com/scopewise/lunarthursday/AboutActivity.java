package com.scopewise.lunarthursday;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
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


/** About, credits, data transparency, and unobtrusive optional easter eggs. */
public final class AboutActivity extends LocalizedActivity {
    private int primary;
    private int onPrimary;
    private int surface;
    private int onSurface;
    private int secondary;
    private int container;
    private int onContainer;
    private int versionTaps;
    private int starTaps;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        DynamicColors.applyToActivityIfAvailable(this);
        primary = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary, 0xFF136F53);
        onPrimary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimary, 0xFFFFFFFF);
        surface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, 0xFFFFFFFF);
        onSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, 0xFF1B211E);
        secondary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF414943);
        container = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimaryContainer, 0xFFBCEAD3);
        onContainer = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimaryContainer, 0xFF002114);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(surface);
        LinearLayout body = column();
        body.setPadding(dp(20), dp(22), dp(20), dp(46));
        scroll.addView(body);
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            scroll.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout());
                body.setPadding(dp(20), dp(22) + bars.top, dp(20), dp(46) + bars.bottom);
                return insets;
            });
        }
        setContentView(scroll);
        add(body, button("←  Back", false, this::finish), 0);

        TextView moon = text("☾", 62, true, primary);
        moon.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        moon.setOnLongClickListener(v -> {
            new AlertDialog.Builder(this).setTitle(UiText.tr(this, "✦ Constellation unlocked ✦"))
                    .setMessage(UiText.tr(this, "       ✦           ·\n  ·       ☾         ✧\n       ✧       ✦\n\nThe night sky has no update button, but your reminder app does."))
                    .setPositiveButton(UiText.tr(this, "Back to Earth"), null).show();
            return true;
        });
        add(body, moon, 20);
        add(body, text(getString(R.string.app_name), 30, true, onSurface), 4);
        add(body, text("A quieter way to remember the rhythm of Pakistan's lunar months.", 15, false, secondary), 8);

        LinearLayout info = panel();
        add(info, text("Made for Pakistan, open to everyone", 19, true, onContainer), 0);
        add(info, text("Developer / maintainer: admknight", 15, true, onContainer), 12);
        add(info, text("An open-source project. Contributions and issue reports are welcome on GitHub.", 13, false, onContainer), 8);
        add(info, button("Source code and issues", false,
                () -> open("https://github.com/admknight/pakistan-lunar-thursday")), 12);
        add(info, button("Download releases", false,
                () -> open("https://github.com/admknight/pakistan-lunar-thursday/releases")), 8);
        add(body, card(info, container), 20);

        LinearLayout detail = panel();
        add(detail, text("How reminders work", 19, true, onSurface), 0);
        add(detail, text("The reminder is scheduled for 11:00 AM Pakistan Standard Time on the first Thursday on or after each new Hijri month's first civil day.", 14, false, secondary), 9);
        add(detail, text("Official Pakistani moon sighting is determined by the Central Ruet-e-Hilal Committee. The app uses third-party reported dates when available and PMD forecasts or calculated estimates otherwise. It does not claim a government-approved date feed.", 13, false, secondary), 12);
        add(detail, text("Privacy", 19, true, onSurface), 19);
        add(detail, text("No account, ads, location request, or app analytics. The phone checks a public Hijri-date feed and GitHub's Releases API; those services can see the connecting IP address.", 13, false, secondary), 9);
        add(body, card(detail, surface), 16);

        LinearLayout credits = panel();
        add(credits, text("Credits and transparency", 19, true, onSurface), 0);
        add(credits, text("Pakistan Meteorological Department (forecast references), public third-party Hijri-date service, Android Material 3 / Material You, and community contributors.", 13, false, secondary), 9);
        add(credits, button("Open project license (MIT)", false,
                () -> open("https://github.com/admknight/pakistan-lunar-thursday/blob/main/LICENSE")), 10);
        TextView version = text(UiText.tr(this, "Version ") + UpdateManager.installed(this) + UiText.tr(this, "  ·  tap for details"), 12, false, secondary);
        version.setOnClickListener(v -> {
            versionTaps++;
            if (versionTaps == 5) {
                versionTaps = 0;
                new AlertDialog.Builder(this).setTitle(UiText.tr(this, "🌟 Five stars"))
                        .setMessage(UiText.tr(this, "A tiny secret from the developer: some dates need patience, but a gentle reminder can still brighten the day.\n\n🌙  ✦  ✧  ✦  🌙"))
                        .setPositiveButton(UiText.tr(this, "Shine on"), null).show();
            }
        });
        add(credits, version, 14);
        add(body, card(credits, surface), 16);
        add(body, text("Made with care under the same moon.", 13, false, secondary), 20);
    }

    private void open(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (RuntimeException ignored) { Toast.makeText(this, UiText.tr(this, "A browser is required"), Toast.LENGTH_SHORT).show(); }
    }

    private LinearLayout column() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        return v;
    }

    private LinearLayout panel() {
        LinearLayout p = column();
        p.setPadding(dp(18), dp(18), dp(18), dp(18));
        return p;
    }

    private MaterialCardView card(LinearLayout child, int color) {
        MaterialCardView v = new MaterialCardView(this);
        v.setRadius(dp(24));
        v.setCardElevation(0f);
        v.setCardBackgroundColor(color);
        v.addView(child);
        return v;
    }

    private TextView text(String value, int size, boolean strong, int color) {
        TextView t = new TextView(this);
        t.setText(UiText.tr(this, value));
        t.setTextColor(color);
        t.setTextSize(size);
        if (strong) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private MaterialButton button(String value, boolean prominent, Runnable run) {
        MaterialButton b = new MaterialButton(this);
        b.setText(UiText.tr(this, value));
        b.setAllCaps(false);
        b.setCornerRadius(dp(18));
        b.setMinHeight(dp(48));
        b.setBackgroundTintList(ColorStateList.valueOf(prominent ? primary : container));
        b.setTextColor(prominent ? onPrimary : onContainer);
        b.setOnClickListener(v -> run.run());
        return b;
    }

    private void add(LinearLayout p, View v, int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(top);
        p.addView(v, params);
    }

    private int dp(int px) {
        return (int) (px * getResources().getDisplayMetrics().density + 0.5f);
    }
}
