package com.scopewise.lunarthursday;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.MaterialColors;

/** Offline original tone selection plus stock Android system alarm-tone picker. */
public final class AlarmToneActivity extends LocalizedActivity {
    private static final int PICK_PHONE_ALARM = 193;
    private int surface, onSurface, secondary, primary, container, onContainer;
    private MediaPlayer preview;
    private String playing = "";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        DynamicColors.applyToActivityIfAvailable(this);
        applyColors();
        render();
    }

    private void applyColors() {
        surface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, 0xFFFFFFFF);
        onSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, 0xFF1B211E);
        secondary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF4F5752);
        primary = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary, 0xFF136F53);
        container = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimaryContainer, 0xFFBCEAD3);
        onContainer = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimaryContainer, 0xFF002114);
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(surface);
        LinearLayout page = column();
        page.setPadding(dp(20), dp(26), dp(20), dp(38));
        scroll.addView(page);
        setContentView(scroll);

        add(page, actionText("‹   " + getString(R.string.tone_back), this::finish), 0);
        add(page, text(getString(R.string.tone_heading), 26, true, onSurface), 14);
        add(page, text(getString(R.string.tone_intro), 14, false, secondary), 8);
        add(page, text(getString(R.string.tone_signature_section), 16, true, onSurface), 20);

        add(page, toneCard(AlarmTonePolicy.NOOR, "🌙", R.string.tone_noor,
                R.string.tone_noor_desc), 12);
        add(page, toneCard(AlarmTonePolicy.SUKOON, "✨", R.string.tone_sukoon,
                R.string.tone_sukoon_desc), 9);
        add(page, toneCard(AlarmTonePolicy.NIDA, "🕌", R.string.tone_nida,
                R.string.tone_nida_desc), 9);
        add(page, text(getString(R.string.tone_phone_section), 16, true, onSurface), 20);
        add(page, toneCard(AlarmTonePolicy.PHONE, "📱", R.string.tone_phone,
                R.string.tone_phone_desc), 12);
        add(page, actionText(getString(R.string.tone_stop), () -> {
            stopPreview();
            render();
        }), 14);
        add(page, text(getString(R.string.tone_footer), 12, false, secondary), 16);
    }

    private View toneCard(String id, String symbol, int titleId, int descriptionId) {
        final boolean selected = id.equals(AlarmTonePlayer.selection(this));
        final boolean active = id.equals(playing);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(14), dp(13), dp(11), dp(13));
        TextView emoji = text(symbol, 25, false, onSurface);
        emoji.setGravity(Gravity.CENTER);
        row.addView(emoji, new LinearLayout.LayoutParams(dp(44), dp(54)));
        LinearLayout copy = column();
        add(copy, text(getString(titleId), 16, true, selected ? onContainer : onSurface), 0);
        add(copy, text(getString(descriptionId), 12, false, selected ? onContainer : secondary), 4);
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));

        TextView chosen = text(selected ? "✓" : "○", 23, true, selected ? primary : secondary);
        chosen.setGravity(Gravity.CENTER);
        chosen.setContentDescription(getString(selected ? R.string.tone_selected : R.string.tone_select));
        chosen.setOnClickListener(v -> select(id));
        row.addView(chosen, new LinearLayout.LayoutParams(dp(34), dp(48)));
        TextView play = text(active ? "■" : "▶", 22, true, primary);
        play.setGravity(Gravity.CENTER);
        play.setContentDescription(getString(active ? R.string.tone_stop : R.string.tone_preview));
        play.setOnClickListener(v -> togglePreview(id));
        row.addView(play, new LinearLayout.LayoutParams(dp(40), dp(48)));
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(20));
        card.setCardBackgroundColor(selected ? container : surface);
        card.setStrokeColor(selected ? primary : secondary);
        card.setStrokeWidth(dp(selected ? 2 : 1));
        card.setCardElevation(0f);
        card.addView(row);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> select(id));
        return card;
    }

    private void select(String id) {
        if (AlarmTonePolicy.PHONE.equals(id)) {
            choosePhoneAlarm();
            return;
        }
        AlarmTonePlayer.save(this, id, null);
        render();
    }

    private void togglePreview(String id) {
        if (id.equals(playing)) {
            stopPreview();
            render();
            return;
        }
        stopPreview();
        MediaPlayer candidate = AlarmTonePlayer.prepare(this, id, false);
        if (candidate == null) {
            Toast.makeText(this, getString(R.string.tone_cannot_play), Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            candidate.setVolume(0.72f, 0.72f);
            candidate.setOnCompletionListener(completed -> {
                stopPreview();
                if (!isFinishing() && !isDestroyed()) render();
            });
            candidate.start();
            preview = candidate;
            playing = id;
            render();
        } catch (RuntimeException failure) {
            candidate.release();
            playing = "";
            Toast.makeText(this, getString(R.string.tone_cannot_play), Toast.LENGTH_SHORT).show();
        }
    }

    private void choosePhoneAlarm() {
        stopPreview();
        Intent picker = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.tone_phone_picker))
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                        AlarmTonePlayer.selectedPhoneUri(this) == null
                                ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                                : AlarmTonePlayer.selectedPhoneUri(this));
        try {
            startActivityForResult(picker, PICK_PHONE_ALARM);
        } catch (RuntimeException noPicker) {
            Toast.makeText(this, getString(R.string.tone_picker_unavailable), Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_PHONE_ALARM || resultCode != Activity.RESULT_OK || data == null) return;
        Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        if (uri == null) return; // Silent sound is intentionally unavailable in this picker.
        String scheme = uri.getScheme();
        if (!"content".equals(scheme) && !"android.resource".equals(scheme)) {
            Toast.makeText(this, getString(R.string.tone_picker_unavailable), Toast.LENGTH_LONG).show();
            return;
        }
        // Some Android media pickers grant durable read access; gracefully handle ones that do not.
        try {
            if ((data.getFlags() & Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) != 0) {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        } catch (SecurityException ignored) { }
        AlarmTonePlayer.save(this, AlarmTonePolicy.PHONE, uri);
        render();
    }

    private void stopPreview() {
        playing = "";
        if (preview == null) return;
        try { if (preview.isPlaying()) preview.stop(); } catch (RuntimeException ignored) { }
        preview.release();
        preview = null;
    }

    @Override protected void onPause() {
        stopPreview();
        super.onPause();
    }

    @Override protected void onDestroy() {
        stopPreview();
        super.onDestroy();
    }

    private LinearLayout column() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        return box;
    }
    private TextView text(String value, int sp, boolean strong, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (strong) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private TextView actionText(String value, Runnable action) {
        TextView button = text(value, 15, true, primary);
        button.setPadding(dp(7), dp(9), dp(7), dp(9));
        button.setClickable(true);
        button.setFocusable(true);
        button.setOnClickListener(v -> action.run());
        return button;
    }
    private void add(LinearLayout parent, View child, int margin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(margin);
        parent.addView(child, p);
    }
    private int dp(int n) { return (int) (n*getResources().getDisplayMetrics().density+.5f); }
}
