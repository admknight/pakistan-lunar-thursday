package com.scopewise.lunarthursday

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.JulianFields

/**
 * A calculated estimate of whether the new crescent can be seen from Pakistan
 * on a given evening. It is a hint only: the month start is decided by the
 * Central Ruet-e-Hilal Committee, not by this calculation.
 *
 * The sun and moon positions use low-precision almanac formulas (the moon to
 * about a third of a degree). Visibility follows Odeh's criterion: the arc of
 * vision at the best time after sunset, compared with a curve that depends on
 * the crescent's width. The best value over five places across Pakistan is
 * used, since one sighting anywhere is enough.
 */
object CrescentVisibility {
    enum class Outlook { LIKELY, BORDERLINE, UNLIKELY }

    class Estimate internal constructor(
        /** Odeh's value: higher is easier to see. */
        @JvmField val value: Double,
        @JvmField val outlook: Outlook
    )

    /** At or above this the crescent is easily seen with the naked eye (Odeh zone A). */
    const val LIKELY_FROM = 5.65
    /** Below this it needs optical aid or cannot be seen (Odeh zones C and D). */
    const val UNLIKELY_BELOW = 2.0

    /** Jiwani, Karachi, Lahore, Quetta and Peshawar. */
    private val SITES = arrayOf(
        doubleArrayOf(25.05, 61.80), doubleArrayOf(24.86, 67.00), doubleArrayOf(31.52, 74.36),
        doubleArrayOf(30.18, 66.98), doubleArrayOf(34.02, 71.52))

    @JvmStatic
    fun estimate(evening: LocalDate): Estimate {
        val value = SITES.maxOf { odeh(evening, it[0], it[1]) }
        val outlook = when {
            value >= LIKELY_FROM -> Outlook.LIKELY
            value < UNLIKELY_BELOW -> Outlook.UNLIKELY
            else -> Outlook.BORDERLINE
        }
        return Estimate(value, outlook)
    }

    private fun sin(d: Double) = Math.sin(Math.toRadians(d))
    private fun cos(d: Double) = Math.cos(Math.toRadians(d))

    private fun julian(utc: LocalDateTime): Double =
        utc.toLocalDate().getLong(JulianFields.JULIAN_DAY) - 0.5 + utc.toLocalTime().toSecondOfDay() / 86400.0

    /** Right ascension and declination of the sun, in degrees. */
    private fun sun(jd: Double): DoubleArray {
        val d = jd - 2451545.0
        val g = 357.529 + 0.98560028 * d
        val q = 280.459 + 0.98564736 * d
        val l = q + 1.915 * sin(g) + 0.020 * sin(2 * g)
        val e = 23.439 - 0.00000036 * d
        val ra = Math.toDegrees(Math.atan2(cos(e) * sin(l), cos(l)))
        val dec = Math.toDegrees(Math.asin(sin(e) * sin(l)))
        return doubleArrayOf(ra, dec)
    }

    /** Right ascension, declination and horizontal parallax of the moon, in degrees. */
    private fun moon(jd: Double): DoubleArray {
        val t = (jd - 2451545.0) / 36525.0
        val lam = 218.32 + 481267.881 * t + 6.29 * sin(135.0 + 477198.87 * t) - 1.27 * sin(259.3 - 413335.36 * t) +
            0.66 * sin(235.7 + 890534.22 * t) + 0.21 * sin(269.9 + 954397.74 * t) -
            0.19 * sin(357.5 + 35999.05 * t) - 0.11 * sin(186.5 + 966404.03 * t)
        val bet = 5.13 * sin(93.3 + 483202.02 * t) + 0.28 * sin(228.2 + 960400.89 * t) -
            0.28 * sin(318.3 + 6003.15 * t) - 0.17 * sin(217.6 - 407332.21 * t)
        val par = 0.9508 + 0.0518 * cos(135.0 + 477198.87 * t) + 0.0095 * cos(259.3 - 413335.36 * t) +
            0.0078 * cos(235.7 + 890534.22 * t) + 0.0028 * cos(269.9 + 954397.74 * t)
        val e = 23.439 - 0.0000004 * (jd - 2451545.0)
        val x = cos(bet) * cos(lam)
        val y = cos(e) * cos(bet) * sin(lam) - sin(e) * sin(bet)
        val z = sin(e) * cos(bet) * sin(lam) + cos(e) * sin(bet)
        return doubleArrayOf(Math.toDegrees(Math.atan2(y, x)), Math.toDegrees(Math.asin(z)), par)
    }

    private fun altitude(ra: Double, dec: Double, jd: Double, lat: Double, lon: Double): Double {
        val gmst = 280.46061837 + 360.98564736629 * (jd - 2451545.0)
        val hourAngle = gmst + lon - ra
        return Math.toDegrees(Math.asin(sin(lat) * sin(dec) + cos(lat) * cos(dec) * cos(hourAngle)))
    }

    /** The moon's altitude as seen from the ground, lowered by its parallax. */
    private fun moonAltitude(jd: Double, lat: Double, lon: Double): Double {
        val m = moon(jd)
        val geocentric = altitude(m[0], m[1], jd, lat, lon)
        return geocentric - m[2] * cos(geocentric)
    }

    private fun odeh(evening: LocalDate, lat: Double, lon: Double): Double {
        // From 16:00 Pakistan time (11:00 UTC), step a minute at a time to sunset.
        val from = evening.atTime(11, 0)
        var sunset: LocalDateTime? = null
        for (i in 0 until 360) {
            val at = from.plusMinutes(i.toLong())
            val jd = julian(at)
            val s = sun(jd)
            if (altitude(s[0], s[1], jd, lat, lon) < -0.833) { sunset = at; break }
        }
        if (sunset == null) return -99.0
        if (moonAltitude(julian(sunset), lat, lon) < 0) return -99.0
        var lagMinutes = 240
        for (i in 0 until 240) {
            if (moonAltitude(julian(sunset.plusMinutes(i.toLong())), lat, lon) < -0.3) { lagMinutes = i; break }
        }
        // Best time: four ninths of the way from sunset to moonset.
        val best = julian(sunset.plusSeconds((lagMinutes * 60.0 * 4.0 / 9.0).toLong()))
        val s = sun(best)
        val m = moon(best)
        val arcOfVision = moonAltitude(best, lat, lon) - altitude(s[0], s[1], best, lat, lon)
        val cosine = sin(s[1]) * sin(m[1]) + cos(s[1]) * cos(m[1]) * cos(s[0] - m[0])
        val elongation = Math.toDegrees(Math.acos(cosine.coerceIn(-1.0, 1.0)))
        val width = 0.27245 * m[2] * 60.0 * (1 - cos(elongation))
        return arcOfVision - (-0.1018 * width * width * width + 0.7319 * width * width - 6.3226 * width + 7.1651)
    }
}
