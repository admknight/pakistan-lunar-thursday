package com.scopewise.lunarthursday;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/** Schedules unattended refreshes; timing can be deferred by Android Doze. */
final class DateSyncScheduler {
    static final int PERIODIC_ID = 20051;
    static final int ONCE_ID = 20052;
    static final int MONTH_FIRST_ID = 20060;
    static final int MONTH_JOB_COUNT = 14;
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
            for (int id=MONTH_FIRST_ID; id<MONTH_FIRST_ID+MONTH_JOB_COUNT; id++) jobs.cancel(id);
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
        ensureMonthEndJobs(context, jobs, component);
    }

    private static void ensureMonthEndJobs(Context context, JobScheduler jobs,
                                           ComponentName component) {
        Instant now = Instant.now();
        LocalDate pakistanToday = now.atZone(LunarEngine.PAKISTAN).toLocalDate();
        Map<String, LocalDate> starts = ReminderStore.confirmedStarts(context);
        LunarEngine.HijriDate today = LunarEngine.expectedOnDate(pakistanToday, starts);
        if (today == null) return;
        LunarEngine.Month active = today.month;
        int previousMonth = active.hijriMonth == 1 ? 12 : active.hijriMonth - 1;
        int previousYear = active.hijriMonth == 1 ? active.hijriYear - 1 : active.hijriYear;
        LunarEngine.Month previous = LunarEngine.month(previousYear, previousMonth, starts);
        int nextMonth = active.hijriMonth == 12 ? 1 : active.hijriMonth + 1;
        int nextYear = active.hijriMonth == 12 ? active.hijriYear + 1 : active.hijriYear;
        LunarEngine.Month next = LunarEngine.month(nextYear, nextMonth, starts);
        // Include the previous month's late-report window even if a forecast
        // already rolled the displayed calendar to the next month.
        long[] planned = new long[MONTH_JOB_COUNT];
        java.time.LocalTime reminder = ReminderStore.reminderTime(context);
        LunarEngine.Month[] potential = new LunarEngine.Month[]{previous, active};
        LocalDate[] following = new LocalDate[]{active.start, next.start};
        for (int block=0; block<2; block++) {
            for (MonthEndChecks.Slot slot : MonthEndChecks.plan(now,
                    potential[block].start, following[block],
                    date -> HijriClock.sunsetForDate(context, date), reminder)) {
                planned[block*7+slot.slot] = slot.when.toEpochMilli();
            }
        }
        long nowMs = System.currentTimeMillis();
        for(int index=0;index<MONTH_JOB_COUNT;index++) {
            int jobId = MONTH_FIRST_ID+index;
            long when = planned[index];
            JobInfo existing = jobs.getPendingJob(jobId);
            if(when <= nowMs) {
                if(existing!=null) jobs.cancel(jobId);
                continue;
            }
            if(existing!=null && existing.getExtras()!=null
                    && existing.getExtras().getLong("source_due_ms",0)==when) continue;
            if(existing!=null) jobs.cancel(jobId);
            PersistableBundle extras=new PersistableBundle();
            extras.putLong("source_due_ms",when);
            JobInfo future=new JobInfo.Builder(jobId,component)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setMinimumLatency(Math.max(1L,when-nowMs))
                    .setPersisted(true)
                    .setBackoffCriteria(30L*60L*1000L,JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                    .setExtras(extras).build();
            jobs.schedule(future);
        }
    }
}
