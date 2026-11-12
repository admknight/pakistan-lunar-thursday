package com.scopewise.lunarthursday;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.DynamicColors;

/** Optional large-format alarm controls; also accessible from notification actions. */
public final class AlarmActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        DynamicColors.applyToActivityIfAvailable(this);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(0x00080000 | 0x00200000); // SHOW_WHEN_LOCKED | TURN_SCREEN_ON
        }
        String label = getIntent().getStringExtra(AlarmRingingService.EXTRA_LABEL);
        boolean test = getIntent().getBooleanExtra(AlarmRingingService.EXTRA_TEST, false);
        if (label == null) label = "Lunar Thursday";
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setGravity(Gravity.CENTER);
        body.setPadding(dp(32), dp(48), dp(32), dp(40));
        TextView title = new TextView(this);
        title.setText(test ? "Test alarm" : "Lunar Thursday alarm");
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        body.addView(title);
        TextView caption = new TextView(this);
        caption.setText(label + "\n11:00 AM Pakistan time");
        caption.setTextSize(18);
        caption.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams cap = new LinearLayout.LayoutParams(-1, -2);
        cap.topMargin = dp(18);
        body.addView(caption, cap);
        MaterialButton dismiss = new MaterialButton(this);
        dismiss.setText("Dismiss alarm");
        dismiss.setAllCaps(false);
        dismiss.setOnClickListener(v -> command(AlarmRingingService.ACTION_DISMISS));
        LinearLayout.LayoutParams buttons = new LinearLayout.LayoutParams(-1, dp(56));
        buttons.topMargin = dp(28);
        body.addView(dismiss, buttons);
        if (!test) {
            MaterialButton snooze = new MaterialButton(this);
            snooze.setText("Snooze 10 minutes");
            snooze.setAllCaps(false);
            snooze.setOnClickListener(v -> command(AlarmRingingService.ACTION_SNOOZE));
            LinearLayout.LayoutParams row = new LinearLayout.LayoutParams(-1, dp(56));
            row.topMargin = dp(12);
            body.addView(snooze, row);
        }
        setContentView(body, new ViewGroup.LayoutParams(-1, -1));
    }

    private void command(String action) {
        AlarmRingingService.command(this, action);
        finish();
    }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + .5f); }
}
