package com.scopewise.lunarthursday;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;

/** Once-a-day best-effort GitHub release checks while the app is closed. */
final class UpdateCheckScheduler {
    private static final int PERIODIC_ID=20053;
    private static final long PERIOD_MS=24L*60L*60L*1000L;

    private UpdateCheckScheduler() { }

    static void ensure(Context context) {
        JobScheduler jobs=context.getSystemService(JobScheduler.class);
        if(jobs==null)return;
        boolean active=ReminderStore.prefs(context).getBoolean(UpdateManager.AUTO_CHECK,true)
                && ReminderStore.prefs(context).getBoolean(UpdateManager.BACKGROUND_CHECK,true);
        if(!active){jobs.cancel(PERIODIC_ID);return;}
        if(jobs.getPendingJob(PERIODIC_ID)==null){
            JobInfo job=new JobInfo.Builder(PERIODIC_ID,new ComponentName(context,UpdateCheckJob.class))
                .setPeriodic(PERIOD_MS)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true).build();
            jobs.schedule(job);
        }
    }
}
