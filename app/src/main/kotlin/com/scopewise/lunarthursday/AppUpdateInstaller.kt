package com.scopewise.lunarthursday

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Downloads a trusted signed GitHub APK inside the app, then invokes Android's installer. */
object AppUpdateInstaller {
    private val IO = Executors.newSingleThreadExecutor()
    private val MAIN = Handler(Looper.getMainLooper())
    private val RUNNING = AtomicBoolean()
    private const val MAX_APK_SIZE = 75L * 1024L * 1024L
    private const val PENDING = "downloaded_verified_update"
    private const val PENDING_INSTALL = "downloaded_update_wants_install"
    private val HEX_DIGEST = Regex("[a-f0-9]{64}")

    @JvmStatic
    fun prompt(activity: Activity, result: UpdateManager.Result?) {
        if (result == null) return
        if (RUNNING.get()) {
            toast(activity, "An update is already downloading")
            return
        }
        if (!HEX_DIGEST.matches(result.digest)) {
            toast(activity, "This release does not provide a verifiable SHA-256 digest")
            return
        }
        AlertDialog.Builder(activity).setTitle(UiText.tr(activity, "Update Hilal Thursday"))
            .setMessage(UiText.tr(activity, "Download version ") + VersionPolicy.display(result.tag) +
                UiText.tr(activity, " securely inside this app? The APK is checked against its SHA-256 digest and the installed signing certificate before Android confirms installation."))
            .setNegativeButton(UiText.tr(activity, "Not now"), null)
            .setPositiveButton(UiText.tr(activity, "Download & install")) { _, _ -> download(activity, result) }
            .show()
    }

    @JvmStatic
    fun resumeIfPermitted(activity: Activity) {
        if (!ReminderStore.prefs(activity).getBoolean(PENDING_INSTALL, false)) return
        if (!activity.packageManager.canRequestPackageInstalls()) return
        val cached = cacheFile(activity)
        if (!cached.isFile) {
            ReminderStore.prefs(activity).edit().remove(PENDING_INSTALL).remove(PENDING).apply()
            return
        }
        ReminderStore.prefs(activity).edit().remove(PENDING_INSTALL).apply()
        try {
            verifyPackage(activity, cached)
            openInstaller(activity, cached)
        } catch (ex: Exception) {
            cached.delete()
            toast(activity, "Previously downloaded APK is no longer compatible")
        }
    }

    private fun cacheFile(activity: Activity): File {
        val dir = File(activity.cacheDir, "updates")
        if (!dir.exists() && !dir.mkdirs()) throw IllegalStateException("Cannot create update cache")
        return File(dir, "verified-update.apk")
    }

    private fun download(activity: Activity, target: UpdateManager.Result) {
        if (!RUNNING.compareAndSet(false, true)) return
        val cancelled = AtomicBoolean()
        val layout = LinearLayout(activity)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(32, 24, 32, 4)
        val progress = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal)
        progress.isIndeterminate = true
        progress.max = 100
        layout.addView(progress, LinearLayout.LayoutParams(-1, 30))
        val message = TextView(activity)
        message.text = UiText.tr(activity, "Downloading securely…")
        message.gravity = Gravity.CENTER_VERTICAL
        message.setPadding(0, 16, 0, 14)
        layout.addView(message)
        val dialog = AlertDialog.Builder(activity)
            .setTitle(UiText.tr(activity, "Installing update"))
            .setView(layout)
            .setNegativeButton(UiText.tr(activity, "Cancel")) { _, _ -> cancelled.set(true) }
            .create()
        dialog.setOnCancelListener { cancelled.set(true) }
        dialog.show()
        IO.execute {
            try {
                val targetFile = cacheFile(activity)
                val working = File(targetFile.parentFile, "download.part")
                if (working.exists()) working.delete()
                var endpoint = URL(target.url)
                var connection: HttpURLConnection? = null
                for (hop in 0 until 7) {
                    if (!isAllowedUrl(endpoint)) throw SecurityException("Untrusted download redirect")
                    val attempt = endpoint.openConnection() as HttpURLConnection
                    attempt.connectTimeout = 12_000
                    attempt.readTimeout = 18_000
                    attempt.instanceFollowRedirects = false
                    attempt.setRequestProperty("User-Agent", "HilalThursday-Android-Updater")
                    val code = attempt.responseCode
                    if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                        val where = attempt.getHeaderField("Location")
                            ?: throw SecurityException("Missing secure download target")
                        val next = URL(endpoint, where)
                        attempt.disconnect()
                        endpoint = next
                        continue
                    }
                    if (code != 200) throw IllegalStateException("Download unavailable (HTTP $code)")
                    connection = attempt
                    break
                }
                if (connection == null) throw SecurityException("Too many download redirects")
                try {
                    val length = connection.contentLengthLong
                    if (length > MAX_APK_SIZE) throw SecurityException("APK is larger than the allowed limit")
                    val hash = MessageDigest.getInstance("SHA-256")
                    var total = 0L
                    var lastUi = 0L
                    connection.inputStream.use { input ->
                        FileOutputStream(working).use { out ->
                            val bytes = ByteArray(32 * 1024)
                            while (true) {
                                val n = input.read(bytes)
                                if (n == -1) break
                                if (cancelled.get()) throw InterruptedException("Download cancelled")
                                total += n
                                if (total > MAX_APK_SIZE) throw SecurityException("APK is larger than the allowed limit")
                                hash.update(bytes, 0, n)
                                out.write(bytes, 0, n)
                                val stamp = SystemClock.elapsedRealtime()
                                if (stamp - lastUi >= 250) {
                                    lastUi = stamp
                                    val pct = if (length > 0) (total * 100 / length).toInt() else -1
                                    MAIN.post {
                                        if (!activity.isFinishing && dialog.isShowing) {
                                            progress.isIndeterminate = pct < 0
                                            if (pct >= 0) progress.progress = pct
                                            message.text = if (pct < 0) "Downloading APK…" else "Downloading APK… $pct%"
                                        }
                                    }
                                }
                            }
                            out.fd.sync()
                        }
                    }
                    if (length >= 0 && length != total) throw SecurityException("Incomplete APK download")
                    val actual = StringBuilder(64)
                    for (b in hash.digest()) actual.append(String.format(Locale.US, "%02x", b.toInt() and 255))
                    if (!MessageDigest.isEqual(actual.toString().toByteArray(StandardCharsets.US_ASCII),
                            target.digest.toByteArray(StandardCharsets.US_ASCII))) {
                        throw SecurityException("SHA-256 mismatch")
                    }
                    verifyPackage(activity, working)
                    if (targetFile.exists() && !targetFile.delete()) throw IllegalStateException("Cannot replace cached APK")
                    if (!working.renameTo(targetFile)) throw IllegalStateException("Cannot cache verified APK")
                } finally {
                    connection.disconnect()
                }
                if (cancelled.get()) throw InterruptedException("Download cancelled")
                ReminderStore.prefs(activity).edit().putString(PENDING, target.tag).apply()
                MAIN.post {
                    if (dialog.isShowing) dialog.dismiss()
                    if (!activity.isFinishing) startInstallation(activity, targetFile)
                }
            } catch (failure: Exception) {
                File(File(activity.cacheDir, "updates"), "download.part").delete()
                val label = if (failure is InterruptedException) "Download cancelled"
                else "Update could not be verified: " + failure.message
                MAIN.post {
                    if (dialog.isShowing) dialog.dismiss()
                    if (!activity.isFinishing) toast(activity, label)
                }
            } finally {
                RUNNING.set(false)
            }
        }
    }

    private fun isAllowedUrl(url: URL): Boolean {
        val host = url.host.lowercase(Locale.ROOT)
        val known = host == "github.com" || host == "release-assets.githubusercontent.com" ||
            host == "objects.githubusercontent.com"
        return known && url.protocol == "https" && url.userInfo == null && url.port == -1
    }

    @Suppress("DEPRECATION")
    private fun verifyPackage(activity: Activity, file: File) {
        val pm = activity.packageManager
        val modern = Build.VERSION.SDK_INT >= 28
        val flags = if (modern) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val candidate = pm.getPackageArchiveInfo(file.absolutePath, flags)
        val installed = pm.getPackageInfo(activity.packageName, flags)
        if (candidate == null || activity.packageName != candidate.packageName) {
            throw SecurityException("Unexpected Android package identity")
        }
        val newest = if (modern) candidate.longVersionCode else candidate.versionCode.toLong()
        val existing = if (modern) installed.longVersionCode else installed.versionCode.toLong()
        if (newest <= existing) throw SecurityException("APK version must be newer than installed version")
        val signers = if (modern) candidate.signingInfo?.apkContentsSigners else candidate.signatures
        val trusted = if (modern) installed.signingInfo?.apkContentsSigners else installed.signatures
        if (signers == null || trusted == null || signers.size != trusted.size || signers.isEmpty()) {
            throw SecurityException("Missing or incompatible APK certificate")
        }
        for (signer in signers) {
            if (trusted.none { it.toByteArray().contentEquals(signer.toByteArray()) }) {
                throw SecurityException("APK signing key differs from installed app")
            }
        }
    }

    private fun startInstallation(activity: Activity, file: File) {
        if (!activity.packageManager.canRequestPackageInstalls()) {
            ReminderStore.prefs(activity).edit().putBoolean(PENDING_INSTALL, true).apply()
            AlertDialog.Builder(activity).setTitle(UiText.tr(activity, "Android installation permission"))
                .setMessage(UiText.tr(activity, "Allow Hilal Thursday to install app updates in Android settings. Return to this app to continue. No web browser is needed."))
                .setNegativeButton(UiText.tr(activity, "Later")) { _, _ ->
                    ReminderStore.prefs(activity).edit().remove(PENDING_INSTALL).apply()
                }
                .setPositiveButton(UiText.tr(activity, "Open Android settings")) { _, _ ->
                    try {
                        activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:" + activity.packageName)))
                    } catch (ex: RuntimeException) {
                        toast(activity, "Open app installation permissions in Android settings")
                    }
                }.show()
            return
        }
        openInstaller(activity, file)
    }

    @Suppress("DEPRECATION")
    private fun openInstaller(activity: Activity, file: File) {
        try {
            val uri = FileProvider.getUriForFile(activity, activity.packageName + ".fileprovider", file)
            val install = Intent(Intent.ACTION_INSTALL_PACKAGE)
            install.setDataAndType(uri, "application/vnd.android.package-archive")
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            activity.startActivity(install)
        } catch (ex: Exception) {
            toast(activity, "Android installer unavailable: " + ex.message)
        }
    }

    private fun toast(activity: Activity, message: String) {
        Toast.makeText(activity, UiText.tr(activity, message), Toast.LENGTH_LONG).show()
    }
}
