package com.scopewise.lunarthursday

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import java.time.LocalDate

object ReminderNotifications {
    private const val CHANNEL_ID = "lunar_thursday_monthly_v1"
    private const val MONTHLY_ID = 1701
    private const val TEST_ID = 1702
    private const val EXTRA_ID = 1703
    private const val OBSERVANCE_ID = 1704

    @JvmStatic
    fun permitted(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    @JvmStatic
    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channel = NotificationChannel(CHANNEL_ID,
            UiText.tr(context, "First Lunar Thursday"), NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = UiText.tr(context, "One reminder on the first Thursday of each lunar month in Pakistan time")
        manager.createNotificationChannel(channel)
    }

    @JvmStatic
    fun show(context: Context, monthLabel: String?, basis: String?) {
        val label = UiText.monthLabel(context, monthLabel)
        val note = if ("LIVE_REPORTED_CONFIRMED" == basis)
            UiText.tr(context, "Live date provider reports confirmation.")
        else UiText.tr(context, "Calendar estimate; official sighting may differ.")
        send(context, MONTHLY_ID, UiText.tr(context, "First Hilal Thursday reminder"),
            label + UiText.tr(context, " at ") + UiText.reminderTimeText(context) +
                UiText.tr(context, " Pakistan time. ") + note)
    }

    /** Unconfirmed month, other candidate Thursday: says plainly that it is not certain. */
    @JvmStatic
    fun possible(context: Context, monthLabel: String?, expectedDateIso: String?) {
        val localized = AppLanguage.wrap(context)
        val forecast = try {
            UiText.date(context, LocalDate.parse(expectedDateIso), true)
        } catch (missingOrInvalid: RuntimeException) {
            "?"
        }
        send(context, MONTHLY_ID, localized.getString(R.string.possible_thursday_title),
            localized.getString(R.string.possible_thursday_body, UiText.monthLabel(context, monthLabel), forecast))
    }

    /** A day's observances: an ordinary notification, separate from the monthly reminder. */
    @JvmStatic
    fun observance(context: Context, title: String, body: String) {
        send(context, OBSERVANCE_ID, title, body)
    }

    /** Prayer-time, Thursday-evening and Friday reminders: ordinary notifications. */
    @JvmStatic
    fun extra(context: Context, title: String, body: String) {
        send(context, EXTRA_ID, title, body)
    }

    @JvmStatic
    fun missed(context: Context, monthLabel: String?, basis: String?) {
        send(context, MONTHLY_ID, UiText.tr(context, "Hilal Thursday alarm was missed"),
            UiText.monthLabel(context, monthLabel ?: "Hilal Thursday") +
                UiText.tr(context, ": the ringing alarm stopped after two minutes. Open the app to review your reminder."))
    }

    @JvmStatic
    fun test(context: Context) {
        send(context, TEST_ID, UiText.tr(context, "Hilal Thursday test notification"),
            UiText.tr(context, "Notifications are enabled. Monthly reminders target ") +
                UiText.reminderTimeText(context) + " PKT.")
    }

    private fun send(context: Context, id: Int, title: String, body: String) {
        if (!permitted(context)) return
        ensureChannel(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (manager == null || !manager.areNotificationsEnabled()) return
        val open = Intent(context, MainActivity::class.java)
        open.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val pending = PendingIntent.getActivity(context, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_crescent)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        manager.notify(id, notification)
    }
}
