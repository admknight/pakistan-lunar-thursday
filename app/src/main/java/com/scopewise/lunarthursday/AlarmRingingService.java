package com.scopewise.lunarthursday;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;

/** Optional real ringing reminder. Always stops audio/vibration after 120 seconds. */
public final class AlarmRingingService extends Service {
    static final String ACTION_START = "com.scopewise.lunarthursday.ALARM_START";
    static final String ACTION_DISMISS = "com.scopewise.lunarthursday.ALARM_DISMISS";
    static final String ACTION_SNOOZE = "com.scopewise.lunarthursday.ALARM_SNOOZE";
    static final String EXTRA_LABEL = "lunar_alarm_label";
    static final String EXTRA_KEY = "lunar_alarm_month_key";
    static final String EXTRA_BASIS = "lunar_alarm_basis";
    static final String EXTRA_TEST = "lunar_alarm_test";
    private static final String CHANNEL = "lunar_thursday_ringing_v1";
    private static final int NOTIFICATION_ID = 1801;
    private static final long MAX_RING_MS = 120_000L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private MediaPlayer player;
    private Vibrator vibrator;
    private String label = "Hilal Thursday";
    private String monthKey = "";
    private String basis = "";
    private boolean test;
    private boolean active;
    private final Runnable expire = this::finishWithMissedAlert;

    @Override public IBinder onBind(Intent intent) { return null; }

    static boolean canUseFullScreen(Context context) {
        if (Build.VERSION.SDK_INT < 34) return true;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        return manager != null && manager.canUseFullScreenIntent();
    }

    static boolean start(Context context, String month, String key, String source, boolean test) {
        Intent i = new Intent(context, AlarmRingingService.class).setAction(ACTION_START)
                .putExtra(EXTRA_LABEL, month == null ? "Hilal Thursday" : month)
                .putExtra(EXTRA_KEY, key == null ? "" : key)
                .putExtra(EXTRA_BASIS, source == null ? "" : source)
                .putExtra(EXTRA_TEST, test);
        try {
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i);
            else context.startService(i);
            return true;
        } catch (RuntimeException notAllowed) {
            return false;
        }
    }

    static boolean startTest(Context context) {
        return start(context, "Test ringing alarm", "test", "TEST", true);
    }

    static void command(Context context, String action) {
        Intent i = new Intent(context, AlarmRingingService.class).setAction(action);
        try { context.startService(i); }
        catch (RuntimeException ignored) { }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if (ACTION_DISMISS.equals(action)) {
            stopRing(false);
            return START_NOT_STICKY;
        }
        if (ACTION_SNOOZE.equals(action)) {
            if (!test && !monthKey.isEmpty()) SnoozeReceiver.schedule(this, monthKey, label, basis);
            stopRing(false);
            return START_NOT_STICKY;
        }
        if (!ACTION_START.equals(action)) return START_NOT_STICKY;
        if (active) return START_NOT_STICKY;

        SharedPreferences p = ReminderStore.prefs(this);
        test = intent.getBooleanExtra(EXTRA_TEST, false);
        if (!test && (!p.getBoolean(ReminderStore.ENABLED, false)
                || !p.getBoolean(ReminderStore.ALARM_MODE, false))) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        label = intent.getStringExtra(EXTRA_LABEL);
        if (label == null || label.isEmpty()) label = "Hilal Thursday";
        monthKey = intent.getStringExtra(EXTRA_KEY);
        if (monthKey == null) monthKey = "";
        basis = intent.getStringExtra(EXTRA_BASIS);
        if (basis == null) basis = "";
        ensureChannel();
        try {
            Notification notification = buildForegroundNotification();
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (RuntimeException failure) {
            stopSelf(startId);
            if (!test) ReminderNotifications.show(this, label, basis);
            return START_NOT_STICKY;
        }
        active = true;
        if (p.getBoolean(ReminderStore.ALARM_SOUND, true)) beginSound();
        if (p.getBoolean(ReminderStore.ALARM_VIBRATE, true)) beginVibration();
        handler.postDelayed(expire, MAX_RING_MS);
        return START_NOT_STICKY;
    }

    private void ensureChannel() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL, UiText.tr(this, "Ringing lunar alarms"),
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(UiText.tr(this, "Optional ringing alarm. Sound and vibration are controlled inside the app."));
        channel.setSound(null, null); // Avoid duplicate ringtone from service and notification.
        channel.enableVibration(false);
        manager.createNotificationChannel(channel);
    }

    private PendingIntent serviceAction(String action, int requestCode) {
        Intent i = new Intent(this, AlarmRingingService.class).setAction(action);
        return PendingIntent.getService(this, requestCode, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private Notification buildForegroundNotification() {
        Intent screen = new Intent(this, AlarmActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_LABEL, label).putExtra(EXTRA_TEST, test);
        PendingIntent display = PendingIntent.getActivity(this, 1901, screen,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_crescent)
                .setContentTitle(UiText.tr(this, test ? "Test ringing alarm" : "Hilal Thursday alarm"))
                .setContentText(UiText.monthLabel(this, label) + UiText.tr(this, " · 11:00 AM Pakistan time"))
                .setCategory(Notification.CATEGORY_ALARM)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(display)
                .addAction(new Notification.Action.Builder(null, UiText.tr(this, "Dismiss"),
                        serviceAction(ACTION_DISMISS, 1902)).build());
        if (!test) builder.addAction(new Notification.Action.Builder(null, UiText.tr(this, "Snooze 10 min"),
                serviceAction(ACTION_SNOOZE, 1903)).build());
        if (canUseFullScreen(this)) builder.setFullScreenIntent(display, true);
        return builder.build();
    }

    private void beginSound() {
        Uri alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (!tryPlay(alarm)) tryPlay(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
    }

    private boolean tryPlay(Uri uri) {
        if (uri == null) return false;
        MediaPlayer candidate = new MediaPlayer();
        try {
            candidate.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            candidate.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
            candidate.setDataSource(this, uri);
            candidate.setLooping(true);
            candidate.prepare();
            candidate.start();
            player = candidate;
            return true;
        } catch (Exception failure) {
            candidate.release();
            return false;
        }
    }

    private void beginVibration() {
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) return;
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 650, 450}, 0));
            } else {
                vibrator.vibrate(new long[]{0, 650, 450}, 0);
            }
        } catch (RuntimeException ignored) { }
    }

    private void finishWithMissedAlert() {
        if (!active) return;
        boolean shouldNotify = !test;
        String lastLabel = label;
        String lastBasis = basis;
        stopRing(true);
        if (shouldNotify) ReminderNotifications.missed(this, lastLabel, lastBasis);
    }

    private void stopRing(boolean timedOut) {
        handler.removeCallbacks(expire);
        if (player != null) {
            try { if (player.isPlaying()) player.stop(); }
            catch (RuntimeException ignored) { }
            player.release();
            player = null;
        }
        if (vibrator != null) {
            try { vibrator.cancel(); } catch (RuntimeException ignored) { }
            vibrator = null;
        }
        active = false;
        try { stopForeground(STOP_FOREGROUND_REMOVE); } catch (RuntimeException ignored) { }
        stopSelf();
    }

    @Override public void onDestroy() {
        stopRing(false);
        super.onDestroy();
    }
}
