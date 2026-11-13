package com.scopewise.lunarthursday

import android.content.Context
import java.io.ByteArrayOutputStream
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.net.ssl.HttpsURLConnection

/**
 * On the days around the 29th, lets the phone itself read the news for the
 * moon decision, so a confirmed month start does not depend on any server
 * job running on time. Runs inside the existing background date check.
 */
object AnnouncementSync {
    const val ENABLED = "announcement_check_enabled"
    private const val LAST_ATTEMPT = "announcement_last_attempt_ms"
    private const val MIN_GAP_MS = 20 * 60 * 1000L
    private const val MAX_BYTES = 1_000_000

    @JvmStatic
    fun isOn(context: Context): Boolean = ReminderStore.prefs(context).getBoolean(ENABLED, true)

    @JvmStatic
    fun setOn(context: Context, on: Boolean) {
        ReminderStore.prefs(context).edit().putBoolean(ENABLED, on).apply()
    }

    /** The month awaiting a decision: the one after the latest confirmed start. */
    internal class Pending(val year: Int, val month: Int, val eveningOf29th: LocalDate)

    internal fun pending(confirmed: Map<String, LocalDate>, today: LocalDate): Pending? {
        val latest = confirmed.entries.filter { !it.value.isAfter(today) }.maxByOrNull { it.value } ?: return null
        val parts = latest.key.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val month = parts.getOrNull(1)?.toIntOrNull() ?: return null
        val nextYear = if (month == 12) year + 1 else year
        val nextMonth = if (month == 12) 1 else month + 1
        if (confirmed.containsKey("$nextYear-$nextMonth")) return null
        return Pending(nextYear, nextMonth, latest.value.plusDays(28))
    }

    /**
     * Checks the news only inside the decision window and at most every twenty
     * minutes. Never throws; a failure leaves the forecast in place.
     */
    @JvmStatic
    fun checkIfDue(context: Context) {
        try {
            val settings = ReminderStore.prefs(context)
            if (!settings.getBoolean(ReminderStore.ENABLED, false) || !isOn(context)) return
            val now = Instant.now()
            val today = now.atZone(LunarEngine.PAKISTAN).toLocalDate()
            val waiting = pending(ReminderStore.confirmedStarts(context), today) ?: return
            val opens = ZonedDateTime.of(waiting.eveningOf29th, AnnouncementReader.WINDOW_OPENS, LunarEngine.PAKISTAN).toInstant()
            val closes = opens.plusSeconds(AnnouncementReader.WINDOW_DAYS * 86400L)
            if (now.isBefore(opens) || now.isAfter(closes)) return
            val nowMs = System.currentTimeMillis()
            if (nowMs - settings.getLong(LAST_ATTEMPT, 0L) in 0 until MIN_GAP_MS) return
            settings.edit().putLong(LAST_ATTEMPT, nowMs).apply()

            val headlines = ArrayList<AnnouncementReader.Headline>()
            for (url in AnnouncementReader.queryUrls(waiting.month, waiting.eveningOf29th)) {
                headlines.addAll(AnnouncementReader.parseRss(download(url)))
            }
            val firstDay = when (AnnouncementReader.decide(headlines, waiting.month, waiting.eveningOf29th)) {
                AnnouncementReader.Outcome.SIGHTED -> waiting.eveningOf29th.plusDays(1)
                AnnouncementReader.Outcome.NOT_SIGHTED -> waiting.eveningOf29th.plusDays(2)
                else -> return
            }
            if (ReminderStore.saveReportedConfirmed(context, "${waiting.year}-${waiting.month}", firstDay)) {
                ReminderScheduler.sync(context)
                DateSyncScheduler.ensure(context)
            }
        } catch (ignored: Exception) {
            // Reading the news is a convenience; the forecast and the corrections file remain.
        }
    }

    private fun download(address: String): String? {
        val connection = URL(address).openConnection() as HttpsURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "HilalThursday-Android")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.useCaches = false
            if (connection.responseCode != 200) return null
            connection.inputStream.use { source ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = source.read(buffer)
                    if (count == -1) break
                    if (output.size() + count > MAX_BYTES) return null
                    output.write(buffer, 0, count)
                }
                String(output.toByteArray(), Charsets.UTF_8)
            }
        } catch (unreachable: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}
