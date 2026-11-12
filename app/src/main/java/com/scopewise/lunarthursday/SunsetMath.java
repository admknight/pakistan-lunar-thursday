package com.scopewise.lunarthursday;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Simplified astronomical sunset approximation; not certified Maghrib times. */
final class SunsetMath {
    private static final ZoneId PKT = ZoneId.of("Asia/Karachi");
    private SunsetMath() { }

    static LocalTime sunsetPakistan(LocalDate date, double latitude, double longitude) {
        if (date == null || !Double.isFinite(latitude) || !Double.isFinite(longitude)
                || Math.abs(latitude) > 90 || Math.abs(longitude) > 180) return null;
        double gamma = 2.0 * Math.PI / date.lengthOfYear() * (date.getDayOfYear() - 1.0);
        double eqTime = 229.18 * (0.000075 + 0.001868 * Math.cos(gamma)
                - 0.032077 * Math.sin(gamma) - 0.014615 * Math.cos(2 * gamma)
                - 0.040849 * Math.sin(2 * gamma));
        double decl = 0.006918 - 0.399912 * Math.cos(gamma)
                + 0.070257 * Math.sin(gamma) - 0.006758 * Math.cos(2 * gamma)
                + 0.000907 * Math.sin(2 * gamma) - 0.002697 * Math.cos(3 * gamma)
                + 0.00148 * Math.sin(3 * gamma);
        double lat = Math.toRadians(latitude);
        double cosHa = Math.cos(Math.toRadians(90.833)) / (Math.cos(lat) * Math.cos(decl))
                - Math.tan(lat) * Math.tan(decl);
        if (!Double.isFinite(cosHa) || Math.abs(cosHa) > 1) return null;
        double ha = Math.toDegrees(Math.acos(cosHa));
        double minutes = 720 - 4 * longitude - eqTime + 4 * ha + 300;
        if (minutes < 0 || minutes >= 1440) return null;
        int result = Math.min(1439, (int) Math.round(minutes));
        return LocalTime.of(result / 60, result % 60);
    }

    static LocalDate hijriCivilDate(Instant now, LocalTime localSunset) {
        ZonedDateTime pkt = now.atZone(PKT);
        if (localSunset == null) return pkt.toLocalDate();
        return pkt.toLocalTime().isBefore(localSunset)
                ? pkt.toLocalDate() : pkt.toLocalDate().plusDays(1);
    }
}
