package com.scopewise.lunarthursday

import android.content.Context
import java.io.ByteArrayOutputStream
import java.net.URL
import java.time.Instant
import javax.net.ssl.HttpsURLConnection

/**
 * Fetches the project's corrections file and stores accepted month starts as
 * confirmed. Called from the existing background date check; never shows UI.
 */
object CorrectionsSync {
    const val FILE_URL =
        "https://raw.githubusercontent.com/admknight/pakistan-lunar-thursday/main/data/month-starts.json"
    const val LAST_SUCCESS = "corrections_last_success_ms"

    /**
     * @return true when the file was fetched (whether or not it changed anything),
     * false when the network request failed and a retry is worthwhile.
     */
    @JvmStatic
    fun fetchAndApply(context: Context): Boolean {
        val settings = ReminderStore.prefs(context)
        if (!settings.getBoolean(ReminderStore.ENABLED, false)) return true
        val body = download() ?: return false
        settings.edit().putLong(LAST_SUCCESS, System.currentTimeMillis()).apply()

        val accepted = CorrectionsPolicy.accept(
            body, ReminderStore.confirmedStarts(context), Instant.now())
        var changed = false
        for (entry in accepted) {
            if (ReminderStore.saveReportedConfirmed(context, entry.monthKey, entry.firstDay)) changed = true
        }
        if (changed && settings.getBoolean(ReminderStore.ENABLED, false)) {
            ReminderScheduler.sync(context)
            DateSyncScheduler.ensure(context)
        }
        return true
    }

    private fun download(): String? {
        val connection = URL(FILE_URL).openConnection() as HttpsURLConnection
        connection.requestMethod = "GET"
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "HilalThursday-Android-Corrections")
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.useCaches = false
        try {
            if (connection.responseCode != 200) return null
            connection.inputStream.use { source ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (true) {
                    val count = source.read(buffer)
                    if (count == -1) break
                    if (output.size() + count > CorrectionsPolicy.MAX_BYTES) return null
                    output.write(buffer, 0, count)
                }
                return String(output.toByteArray(), Charsets.UTF_8)
            }
        } finally {
            connection.disconnect()
        }
    }
}
