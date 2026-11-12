package com.scopewise.lunarthursday;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Opt-in foreground coarse location and weather, used ONLY to decorate the home sky. */
final class WeatherManager {
    static final String ENABLED = "weather_visuals_enabled";
    private static final String LAST_ATTEMPT = "weather_attempt";
    private static final String LAST_SUCCESS = "weather_success";
    private static final String CODE = "weather_wmo";
    private static final String DAY = "weather_is_day";
    private static final String TEMP = "weather_temp_c";
    private static final long RECHECK_MS = 45L * 60L * 1000L;
    private static final long MAX_AGE_MS = 3L * 60L * 60L * 1000L;
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final AtomicBoolean BUSY = new AtomicBoolean();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    static final class Scene {
        final int code;
        final boolean day;
        final int temp;
        Scene(int code, boolean day, int temp) { this.code=code; this.day=day; this.temp=temp; }
        String description(Context context) {
            return UiText.tr(context, WeatherCode.title(code)) + " · " + temp + "°C";
        }
    }

    static boolean enabled(Context context) {
        return ReminderStore.prefs(context).getBoolean(ENABLED, false);
    }

    static boolean permissionGranted(Context context) {
        return context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    static Scene current(Context context) {
        if (!enabled(context) || !permissionGranted(context)) return null;
        SharedPreferences p = ReminderStore.prefs(context);
        long then = p.getLong(LAST_SUCCESS, 0);
        long age = System.currentTimeMillis() - then;
        if (then <= 0 || age < 0 || age > MAX_AGE_MS) return null;
        return new Scene(p.getInt(CODE, 3), p.getBoolean(DAY, true), p.getInt(TEMP, 0));
    }

    static void disable(Context context) {
        HijriClock.clearWeatherSunsets(context);
        ReminderStore.prefs(context).edit().putBoolean(ENABLED, false)
                .remove(CODE).remove(DAY).remove(TEMP)
                .remove(LAST_SUCCESS).remove(LAST_ATTEMPT).apply();
    }

    static void refresh(Activity activity, boolean forced, Runnable onComplete) {
        if (!enabled(activity) || !permissionGranted(activity)) return;
        SharedPreferences p = ReminderStore.prefs(activity);
        long now=System.currentTimeMillis();
        long attempt=p.getLong(LAST_ATTEMPT, 0);
        if (!forced && now>=attempt && now-attempt < RECHECK_MS) return;
        if (!BUSY.compareAndSet(false, true)) return;
        p.edit().putLong(LAST_ATTEMPT, now).apply();
        LocationManager location = activity.getSystemService(LocationManager.class);
        if (location == null || !location.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            finish(onComplete); return;
        }
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                CancellationSignal cancel = new CancellationSignal();
                AtomicBoolean responded = new AtomicBoolean();
                location.getCurrentLocation(LocationManager.NETWORK_PROVIDER, cancel,
                        activity.getMainExecutor(), found -> {
                            if (!responded.compareAndSet(false, true)) return;
                            Location chosen = found == null ? recentLastLocation(location) : found;
                            fetch(activity.getApplicationContext(), chosen, onComplete);
                        });
                MAIN.postDelayed(() -> {
                    if (responded.compareAndSet(false, true)) {
                        cancel.cancel();
                        fetch(activity.getApplicationContext(), recentLastLocation(location), onComplete);
                    }
                }, 16_000L);
            } else {
                Location recent = recentLastLocation(location);
                if (recent != null) { fetch(activity.getApplicationContext(), recent, onComplete); return; }
                AtomicBoolean responded = new AtomicBoolean();
                final LocationListener listener = new LocationListener() {
                    @Override public void onLocationChanged(Location where) {
                        if (responded.compareAndSet(false, true)) {
                            fetch(activity.getApplicationContext(), where, onComplete);
                        }
                    }
                    @Override public void onProviderEnabled(String provider) { }
                    @Override public void onProviderDisabled(String provider) { }
                    @Override public void onStatusChanged(String provider, int status, android.os.Bundle extras) { }
                };
                location.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, Looper.getMainLooper());
                MAIN.postDelayed(() -> {
                    if (responded.compareAndSet(false, true)) {
                        location.removeUpdates(listener);
                        fetch(activity.getApplicationContext(), recentLastLocation(location), onComplete);
                    }
                }, 14_000L);
            }
        } catch (SecurityException | IllegalArgumentException e) { finish(onComplete); }
    }

    private static Location recentLastLocation(LocationManager manager) {
        try {
            Location loc=manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (loc == null) return null;
            long age=System.currentTimeMillis()-loc.getTime();
            return age>=0 && age<=6L*60L*60L*1000L ? loc : null;
        } catch (RuntimeException e) { return null; }
    }

    private static void fetch(Context context, Location found, Runnable onComplete) {
        if (found == null) { finish(onComplete); return; }
        // Approximate the coordinates before sending to the weather service (~1 km).
        double latitude=Math.rint(found.getLatitude()*100.0)/100.0;
        double longitude=Math.rint(found.getLongitude()*100.0)/100.0;
        if (Math.abs(latitude)>90||Math.abs(longitude)>180) {finish(onComplete);return;}
        IO.execute(() -> {
            HttpURLConnection conn=null;
            try {
                String query=String.format(Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f&current=temperature_2m,weather_code,is_day&forecast_days=1", latitude, longitude);
                conn=(HttpURLConnection)new URL(query).openConnection();
                conn.setConnectTimeout(8000);conn.setReadTimeout(8000);
                conn.setRequestProperty("Accept", "application/json");
                if (conn.getResponseCode()!=200) throw new IllegalStateException("Weather unavailable");
                byte[] data;
                try(InputStream in=conn.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                    byte[] chunk=new byte[4096];int n;
                    while((n=in.read(chunk))!=-1){if(out.size()+n>16*1024)throw new IllegalStateException("Response too large");out.write(chunk,0,n);}
                    data=out.toByteArray();
                }
                JSONObject current=new JSONObject(new String(data, StandardCharsets.UTF_8)).getJSONObject("current");
                int code=current.getInt("weather_code");
                int isDay=current.getInt("is_day");
                double temp=current.getDouble("temperature_2m");
                if(code<0||code>99||(isDay!=0&&isDay!=1)||!Double.isFinite(temp)||temp< -100||temp>70)
                    throw new IllegalStateException("Bad weather values");
                if (enabled(context) && permissionGranted(context)) {
                    ReminderStore.prefs(context).edit().putInt(CODE,code).putBoolean(DAY,isDay==1)
                      .putInt(TEMP,(int)Math.round(temp)).putLong(LAST_SUCCESS,System.currentTimeMillis()).apply();
                    HijriClock.saveWeatherSunsets(context, latitude, longitude);
                }
            } catch (Exception ignored) { /* Retain time-aware fallback when offline. */ }
            finally { if(conn!=null)conn.disconnect();finish(onComplete); }
        });
    }

    private static void finish(Runnable done) {
        BUSY.set(false);
        if(done!=null) MAIN.post(done);
    }
}
