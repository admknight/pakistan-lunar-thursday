package com.scopewise.lunarthursday

/** Direction of the Kaaba from a place, as a compass bearing. */
object Qibla {
    private const val KAABA_LATITUDE = 21.4225
    private const val KAABA_LONGITUDE = 39.8262

    /** Degrees clockwise from true north, 0 up to 360, along the great circle. */
    @JvmStatic
    fun bearing(latitude: Double, longitude: Double): Double {
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(KAABA_LATITUDE)
        val delta = Math.toRadians(KAABA_LONGITUDE - longitude)
        val y = Math.sin(delta) * Math.cos(lat2)
        val x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(delta)
        return (Math.toDegrees(Math.atan2(y, x)) + 360.0) % 360.0
    }
}
