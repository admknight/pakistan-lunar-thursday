package com.scopewise.lunarthursday

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Simplified astronomical sunset approximation; not certified Maghrib times. */
object SunsetMath {
    private val PKT: ZoneId = ZoneId.of("Asia/Karachi")

    @JvmStatic
    fun sunsetPakistan(date: LocalDate?, latitude: Double, longitude: Double): LocalTime? {
        if (date == null || !java.lang.Double.isFinite(latitude) || !java.lang.Double.isFinite(longitude)
            || Math.abs(latitude) > 90 || Math.abs(longitude) > 180) return null
        val gamma = 2.0 * Math.PI / date.lengthOfYear() * (date.dayOfYear - 1.0)
        val eqTime = 229.18 * (0.000075 + 0.001868 * Math.cos(gamma)
                - 0.032077 * Math.sin(gamma) - 0.014615 * Math.cos(2 * gamma)
                - 0.040849 * Math.sin(2 * gamma))
        val decl = (0.006918 - 0.399912 * Math.cos(gamma)
                + 0.070257 * Math.sin(gamma) - 0.006758 * Math.cos(2 * gamma)
                + 0.000907 * Math.sin(2 * gamma) - 0.002697 * Math.cos(3 * gamma)
                + 0.00148 * Math.sin(3 * gamma))
        val lat = Math.toRadians(latitude)
        val cosHa = (Math.cos(Math.toRadians(90.833)) / (Math.cos(lat) * Math.cos(decl))
                - Math.tan(lat) * Math.tan(decl))
        if (!java.lang.Double.isFinite(cosHa) || Math.abs(cosHa) > 1) return null
        val ha = Math.toDegrees(Math.acos(cosHa))
        val minutes = 720 - 4 * longitude - eqTime + 4 * ha + 300
        if (minutes < 0 || minutes >= 1440) return null
        val result = Math.min(1439, Math.round(minutes).toInt())
        return LocalTime.of(result / 60, result % 60)
    }

    @JvmStatic
    fun hijriCivilDate(now: Instant, localSunset: LocalTime?): LocalDate {
        val pkt = now.atZone(PKT)
        if (localSunset == null) return pkt.toLocalDate()
        return if (pkt.toLocalTime().isBefore(localSunset)) pkt.toLocalDate() else pkt.toLocalDate().plusDays(1)
    }
}
