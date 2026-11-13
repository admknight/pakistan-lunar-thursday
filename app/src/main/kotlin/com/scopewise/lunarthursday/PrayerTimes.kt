package com.scopewise.lunarthursday

import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.JulianFields

/**
 * Prayer times for a place in Pakistan (UTC+5), from the sun's position.
 *
 * The sun's declination and the equation of time use the standard
 * low-precision solar formulas, good to about a minute. The twilight angles
 * and the Asr rule come from the chosen method. These are calculated times,
 * not a mosque's timetable; local timetables may differ by a few minutes.
 */
object PrayerTimes {
    private const val UTC_OFFSET_HOURS = 5.0
    /** Sun's centre this far below the horizon at sunrise and sunset (refraction and radius). */
    private const val HORIZON_ANGLE = 0.833

    enum class Method(
        @JvmField val id: String,
        @JvmField val fajrAngle: Double,
        @JvmField val ishaAngle: Double,
        /** Sun this far below the horizon at Maghrib; 0 means Maghrib is sunset. */
        @JvmField val maghribAngle: Double,
        /** Asr begins when a shadow is this many times its object's length, plus the noon shadow. */
        @JvmField val asrShadowFactor: Int
    ) {
        /** University of Islamic Sciences, Karachi, with Hanafi Asr. */
        KARACHI_HANAFI("karachi_hanafi", 18.0, 18.0, 0.0, 2),
        /** Shia Ithna Ashari (Leva Institute, Qum). */
        JAFARI("jafari", 16.0, 14.0, 4.0, 1);

        companion object {
            @JvmStatic
            fun fromId(id: String?): Method = values().firstOrNull { it.id == id } ?: KARACHI_HANAFI
        }
    }

    /** One day's times in Pakistan time. A value is null only where the sun never reaches that angle. */
    class Day internal constructor(
        @JvmField val fajr: LocalTime?,
        @JvmField val sunrise: LocalTime?,
        @JvmField val dhuhr: LocalTime?,
        @JvmField val asr: LocalTime?,
        @JvmField val sunset: LocalTime?,
        @JvmField val maghrib: LocalTime?,
        @JvmField val isha: LocalTime?
    )

    @JvmStatic
    fun compute(date: LocalDate, latitude: Double, longitude: Double, method: Method): Day {
        require(latitude in -66.0..66.0 && longitude in -180.0..180.0) { "Location out of range" }
        // Julian day at local noon-independent start, shifted to the place's longitude.
        val julian = date.getLong(JulianFields.JULIAN_DAY) - 0.5 - longitude / (15.0 * 24.0)

        fun declination(dayFraction: Double) = sun(julian + dayFraction).first
        fun midday(dayFraction: Double) = fixHour(12.0 - sun(julian + dayFraction).second)
        fun atAngle(angle: Double, dayFraction: Double, beforeNoon: Boolean): Double {
            val decl = declination(dayFraction)
            val cosine = (-sin(angle) - sin(decl) * sin(latitude)) / (cos(decl) * cos(latitude))
            if (cosine < -1.0 || cosine > 1.0) return Double.NaN
            val hours = Math.toDegrees(Math.acos(cosine)) / 15.0
            return midday(dayFraction) + if (beforeNoon) -hours else hours
        }
        fun asr(dayFraction: Double): Double {
            val decl = declination(dayFraction)
            val angle = -Math.toDegrees(Math.atan(1.0 / (method.asrShadowFactor + tan(Math.abs(latitude - decl)))))
            return atAngle(angle, dayFraction, false)
        }

        // One refinement pass from rough guesses, as the sun moves during the day.
        val fajr = atAngle(method.fajrAngle, 5.0 / 24, true)
        val sunrise = atAngle(HORIZON_ANGLE, 6.0 / 24, true)
        val dhuhr = midday(12.0 / 24)
        val asrTime = asr(13.0 / 24)
        val sunset = atAngle(HORIZON_ANGLE, 18.0 / 24, false)
        val maghrib = if (method.maghribAngle > 0.0) atAngle(method.maghribAngle, 18.0 / 24, false) else sunset
        val isha = atAngle(method.ishaAngle, 18.0 / 24, false)

        val shift = UTC_OFFSET_HOURS - longitude / 15.0
        fun clock(hours: Double): LocalTime? {
            if (hours.isNaN()) return null
            val minutes = Math.floorMod(Math.round(fixHour(hours + shift) * 60.0), 1440L).toInt()
            return LocalTime.of(minutes / 60, minutes % 60)
        }
        return Day(clock(fajr), clock(sunrise), clock(dhuhr), clock(asrTime), clock(sunset), clock(maghrib), clock(isha))
    }

    /** Sun's declination in degrees and the equation of time in hours, for a Julian date. */
    private fun sun(julianDate: Double): Pair<Double, Double> {
        val d = julianDate - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(g) + 0.020 * sin(2 * g))
        val e = 23.439 - 0.00000036 * d
        val rightAscension = Math.toDegrees(Math.atan2(cos(e) * sin(l), cos(l))) / 15.0
        val equationOfTime = q / 15.0 - fixHour(rightAscension)
        val declination = Math.toDegrees(Math.asin(sin(e) * sin(l)))
        return Pair(declination, equationOfTime)
    }

    private fun sin(degrees: Double) = Math.sin(Math.toRadians(degrees))
    private fun cos(degrees: Double) = Math.cos(Math.toRadians(degrees))
    private fun tan(degrees: Double) = Math.tan(Math.toRadians(degrees))
    private fun fixAngle(a: Double): Double { val r = a % 360.0; return if (r < 0) r + 360.0 else r }
    private fun fixHour(h: Double): Double { val r = h % 24.0; return if (r < 0) r + 24.0 else r }
}
