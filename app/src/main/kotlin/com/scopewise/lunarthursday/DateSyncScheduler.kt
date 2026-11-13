package com.scopewise.lunarthursday

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.function.Function

/** Schedules unattended refreshes; timing can be deferred by Android Doze. */
object DateSyncScheduler {
    const val PERIODIC_ID = 20051
    const val ONCE_ID = 20052
    const val MONTH_FIRST_ID = 20060
    const val MONTH_JOB_COUNT = 14
    private const val PERIOD_MS = 6L * 60L * 60L * 1000L
    private const val MIN_RECHECK_MS = 60L * 60L * 1000L
    private const val BACKOFF_MS = 30L * 60L * 1000L

    /** User-requested refresh queues a one-off job; Android decides when it executes. */
    @JvmStatic
    fun requestNow(context: Context): Boolean {
        if (!ReminderStore.prefs(context).getBoolean(ReminderStore.ENABLED, false)) return false
        val jobs = context.getSystemService(JobScheduler::class.java) ?: return false
        jobs.cancel(ONCE_ID)
        val refresh = JobInfo.Builder(ONCE_ID, ComponentName(context, DateSyncJob::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setBackoffCriteria(BACKOFF_MS, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
            .build()
        return jobs.schedule(refresh) == JobScheduler.RESULT_SUCCESS
    }

    @JvmStatic
    fun ensure(context: Context) {
        val jobs = context.getSystemService(JobScheduler::class.java) ?: return
        if (!ReminderStore.prefs(context).getBoolean(ReminderStore.ENABLED, false)) {
            jobs.cancel(PERIODIC_ID)
            jobs.cancel(ONCE_ID)
            for (id in MONTH_FIRST_ID until MONTH_FIRST_ID + MONTH_JOB_COUNT) jobs.cancel(id)
            return
        }
        val component = ComponentName(context, DateSyncJob::class.java)
        if (jobs.getPendingJob(PERIODIC_ID) == null) {
            val periodic = JobInfo.Builder(PERIODIC_ID, component)
                .setPeriodic(PERIOD_MS)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .build()
            jobs.schedule(periodic)
        }
        val previous = ReminderStore.prefs(context).getLong(ReminderStore.LAST_REFRESH_ATTEMPT, 0L)
        val current = System.currentTimeMillis()
        if (current < previous || current - previous >= MIN_RECHECK_MS) {
            if (jobs.getPendingJob(ONCE_ID) == null) {
                val now = JobInfo.Builder(ONCE_ID, component)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setBackoffCriteria(BACKOFF_MS, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                    .build()
                jobs.schedule(now)
            }
        }
        ensureMonthEndJobs(context, jobs, component)
    }

    private fun ensureMonthEndJobs(context: Context, jobs: JobScheduler, component: ComponentName) {
        val now = Instant.now()
        val pakistanToday = now.atZone(LunarEngine.PAKISTAN).toLocalDate()
        val starts = ReminderStore.confirmedStarts(context)
        val today = LunarEngine.expectedOnDate(pakistanToday, starts) ?: return
        val active = today.month
        val previousMonth = if (active.hijriMonth == 1) 12 else active.hijriMonth - 1
        val previousYear = if (active.hijriMonth == 1) active.hijriYear - 1 else active.hijriYear
        val previous = LunarEngine.month(previousYear, previousMonth, starts)
        val nextMonth = if (active.hijriMonth == 12) 1 else active.hijriMonth + 1
        val nextYear = if (active.hijriMonth == 12) active.hijriYear + 1 else active.hijriYear
        val next = LunarEngine.month(nextYear, nextMonth, starts)
        // Include the previous month's late-report window even if a forecast
        // already rolled the displayed calendar to the next month.
        val planned = LongArray(MONTH_JOB_COUNT)
        val reminder = ReminderStore.reminderTime(context)
        val potential = arrayOf(previous, active)
        val following = arrayOf(active.start, next.start)
        val sunset = Function<LocalDate, LocalTime?> { date -> HijriClock.sunsetForDate(context, date) }
        for (block in 0 until 2) {
            for (slot in MonthEndChecks.plan(now, potential[block].start, following[block], sunset, reminder)) {
                planned[block * 7 + slot.slot] = slot.`when`.toEpochMilli()
            }
        }
        val nowMs = System.currentTimeMillis()
        for (index in 0 until MONTH_JOB_COUNT) {
            val jobId = MONTH_FIRST_ID + index
            val due = planned[index]
            val existing = jobs.getPendingJob(jobId)
            if (due <= nowMs) {
                if (existing != null) jobs.cancel(jobId)
                continue
            }
            if (existing != null && existing.extras != null
                && existing.extras.getLong("source_due_ms", 0) == due) continue
            if (existing != null) jobs.cancel(jobId)
            val extras = PersistableBundle()
            extras.putLong("source_due_ms", due)
            val future = JobInfo.Builder(jobId, component)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(Math.max(1L, due - nowMs))
                .setPersisted(true)
                .setBackoffCriteria(BACKOFF_MS, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                .setExtras(extras).build()
            jobs.schedule(future)
        }
    }
}
