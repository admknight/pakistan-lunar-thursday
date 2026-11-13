package com.scopewise.lunarthursday

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

/** Best-effort notification for newly published stable releases; no unattended APK install. */
class UpdateCheckJob : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        UpdateManager.checkInBackground(this, Runnable { jobFinished(params, false) })
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = false

    companion object {
        private const val CHANNEL = "hilal_version_updates_v1"

        @JvmStatic
        fun notifyUpdate(context: Context, version: String?): Boolean {
            if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return false
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager == null || !manager.areNotificationsEnabled()) return false
            val channel = NotificationChannel(CHANNEL, "App version updates", NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = "New permanently signed Hilal Thursday releases available to download"
            manager.createNotificationChannel(channel)
            val open = Intent(context, MainActivity::class.java)
                .putExtra("open_tab", 2)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val click = PendingIntent.getActivity(context, 20054, open,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification = Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_crescent)
                .setContentTitle(UiText.tr(context, "Hilal Thursday update available"))
                .setContentText(UiText.tr(context, "Version ") + VersionPolicy.display(version) +
                    UiText.tr(context, " is ready. Tap to download inside this app."))
                .setContentIntent(click).setAutoCancel(true).build()
            manager.notify(20054, notification)
            return true
        }
    }
}
