package com.scopewise.lunarthursday;

/** WMO weather classification for decorative scenes (not a weather warning service). */
final class WeatherCode {
    static final int CLEAR = 0;
    static final int CLOUD = 1;
    static final int FOG = 2;
    static final int RAIN = 3;
    static final int SNOW = 4;
    static final int STORM = 5;

    private WeatherCode() { }

    static int category(int code) {
        if (code == 0 || code == 1) return CLEAR;
        if (code == 2 || code == 3) return CLOUD;
        if (code == 45 || code == 48) return FOG;
        if (code >= 95 && code <= 99) return STORM;
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return SNOW;
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) return RAIN;
        return CLOUD;
    }

    static String title(int code) {
        switch (category(code)) {
            case CLEAR: return "Clear";
            case FOG: return "Foggy";
            case RAIN: return "Rainy";
            case SNOW: return "Snowy";
            case STORM: return "Thunderstorm";
            default: return "Cloudy";
        }
    }
}
