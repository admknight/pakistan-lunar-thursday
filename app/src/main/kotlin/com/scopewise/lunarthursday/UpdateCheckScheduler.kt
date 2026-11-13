package com.scopewise.lunarthursday

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context

/** Once-a-day best-effort GitHub release checks while the app is closed. */
object UpdateCheckScheduler {
    private const val PERIODIC_ID = 20053
    private const val PERIOD_MS = 24L * 60L * 60L * 1000L

    @JvmStatic
    fun ensure(context: Context) {
        val jobs = context.getSystemService(JobScheduler::class.java) ?: return
        val active = ReminderStore.prefs(context).getBoolean(UpdateManager.AUTO_CHECK, true)
            && ReminderStore.prefs(context).getBoolean(UpdateManager.BACKGROUND_CHECK, true)
        if (!active) {
            jobs.cancel(PERIODIC_ID)
            return
        }
        if (jobs.getPendingJob(PERIODIC_ID) == null) {
            val job = JobInfo.Builder(PERIODIC_ID, ComponentName(context, UpdateCheckJob::class.java))
                .setPeriodic(PERIOD_MS)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true).build()
            jobs.schedule(job)
        }
    }
}
