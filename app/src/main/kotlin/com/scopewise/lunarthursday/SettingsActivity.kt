package com.scopewise.lunarthursday

import android.Manifest
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import com.google.android.material.materialswitch.MaterialSwitch
import java.time.Instant
import java.time.format.DateTimeFormatter

/** User preferences, permission guidance, and the release update manager. No date editing. */
class SettingsActivity : LocalizedActivity() {
    private var p: SharedPreferences? = null
    private var primary = 0
    private var onPrimary = 0
    private var surface = 0
    private var onSurface = 0
    private var onSurfaceVariant = 0
    private var primaryContainer = 0
    private var surfaceVariant = 0

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        DynamicColors.applyToActivityIfAvailable(this)
        p = ReminderStore.prefs(this)
        render()
        if (intent.getBooleanExtra("open_language_dialog", false)) {
            intent.removeExtra("open_language_dialog")
            Handler(Looper.getMainLooper()).post { chooseLanguage() }
        }
    }

    override fun onResume() {
        super.onResume()
        if (p != null) render()
        AppUpdateInstaller.resumeIfPermitted(this)
    }

    private fun render() {
        val p = ReminderStore.prefs(this)
        primary = col(androidx.appcompat.R.attr.colorPrimary, 0xFF136F53)
        onPrimary = col(com.google.android.material.R.attr.colorOnPrimary, 0xFFFFFFFF)
        surface = col(com.google.android.material.R.attr.colorSurface, 0xFFFFFFFF)
        onSurface = col(com.google.android.material.R.attr.colorOnSurface, 0xFF1B211E)
        onSurfaceVariant = col(com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF414943)
        primaryContainer = col(com.google.android.material.R.attr.colorPrimaryContainer, 0xFFBCEAD3)
        surfaceVariant = col(com.google.android.material.R.attr.colorSurfaceVariant, 0xFFE1E8E2)
        val scroll = ScrollView(this)
        scroll.isFillViewport = true
        scroll.setBackgroundColor(surface)
        val body = column()
        body.setPadding(dp(20), dp(32), dp(20), dp(44))
        scroll.addView(body)
        setContentView(scroll)

        add(body, slimAction("←", "Back") { finish() }, 0)
        add(body, text("Settings", 28, true, onSurface), 9)
        add(body, slimAction("🌐", "App language") { chooseLanguage() }, 10)
        add(body, text("Choose the language used for the app screens and reminders.", 12, false, onSurfaceVariant), 5)
        add(body, text("Alert delivery, permissions and app updates", 14, false, onSurfaceVariant), 4)

        val alert = panel()
        add(alert, text("How reminders reach you", 19, true, onSurface), 0)
        add(alert, slimAction("⏰", getString(R.string.hijri_reminder_time) + " · " +
            UiText.reminderTimeText(this) + " PKT") { chooseReminderTime() }, 9)
        add(alert, text(getString(R.string.hijri_time_note), 12, false, onSurfaceVariant), 5)
        val ringing = p.getBoolean(ReminderStore.ALARM_MODE, false)
        toggle(alert, "Ring audible alarm",
            "Use a ringing alarm with Dismiss and Snooze, instead of a simple notification.", ringing) { value ->
            p.edit().putBoolean(ReminderStore.ALARM_MODE, value).apply()
            render()
        }
        if (ringing) {
            toggle(alert, "Play alarm sound", getString(R.string.tone_sound_help),
                p.getBoolean(ReminderStore.ALARM_SOUND, true)) { value ->
                p.edit().putBoolean(ReminderStore.ALARM_SOUND, value).apply()
            }
            val toneName = when (AlarmTonePlayer.selection(this)) {
                AlarmTonePolicy.SUKOON -> R.string.tone_sukoon
                AlarmTonePolicy.NIDA -> R.string.tone_nida
                AlarmTonePolicy.PHONE -> R.string.tone_phone
                else -> R.string.tone_noor
            }
            add(alert, slimAction("♪", getString(R.string.tone_heading) + " · " + getString(toneName)) {
                startActivity(Intent(this, AlarmToneActivity::class.java))
            }, 9)
            toggle(alert, "Vibrate", "Vibration follows the device's vibration policy.",
                p.getBoolean(ReminderStore.ALARM_VIBRATE, true)) { value ->
                p.edit().putBoolean(ReminderStore.ALARM_VIBRATE, value).apply()
            }
            add(alert, slimAction("🔔", "Test ringing alarm") {
                if (!ReminderNotifications.permitted(this)) {
                    askNotifications()
                } else if (!AlarmRingingService.startTest(this)) {
                    toast("Alarm service unavailable on this device")
                }
            }, 16)
            if (Build.VERSION.SDK_INT >= 34 && !AlarmRingingService.canUseFullScreen(this)) {
                add(alert, text("Full-screen alarm display is not allowed. Android may show a heads-up alert instead. Alarm sound can still play when permitted.",
                    13, false, onSurfaceVariant), 12)
                add(alert, button("Allow full-screen alarm display", false) { askFullScreen() }, 8)
            }
        }
        add(alert, text(getString(R.string.hijri_first_thursday_note), 13, false, onSurfaceVariant), 16)
        add(body, card(alert), 22)

        val observances = panel()
        add(observances, text(getString(R.string.events_settings_title), 19, true, onSurface), 0)
        add(observances, text(getString(R.string.events_settings_help), 13, false, onSurfaceVariant), 6)
        toggle(observances, getString(R.string.events_set_shared), getString(R.string.events_set_shared_help),
            EventsStore.isShown(this, IslamicEvents.SET_SHARED)) { value ->
            EventsStore.setShown(this, IslamicEvents.SET_SHARED, value)
        }
        toggle(observances, getString(R.string.events_set_a), getString(R.string.events_set_a_help),
            EventsStore.isShown(this, IslamicEvents.SET_A)) { value ->
            EventsStore.setShown(this, IslamicEvents.SET_A, value)
        }
        toggle(observances, getString(R.string.events_set_b), getString(R.string.events_set_b_help),
            EventsStore.isShown(this, IslamicEvents.SET_B)) { value ->
            EventsStore.setShown(this, IslamicEvents.SET_B, value)
        }
        toggle(observances, getString(R.string.events_remind), getString(R.string.events_remind_help),
            EventsStore.remindersOn(this)) { value -> EventsStore.setRemindersOn(this, value) }
        add(body, card(observances), 16)

        val extras = panel()
        add(extras, text(getString(R.string.extra_title), 19, true, onSurface), 0)
        toggle(extras, getString(R.string.extra_prayer_toggle), getString(R.string.extra_prayer_help),
            ExtraReminderScheduler.isOn(this, ExtraReminderScheduler.PRAYER_ALERTS)) { value ->
            ExtraReminderScheduler.setOn(this, ExtraReminderScheduler.PRAYER_ALERTS, value)
        }
        toggle(extras, getString(R.string.extra_thursday_toggle), getString(R.string.extra_thursday_help),
            ExtraReminderScheduler.isOn(this, ExtraReminderScheduler.THURSDAY_EVENING)) { value ->
            ExtraReminderScheduler.setOn(this, ExtraReminderScheduler.THURSDAY_EVENING, value)
        }
        toggle(extras, getString(R.string.extra_friday_toggle), getString(R.string.extra_friday_help),
            ExtraReminderScheduler.isOn(this, ExtraReminderScheduler.FRIDAY)) { value ->
            ExtraReminderScheduler.setOn(this, ExtraReminderScheduler.FRIDAY, value)
        }
        add(body, card(extras), 16)

        val announcement = panel()
        add(announcement, text(getString(R.string.announcement_title), 19, true, onSurface), 0)
        toggle(announcement, getString(R.string.announcement_toggle), getString(R.string.announcement_help),
            AnnouncementSync.isOn(this)) { value -> AnnouncementSync.setOn(this, value) }
        add(body, card(announcement), 16)

        val permissions = panel()
        add(permissions, text("Android permissions", 19, true, onSurface), 0)
        add(permissions, text(if (ReminderNotifications.permitted(this)) "Notifications: allowed"
            else "Notifications: permission needed", 14, false, onSurfaceVariant), 10)
        if (!ReminderNotifications.permitted(this)) {
            add(permissions, button("Allow notifications", false) { askNotifications() }, 10)
        }
        add(permissions, text(if (ReminderScheduler.exactAlarmsAllowed(this)) "Precise alarm scheduling: available"
            else "Precise alarm scheduling: not available; reminder may be delayed", 14, false, onSurfaceVariant), 10)
        if (!ReminderScheduler.exactAlarmsAllowed(this)) {
            add(permissions, button("Allow exact alarms", false) { askExactAlarms() }, 10)
        }
        add(body, card(permissions), 16)

        val hijriTiming = panel()
        add(hijriTiming, text(getString(R.string.hijri_clock_section), 19, true, onSurface), 0)
        val cityIndex = HijriClock.selectedCity(this)
        val cityLabels = HijriClock.cityLabels(this)
        val cityName = if (cityIndex < cityLabels.size) cityLabels[cityIndex] else cityLabels[0]
        add(hijriTiming, slimAction("🌇", getString(R.string.hijri_select_city) + " · " + cityName) {
            chooseHijriCity()
        }, 10)
        add(hijriTiming, text(getString(R.string.hijri_clock_explanation), 12, false, onSurfaceVariant), 6)
        add(hijriTiming, slimAction("🕌", getString(R.string.prayer_method_title) + " · " +
            getString(if (PrayerStore.method(this) == PrayerTimes.Method.JAFARI) R.string.prayer_method_jafari
            else R.string.prayer_method_karachi)) { choosePrayerMethod() }, 12)
        add(hijriTiming, text(getString(R.string.prayer_method_help), 12, false, onSurfaceVariant), 6)
        add(body, card(hijriTiming), 16)

        val scenes = panel()
        add(scenes, text("🌤 Sky and local weather", 19, true, onSurface), 0)
        add(scenes, text("The moon and mosque respond to your device time even without location access.",
            13, false, onSurfaceVariant), 8)
        toggle(scenes, "Weather-based sky effects",
            "Optional: ask for approximate location only while you use the app. Weather data by Open-Meteo (CC BY 4.0).",
            WeatherManager.enabled(this)) { enabled ->
            if (!enabled) {
                WeatherManager.disable(this)
                render()
            } else if (WeatherManager.permissionGranted(this)) {
                p.edit().putBoolean(WeatherManager.ENABLED, true).apply()
                WeatherManager.refresh(this, true, Runnable { render() })
                render()
            } else {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), WEATHER_LOCATION_REQUEST)
            }
        }
        if (WeatherManager.enabled(this)) {
            val scene = WeatherManager.current(this)
            add(scenes, text(scene?.description(this)
                ?: "Weather not available yet. A time-aware scene is still displayed.", 13, false, onSurfaceVariant), 7)
            add(scenes, slimAction("↻", "Refresh weather now") {
                WeatherManager.refresh(this, true, Runnable { render() })
            }, 8)
        }
        add(scenes, text("Approximate coordinates are rounded before weather requests and never saved. Disabling this setting clears cached conditions. No background-location permission is requested.",
            12, false, onSurfaceVariant), 8)
        add(body, card(scenes), 16)

        val updates = panel()
        add(updates, text("App updates", 19, true, onSurface), 0)
        add(updates, text(UiText.tr(this, "Installed version: ") + UpdateManager.installed(this), 14, false, onSurfaceVariant), 10)
        toggle(updates, "Check automatically", "Checks GitHub when you open the app, normally at most twice per day.",
            p.getBoolean(UpdateManager.AUTO_CHECK, true)) { value ->
            p.edit().putBoolean(UpdateManager.AUTO_CHECK, value).apply()
            UpdateCheckScheduler.ensure(this)
        }
        toggle(updates, "Background update notifications",
            "Android checks GitHub roughly daily, even when this app is closed; you decide when to download and install.",
            p.getBoolean(UpdateManager.BACKGROUND_CHECK, true)) { value ->
            p.edit().putBoolean(UpdateManager.BACKGROUND_CHECK, value).apply()
            UpdateCheckScheduler.ensure(this)
        }
        if (UpdateManager.isReleaseBuild(this)) {
            add(updates, text("Stable release signing active. Incompatible debug-signed previews are excluded from update checks.",
                12, false, onSurfaceVariant), 10)
        } else {
            toggle(updates, "Include preview versions", "Preview APKs use debug signing and may require reinstalling.",
                p.getBoolean(UpdateManager.INCLUDE_PREVIEWS, true)) { value ->
                p.edit().putBoolean(UpdateManager.INCLUDE_PREVIEWS, value).apply()
                UpdateManager.check(this, true) { _, message ->
                    toast(message)
                    render()
                }
                render()
            }
        }
        val last = UpdateManager.lastChecked(this)
        val label = if (last <= 0) "Never checked"
        else Instant.ofEpochMilli(last).atZone(LunarEngine.PAKISTAN)
            .format(DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", AppLanguage.locale(this))) + " PKT"
        add(updates, text(UiText.tr(this, "Last checked: ") + label, 12, false, onSurfaceVariant), 10)
        val available = UpdateManager.available(this)
        if (available == null) {
            add(updates, text(UiText.tr(this, "No newer release detected") +
                (if (last <= 0) UiText.tr(this, " (not yet checked)") else ""), 14, true, onSurface), 8)
        } else {
            add(updates, text(UiText.tr(this, "Update available: ") + VersionPolicy.display(available.tag) +
                (if (available.preview) UiText.tr(this, " (preview)") else ""), 16, true, primary), 12)
            add(updates, slimAction("⬇", "Download and install update") {
                AppUpdateInstaller.prompt(this, available)
            }, 8)
        }
        add(updates, slimAction("↻", if (UpdateManager.isChecking()) "Checking GitHub..." else "Check for updates now") {
            if (UpdateManager.isChecking()) {
                toast("Update check already running")
            } else {
                UpdateManager.check(this, true) { _, message ->
                    toast(message)
                    render()
                }
                toast("Checking GitHub releases")
            }
        }, 12)
        add(updates, text("Verified APKs download inside the app. Android confirms installation and may first require permission to install from this app. Permanent signing allows later stable releases to update in place.",
            12, false, onSurfaceVariant), 12)
        add(body, card(updates), 16)
    }

    /** Tappable icon row; keeps the screen scannable without large button blocks. */
    private fun slimAction(emblem: String, title: String, operation: () -> Unit): LinearLayout {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(7), dp(8), dp(7), dp(8))
        val symbol = text(emblem, 22, false, primary)
        symbol.gravity = Gravity.CENTER
        row.addView(symbol, LinearLayout.LayoutParams(dp(44), dp(40)))
        row.addView(text(title, 15, true, onSurface), LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(text("›", 22, false, onSurfaceVariant))
        row.isClickable = true
        row.isFocusable = true
        row.setOnClickListener { operation() }
        return row
    }

    private fun toggle(parent: LinearLayout, title: String, help: String, checked: Boolean, changed: (Boolean) -> Unit) {
        val control = MaterialSwitch(this)
        control.text = UiText.tr(this, title)
        control.setTextColor(onSurface)
        control.textSize = 16f
        control.isChecked = checked
        add(parent, control, 14)
        add(parent, text(help, 12, false, onSurfaceVariant), 0)
        control.setOnCheckedChangeListener { _, value -> changed(value) }
    }

    private fun chooseReminderTime() {
        val current = ReminderStore.reminderTime(this)
        TimePickerDialog(this, { _, hour, minute ->
            if (ReminderStore.setReminderTime(this, hour, minute)) {
                ReminderScheduler.sync(this) // Cancel old time, reschedule first Thursday.
                DateSyncScheduler.ensure(this) // Update the pre-reminder date refresh window.
                render()
            } else {
                toast("Unable to save reminder time")
            }
        }, current.hour, current.minute, DateFormat.is24HourFormat(this)).show()
    }

    private fun choosePrayerMethod() {
        val methods = arrayOf(PrayerTimes.Method.KARACHI_HANAFI, PrayerTimes.Method.JAFARI)
        val names = arrayOf(getString(R.string.prayer_method_karachi), getString(R.string.prayer_method_jafari))
        val current = if (PrayerStore.method(this) == PrayerTimes.Method.JAFARI) 1 else 0
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.prayer_method_title))
            .setSingleChoiceItems(names, current) { dialog, index ->
                PrayerStore.setMethod(this, methods[index])
                ExtraReminderScheduler.sync(this) // Prayer alerts follow the method.
                dialog.dismiss()
                render()
            }
            .setNegativeButton(UiText.tr(this, "Close"), null)
            .show()
    }

    private fun chooseHijriCity() {
        val names = HijriClock.cityLabels(this)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.hijri_select_city))
            .setSingleChoiceItems(names, HijriClock.selectedCity(this)) { dialog, index ->
                HijriClock.selectCity(this, index)
                ExtraReminderScheduler.sync(this) // Prayer alerts follow the city.
                DateSyncScheduler.ensure(this) // Recalculate month-end Maghrib check times.
                dialog.dismiss()
                render()
            }
            .setNegativeButton(UiText.tr(this, "Close"), null)
            .show()
    }

    private fun chooseLanguage() {
        val names = arrayOf(UiText.tr(this, "Follow device language"), "English", "اردو")
        val selected = when (AppLanguage.choice(this)) {
            "ur" -> 2
            "en" -> 1
            else -> 0
        }
        AlertDialog.Builder(this)
            .setTitle(UiText.tr(this, "App language"))
            .setSingleChoiceItems(names, selected) { dialog, index ->
                val code = if (index == 2) "ur" else if (index == 1) "en" else ""
                dialog.dismiss()
                if (code != AppLanguage.choice(this)) {
                    AppLanguage.select(this, code)
                    val restart = Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(restart)
                    finish()
                }
            }
            .setNegativeButton(UiText.tr(this, "Close"), null)
            .show()
    }

    private fun askNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && !ReminderNotifications.permitted(this)) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_REQUEST)
        } else {
            try {
                startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
            } catch (error: RuntimeException) {
                toast("Open notification settings in Android")
            }
        }
    }

    override fun onRequestPermissionsResult(code: Int, permissions: Array<String>, grants: IntArray) {
        super.onRequestPermissionsResult(code, permissions, grants)
        if (code == NOTIFICATION_REQUEST) render()
        if (code == WEATHER_LOCATION_REQUEST) {
            if (WeatherManager.permissionGranted(this)) {
                ReminderStore.prefs(this).edit().putBoolean(WeatherManager.ENABLED, true).apply()
                WeatherManager.refresh(this, true, Runnable { render() })
            } else {
                WeatherManager.disable(this)
                toast("Approximate location denied; time-aware sky remains available")
            }
            render()
        }
    }

    private fun askExactAlarms() {
        if (Build.VERSION.SDK_INT < 31) return
        try {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .setData(Uri.parse("package:$packageName")))
        } catch (unavailable: RuntimeException) {
            toast("Open Android Settings > Alarms & reminders")
        }
    }

    private fun askFullScreen() {
        if (Build.VERSION.SDK_INT < 34) return
        try {
            startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                .setData(Uri.parse("package:$packageName")))
        } catch (unavailable: RuntimeException) {
            toast("Full-screen alarm access can be managed in Android settings")
        }
    }

    private fun col(attr: Int, fallback: Long): Int = MaterialColors.getColor(this, attr, fallback.toInt())

    private fun column(): LinearLayout {
        val l = LinearLayout(this)
        l.orientation = LinearLayout.VERTICAL
        return l
    }

    private fun panel(): LinearLayout {
        val box = column()
        box.setPadding(dp(18), dp(18), dp(18), dp(18))
        return box
    }

    private fun card(content: LinearLayout): MaterialCardView {
        val card = MaterialCardView(this)
        card.radius = dp(24).toFloat()
        card.cardElevation = 0f
        card.setCardBackgroundColor(surfaceVariant)
        card.addView(content)
        return card
    }

    private fun text(value: String, size: Int, bold: Boolean, color: Int): TextView {
        val v = TextView(this)
        v.text = UiText.tr(this, value)
        v.textSize = size.toFloat()
        v.setTextColor(color)
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        return v
    }

    private fun button(value: String, primaryButton: Boolean, run: () -> Unit): MaterialButton {
        val button = MaterialButton(this)
        button.text = UiText.tr(this, value)
        button.isAllCaps = false
        button.minHeight = dp(48)
        button.cornerRadius = dp(18)
        button.backgroundTintList = ColorStateList.valueOf(if (primaryButton) primary else primaryContainer)
        button.setTextColor(if (primaryButton) onPrimary else onSurface)
        button.setOnClickListener { run() }
        return button
    }

    private fun add(parent: LinearLayout, view: View, margin: Int) {
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.topMargin = dp(margin)
        parent.addView(view, params)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + .5f).toInt()

    private fun toast(text: String) {
        Toast.makeText(this, UiText.tr(this, text), Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val NOTIFICATION_REQUEST = 85
        private const val WEATHER_LOCATION_REQUEST = 86
    }
}
