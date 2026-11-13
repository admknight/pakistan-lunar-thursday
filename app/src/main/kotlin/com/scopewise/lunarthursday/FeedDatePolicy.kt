package com.scopewise.lunarthursday

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

/**
 * Strict, dependency-free gate for observations from a third-party Pakistan
 * Hijri-date feed. The feed itself is NOT a government-issued API.
 * A forecast / estimated feed row must never be marked confirmed.
 */
object FeedDatePolicy {
    class Observation internal constructor(
        @JvmField val monthKey: String,
        @JvmField val firstDay: LocalDate
    )

    @JvmStatic
    fun evaluate(gregorianDate: String?, hijriDay: Int, hijriMonth: Int, hijriYear: Int,
                 method: String?, timezone: String?, generatedAt: String?, now: Instant?): Observation? {
        if ("Asia/Karachi" != timezone) return null
        if (method == null || !method.startsWith("pakistan_ruet_e_hilal_")) return null
        val reportedConfirmed = method == "pakistan_ruet_e_hilal_confirmed"
                || method == "pakistan_ruet_e_hilal_announced"
                || method == "pakistan_ruet_e_hilal_official"
        if (!reportedConfirmed) return null
        if (hijriDay < 1 || hijriDay > 30 || hijriMonth < 1 || hijriMonth > 12
            || hijriYear < 1400 || hijriYear > 1600) return null

        return try {
            val date = LocalDate.parse(gregorianDate!!)
            val stamp = OffsetDateTime.parse(generatedAt!!)
            val ageSeconds = now!!.epochSecond - stamp.toInstant().epochSecond
            if (ageSeconds < -15 * 60L || ageSeconds > 48 * 60 * 60L) return null
            if (stamp.toInstant().atZone(LunarEngine.PAKISTAN).toLocalDate() != date) return null
            val start = date.minusDays(hijriDay - 1L)
            val pakistanNow = now.atZone(LunarEngine.PAKISTAN).toLocalDate()
            if (start.isAfter(pakistanNow)) return null
            val baseline = LunarEngine.month(hijriYear, hijriMonth, emptyMap()).start
            // A wildly inconsistent remote record should not replace a calendar.
            if (Math.abs(ChronoUnit.DAYS.between(baseline, start)) > 3) return null
            Observation("$hijriYear-$hijriMonth", start)
        } catch (invalidFeedData: RuntimeException) {
            null
        }
    }
}
