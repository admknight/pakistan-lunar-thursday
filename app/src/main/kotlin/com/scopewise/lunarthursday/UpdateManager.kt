package com.scopewise.lunarthursday

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Read-only in-app update indicator: checks the project's own GitHub releases. */
object UpdateManager {
    const val AUTO_CHECK = "updates_auto_check"
    const val INCLUDE_PREVIEWS = "updates_include_previews"
    const val BACKGROUND_CHECK = "updates_background_check"
    private const val LAST_ATTEMPT = "updates_last_attempt"
    private const val LAST_SUCCESS = "updates_last_success"
    private const val LATEST_TAG = "updates_latest_tag"
    private const val LATEST_URL = "updates_latest_download_url"
    private const val LATEST_PAGE = "updates_latest_release_page"
    private const val LATEST_PREVIEW = "updates_latest_is_preview"
    private const val LATEST_DIGEST = "updates_latest_digest"
    private const val LAST_NOTIFIED = "updates_last_notified_version"
    private const val FEED = "https://api.github.com/repos/admknight/pakistan-lunar-thursday/releases?per_page=20"
    private const val RELEASE_PREFIX = "https://github.com/admknight/pakistan-lunar-thursday/releases/"
    private const val DOWNLOAD_PREFIX = RELEASE_PREFIX + "download/"
    private const val SUCCESS_INTERVAL = 12L * 60 * 60 * 1000
    private const val FAILURE_INTERVAL = 30L * 60 * 1000
    private const val MAX_BYTES = 256 * 1024
    private const val NEW_VERSION = "New version available: "
    private val HEX_DIGEST = Regex("[a-f0-9]{64}")
    private val PUBLISHED_DIGEST = Regex("sha256:[a-fA-F0-9]{64}")
    private val IO = Executors.newSingleThreadExecutor()
    private val BUSY = AtomicBoolean()
    private val MAIN = Handler(Looper.getMainLooper())

    fun interface Callback {
        fun finished(success: Boolean, message: String)
    }

    class Result(
        @JvmField val tag: String,
        @JvmField val url: String,
        @JvmField val page: String,
        @JvmField val preview: Boolean,
        @JvmField val digest: String
    )

    @JvmStatic
    fun installed(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
        } catch (ignored: Exception) {
            "0.0.0"
        }
    }

    /** Release builds must not offer debug-signed preview APKs as in-place updates. */
    @JvmStatic
    fun isReleaseBuild(context: Context): Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) == 0

    @JvmStatic
    fun available(context: Context): Result? {
        val p = ReminderStore.prefs(context)
        val tag = p.getString(LATEST_TAG, "") ?: ""
        val url = p.getString(LATEST_URL, "") ?: ""
        val page = p.getString(LATEST_PAGE, "") ?: ""
        val preview = p.getBoolean(LATEST_PREVIEW, true)
        val digest = p.getString(LATEST_DIGEST, "") ?: ""
        if (preview && (isReleaseBuild(context) || !p.getBoolean(INCLUDE_PREVIEWS, true))) return null
        if (VersionPolicy.compare(tag, installed(context)) <= 0) return null
        if (!safeUrl(url, DOWNLOAD_PREFIX, ".apk") || !safeUrl(page, RELEASE_PREFIX, "")) return null
        if (!HEX_DIGEST.matches(digest)) return null
        return Result(tag, url, page, preview, digest)
    }

    @JvmStatic
    fun lastChecked(context: Context): Long = ReminderStore.prefs(context).getLong(LAST_SUCCESS, 0L)

    @JvmStatic
    fun isChecking(): Boolean = BUSY.get()

    @JvmStatic
    fun check(activity: Activity, forced: Boolean, callback: Callback?) {
        val p = ReminderStore.prefs(activity)
        if (!forced && !p.getBoolean(AUTO_CHECK, true)) return
        val now = System.currentTimeMillis()
        val lastSuccess = p.getLong(LAST_SUCCESS, 0L)
        val lastAttempt = p.getLong(LAST_ATTEMPT, 0L)
        if (!forced && now >= lastSuccess && now - lastSuccess < SUCCESS_INTERVAL) return
        if (!forced && now >= lastAttempt && now - lastAttempt < FAILURE_INTERVAL) return
        if (!BUSY.compareAndSet(false, true)) {
            if (forced && callback != null) callback.finished(false, "Update check already in progress")
            return
        }
        val app = activity.applicationContext
        p.edit().putLong(LAST_ATTEMPT, now).apply()
        IO.execute {
            var success = false
            var status: String
            try {
                val best = fetchLatest(!isReleaseBuild(app) && p.getBoolean(INCLUDE_PREVIEWS, true), installed(app))
                val e = ReminderStore.prefs(app).edit()
                if (best == null) {
                    e.remove(LATEST_TAG).remove(LATEST_URL).remove(LATEST_PAGE).remove(LATEST_PREVIEW).remove(LATEST_DIGEST)
                    status = "No newer compatible release found"
                } else {
                    e.putString(LATEST_TAG, best.tag).putString(LATEST_URL, best.url)
                        .putString(LATEST_PAGE, best.page).putBoolean(LATEST_PREVIEW, best.preview)
                        .putString(LATEST_DIGEST, best.digest)
                    status = NEW_VERSION + VersionPolicy.display(best.tag)
                }
                e.putLong(LAST_SUCCESS, System.currentTimeMillis()).apply()
                success = true
            } catch (ex: Exception) {
                status = "Could not check GitHub. Check your connection or try later."
            } finally {
                BUSY.set(false)
            }
            val finalSuccess = success
            val finalStatus = status
            if (callback != null) MAIN.post {
                if (!activity.isFinishing && !activity.isDestroyed) {
                    callback.finished(finalSuccess,
                        if (finalStatus.startsWith(NEW_VERSION))
                            UiText.tr(activity, NEW_VERSION) + VersionPolicy.display(finalStatus.substring(NEW_VERSION.length))
                        else UiText.tr(activity, finalStatus))
                }
            }
        }
    }

    /** Periodic read-only GitHub check; never downloads or installs APKs unattended. */
    @JvmStatic
    fun checkInBackground(context: Context, finished: Runnable?) {
        val app = context.applicationContext
        val p = ReminderStore.prefs(app)
        if (!p.getBoolean(AUTO_CHECK, true) || !p.getBoolean(BACKGROUND_CHECK, true)) {
            finished?.run()
            return
        }
        if (!BUSY.compareAndSet(false, true)) {
            finished?.run()
            return
        }
        IO.execute {
            try {
                val best = fetchLatest(false, installed(app))
                val e = p.edit()
                if (best == null) {
                    e.remove(LATEST_TAG).remove(LATEST_URL).remove(LATEST_PAGE)
                        .remove(LATEST_PREVIEW).remove(LATEST_DIGEST)
                } else {
                    e.putString(LATEST_TAG, best.tag).putString(LATEST_URL, best.url)
                        .putString(LATEST_PAGE, best.page).putBoolean(LATEST_PREVIEW, best.preview)
                        .putString(LATEST_DIGEST, best.digest)
                }
                e.putLong(LAST_SUCCESS, System.currentTimeMillis()).apply()
                if (best != null && !best.preview && best.tag != p.getString(LAST_NOTIFIED, "")) {
                    if (UpdateCheckJob.notifyUpdate(app, best.tag)) {
                        p.edit().putString(LAST_NOTIFIED, best.tag).apply()
                    }
                }
            } catch (ignored: Exception) {
                // Periodic job will retry later.
            } finally {
                BUSY.set(false)
                if (finished != null) MAIN.post(finished)
            }
        }
    }

    private fun fetchLatest(includePreviews: Boolean, installed: String): Result? {
        val connection = URL(FEED).openConnection() as HttpURLConnection
        connection.connectTimeout = 8500
        connection.readTimeout = 8500
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        connection.setRequestProperty("User-Agent", "LunarThursday-Android-UpdateCheck")
        try {
            if (connection.responseCode != 200) throw IllegalStateException("GitHub API unavailable")
            val out = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buf = ByteArray(8192)
                while (true) {
                    val n = input.read(buf)
                    if (n == -1) break
                    if (out.size() + n > MAX_BYTES) throw IllegalStateException("Unexpected release list size")
                    out.write(buf, 0, n)
                }
            }
            val releases = JSONArray(String(out.toByteArray(), StandardCharsets.UTF_8))
            var best: Result? = null
            for (i in 0 until releases.length()) {
                val entry = releases.optJSONObject(i)
                if (entry == null || entry.optBoolean("draft", false)) continue
                val preview = entry.optBoolean("prerelease", false)
                if (preview && !includePreviews) continue
                val tag = entry.optString("tag_name", "")
                if (!VersionPolicy.validTag(tag) || VersionPolicy.compare(tag, installed) <= 0) continue
                val page = entry.optString("html_url", "")
                if (!safeUrl(page, RELEASE_PREFIX + "tag/", "")) continue
                val assets = entry.optJSONArray("assets") ?: continue
                var apk = ""
                var digest = ""
                for (j in 0 until assets.length()) {
                    val asset = assets.optJSONObject(j)
                    if (asset == null || !asset.optString("name", "").endsWith(".apk")) continue
                    val candidate = asset.optString("browser_download_url", "")
                    if (!safeUrl(candidate, DOWNLOAD_PREFIX, ".apk")) continue
                    val publishedDigest = asset.optString("digest", "")
                    if (!PUBLISHED_DIGEST.matches(publishedDigest)) continue
                    digest = publishedDigest.substring(7).lowercase(Locale.ROOT)
                    apk = candidate
                    break
                }
                if (apk.isEmpty()) continue
                val currentBest = best
                if (currentBest == null || VersionPolicy.compare(tag, currentBest.tag) > 0) {
                    best = Result(tag, apk, page, preview, digest)
                }
            }
            return best
        } finally {
            connection.disconnect()
        }
    }

    private fun safeUrl(raw: String?, prefix: String, suffix: String): Boolean {
        if (raw == null || raw.length > 2048 || !raw.startsWith(prefix) || !raw.endsWith(suffix)) return false
        return try {
            val parsed = URL(raw)
            "https" == parsed.protocol && "github.com" == parsed.host &&
                parsed.userInfo == null && parsed.port == -1
        } catch (ex: Exception) {
            false
        }
    }
}
