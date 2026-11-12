package com.scopewise.lunarthursday;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

final class ReminderNotifications {
    private static final String CHANNEL_ID = "lunar_thursday_monthly_v1";
    private static final int MONTHLY_ID = 1701;
    private static final int TEST_ID = 1702;

    private ReminderNotifications() { }

    static boolean permitted(Context context) {
        return Build.VERSION.SDK_INT < 33 ||
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED;
    }

    static void ensureChannel(Context context) {
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "First Lunar Thursday", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("One reminder on the first Thursday of each lunar month in Pakistan time");
        manager.createNotificationChannel(channel);
    }

    static void show(Context context, String monthLabel, String basis) {
        String label = monthLabel == null ? "this lunar month" : monthLabel;
        String note = "LIVE_REPORTED_CONFIRMED".equals(basis)
                ? "Live date provider reports confirmation."
                : "Calendar estimate; official sighting may differ.";
        send(context, MONTHLY_ID, "First Lunar Thursday reminder",
                label + " - 11:00 AM Pakistan time. " + note);
    }

    static void test(Context context) {
        send(context, TEST_ID, "Lunar Thursday test notification",
                "Notifications are enabled. Monthly reminders target 11:00 AM PKT.");
    }

    private static void send(Context context, int id, String title, String body) {
        if (!permitted(context)) return;
        ensureChannel(context);
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null || !manager.areNotificationsEnabled()) return;
        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pending = PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_crescent)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build();
        manager.notify(id, notification);
    }
}
