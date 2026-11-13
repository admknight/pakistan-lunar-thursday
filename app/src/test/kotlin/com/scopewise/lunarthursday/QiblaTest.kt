package com.scopewise.lunarthursday

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QiblaTest {
    /** Reference bearings from api.aladhan.com/v1/qibla, fetched 14 Nov 2026. */
    @Test fun matchesAnIndependentCalculation() {
        assertEquals(REFERENCE_LAHORE, Qibla.bearing(31.5204, 74.3587), 0.2)
        assertEquals(REFERENCE_KARACHI, Qibla.bearing(24.8607, 67.0011), 0.2)
        assertEquals(REFERENCE_GILGIT, Qibla.bearing(35.9208, 74.3144), 0.2)
    }

    @Test fun pakistanFacesRoughlyWest() {
        for ((lat, lng) in listOf(24.8607 to 67.0011, 31.5204 to 74.3587, 33.6844 to 73.0479, 34.0151 to 71.5249,
            30.1798 to 66.9750, 30.1575 to 71.5249, 31.4504 to 73.1350, 25.3960 to 68.3578, 35.9208 to 74.3144)) {
            assertTrue(Qibla.bearing(lat, lng) in 245.0..275.0)
        }
        // Due north of the Kaaba faces south; due east of it faces close to west.
        assertEquals(180.0, Qibla.bearing(40.0, 39.8262), 0.01)
    }

    companion object {
        const val REFERENCE_LAHORE = 260.366
        const val REFERENCE_KARACHI = 267.741
        const val REFERENCE_GILGIT = 253.678
    }
}
