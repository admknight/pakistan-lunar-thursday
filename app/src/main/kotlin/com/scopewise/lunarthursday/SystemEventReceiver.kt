package com.scopewise.lunarthursday

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Device restart, manual clock change, app update and alarm-access grant. */
class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        if (Intent.ACTION_BOOT_COMPLETED == action
            || Intent.ACTION_MY_PACKAGE_REPLACED == action
            || Intent.ACTION_TIME_CHANGED == action
            || Intent.ACTION_TIMEZONE_CHANGED == action
            || AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED == action) {
            ReminderScheduler.sync(context)
            SnoozeReceiver.rearm(context)
            DateSyncScheduler.ensure(context)
            UpdateCheckScheduler.ensure(context)
        }
    }
}
