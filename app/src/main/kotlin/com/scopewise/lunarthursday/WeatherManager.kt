package com.scopewise.lunarthursday

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Opt-in foreground coarse location and weather, used ONLY to decorate the home sky. */
object WeatherManager {
    const val ENABLED = "weather_visuals_enabled"
    private const val LAST_ATTEMPT = "weather_attempt"
    private const val LAST_SUCCESS = "weather_success"
    private const val CODE = "weather_wmo"
    private const val DAY = "weather_is_day"
    private const val TEMP = "weather_temp_c"
    private const val RECHECK_MS = 45L * 60L * 1000L
    private const val MAX_AGE_MS = 3L * 60L * 60L * 1000L
    private val IO = Executors.newSingleThreadExecutor()
    private val BUSY = AtomicBoolean()
    private val MAIN = Handler(Looper.getMainLooper())

    class Scene(@JvmField val code: Int, @JvmField val day: Boolean, @JvmField val temp: Int) {
        fun description(context: Context): String =
            UiText.tr(context, WeatherCode.title(code)) + " · " + temp + "°C"
    }

    @JvmStatic
    fun enabled(context: Context): Boolean = ReminderStore.prefs(context).getBoolean(ENABLED, false)

    @JvmStatic
    fun permissionGranted(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @JvmStatic
    fun current(context: Context): Scene? {
        if (!enabled(context) || !permissionGranted(context)) return null
        val p = ReminderStore.prefs(context)
        val then = p.getLong(LAST_SUCCESS, 0)
        val age = System.currentTimeMillis() - then
        if (then <= 0 || age < 0 || age > MAX_AGE_MS) return null
        return Scene(p.getInt(CODE, 3), p.getBoolean(DAY, true), p.getInt(TEMP, 0))
    }

    @JvmStatic
    fun disable(context: Context) {
        HijriClock.clearWeatherSunsets(context)
        ReminderStore.prefs(context).edit().putBoolean(ENABLED, false)
            .remove(CODE).remove(DAY).remove(TEMP)
            .remove(LAST_SUCCESS).remove(LAST_ATTEMPT).apply()
    }

    @JvmStatic
    fun refresh(activity: Activity, forced: Boolean, onComplete: Runnable?) {
        if (!enabled(activity) || !permissionGranted(activity)) return
        val p = ReminderStore.prefs(activity)
        val now = System.currentTimeMillis()
        val attempt = p.getLong(LAST_ATTEMPT, 0)
        if (!forced && now >= attempt && now - attempt < RECHECK_MS) return
        if (!BUSY.compareAndSet(false, true)) return
        p.edit().putLong(LAST_ATTEMPT, now).apply()
        val location = activity.getSystemService(LocationManager::class.java)
        if (location == null || !location.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            finish(onComplete)
            return
        }
        val app = activity.applicationContext
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                val cancel = CancellationSignal()
                val responded = AtomicBoolean()
                location.getCurrentLocation(LocationManager.NETWORK_PROVIDER, cancel, activity.mainExecutor) { found: Location? ->
                    if (responded.compareAndSet(false, true)) {
                        fetch(app, found ?: recentLastLocation(location), onComplete)
                    }
                }
                MAIN.postDelayed({
                    if (responded.compareAndSet(false, true)) {
                        cancel.cancel()
                        fetch(app, recentLastLocation(location), onComplete)
                    }
                }, 16_000L)
            } else {
                val recent = recentLastLocation(location)
                if (recent != null) {
                    fetch(app, recent, onComplete)
                    return
                }
                val responded = AtomicBoolean()
                val listener = object : LocationListener {
                    override fun onLocationChanged(where: Location) {
                        if (responded.compareAndSet(false, true)) fetch(app, where, onComplete)
                    }
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                    @Deprecated("Required on API levels below 29")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                }
                @Suppress("DEPRECATION")
                location.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, Looper.getMainLooper())
                MAIN.postDelayed({
                    if (responded.compareAndSet(false, true)) {
                        location.removeUpdates(listener)
                        fetch(app, recentLastLocation(location), onComplete)
                    }
                }, 14_000L)
            }
        } catch (e: SecurityException) {
            finish(onComplete)
        } catch (e: IllegalArgumentException) {
            finish(onComplete)
        }
    }

    private fun recentLastLocation(manager: LocationManager): Location? {
        return try {
            val loc = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) ?: return null
            val age = System.currentTimeMillis() - loc.time
            if (age >= 0 && age <= 6L * 60L * 60L * 1000L) loc else null
        } catch (e: RuntimeException) {
            null
        }
    }

    private fun fetch(context: Context, found: Location?, onComplete: Runnable?) {
        if (found == null) {
            finish(onComplete)
            return
        }
        // Approximate the coordinates before sending to the weather service (~1 km).
        val latitude = Math.rint(found.latitude * 100.0) / 100.0
        val longitude = Math.rint(found.longitude * 100.0) / 100.0
        if (Math.abs(latitude) > 90 || Math.abs(longitude) > 180) {
            finish(onComplete)
            return
        }
        IO.execute {
            var conn: HttpURLConnection? = null
            try {
                val query = String.format(Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f&current=temperature_2m,weather_code,is_day&forecast_days=1",
                    latitude, longitude)
                conn = URL(query).openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("Accept", "application/json")
                if (conn.responseCode != 200) throw IllegalStateException("Weather unavailable")
                val out = ByteArrayOutputStream()
                conn.inputStream.use { input ->
                    val chunk = ByteArray(4096)
                    while (true) {
                        val n = input.read(chunk)
                        if (n == -1) break
                        if (out.size() + n > 16 * 1024) throw IllegalStateException("Response too large")
                        out.write(chunk, 0, n)
                    }
                }
                val current = JSONObject(String(out.toByteArray(), StandardCharsets.UTF_8)).getJSONObject("current")
                val code = current.getInt("weather_code")
                val isDay = current.getInt("is_day")
                val temp = current.getDouble("temperature_2m")
                if (code < 0 || code > 99 || (isDay != 0 && isDay != 1) ||
                    !java.lang.Double.isFinite(temp) || temp < -100 || temp > 70) {
                    throw IllegalStateException("Bad weather values")
                }
                if (enabled(context) && permissionGranted(context)) {
                    ReminderStore.prefs(context).edit().putInt(CODE, code).putBoolean(DAY, isDay == 1)
                        .putInt(TEMP, Math.round(temp).toInt())
                        .putLong(LAST_SUCCESS, System.currentTimeMillis()).apply()
                    HijriClock.saveWeatherSunsets(context, latitude, longitude)
                }
            } catch (ignored: Exception) {
                // Retain time-aware fallback when offline.
            } finally {
                conn?.disconnect()
                finish(onComplete)
            }
        }
    }

    private fun finish(done: Runnable?) {
        BUSY.set(false)
        if (done != null) MAIN.post(done)
    }
}
