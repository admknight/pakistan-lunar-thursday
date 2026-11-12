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
            if (!stopped) {
                boolean retry = !connected && params.getJobId() == DateSyncScheduler.ONCE_ID;
                jobFinished(params, retry);
            }
        }, "lunar-feed-sync");
        worker.start();
        return true;
    }

    @Override public boolean onStopJob(JobParameters params) {
        stopped = true;
        if (worker != null) worker.interrupt();
        return params.getJobId() == DateSyncScheduler.ONCE_ID;
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
                }
            }
            return true;
        } finally {
            connection.disconnect();
        }
    }
}
