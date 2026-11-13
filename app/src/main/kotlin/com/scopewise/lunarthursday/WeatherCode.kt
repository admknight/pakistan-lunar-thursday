package com.scopewise.lunarthursday

/** WMO weather classification for decorative scenes (not a weather warning service). */
object WeatherCode {
    const val CLEAR = 0
    const val CLOUD = 1
    const val FOG = 2
    const val RAIN = 3
    const val SNOW = 4
    const val STORM = 5

    @JvmStatic
    fun category(code: Int): Int {
        if (code == 0 || code == 1) return CLEAR
        if (code == 2 || code == 3) return CLOUD
        if (code == 45 || code == 48) return FOG
        if (code in 95..99) return STORM
        if (code in 71..77 || code == 85 || code == 86) return SNOW
        if (code in 51..67 || code in 80..82) return RAIN
        return CLOUD
    }

    @JvmStatic
    fun title(code: Int): String = when (category(code)) {
        CLEAR -> "Clear"
        FOG -> "Foggy"
        RAIN -> "Rainy"
        SNOW -> "Snowy"
        STORM -> "Thunderstorm"
        else -> "Cloudy"
    }
}
