#!/usr/bin/env bash
set -euo pipefail

# Real Android emulator captures of the installed, signed GitHub release APK.
# No mocked app screens, synthetic dates, or Photoshop composites.
PACKAGE=com.scopewise.lunarthursday
APK_PATH="${APK_PATH:-release-apk/HilalThursday-v1.9.0.apk}"
OUTPUT=docs/screenshots
mkdir -p "$OUTPUT"

echo "[capture] Checking installed release APK"
test -s "$APK_PATH"
adb wait-for-device
adb shell input keyevent KEYCODE_WAKEUP || true
adb shell settings put global window_animation_scale 0 || true
adb shell settings put global transition_animation_scale 0 || true
adb shell settings put global animator_duration_scale 0 || true
adb shell settings put system font_scale 1.0 || true
adb shell settings put system accelerometer_rotation 0 || true
adb shell settings put system user_rotation 0 || true
adb install -r "$APK_PATH"
adb shell pm grant "$PACKAGE" android.permission.POST_NOTIFICATIONS || true
adb shell input keyevent 3 || true
adb shell am start -W -n "$PACKAGE/.MainActivity"
sleep 5
# On a fresh emulator the launcher can regain focus during first-boot setup.
# Bring the real installed app to the foreground again if needed.
for attempt in 1 2 3 4; do
  adb shell am start -W -n "$PACKAGE/.MainActivity" >/dev/null
  sleep 3
  if adb shell uiautomator dump /sdcard/hilal_initial.xml >/dev/null 2>&1 && \
     adb exec-out cat /sdcard/hilal_initial.xml > /tmp/hilal_initial.xml && \
     grep -q 'Your next lunar Thursday' /tmp/hilal_initial.xml; then
    echo "Confirmed app is in foreground after attempt $attempt"
    break
  fi
done

# Query accessibility's actual UI hierarchy, then tap the requested real control.
# Prefer a clickable exact-label node over child labels, and fail rather than guess.
tap_ui() {
  local label="$1" retry
  for retry in 1 2 3; do
    if adb shell uiautomator dump /sdcard/hilal_window.xml >/dev/null 2>&1 && \
       adb exec-out cat /sdcard/hilal_window.xml > /tmp/hilal_window.xml; then
      break
    fi
    sleep 2
  done
  python3 - "$label" <<'PY'
import re, subprocess, sys, xml.etree.ElementTree as ET
label=sys.argv[1]
root=ET.parse('/tmp/hilal_window.xml').getroot()
items=[]
for node in root.iter('node'):
    attrs=node.attrib
    desc=attrs.get('content-desc','').strip()
    text=attrs.get('text','').strip()
    if label in (desc,text):
        bounds=re.fullmatch(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]',attrs.get('bounds',''))
        if bounds:
            x1,y1,x2,y2=map(int,bounds.groups())
            items.append((not (attrs.get('clickable')=='true'),abs((x2-x1)*(y2-y1)),(x1+x2)//2,(y1+y2)//2,desc,text))
if not items:
    print('Target missing in live UI:',repr(label),file=sys.stderr)
    print('Visible:',[n.attrib.get('text') or n.attrib.get('content-desc') for n in root.iter('node') if n.attrib.get('text') or n.attrib.get('content-desc')][:70],file=sys.stderr)
    sys.exit(1)
items.sort()
_,_,x,y,desc,text=items[0]
print('Tapping actual UI control', repr(label), 'at',x,y)
subprocess.run(['adb','shell','input','tap',str(x),str(y)],check=True)
PY
  sleep 2
}

assert_ui() {
  local text="$1"
  adb shell uiautomator dump /sdcard/hilal_window.xml >/dev/null 2>&1
  adb exec-out cat /sdcard/hilal_window.xml > /tmp/hilal_window.xml
  python3 - "$text" <<'PY'
import sys, xml.etree.ElementTree as ET
needle=sys.argv[1]
root=ET.parse('/tmp/hilal_window.xml').getroot()
words=[n.attrib.get('text','') for n in root.iter('node')]
if not any(needle in w for w in words):
    print('Expected screen content missing:',repr(needle),'available:',words[:75],file=sys.stderr)
    sys.exit(1)
print('Screen confirmed from app UI:',needle)
PY
}

capture() {
  local name="$1" check="$2"
  assert_ui "$check"
  sleep 2
  adb exec-out screencap -p > "$OUTPUT/$name.png"
  python3 - "$OUTPUT/$name.png" <<'PY'
import os,struct,sys
p=sys.argv[1]
with open(p,'rb') as f:
    head=f.read(24)
if not head.startswith(b'\x89PNG\r\n\x1a\n') or head[12:16] != b'IHDR':
    raise SystemExit('Invalid PNG screenshot: '+p)
w,h=struct.unpack('>II',head[16:24]); size=os.path.getsize(p)
if w<480 or h<750 or size<20000:
    raise SystemExit(f'Screenshot appears empty/wrong size: {p} {w}x{h} {size} bytes')
print('Verified PNG',p, f'{w}x{h}',f'{size} bytes')
PY
}

capture home-en 'Your next lunar Thursday'
assert_ui 'Expected Hijri'
tap_ui Calendar
capture calendar-en 'Upcoming lunar months'
assert_ui 'Expected Hijri'
tap_ui Updates
capture updates-en 'Updates & calendar'
tap_ui About
capture about-en 'About / Developer'
adb shell input keyevent 4
sleep 2
tap_ui 'More options'
tap_ui Settings
capture settings-en 'How reminders reach you'
# Enable audible ringing only in this disposable emulator, to expose the real sound selector.
tap_ui 'Ring audible alarm'
capture settings-alarm-en 'Alarm sound'
tap_ui 'Alarm sound · Hilal Noor'
capture alarm-sounds-en 'Hilal signature sounds'
assert_ui 'Choose from phone'
adb shell input keyevent 4
sleep 2
# The second Settings view shows the weather/location controls without opting in.
adb shell input swipe 550 1800 550 750 650
sleep 2
capture weather-settings-en 'Sky and local weather'
# Return to Settings top, select Urdu through app's own Settings dialog.
adb shell input swipe 550 500 550 1970 500
sleep 2
tap_ui 'App language'
tap_ui 'اردو'
sleep 5
# The normal app restart should land on the translated home dashboard.
assert_ui 'ہلال'
# Ensure real localized text is present (the English heading must be translated).
python3 - <<'PY'
import xml.etree.ElementTree as ET
root=ET.parse('/tmp/hilal_window.xml').getroot()
texts=[n.attrib.get('text','') for n in root.iter('node')]
if any('Your next lunar Thursday, at a glance' in t for t in texts):
    raise SystemExit('Urdu selection did not localize the home heading')
if not any(any('\u0600'<=ch<='\u06ff' for ch in t) for t in texts):
    raise SystemExit('No Urdu text found on Urdu dashboard')
print('Urdu UI confirmed on live emulator')
PY
capture home-ur 'ہلال'
assert_ui 'متوقع ہجری تاریخ'

echo "[capture] All screenshots are live emulator captures from signed APK."
ls -lh "$OUTPUT"/*.png
