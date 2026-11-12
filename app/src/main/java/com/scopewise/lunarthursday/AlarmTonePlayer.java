package com.scopewise.lunarthursday;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;

/** Shared source selection for previews and live ringing; never modifies system settings. */
final class AlarmTonePlayer {
    private AlarmTonePlayer() { }

    static String selection(Context context) {
        String saved = ReminderStore.prefs(context).getString(ReminderStore.ALARM_TONE_ID,
                AlarmTonePolicy.NOOR);
        return AlarmTonePolicy.normalize(saved);
    }

    static void save(Context context, String id, Uri phoneUri) {
        String chosen = AlarmTonePolicy.normalize(id);
        android.content.SharedPreferences.Editor edit = ReminderStore.prefs(context).edit()
                .putString(ReminderStore.ALARM_TONE_ID, chosen);
        if (AlarmTonePolicy.PHONE.equals(chosen) && phoneUri != null) {
            edit.putString(ReminderStore.ALARM_TONE_URI, phoneUri.toString());
        }
        edit.apply();
    }

    static Uri selectedPhoneUri(Context context) {
        String raw = ReminderStore.prefs(context).getString(ReminderStore.ALARM_TONE_URI, "");
        if (raw == null || raw.length() > 2048) return null;
        try {
            Uri uri = Uri.parse(raw);
            String scheme = uri.getScheme();
            return "content".equals(scheme) || "android.resource".equals(scheme) ? uri : null;
        } catch (RuntimeException badUri) { return null; }
    }

    static MediaPlayer prepare(Context context, String selection, boolean looping) {
        String id = AlarmTonePolicy.normalize(selection);
        if (AlarmTonePolicy.PHONE.equals(id)) {
            Uri chosen = selectedPhoneUri(context);
            MediaPlayer saved = tryUri(context, chosen, looping);
            if (saved != null) return saved;
            MediaPlayer def = tryUri(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), looping);
            if (def != null) return def;
        } else {
            MediaPlayer builtIn = tryResource(context, raw(id), looping);
            if (builtIn != null) return builtIn;
        }
        // No silent failure if a selected device tone is removed or inaccessible.
        MediaPlayer backup = tryResource(context, R.raw.hilal_noor, looping);
        if (backup != null) return backup;
        return tryUri(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), looping);
    }

    private static int raw(String id) {
        switch (id) {
            case AlarmTonePolicy.SUKOON: return R.raw.sukoon;
            case AlarmTonePolicy.NIDA: return R.raw.hilal_nida;
            default: return R.raw.hilal_noor;
        }
    }

    private static MediaPlayer fresh() {
        MediaPlayer m = new MediaPlayer();
        m.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
        return m;
    }

    private static MediaPlayer tryResource(Context context, int resource, boolean looping) {
        MediaPlayer m = fresh();
        try {
            try (AssetFileDescriptor fd = context.getResources().openRawResourceFd(resource)) {
                if (fd != null) m.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
                else m.setDataSource(context, Uri.parse("android.resource://" + context.getPackageName() + "/" + resource));
            }
            m.prepare();
            m.setLooping(looping);
            return m;
        } catch (Exception failure) {
            m.release();
            return null;
        }
    }

    private static MediaPlayer tryUri(Context context, Uri uri, boolean looping) {
        if (uri == null) return null;
        MediaPlayer m = fresh();
        try {
            m.setDataSource(context, uri);
            m.prepare();
            m.setLooping(looping);
            return m;
        } catch (Exception failure) {
            m.release();
            return null;
        }
    }
}
