package com.scopewise.lunarthursday

import android.app.job.JobParameters
import android.app.job.JobService
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.Instant
import javax.net.ssl.HttpsURLConnection

/**
 * Polls an openly documented public date feed. We only store rows that the
 * provider explicitly marks confirmed, never its calculated estimates.
 * This provider is independent of the Government of Pakistan.
 */
class DateSyncJob : JobService() {
    @Volatile private var worker: Thread? = null
    @Volatile private var stopped = false

    override fun onStartJob(params: JobParameters): Boolean {
        stopped = false
        val thread = Thread({
            var connected = false
            try {
                connected = updateFromFeed()
            } catch (ignored: Exception) {
                // Keep cached observed dates; scheduling continues from fallback.
            }
            // The project's own corrections file is read after the feed, so a
            // confirmed month start from it is the value that stays stored.
            var corrections = false
            try {
                corrections = CorrectionsSync.fetchAndApply(this)
            } catch (ignored: Exception) {
                // Same fallback: keep what is cached and retry later.
            }
            connected = connected && corrections
            // Around the 29th the phone also reads the news for the moon decision
            // itself, so confirmation does not depend on a server job running on time.
            AnnouncementSync.checkIfDue(this)
            if (!stopped) {
                val id = params.jobId
                val retry = !connected && (id == DateSyncScheduler.ONCE_ID || isFollowup(id))
                jobFinished(params, retry)
                // Recalculate future windows after a newly reported month start,
                // or when the final possible day of a month has passed.
                DateSyncScheduler.ensure(this)
            }
        }, "lunar-feed-sync")
        worker = thread
        thread.start()
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        stopped = true
        worker?.interrupt()
        val id = params.jobId
        return id == DateSyncScheduler.ONCE_ID || isFollowup(id)
    }

    private fun isFollowup(id: Int): Boolean =
        id >= DateSyncScheduler.MONTH_FIRST_ID &&
            id < DateSyncScheduler.MONTH_FIRST_ID + DateSyncScheduler.MONTH_JOB_COUNT

    private fun updateFromFeed(): Boolean {
        val settings = ReminderStore.prefs(this)
        if (!settings.getBoolean(ReminderStore.ENABLED, false)) return true
        settings.edit().putLong(ReminderStore.LAST_REFRESH_ATTEMPT, System.currentTimeMillis()).apply()
        val connection = URL(FEED_URL).openConnection() as HttpsURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "HilalThursday-Android")
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.useCaches = false
        try {
            if (connection.responseCode != 200) return false
            val output = ByteArrayOutputStream()
            connection.inputStream.use { source ->
                val buffer = ByteArray(2048)
                while (true) {
                    val count = source.read(buffer)
                    if (count == -1) break
                    if (output.size() + count > 16384) return false
                    output.write(buffer, 0, count)
                }
            }
            val json = JSONObject(String(output.toByteArray(), StandardCharsets.UTF_8))
            val confirmed = FeedDatePolicy.evaluate(
                json.optString("gregorian_date", ""),
                json.optInt("hijri_day", -1),
                json.optInt("hijri_month", -1),
                json.optInt("hijri_year", -1),
                json.optString("calendar_method", ""),
                json.optString("timezone", ""),
                json.optString("generated_at", ""),
                Instant.now())
            settings.edit().putLong(ReminderStore.LAST_REFRESH_SUCCESS, System.currentTimeMillis()).apply()
            if (confirmed != null && !stopped && settings.getBoolean(ReminderStore.ENABLED, false)) {
                if (ReminderStore.saveReportedConfirmed(this, confirmed.monthKey, confirmed.firstDay)) {
                    ReminderScheduler.sync(this)
                    DateSyncScheduler.ensure(this) // Source changes can shift the next check window.
                }
            }
            return true
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val FEED_URL = "https://islamicdatetoday.pk/data/today.json"
    }
}
