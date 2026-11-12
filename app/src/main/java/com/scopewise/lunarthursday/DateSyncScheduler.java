package com.scopewise.lunarthursday;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;

/** Schedules unattended refreshes; timing can be deferred by Android Doze. */
final class DateSyncScheduler {
    static final int PERIODIC_ID = 20051;
    static final int ONCE_ID = 20052;
    private static final long PERIOD_MS = 6L * 60L * 60L * 1000L;
    private static final long MIN_RECHECK_MS = 60L * 60L * 1000L;

    private DateSyncScheduler() { }

    /** User-requested refresh queues a one-off job; Android decides when it executes. */
    static boolean requestNow(Context context) {
        if (!ReminderStore.prefs(context).getBoolean(ReminderStore.ENABLED, false)) return false;
        JobScheduler jobs = context.getSystemService(JobScheduler.class);
        if (jobs == null) return false;
        jobs.cancel(ONCE_ID);
        JobInfo refresh = new JobInfo.Builder(ONCE_ID,
                new ComponentName(context, DateSyncJob.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setBackoffCriteria(30L * 60L * 1000L,
                        JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                .build();
        return jobs.schedule(refresh) == JobScheduler.RESULT_SUCCESS;
    }

    static void ensure(Context context) {
        JobScheduler jobs = context.getSystemService(JobScheduler.class);
        if (jobs == null) return;
        if (!ReminderStore.prefs(context).getBoolean(ReminderStore.ENABLED, false)) {
            jobs.cancel(PERIODIC_ID);
            jobs.cancel(ONCE_ID);
            return;
        }
        ComponentName component = new ComponentName(context, DateSyncJob.class);
        if (jobs.getPendingJob(PERIODIC_ID) == null) {
            JobInfo periodic = new JobInfo.Builder(PERIODIC_ID, component)
                    .setPeriodic(PERIOD_MS)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true)
                    .build();
            jobs.schedule(periodic);
        }
        long previous = ReminderStore.prefs(context).getLong(
                ReminderStore.LAST_REFRESH_ATTEMPT, 0L);
        long current = System.currentTimeMillis();
        if (current < previous || current - previous >= MIN_RECHECK_MS) {
            if (jobs.getPendingJob(ONCE_ID) == null) {
                JobInfo now = new JobInfo.Builder(ONCE_ID, component)
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .setBackoffCriteria(30L * 60L * 1000L,
                                JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                        .build();
                jobs.schedule(now);
            }
        }
    }
}
