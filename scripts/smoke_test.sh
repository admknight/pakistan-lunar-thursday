#!/usr/bin/env bash
# Emulator smoke test: installs the freshly built debug APK, opens every screen in
# English and Urdu, rings the test alarm, runs the background jobs, and fails on any crash.
# It proves the app starts and its screens open. It does not check dates, alarm timing or sound.
set -uo pipefail

PACKAGE=com.scopewise.lunarthursday
APK="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"
OUT="${SMOKE_OUT:-smoke-output}"
mkdir -p "$OUT"
failures=0

fail() { echo "[smoke] FAIL: $1"; failures=$((failures + 1)); }

shot() { adb exec-out screencap -p > "$OUT/$1.png" 2>/dev/null || true; }

crashed() {
  adb logcat -d -b crash 2>/dev/null | grep -q "$PACKAGE" && return 0
  adb logcat -d 2>/dev/null | grep -A3 "FATAL EXCEPTION" | grep -q "$PACKAGE"
}

# A slow emulator sometimes shows "<system app> isn't responding" over the app; choose Wait.
dump_screen() {
  local try
  for try in 1 2 3 4; do
    adb shell rm -f /sdcard/smoke.xml > /dev/null 2>&1
    adb shell uiautomator dump /sdcard/smoke.xml > /dev/null 2>&1
    adb exec-out cat /sdcard/smoke.xml > "$OUT/screen.xml" 2>/dev/null
    grep -q "<hierarchy" "$OUT/screen.xml" && return 0
    sleep 2
  done
  : > "$OUT/screen.xml"
}

dismiss_system_dialog() {
  local attempt
  for attempt in 1 2 3; do
    dump_screen
    grep -q "responding" "$OUT/screen.xml" || return 0
    adb shell input tap 330 1338
    sleep 3
  done
}

swipe_up() {
  dismiss_system_dialog
  adb shell input swipe 500 1500 500 "$1" 300
  sleep 2
}

# Opens one screen and requires it to be the resumed activity with no crash recorded.
open_screen() {
  local name="$1" activity="$2"; shift 2
  adb shell am start -W -n "$PACKAGE/.$activity" "$@" > /dev/null 2>&1
  sleep 4
  dismiss_system_dialog
  shot "$name"
  if crashed; then fail "$name: crash recorded"; return; fi
  if adb shell dumpsys activity activities | grep -E "ResumedActivity" | grep -q "$PACKAGE/.$activity"; then
    echo "[smoke] ok: $name"
  else
    fail "$name: $activity is not in the foreground"
  fi
}

# True when the text is on screen. dismiss_system_dialog leaves a fresh copy of the screen in a file;
# piping adb straight into "grep -q" fails under pipefail.
on_screen() {
  dismiss_system_dialog
  grep -q "$1" "$OUT/screen.xml"
}

test -s "$APK" || { echo "[smoke] APK missing: $APK"; exit 1; }
adb wait-for-device
adb root > /dev/null 2>&1 || true
sleep 3
adb wait-for-device
adb shell input keyevent KEYCODE_WAKEUP || true
adb shell wm dismiss-keyguard || true
adb install -r "$APK" || { echo "[smoke] install failed"; exit 1; }
adb shell pm grant "$PACKAGE" android.permission.POST_NOTIFICATIONS || true
adb logcat -c || true
adb logcat -b crash -c || true
sleep 20  # let the freshly booted launcher settle

echo "[smoke] English screens"
open_screen 01-home-en MainActivity
on_screen "Your next lunar Thursday" || fail "home: expected heading not shown"
adb shell am force-stop "$PACKAGE"
open_screen 02-calendar-en MainActivity --ei open_tab 1
swipe_up 500; shot 02b-calendar-observances-en
adb shell am force-stop "$PACKAGE"
open_screen 03-updates-en MainActivity --ei open_tab 2
open_screen 04-settings-en SettingsActivity
swipe_up 300; shot 04b-settings-lower-en
open_screen 05-about-en AboutActivity
open_screen 06-verse-card MorningCardActivity
open_screen 07-tasbih TasbihActivity
open_screen 08-qibla-compass QiblaCompassActivity
open_screen 09-alarm-tones AlarmToneActivity

# Requires a piece of text to be on the current screen, scrolling down in small steps to find it.
# Swipes are sometimes dropped on a slow emulator, so one fixed swipe is not relied on.
expect_text() {
  local step
  for step in 1 2 3 4 5 6 7 8; do
    if on_screen "$2"; then echo "[smoke] ok: $1 shows '$2'"; return 0; fi
    adb shell input swipe 500 1400 500 1000 400; sleep 2
  done
  fail "$1: '$2' was not found on the screen, even after scrolling"
}

echo "[smoke] Verse card carries both dates"
open_screen 06b-verse-card-dates MorningCardActivity
expect_text "verse card" " AH"

write_prefs() {
  # $1 = extra <map> entries. Rewrites the app settings file while the app is stopped.
  adb shell am force-stop "$PACKAGE"
  adb shell "run-as $PACKAGE sh -c 'cat shared_prefs/lunar_thursday_settings.xml'" > "$OUT/prefs-read.xml" 2>/dev/null || true
  if ! grep -q "</map>" "$OUT/prefs-read.xml" 2>/dev/null; then fail "could not read the settings file"; return 1; fi
  grep -v "hijri_sunset_city_index\|auto_place_\|weather_visuals_enabled" "$OUT/prefs-read.xml" \
    | sed "s#</map>#$1</map>#" > "$OUT/prefs-write.xml"
  adb push "$OUT/prefs-write.xml" /data/local/tmp/prefs-write.xml > /dev/null
  adb shell "run-as $PACKAGE sh -c 'cat /data/local/tmp/prefs-write.xml > shared_prefs/lunar_thursday_settings.xml'" || true
}

echo "[smoke] No city, weather location on: prayer times and Qibla come from the kept position"
adb emu geo fix 74.36 31.52 > /dev/null 2>&1 || true
adb shell pm grant "$PACKAGE" android.permission.ACCESS_COARSE_LOCATION || true
write_prefs '<boolean name="weather_visuals_enabled" value="true" /><int name="auto_place_lat_tenths" value="315" /><int name="auto_place_lon_tenths" value="744" />'
open_screen 09b-home-auto-location MainActivity
swipe_up 500; shot 09c-home-auto-location-prayer-times
expect_text "auto location" "Fajr"
expect_text "auto location" "Your location"
expect_text "auto location" "Find the Qibla direction"
shot 09c2-home-qibla-row
open_screen 09d-qibla-compass-auto-location QiblaCompassActivity
if on_screen "Choose a city"; then fail "compass: still asks for a city with a kept position"; fi

echo "[smoke] Neither city nor location: the app says what to do"
adb shell pm revoke "$PACKAGE" android.permission.ACCESS_COARSE_LOCATION || true
write_prefs ''
open_screen 09e-home-no-place MainActivity
swipe_up 500
expect_text "no place" "Choose a city in Settings"

echo "[smoke] City chosen, so prayer times are shown"
write_prefs '<int name="hijri_sunset_city_index" value="2" />'
open_screen 10-home-with-city MainActivity
swipe_up 500; shot 10b-home-prayer-times
expect_text "city" "Fajr"
expect_text "city" "Lahore"
open_screen 10c-qibla-compass-city QiblaCompassActivity

echo "[smoke] Ringing test alarm"
# Extras are single words: adb re-splits quoted arguments on the device.
adb shell am start-foreground-service -n "$PACKAGE/.AlarmRingingService" \
  -a com.scopewise.lunarthursday.ALARM_START --es lunar_alarm_label SmokeTest \
  --es lunar_alarm_month_key test --es lunar_alarm_basis TEST --ez lunar_alarm_test true > /dev/null 2>&1
sleep 4
shot 11-alarm-ringing
adb shell dumpsys activity services "$PACKAGE" | grep -q "AlarmRingingService" || fail "alarm: ringing service is not running"
open_screen 12-alarm-screen AlarmActivity --es lunar_alarm_label SmokeTest --ez lunar_alarm_test true
adb shell am startservice -n "$PACKAGE/.AlarmRingingService" -a com.scopewise.lunarthursday.ALARM_DISMISS > /dev/null 2>&1
sleep 3
crashed && fail "alarm: crash recorded"

echo "[smoke] Restart handling and background jobs"
adb shell am broadcast -a android.intent.action.BOOT_COMPLETED -n "$PACKAGE/.SystemEventReceiver" > /dev/null 2>&1
sleep 3
for job in 20051 20052 20053; do
  adb shell cmd jobscheduler run -f "$PACKAGE" "$job" > /dev/null 2>&1 || true
done
sleep 20
crashed && fail "background: crash recorded after restart broadcast or jobs"

echo "[smoke] Urdu screens"
adb shell cmd locale set-app-locales "$PACKAGE" --locales ur > /dev/null 2>&1 || fail "could not switch the app to Urdu"
adb shell am force-stop "$PACKAGE"
open_screen 13-home-ur MainActivity
swipe_up 500; shot 13b-home-lower-ur
adb shell am force-stop "$PACKAGE"
open_screen 14-calendar-ur MainActivity --ei open_tab 1
open_screen 15-settings-ur SettingsActivity
open_screen 16-about-ur AboutActivity
open_screen 17-verse-card-ur MorningCardActivity

echo "[smoke] Verse card on later dates (its look changes with the date)"
adb shell settings put global auto_time 0 > /dev/null 2>&1 || true
for ahead in 1 9 17; do
  adb shell "date $(date -u -d "+$ahead days" +%m%d%H%M%Y.%S)" > /dev/null 2>&1 || true
  adb shell am force-stop "$PACKAGE"
  open_screen "18-verse-card-plus-$ahead-days" MorningCardActivity
done

adb logcat -d > "$OUT/logcat.txt" 2>/dev/null || true
adb logcat -d -b crash > "$OUT/crash.txt" 2>/dev/null || true
if [ "$failures" -gt 0 ]; then
  echo "[smoke] $failures check(s) failed"
  grep -B2 -A25 "FATAL EXCEPTION" "$OUT/logcat.txt" | head -120 || true
  exit 1
fi
echo "[smoke] All checks passed"
