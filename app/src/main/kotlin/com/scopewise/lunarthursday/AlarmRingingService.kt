package com.scopewise.lunarthursday

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator

/** Optional real ringing reminder. Always stops audio/vibration after 120 seconds. */
class AlarmRingingService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var label = "Hilal Thursday"
    private var monthKey = ""
    private var basis = ""
    private var test = false
    private var active = false
    private val expire = Runnable { finishWithMissedAlert() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY
        val action = intent.action
        if (ACTION_DISMISS == action) {
            stopRing()
            return START_NOT_STICKY
        }
        if (ACTION_SNOOZE == action) {
            if (!test && monthKey.isNotEmpty()) SnoozeReceiver.schedule(this, monthKey, label, basis)
            stopRing()
            return START_NOT_STICKY
        }
        if (ACTION_START != action) return START_NOT_STICKY
        if (active) return START_NOT_STICKY

        val p = ReminderStore.prefs(this)
        test = intent.getBooleanExtra(EXTRA_TEST, false)
        if (!test && (!p.getBoolean(ReminderStore.ENABLED, false) || !p.getBoolean(ReminderStore.ALARM_MODE, false))) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        val givenLabel = intent.getStringExtra(EXTRA_LABEL)
        label = if (givenLabel.isNullOrEmpty()) "Hilal Thursday" else givenLabel
        monthKey = intent.getStringExtra(EXTRA_KEY) ?: ""
        basis = intent.getStringExtra(EXTRA_BASIS) ?: ""
        ensureChannel()
        try {
            val notification = buildForegroundNotification()
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (failure: RuntimeException) {
            stopSelf(startId)
            if (!test) ReminderNotifications.show(this, label, basis)
            return START_NOT_STICKY
        }
        active = true
        if (p.getBoolean(ReminderStore.ALARM_SOUND, true)) beginSound()
        if (p.getBoolean(ReminderStore.ALARM_VIBRATE, true)) beginVibration()
        handler.postDelayed(expire, MAX_RING_MS)
        return START_NOT_STICKY
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(CHANNEL, UiText.tr(this, "Ringing lunar alarms"),
            NotificationManager.IMPORTANCE_HIGH)
        channel.description = UiText.tr(this, "Optional ringing alarm. Sound and vibration are controlled inside the app.")
        channel.setSound(null, null) // Avoid duplicate ringtone from service and notification.
        channel.enableVibration(false)
        manager.createNotificationChannel(channel)
    }

    private fun serviceAction(action: String, requestCode: Int): PendingIntent {
        val i = Intent(this, AlarmRingingService::class.java).setAction(action)
        return PendingIntent.getService(this, requestCode, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun buildForegroundNotification(): Notification {
        val screen = Intent(this, AlarmActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_LABEL, label).putExtra(EXTRA_TEST, test)
        val display = PendingIntent.getActivity(this, 1901, screen,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val noIcon: Icon? = null
        val builder = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_crescent)
            .setContentTitle(UiText.tr(this, if (test) "Test ringing alarm" else "Hilal Thursday alarm"))
            .setContentText(UiText.monthLabel(this, label) + UiText.tr(this, " · 11:00 AM Pakistan time"))
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(display)
            .addAction(Notification.Action.Builder(noIcon, UiText.tr(this, "Dismiss"),
                serviceAction(ACTION_DISMISS, 1902)).build())
        if (!test) {
            builder.addAction(Notification.Action.Builder(noIcon, UiText.tr(this, "Snooze 10 min"),
                serviceAction(ACTION_SNOOZE, 1903)).build())
        }
        if (canUseFullScreen(this)) builder.setFullScreenIntent(display, true)
        return builder.build()
    }

    private fun beginSound() {
        // Without a playable tone the service and optional vibration remain functional.
        val prepared = AlarmTonePlayer.prepare(this, AlarmTonePlayer.selection(this), true) ?: return
        try {
            prepared.start()
            player = prepared
        } catch (failure: RuntimeException) {
            prepared.release()
            player = null
        }
    }

    @Suppress("DEPRECATION")
    private fun beginVibration() {
        val device = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator = device
        if (device == null || !device.hasVibrator()) return
        try {
            device.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 650, 450), 0))
        } catch (ignored: RuntimeException) {
        }
    }

    private fun finishWithMissedAlert() {
        if (!active) return
        val shouldNotify = !test
        val lastLabel = label
        val lastBasis = basis
        stopRing()
        if (shouldNotify) ReminderNotifications.missed(this, lastLabel, lastBasis)
    }

    private fun stopRing() {
        handler.removeCallbacks(expire)
        val playing = player
        if (playing != null) {
            try {
                if (playing.isPlaying) playing.stop()
            } catch (ignored: RuntimeException) {
            }
            playing.release()
            player = null
        }
        val device = vibrator
        if (device != null) {
            try {
                device.cancel()
            } catch (ignored: RuntimeException) {
            }
            vibrator = null
        }
        active = false
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (ignored: RuntimeException) {
        }
        stopSelf()
    }

    override fun onDestroy() {
        stopRing()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.scopewise.lunarthursday.ALARM_START"
        const val ACTION_DISMISS = "com.scopewise.lunarthursday.ALARM_DISMISS"
        const val ACTION_SNOOZE = "com.scopewise.lunarthursday.ALARM_SNOOZE"
        const val EXTRA_LABEL = "lunar_alarm_label"
        const val EXTRA_KEY = "lunar_alarm_month_key"
        const val EXTRA_BASIS = "lunar_alarm_basis"
        const val EXTRA_TEST = "lunar_alarm_test"
        private const val CHANNEL = "lunar_thursday_ringing_v1"
        private const val NOTIFICATION_ID = 1801
        private const val MAX_RING_MS = 120_000L

        @JvmStatic
        fun canUseFullScreen(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < 34) return true
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            return manager != null && manager.canUseFullScreenIntent()
        }

        @JvmStatic
        fun start(context: Context, month: String?, key: String?, source: String?, test: Boolean): Boolean {
            val i = Intent(context, AlarmRingingService::class.java).setAction(ACTION_START)
                .putExtra(EXTRA_LABEL, month ?: "Hilal Thursday")
                .putExtra(EXTRA_KEY, key ?: "")
                .putExtra(EXTRA_BASIS, source ?: "")
                .putExtra(EXTRA_TEST, test)
            return try {
                context.startForegroundService(i)
                true
            } catch (notAllowed: RuntimeException) {
                false
            }
        }

        @JvmStatic
        fun startTest(context: Context): Boolean = start(context, "Test ringing alarm", "test", "TEST", true)

        @JvmStatic
        fun command(context: Context, action: String) {
            val i = Intent(context, AlarmRingingService::class.java).setAction(action)
            try {
                context.startService(i)
            } catch (ignored: RuntimeException) {
            }
        }
    }
}
