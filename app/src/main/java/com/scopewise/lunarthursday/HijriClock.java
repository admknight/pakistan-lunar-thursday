package com.scopewise.lunarthursday;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/** Display-only sunset clock; never changes the first-Thursday reminder date. */
final class HijriClock {
    private static final String CITY = "hijri_sunset_city_index";
    private static final String DAY_0 = "hijri_weather_sunset_date0";
    private static final String TIME_0 = "hijri_weather_sunset_min0";
    private static final String DAY_1 = "hijri_weather_sunset_date1";
    private static final String TIME_1 = "hijri_weather_sunset_min1";
    // Index 0: existing optional weather-derived sunset, otherwise civil fallback.
    private static final double[][] CITY_COORDS = {
        {0, 0}, {24.8607, 67.0011}, {31.5204, 74.3587},
        {33.6844, 73.0479}, {34.0151, 71.5249}, {30.1798, 66.9750},
        {30.1575, 71.5249}, {31.4504, 73.1350}, {25.3960, 68.3578},
        {35.9208, 74.3144}
    };

    static final class Result {
        final LocalDate todayGregorian, hijriReferenceDate;
        final LocalTime approximateSunset;
        final String locationName;
        Result(LocalDate gregorian, LocalDate hijri, LocalTime sunset, String name) {
            todayGregorian = gregorian;
            hijriReferenceDate = hijri;
            approximateSunset = sunset;
            locationName = name;
        }
    }

    private HijriClock() { }

    static int selectedCity(Context context) {
        int value = ReminderStore.prefs(context).getInt(CITY, 0);
        return value >= 0 && value < CITY_COORDS.length ? value : 0;
    }

    static void selectCity(Context context, int index) {
        if (index < 0 || index >= CITY_COORDS.length)
            throw new IllegalArgumentException("Invalid city selection");
        ReminderStore.prefs(context).edit().putInt(CITY, index).apply();
    }

    /** Latitude and longitude of the chosen city, or null for the automatic option. */
    static double[] cityCoordinates(Context context) {
        int city = selectedCity(context);
        return city > 0 ? new double[] {CITY_COORDS[city][0], CITY_COORDS[city][1]} : null;
    }

    static String[] cityLabels(Context context) {
        return AppLanguage.wrap(context).getResources().getStringArray(R.array.hijri_sunset_cities);
    }

    static LocalTime sunsetForDate(Context context, LocalDate date) {
        int city = selectedCity(context);
        if (city > 0) {
            return SunsetMath.sunsetPakistan(date,
                    CITY_COORDS[city][0], CITY_COORDS[city][1]);
        }
        if (!WeatherManager.enabled(context) || !WeatherManager.permissionGranted(context))
            return null;
        SharedPreferences p = ReminderStore.prefs(context);
        int minute = -1;
        String key = date.toString();
        if (key.equals(p.getString(DAY_0, ""))) minute = p.getInt(TIME_0, -1);
        else if (key.equals(p.getString(DAY_1, ""))) minute = p.getInt(TIME_1, -1);
        return minute >= 0 && minute < 1440
                ? LocalTime.of(minute / 60, minute % 60) : null;
    }

    static Result current(Context context, Instant instant) {
        ZonedDateTime pkt = instant.atZone(LunarEngine.PAKISTAN);
        LocalDate today = pkt.toLocalDate();
        LocalTime sunset = sunsetForDate(context, today);
        String name = null;
        if (sunset != null) {
            int selected = selectedCity(context);
            name = selected > 0
                    ? cityLabels(context)[selected]
                    : AppLanguage.wrap(context).getString(R.string.hijri_weather_location);
        }
        return new Result(today, SunsetMath.hijriCivilDate(instant, sunset), sunset, name);
    }

    /** Cache two local sunset minutes, never location coordinates. */
    static void saveWeatherSunsets(Context context, double latitude, double longitude) {
        if (!WeatherManager.enabled(context) || !WeatherManager.permissionGranted(context)) return;
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < 23 || latitude > 38.5 || longitude < 60 || longitude > 78.5) {
            clearWeatherSunsets(context); return;
        }
        LocalDate today = LocalDate.now(LunarEngine.PAKISTAN);
        LocalTime first = SunsetMath.sunsetPakistan(today, latitude, longitude);
        LocalTime second = SunsetMath.sunsetPakistan(today.plusDays(1), latitude, longitude);
        if (first == null || second == null) return;
        ReminderStore.prefs(context).edit().putString(DAY_0, today.toString())
                .putInt(TIME_0, first.getHour() * 60 + first.getMinute())
                .putString(DAY_1, today.plusDays(1).toString())
                .putInt(TIME_1, second.getHour() * 60 + second.getMinute()).apply();
        DateSyncScheduler.ensure(context);
    }

    static void clearWeatherSunsets(Context context) {
        ReminderStore.prefs(context).edit()
                .remove(DAY_0).remove(TIME_0).remove(DAY_1).remove(TIME_1).apply();
    }
}
