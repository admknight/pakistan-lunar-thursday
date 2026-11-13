package com.scopewise.lunarthursday

import android.content.Context
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Display-only sunset clock; never changes the first-Thursday reminder date. */
object HijriClock {
    private const val CITY = "hijri_sunset_city_index"
    private const val DAY_0 = "hijri_weather_sunset_date0"
    private const val TIME_0 = "hijri_weather_sunset_min0"
    private const val DAY_1 = "hijri_weather_sunset_date1"
    private const val TIME_1 = "hijri_weather_sunset_min1"
    // Index 0: existing optional weather-derived sunset, otherwise civil fallback.
    private val CITY_COORDS = arrayOf(
        doubleArrayOf(0.0, 0.0), doubleArrayOf(24.8607, 67.0011), doubleArrayOf(31.5204, 74.3587),
        doubleArrayOf(33.6844, 73.0479), doubleArrayOf(34.0151, 71.5249), doubleArrayOf(30.1798, 66.9750),
        doubleArrayOf(30.1575, 71.5249), doubleArrayOf(31.4504, 73.1350), doubleArrayOf(25.3960, 68.3578),
        doubleArrayOf(35.9208, 74.3144))

    class Result internal constructor(
        @JvmField val todayGregorian: LocalDate,
        @JvmField val hijriReferenceDate: LocalDate,
        @JvmField val approximateSunset: LocalTime?,
        @JvmField val locationName: String?
    )

    @JvmStatic
    fun selectedCity(context: Context): Int {
        val value = ReminderStore.prefs(context).getInt(CITY, 0)
        return if (value >= 0 && value < CITY_COORDS.size) value else 0
    }

    @JvmStatic
    fun selectCity(context: Context, index: Int) {
        if (index < 0 || index >= CITY_COORDS.size) throw IllegalArgumentException("Invalid city selection")
        ReminderStore.prefs(context).edit().putInt(CITY, index).apply()
    }

    /** Latitude and longitude of the chosen city, or null for the automatic option. */
    @JvmStatic
    fun cityCoordinates(context: Context): DoubleArray? {
        val city = selectedCity(context)
        return if (city > 0) doubleArrayOf(CITY_COORDS[city][0], CITY_COORDS[city][1]) else null
    }

    @JvmStatic
    fun cityLabels(context: Context): Array<String> =
        AppLanguage.wrap(context).resources.getStringArray(R.array.hijri_sunset_cities)

    @JvmStatic
    fun sunsetForDate(context: Context, date: LocalDate): LocalTime? {
        val city = selectedCity(context)
        if (city > 0) return SunsetMath.sunsetPakistan(date, CITY_COORDS[city][0], CITY_COORDS[city][1])
        if (!WeatherManager.enabled(context) || !WeatherManager.permissionGranted(context)) return null
        val p = ReminderStore.prefs(context)
        var minute = -1
        val key = date.toString()
        if (key == p.getString(DAY_0, "")) minute = p.getInt(TIME_0, -1)
        else if (key == p.getString(DAY_1, "")) minute = p.getInt(TIME_1, -1)
        return if (minute >= 0 && minute < 1440) LocalTime.of(minute / 60, minute % 60) else null
    }

    @JvmStatic
    fun current(context: Context, instant: Instant): Result {
        val pkt = instant.atZone(LunarEngine.PAKISTAN)
        val today = pkt.toLocalDate()
        val sunset = sunsetForDate(context, today)
        var name: String? = null
        if (sunset != null) {
            val selected = selectedCity(context)
            name = if (selected > 0) cityLabels(context)[selected]
            else AppLanguage.wrap(context).getString(R.string.hijri_weather_location)
        }
        return Result(today, SunsetMath.hijriCivilDate(instant, sunset), sunset, name)
    }

    /** Cache two local sunset minutes, never location coordinates. */
    @JvmStatic
    fun saveWeatherSunsets(context: Context, latitude: Double, longitude: Double) {
        if (!WeatherManager.enabled(context) || !WeatherManager.permissionGranted(context)) return
        if (!java.lang.Double.isFinite(latitude) || !java.lang.Double.isFinite(longitude)
            || latitude < 23 || latitude > 38.5 || longitude < 60 || longitude > 78.5) {
            clearWeatherSunsets(context)
            return
        }
        val today = LocalDate.now(LunarEngine.PAKISTAN)
        val first = SunsetMath.sunsetPakistan(today, latitude, longitude)
        val second = SunsetMath.sunsetPakistan(today.plusDays(1), latitude, longitude)
        if (first == null || second == null) return
        ReminderStore.prefs(context).edit().putString(DAY_0, today.toString())
            .putInt(TIME_0, first.hour * 60 + first.minute)
            .putString(DAY_1, today.plusDays(1).toString())
            .putInt(TIME_1, second.hour * 60 + second.minute).apply()
        DateSyncScheduler.ensure(context)
    }

    @JvmStatic
    fun clearWeatherSunsets(context: Context) {
        ReminderStore.prefs(context).edit()
            .remove(DAY_0).remove(TIME_0).remove(DAY_1).remove(TIME_1).apply()
    }
}
