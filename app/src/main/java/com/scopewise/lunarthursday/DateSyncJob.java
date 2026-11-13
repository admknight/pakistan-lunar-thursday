package com.scopewise.lunarthursday;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import javax.net.ssl.HttpsURLConnection;

/**
 * Polls an openly documented public date feed. We only store rows that the
 * provider explicitly marks confirmed, never its calculated estimates.
 * This provider is independent of the Government of Pakistan.
 */
public final class DateSyncJob extends JobService {
    static final String FEED_URL = "https://islamicdatetoday.pk/data/today.json";
    private volatile Thread worker;
    private volatile boolean stopped;

    @Override public boolean onStartJob(JobParameters params) {
        stopped = false;
        worker = new Thread(() -> {
            boolean connected = false;
            try {
                connected = updateFromFeed();
            } catch (Exception ignored) {
                // Keep cached observed dates; scheduling continues from fallback.
            }
            // The project's own corrections file is read after the feed, so a
            // confirmed month start from it is the value that stays stored.
            boolean corrections = false;
            try {
                corrections = CorrectionsSync.fetchAndApply(this);
            } catch (Exception ignored) {
                // Same fallback: keep what is cached and retry later.
            }
            connected = connected && corrections;
            if (!stopped) {
                int id = params.getJobId();
                boolean isFollowup = id >= DateSyncScheduler.MONTH_FIRST_ID
                        && id < DateSyncScheduler.MONTH_FIRST_ID + DateSyncScheduler.MONTH_JOB_COUNT;
                boolean retry = !connected && (id == DateSyncScheduler.ONCE_ID || isFollowup);
                jobFinished(params, retry);
                // Recalculate future windows after a newly reported month start,
                // or when the final possible day of a month has passed.
                DateSyncScheduler.ensure(this);
            }
        }, "lunar-feed-sync");
        worker.start();
        return true;
    }

    @Override public boolean onStopJob(JobParameters params) {
        stopped = true;
        if (worker != null) worker.interrupt();
        int id = params.getJobId();
        return id == DateSyncScheduler.ONCE_ID
                || (id >= DateSyncScheduler.MONTH_FIRST_ID
                    && id < DateSyncScheduler.MONTH_FIRST_ID + DateSyncScheduler.MONTH_JOB_COUNT);
    }

    private boolean updateFromFeed() throws Exception {
        SharedPreferences settings = ReminderStore.prefs(this);
        if (!settings.getBoolean(ReminderStore.ENABLED, false)) return true;
        settings.edit().putLong(ReminderStore.LAST_REFRESH_ATTEMPT,
                System.currentTimeMillis()).apply();
        HttpsURLConnection connection = (HttpsURLConnection)
                new URL(FEED_URL).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "LunarThursdayAndroid/1.2.0");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setUseCaches(false);
        try {
            if (connection.getResponseCode() != 200) return false;
            byte[] data;
            try (InputStream source = connection.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[2048];
                int count;
                while ((count = source.read(buffer)) != -1) {
                    if (output.size() + count > 16384) return false;
                    output.write(buffer, 0, count);
                }
                data = output.toByteArray();
            }
            JSONObject json = new JSONObject(new String(data,
                    java.nio.charset.StandardCharsets.UTF_8));
            FeedDatePolicy.Observation confirmed = FeedDatePolicy.evaluate(
                    json.optString("gregorian_date", ""),
                    json.optInt("hijri_day", -1),
                    json.optInt("hijri_month", -1),
                    json.optInt("hijri_year", -1),
                    json.optString("calendar_method", ""),
                    json.optString("timezone", ""),
                    json.optString("generated_at", ""),
                    Instant.now());
            settings.edit().putLong(ReminderStore.LAST_REFRESH_SUCCESS,
                    System.currentTimeMillis()).apply();
            if (confirmed != null && !stopped
                    && settings.getBoolean(ReminderStore.ENABLED, false)) {
                if (ReminderStore.saveReportedConfirmed(this,
                        confirmed.monthKey, confirmed.firstDay)) {
                    ReminderScheduler.sync(this);
                    DateSyncScheduler.ensure(this); // Source changes can shift the next check window.
                }
            }
            return true;
        } finally {
            connection.disconnect();
        }
    }
}
