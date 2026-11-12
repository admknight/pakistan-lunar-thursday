package com.scopewise.lunarthursday;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Read-only in-app update indicator: checks the project's own GitHub releases. */
final class UpdateManager {
    static final String AUTO_CHECK = "updates_auto_check";
    static final String INCLUDE_PREVIEWS = "updates_include_previews";
    private static final String LAST_ATTEMPT = "updates_last_attempt";
    private static final String LAST_SUCCESS = "updates_last_success";
    private static final String LATEST_TAG = "updates_latest_tag";
    private static final String LATEST_URL = "updates_latest_download_url";
    private static final String LATEST_PAGE = "updates_latest_release_page";
    private static final String LATEST_PREVIEW = "updates_latest_is_preview";
    private static final String FEED = "https://api.github.com/repos/admknight/pakistan-lunar-thursday/releases?per_page=20";
    private static final String RELEASE_PREFIX = "https://github.com/admknight/pakistan-lunar-thursday/releases/";
    private static final String DOWNLOAD_PREFIX = RELEASE_PREFIX + "download/";
    private static final long SUCCESS_INTERVAL = 12L * 60 * 60 * 1000;
    private static final long FAILURE_INTERVAL = 30L * 60 * 1000;
    private static final int MAX_BYTES = 256 * 1024;
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final AtomicBoolean BUSY = new AtomicBoolean();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private UpdateManager() { }

    interface Callback { void finished(boolean success, String message); }

    static final class Result {
        final String tag;
        final String url;
        final String page;
        final boolean preview;
        Result(String tag, String url, String page, boolean preview) {
            this.tag = tag;
            this.url = url;
            this.page = page;
            this.preview = preview;
        }
    }

    static String installed(Context context) {
        try {
            String value = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            return value == null ? "0.0.0" : value;
        } catch (Exception ignored) { return "0.0.0"; }
    }

    /** Release builds must not offer debug-signed preview APKs as in-place updates. */
    static boolean isReleaseBuild(Context context) {
        return (context.getApplicationInfo().flags
                & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) == 0;
    }

    static Result available(Context context) {
        SharedPreferences p = ReminderStore.prefs(context);
        String tag = p.getString(LATEST_TAG, "");
        String url = p.getString(LATEST_URL, "");
        String page = p.getString(LATEST_PAGE, "");
        boolean preview = p.getBoolean(LATEST_PREVIEW, true);
        if (preview && (isReleaseBuild(context)
                || !p.getBoolean(INCLUDE_PREVIEWS, true))) return null;
        if (VersionPolicy.compare(tag, installed(context)) <= 0) return null;
        if (!safeUrl(url, DOWNLOAD_PREFIX, ".apk") || !safeUrl(page, RELEASE_PREFIX, "")) return null;
        return new Result(tag, url, page, preview);
    }

    static long lastChecked(Context context) {
        return ReminderStore.prefs(context).getLong(LAST_SUCCESS, 0L);
    }

    static boolean isChecking() { return BUSY.get(); }

    static void check(Activity activity, boolean forced, Callback callback) {
        SharedPreferences p = ReminderStore.prefs(activity);
        if (!forced && !p.getBoolean(AUTO_CHECK, true)) return;
        long now = System.currentTimeMillis();
        long lastSuccess = p.getLong(LAST_SUCCESS, 0L);
        long lastAttempt = p.getLong(LAST_ATTEMPT, 0L);
        if (!forced && now >= lastSuccess && now - lastSuccess < SUCCESS_INTERVAL) return;
        if (!forced && now >= lastAttempt && now - lastAttempt < FAILURE_INTERVAL) return;
        if (!BUSY.compareAndSet(false, true)) {
            if (forced && callback != null) callback.finished(false, "Update check already in progress");
            return;
        }
        Context app = activity.getApplicationContext();
        p.edit().putLong(LAST_ATTEMPT, now).apply();
        IO.execute(() -> {
            boolean success = false;
            String status;
            try {
                Result best = fetchLatest(!isReleaseBuild(app)
                        && p.getBoolean(INCLUDE_PREVIEWS, true), installed(app));
                SharedPreferences.Editor e = ReminderStore.prefs(app).edit();
                if (best == null) {
                    e.remove(LATEST_TAG).remove(LATEST_URL).remove(LATEST_PAGE).remove(LATEST_PREVIEW);
                    status = "No newer compatible release found";
                } else {
                    e.putString(LATEST_TAG, best.tag).putString(LATEST_URL, best.url)
                            .putString(LATEST_PAGE, best.page).putBoolean(LATEST_PREVIEW, best.preview);
                    status = "New version available: " + VersionPolicy.display(best.tag);
                }
                e.putLong(LAST_SUCCESS, System.currentTimeMillis()).apply();
                success = true;
            } catch (Exception ex) {
                status = "Could not check GitHub. Check your connection or try later.";
            } finally {
                BUSY.set(false);
            }
            final boolean finalSuccess = success;
            final String finalStatus = status;
            if (callback != null) MAIN.post(() -> {
                if (!activity.isFinishing() && !activity.isDestroyed()) callback.finished(finalSuccess, finalStatus);
            });
        });
    }

    private static Result fetchLatest(boolean includePreviews, String installed) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(FEED).openConnection();
        connection.setConnectTimeout(8500);
        connection.setReadTimeout(8500);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        connection.setRequestProperty("User-Agent", "LunarThursday-Android-UpdateCheck");
        try {
            if (connection.getResponseCode() != 200) throw new IllegalStateException("GitHub API unavailable");
            byte[] payload;
            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = input.read(buf)) != -1) {
                    if (out.size() + n > MAX_BYTES) throw new IllegalStateException("Unexpected release list size");
                    out.write(buf, 0, n);
                }
                payload = out.toByteArray();
            }
            JSONArray releases = new JSONArray(new String(payload, StandardCharsets.UTF_8));
            Result best = null;
            for (int i = 0; i < releases.length(); i++) {
                JSONObject entry = releases.optJSONObject(i);
                if (entry == null || entry.optBoolean("draft", false)) continue;
                boolean preview = entry.optBoolean("prerelease", false);
                if (preview && !includePreviews) continue;
                String tag = entry.optString("tag_name", "");
                if (!VersionPolicy.validTag(tag) || VersionPolicy.compare(tag, installed) <= 0) continue;
                String page = entry.optString("html_url", "");
                if (!safeUrl(page, RELEASE_PREFIX + "tag/", "")) continue;
                JSONArray assets = entry.optJSONArray("assets");
                if (assets == null) continue;
                String apk = "";
                for (int j = 0; j < assets.length(); j++) {
                    JSONObject asset = assets.optJSONObject(j);
                    if (asset == null || !asset.optString("name", "").endsWith(".apk")) continue;
                    String candidate = asset.optString("browser_download_url", "");
                    if (!safeUrl(candidate, DOWNLOAD_PREFIX, ".apk")) continue;
                    apk = candidate;
                    break;
                }
                if (apk.isEmpty()) continue;
                if (best == null || VersionPolicy.compare(tag, best.tag) > 0) {
                    best = new Result(tag, apk, page, preview);
                }
            }
            return best;
        } finally { connection.disconnect(); }
    }

    private static boolean safeUrl(String raw, String prefix, String suffix) {
        if (raw == null || raw.length() > 2048 || !raw.startsWith(prefix) || !raw.endsWith(suffix)) return false;
        try {
            URL parsed = new URL(raw);
            return "https".equals(parsed.getProtocol()) && "github.com".equals(parsed.getHost())
                    && parsed.getUserInfo() == null && parsed.getPort() == -1;
        } catch (Exception ex) { return false; }
    }
}
