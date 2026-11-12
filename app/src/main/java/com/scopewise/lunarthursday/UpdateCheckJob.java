package com.scopewise.lunarthursday;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

/** Best-effort notification for newly published stable releases; no unattended APK install. */
public final class UpdateCheckJob extends JobService {
    private static final String CHANNEL="hilal_version_updates_v1";
    @Override public boolean onStartJob(JobParameters params) {
        UpdateManager.checkInBackground(this,()->jobFinished(params,false));
        return true;
    }
    @Override public boolean onStopJob(JobParameters params) {return false;}

    static boolean notifyUpdate(Context context,String version) {
        if(Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                !=PackageManager.PERMISSION_GRANTED)return false;
        NotificationManager manager=context.getSystemService(NotificationManager.class);
        if(manager==null||!manager.areNotificationsEnabled())return false;
        NotificationChannel channel=new NotificationChannel(CHANNEL,"App version updates",NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("New permanently signed Hilal Thursday releases available to download");
        manager.createNotificationChannel(channel);
        Intent open=new Intent(context,MainActivity.class)
                .putExtra("open_tab",2)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent click=PendingIntent.getActivity(context,20054,open,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification notification=new Notification.Builder(context,CHANNEL)
                .setSmallIcon(R.drawable.ic_crescent)
                .setContentTitle(UiText.tr(context,"Hilal Thursday update available"))
                .setContentText(UiText.tr(context,"Version ")+VersionPolicy.display(version)+UiText.tr(context," is ready. Tap to download inside this app."))
                .setContentIntent(click).setAutoCancel(true).build();
        manager.notify(20054,notification);
        return true;
    }
}
