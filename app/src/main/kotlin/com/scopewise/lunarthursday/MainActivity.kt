package com.scopewise.lunarthursday

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Compact dashboard with three-dot navigation and persistent bottom tabs. */
class MainActivity : LocalizedActivity() {
    private var preferences: SharedPreferences? = null
    private var activeTab = TAB_HOME
    private var surface = 0
    private var surfaceVariant = 0
    private var onSurface = 0
    private var secondary = 0
    private var primary = 0
    private var primaryContainer = 0
    private var onPrimaryContainer = 0
    private var monthTaps = 0
    private var countdownTarget: Instant? = null
    private var displayedPakistanDate: LocalDate? = null
    private var displayedHijriReferenceDate: LocalDate? = null
    private var daysValue: TextView? = null
    private var hoursValue: TextView? = null
    private var minutesValue: TextView? = null
    private val ticker = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            if (isFinishing || isDestroyed || activeTab != TAB_HOME) return
            val actual = HijriClock.current(this@MainActivity, Instant.now())
            if (displayedPakistanDate != null &&
                (displayedPakistanDate != actual.todayGregorian ||
                    displayedHijriReferenceDate != actual.hijriReferenceDate)) {
                render() // Midnight or Maghrib boundary: refresh the visible dates.
                return
            }
            updateCounters()
            ticker.postDelayed(this, 60_000L)
        }
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        DynamicColors.applyToActivityIfAvailable(this)
        val prefs = ReminderStore.prefs(this)
        preferences = prefs
        if (!prefs.contains(ReminderStore.ENABLED)) {
            prefs.edit().putBoolean(ReminderStore.ENABLED, true).apply()
        }
        activeTab = state?.getInt("dashboard_tab", TAB_HOME) ?: intent.getIntExtra("open_tab", TAB_HOME)
        ReminderNotifications.ensureChannel(this)
        ReminderScheduler.sync(this)
        DateSyncScheduler.ensure(this)
        UpdateCheckScheduler.ensure(this)
        if (Build.VERSION.SDK_INT >= 33) requestNotificationPermission()
        render()
    }

    override fun onSaveInstanceState(out: Bundle) {
        out.putInt("dashboard_tab", activeTab)
        super.onSaveInstanceState(out)
    }

    override fun onResume() {
        super.onResume()
        if (preferences == null) return
        ReminderScheduler.sync(this)
        DateSyncScheduler.ensure(this)
        UpdateCheckScheduler.ensure(this)
        render()
        AppUpdateInstaller.resumeIfPermitted(this)
        WeatherManager.refresh(this, false, Runnable {
            if (!isFinishing && !isDestroyed) render()
        })
        UpdateManager.check(this, false) { _, _ ->
            if (!isFinishing && !isDestroyed) render()
        }
    }

    override fun onPause() {
        ticker.removeCallbacks(tick)
        super.onPause()
    }

    private fun palette() {
        surface = color(com.google.android.material.R.attr.colorSurface, 0xFFFAFCF8)
        surfaceVariant = color(com.google.android.material.R.attr.colorSurfaceVariant, 0xFFE9F1EB)
        onSurface = color(com.google.android.material.R.attr.colorOnSurface, 0xFF1C2A25)
        secondary = color(com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF4D6258)
        primary = color(androidx.appcompat.R.attr.colorPrimary, 0xFF087F55)
        primaryContainer = color(com.google.android.material.R.attr.colorPrimaryContainer, 0xFFD9F4DF)
        onPrimaryContainer = color(com.google.android.material.R.attr.colorOnPrimaryContainer, 0xFF0D3826)
    }

    private fun render() {
        palette()
        ticker.removeCallbacks(tick)
        daysValue = null
        hoursValue = null
        minutesValue = null
        countdownTarget = null
        val root = column()
        root.setBackgroundColor(surface)
        if (Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener { v, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                v.setPadding(0, bars.top, 0, bars.bottom)
                insets
            }
        } else {
            root.fitsSystemWindows = true
        }
        setContentView(root)
        root.addView(toolbar(), LinearLayout.LayoutParams(-1, dp(64)))

        val scroller = ScrollView(this)
        scroller.isFillViewport = true
        scroller.clipToPadding = false
        val body = column()
        body.setPadding(dp(18), dp(12), dp(18), dp(24))
        scroller.addView(body)
        root.addView(scroller, LinearLayout.LayoutParams(-1, 0, 1f))
        when (activeTab) {
            TAB_MONTHS -> renderMonths(body)
            TAB_UPDATES -> renderUpdates(body)
            else -> renderHome(body)
        }
        root.addView(bottomNavigation(), LinearLayout.LayoutParams(-1, dp(74)))
    }

    private fun toolbar(): LinearLayout {
        val bar = row()
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setPadding(dp(18), 0, dp(12), 0)
        val appIcon = ImageView(this)
        appIcon.setImageResource(R.mipmap.ic_launcher)
        appIcon.contentDescription = UiText.tr(this, "Hilal Thursday")
        bar.addView(appIcon, LinearLayout.LayoutParams(dp(43), dp(43)))
        val heading = column()
        heading.setPadding(dp(12), 0, 0, 0)
        heading.addView(text("Hilal Thursday", 18, true, onSurface))
        heading.addView(text("ماہِ نو", 12, false, secondary))
        bar.addView(heading, LinearLayout.LayoutParams(0, -2, 1f))
        val moonEgg = View.OnClickListener {
            if (++monthTaps >= 7) {
                monthTaps = 0
                AlertDialog.Builder(this)
                    .setTitle(UiText.tr(this, "🌙 Moonlight unlocked"))
                    .setMessage(UiText.tr(this, "Seven taps, seven skies. Your next Thursday is written in the stars — but the date still comes from the calendar, not the stars."))
                    .setPositiveButton(UiText.tr(this, "Lovely"), null).show()
            }
        }
        appIcon.setOnClickListener(moonEgg)
        heading.setOnClickListener(moonEgg)
        val menu = icon(R.drawable.ic_ui_more, UiText.tr(this, "More options"), primary)
        menu.setOnClickListener { openMenu(it) }
        bar.addView(menu, LinearLayout.LayoutParams(dp(46), dp(46)))
        return bar
    }

    private fun openMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, UiText.tr(this, "Settings"))
        popup.menu.add(0, 2, 1, UiText.tr(this, "App language"))
        popup.menu.add(0, 3, 2, UiText.tr(this, "Test ringing alarm"))
        popup.menu.add(0, 4, 3, UiText.tr(this, "Check for updates now"))
        popup.menu.add(0, 5, 4, UiText.tr(this, "About / Developer"))
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1, 2 -> {
                    openSettings(item.itemId == 2)
                    true
                }
                3 -> {
                    if (!ReminderNotifications.permitted(this)) requestNotificationPermission()
                    else if (!AlarmRingingService.startTest(this)) toast("Alarm service unavailable on this device")
                    true
                }
                4 -> {
                    checkUpdates()
                    true
                }
                5 -> {
                    startActivity(Intent(this, AboutActivity::class.java))
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun bottomNavigation(): LinearLayout {
        val nav = row()
        nav.gravity = Gravity.CENTER_VERTICAL
        nav.setPadding(dp(7), dp(4), dp(7), dp(4))
        nav.setBackgroundColor(surfaceVariant)
        nav.addView(tab(R.drawable.ic_ui_home, "Home", activeTab == TAB_HOME) { switchTab(TAB_HOME) },
            LinearLayout.LayoutParams(0, -1, 1f))
        nav.addView(tab(R.drawable.ic_ui_calendar, "Calendar", activeTab == TAB_MONTHS) { switchTab(TAB_MONTHS) },
            LinearLayout.LayoutParams(0, -1, 1f))
        nav.addView(tab(R.drawable.ic_ui_refresh, "Updates", activeTab == TAB_UPDATES) { switchTab(TAB_UPDATES) },
            LinearLayout.LayoutParams(0, -1, 1f))
        nav.addView(tab(R.drawable.ic_ui_info, "About", false) {
            startActivity(Intent(this, AboutActivity::class.java))
        }, LinearLayout.LayoutParams(0, -1, 1f))
        return nav
    }

    private fun tab(art: Int, title: String, selected: Boolean, click: () -> Unit): LinearLayout {
        val item = column()
        item.gravity = Gravity.CENTER
        item.contentDescription = UiText.tr(this, title)
        item.isClickable = true
        item.isFocusable = true
        val img = icon(art, title, if (selected) primary else secondary)
        if (selected) {
            val pill = row()
            pill.gravity = Gravity.CENTER
            pill.background = round(primaryContainer, 20)
            pill.addView(img, LinearLayout.LayoutParams(dp(22), dp(22)))
            item.addView(pill, LinearLayout.LayoutParams(dp(55), dp(34)))
        } else {
            item.addView(img, LinearLayout.LayoutParams(dp(24), dp(34)))
        }
        val name = text(title, 11, selected, if (selected) primary else secondary)
        name.gravity = Gravity.CENTER
        item.addView(name)
        item.setOnClickListener { click() }
        return item
    }

    private fun switchTab(next: Int) {
        if (activeTab == next) return
        activeTab = next
        render()
    }

    private fun renderHome(body: LinearLayout) {
        val prefs = ReminderStore.prefs(this)
        val upcoming = months(6)
        add(body, text("Your next lunar Thursday, at a glance", 25, true, onSurface), 0)
        val civilClock = HijriClock.current(this, Instant.now())
        displayedPakistanDate = civilClock.todayGregorian
        displayedHijriReferenceDate = civilClock.hijriReferenceDate
        add(body, text(UiText.tr(this, "Today in Pakistan: ") +
            UiText.date(this, civilClock.todayGregorian, true), 13, false, secondary), 6)
        val currentHijri = LunarEngine.expectedOnDate(civilClock.hijriReferenceDate, ReminderStore.confirmedStarts(this))
        if (currentHijri != null) {
            add(body, text(UiText.currentHijriDate(this, currentHijri), 14, true, onSurface), 3)
            add(body, text(sourceLabel(currentHijri.month.basis) + " · " +
                UiText.hijriClockStatus(this, civilClock), 12, false, secondary), 3)
        }

        if (upcoming.isNotEmpty()) {
            val next = upcoming[0]
            val card = card(primaryContainer, 27)
            val hero = column()
            card.addView(hero)
            val sky = MoonSkyView(this)
            sky.setWeather(WeatherManager.current(this))
            hero.addView(sky, LinearLayout.LayoutParams(-1, dp(151)))
            val details = column()
            details.setPadding(dp(17), dp(12), dp(17), dp(19))
            add(details, text("NEXT REMINDER", 12, true, onPrimaryContainer), 0)
            add(details, text(UiText.month(this, next), 22, true, onPrimaryContainer), 6)
            add(details, text(UiText.date(this, next.thursday(), true), 16, true, onPrimaryContainer), 7)
            add(details, text(UiText.expectedHijriDate(this, next, next.thursday()), 14, true, onPrimaryContainer), 4)
            add(details, text(UiText.reminderTimeText(this) + " · Pakistan Standard Time", 13, false, onPrimaryContainer), 3)
            val countdown = row()
            countdown.gravity = Gravity.CENTER
            daysValue = addCounter(countdown, "Days")
            hoursValue = addCounter(countdown, "Hours")
            minutesValue = addCounter(countdown, "Minutes")
            add(details, countdown, 15)
            add(details, text(sourceLine(next), 12, false, onPrimaryContainer), 12)
            val live = WeatherManager.current(this)
            if (live != null) {
                add(details, text("🌤 " + live.description(this) + "  ·  Weather: Open-Meteo", 12, false, onPrimaryContainer), 7)
            } else if (WeatherManager.enabled(this)) {
                add(details, text("🌤 " + (if (WeatherManager.permissionGranted(this))
                    "Weather unavailable · time-aware sky shown" else "Allow approximate location in Settings"),
                    12, false, onPrimaryContainer), 7)
            }
            add(details, text("Tap for the month-start date and source", 11, false, onPrimaryContainer), 6)
            hero.addView(details)
            card.setOnClickListener { showMonthDetails(next) }
            add(body, card, 17)
            renderPrayerTimes(body, currentHijri)
            add(body, card(compactSurfaceRow("❖", str(R.string.verse_title), str(R.string.verse_open_hint)) {
                startActivity(Intent(this, MorningCardActivity::class.java))
            }, surfaceVariant, 19), 12)
            add(body, card(compactSurfaceRow("◎", str(R.string.tasbih_title), str(R.string.tasbih_open_hint)) {
                startActivity(Intent(this, TasbihActivity::class.java))
            }, surfaceVariant, 19), 9)
            add(body, card(compactSurfaceRow("🧭", str(R.string.compass_title), str(R.string.compass_open_hint)) {
                startActivity(Intent(this, QiblaCompassActivity::class.java))
            }, surfaceVariant, 19), 9)
            countdownTarget = next.remindAt(ReminderStore.reminderTime(this))
            updateCounters()
            ticker.postDelayed(tick, 60_000L)
        }

        val status = compactSurfaceRow("🔔",
            if (prefs.getBoolean(ReminderStore.ENABLED, false)) "Reminders enabled" else "Reminders paused",
            UiText.tr(this, if (prefs.getBoolean(ReminderStore.ALARM_MODE, false)) "Ringing alarm"
            else "Standard notification")) { openSettings(false) }
        add(body, card(status, surfaceVariant, 21), 14)
        if (!ReminderNotifications.permitted(this) || !ReminderScheduler.exactAlarmsAllowed(this)) {
            add(body, compactSurfaceRow("⚠", "Review alert permissions",
                "An Android permission may delay reminders") { openSettings(false) }, 9)
        }
        val release = UpdateManager.available(this)
        if (release != null) {
            add(body, card(compactSurfaceRow("✨", "Update available", VersionPolicy.display(release.tag)) {
                switchTab(TAB_UPDATES)
            }, primaryContainer, 20), 12)
        }

        val subhead = row()
        subhead.gravity = Gravity.CENTER_VERTICAL
        subhead.addView(weightedText("Upcoming lunar months", 18, true, onSurface))
        val see = text("See all  ›", 13, true, primary)
        see.setPadding(dp(8), dp(12), 0, dp(12))
        see.setOnClickListener { switchTab(TAB_MONTHS) }
        subhead.addView(see)
        add(body, subhead, 17)
        for (i in 1 until Math.min(3, upcoming.size)) {
            add(body, card(monthRow(upcoming[i]), surfaceVariant, 18), 8)
        }
        if (upcoming.size <= 1) {
            add(body, text("Dates are calculated automatically and may be estimated.", 12, false, secondary), 9)
        }
        add(body, text("Forecasts and third-party reports may differ from the official Ruet-e-Hilal decision.",
            12, false, secondary), 18)
    }

    private fun addCounter(parent: LinearLayout, unit: String): TextView {
        val tile = column()
        tile.gravity = Gravity.CENTER
        tile.setPadding(dp(4), dp(8), dp(4), dp(8))
        tile.background = round(surface, 16)
        val value = text("0", 24, true, onSurface) // Readable against Material surface tile
        value.gravity = Gravity.CENTER
        tile.addView(value)
        val title = text(unit, 12, false, secondary)
        title.gravity = Gravity.CENTER
        tile.addView(title)
        val lp = LinearLayout.LayoutParams(0, dp(76), 1f)
        lp.marginEnd = dp(7)
        parent.addView(tile, lp)
        return value
    }

    private fun updateCounters() {
        val days = daysValue ?: return
        val hours = hoursValue ?: return
        val minutes = minutesValue ?: return
        val target = countdownTarget ?: return
        val min = Math.max(0L, (Duration.between(Instant.now(), target).toMillis() + 59_999L) / 60_000L)
        days.text = String.format(AppLanguage.locale(this), "%d", min / 1_440L)
        hours.text = String.format(AppLanguage.locale(this), "%d", (min % 1_440L) / 60L)
        minutes.text = String.format(AppLanguage.locale(this), "%d", min % 60L)
    }

    private fun renderMonths(body: LinearLayout) {
        add(body, text("Upcoming lunar months", 26, true, onSurface), 2)
        add(body, text(UiText.tr(this, "First Thursday · ") + UiText.reminderTimeText(this) + " PKT", 13, false, secondary), 7)
        val next = months(7)
        for (i in next.indices) {
            add(body, card(monthRow(next[i]), if (i == 0) primaryContainer else surfaceVariant, 19), if (i == 0) 17 else 11)
        }
        renderObservances(body)
        add(body, card(compactSurfaceRow("ⓘ", "Calendar source information",
            "Forecasts and calculations are not official confirmations") { calendarDisclaimer() }, surfaceVariant, 19), 18)
    }

    /** Today's calculated prayer times for the city chosen in Settings. */
    private fun renderPrayerTimes(body: LinearLayout, currentHijri: LunarEngine.HijriDate?) {
        val panel = column()
        panel.setPadding(dp(16), dp(14), dp(16), dp(14))
        panel.addView(text(str(R.string.prayer_title), 17, true, onSurface))
        val day = PrayerStore.today(this)
        if (day == null) {
            add(panel, text(str(R.string.prayer_choose_city), 13, false, secondary), 6)
            panel.setOnClickListener { openSettings(false) }
            add(body, card(panel, surfaceVariant, 19), 14)
            return
        }
        val clock = DateTimeFormatter.ofPattern("h:mm a", AppLanguage.locale(this))
        val fajr = day.fajr
        val maghrib = day.maghrib
        if (currentHijri != null && currentHijri.month.hijriMonth == 9 && fajr != null && maghrib != null) {
            add(panel, text(str(R.string.prayer_roza, String.format(AppLanguage.locale(this), "%d", currentHijri.day)) +
                " · " + str(R.string.prayer_sehri, clock.format(fajr)) +
                " · " + str(R.string.prayer_iftar, clock.format(maghrib)), 14, true, primary), 6)
        }
        val labels = intArrayOf(R.string.prayer_fajr, R.string.prayer_sunrise, R.string.prayer_dhuhr,
            R.string.prayer_asr, R.string.prayer_maghrib, R.string.prayer_isha)
        val times = arrayOf(day.fajr, day.sunrise, day.dhuhr, day.asr, day.maghrib, day.isha)
        for (i in labels.indices) {
            val line = row()
            line.addView(weightedText(str(labels[i]), 14, false, onSurface))
            val time = times[i]
            line.addView(text(if (time == null) "—" else clock.format(time), 14, true, onSurface))
            add(panel, line, if (i == 0) 8 else 4)
        }
        val method = str(if (PrayerStore.method(this) == PrayerTimes.Method.JAFARI) R.string.prayer_method_jafari
        else R.string.prayer_method_karachi)
        val place = HijriClock.cityCoordinates(this)
        if (place != null) {
            val qibla = text(str(R.string.qibla_line, String.format(AppLanguage.locale(this), "%d",
                Math.round(Qibla.bearing(place[0], place[1])))) + " · " + str(R.string.compass_open), 13, true, primary)
            qibla.setOnClickListener { startActivity(Intent(this, QiblaCompassActivity::class.java)) }
            add(panel, qibla, 8)
        }
        add(panel, text(HijriClock.placeLabel(this) + " · " + method +
            "\n" + str(R.string.prayer_note), 11, false, secondary), 8)
        add(body, card(panel, surfaceVariant, 19), 14)
    }

    private fun str(id: Int, vararg values: Any?): String = AppLanguage.wrap(this).getString(id, *values)

    /** Upcoming observances from the sets chosen in Settings. */
    private fun renderObservances(body: LinearLayout) {
        add(body, text(str(R.string.events_heading), 20, true, onSurface), 24)
        if (EventsStore.enabledSets(this).isEmpty()) {
            add(body, text(str(R.string.events_none), 13, false, secondary), 8)
            return
        }
        for (item in EventsStore.upcoming(this, 8)) {
            add(body, card(compactSurfaceRow("✦", EventsStore.name(this, item.event), observanceLine(item)) {
                showObservance(item)
            }, surfaceVariant, 19), 9)
        }
    }

    private fun observanceHijri(item: IslamicEvents.Occurrence): String =
        String.format(AppLanguage.locale(this), "%d", item.month.hijriDay(item.date)) + " " + UiText.month(this, item.month)

    private fun observanceLine(item: IslamicEvents.Occurrence): String {
        val line = StringBuilder(UiText.date(this, item.date, false)).append(" · ").append(observanceHijri(item))
        if (item.event.days > 1) {
            line.append(" · ").append(str(R.string.events_days,
                String.format(AppLanguage.locale(this), "%d", item.event.days)))
        }
        if (item.event.eve) line.append(" · ").append(str(R.string.events_night_before))
        if (!item.confirmed) line.append("\n").append(str(R.string.events_may_shift))
        return line.toString()
    }

    private fun showObservance(item: IslamicEvents.Occurrence) {
        val message = StringBuilder(EventsStore.name(this, item.event))
            .append("\n\n").append(str(R.string.events_details_date, UiText.date(this, item.date, true)))
            .append("\n").append(observanceHijri(item))
        if (item.event.eve) {
            message.append("\n").append(str(R.string.events_details_eve, UiText.date(this, item.date.minusDays(1), true)))
        }
        if (item.event.noteEn != null && !AppLanguage.isUrdu(this)) {
            message.append("\n\n").append(item.event.noteEn)
        }
        if (!item.confirmed) message.append("\n\n").append(str(R.string.events_details_unconfirmed))
        message.append("\n\n").append(str(R.string.events_source_note))
        val reminds = EventsStore.reminds(this, item.event)
        AlertDialog.Builder(this).setTitle(str(R.string.events_details_title))
            .setMessage(message.toString()).setPositiveButton(UiText.tr(this, "Close"), null)
            .setNeutralButton(str(if (reminds) R.string.events_reminder_turn_off else R.string.events_reminder_turn_on)) { _, _ ->
                EventsStore.setReminds(this, item.event, !reminds)
            }
            .show()
    }

    private fun monthRow(item: LunarEngine.Month): LinearLayout {
        val row = row()
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(13), dp(12), dp(12), dp(12))
        val glyph = text("☾", 29, true, primary)
        glyph.gravity = Gravity.CENTER
        glyph.background = round(primaryContainer, 18)
        row.addView(glyph, LinearLayout.LayoutParams(dp(48), dp(48)))
        val details = column()
        details.setPadding(dp(11), 0, 0, 0)
        details.addView(text(UiText.month(this, item), 15, true, onSurface))
        details.addView(text(UiText.date(this, item.thursday(), false), 13, false, secondary))
        details.addView(text(UiText.expectedHijriDate(this, item, item.thursday()), 12, true, onSurface))
        details.addView(text(sourceLine(item), 11, false, secondary))
        row.addView(details, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(text(chevron(), 27, false, secondary))
        row.setOnClickListener { showMonthDetails(item) }
        row.contentDescription = UiText.month(this, item) + ", " +
            UiText.date(this, item.thursday(), true) + ", " +
            UiText.expectedHijriDate(this, item, item.thursday())
        return row
    }

    private fun renderUpdates(body: LinearLayout) {
        add(body, text("Updates & calendar", 26, true, onSurface), 2)
        add(body, text("App releases and automatic Pakistan lunar-date checks", 13, false, secondary), 6)
        val releaseCard = column()
        releaseCard.setPadding(dp(16), dp(14), dp(16), dp(14))
        releaseCard.addView(text("APP VERSION", 12, true, secondary))
        add(releaseCard, text(UiText.tr(this, "Version ") + UpdateManager.installed(this), 19, true, onSurface), 6)
        val result = UpdateManager.available(this)
        if (result == null) {
            add(releaseCard, text("No newer release detected", 13, false, secondary), 9)
        } else {
            add(releaseCard, compactSurfaceRow("✨", "Update available", VersionPolicy.display(result.tag)) {
                AppUpdateInstaller.prompt(this, result)
            }, 9)
        }
        add(releaseCard, actionRow(R.drawable.ic_ui_refresh, "Check for updates now") { checkUpdates() }, 12)
        add(body, card(releaseCard, surfaceVariant, 23), 18)

        val feed = column()
        feed.setPadding(dp(16), dp(14), dp(16), dp(14))
        add(feed, text("AUTOMATIC HIJRI DATE CHECKS", 12, true, secondary), 0)
        val checked = ReminderStore.prefs(this).getLong(ReminderStore.LAST_REFRESH_SUCCESS, 0L)
        val checkedText = if (checked <= 0) UiText.tr(this, "Not checked online yet")
        else UiText.dateTime(this, Instant.ofEpochMilli(checked)) + " PKT"
        add(feed, text(UiText.tr(this, "Last successful feed check: ") + checkedText, 14, false, onSurface), 9)
        add(feed, actionRow(R.drawable.ic_ui_refresh, "Request a date refresh") {
            if (DateSyncScheduler.requestNow(this)) toast("Refresh requested; Android will run it when network is available")
            else toast("Enable reminders to allow date checks")
        }, 10)
        add(feed, actionRow(R.drawable.ic_ui_calendar, "View Pakistan PMD calendar") {
            open("https://weather.gov.pk/cdpc/lunar-calendar")
        }, 6)
        add(body, card(feed, surfaceVariant, 23), 14)
        add(body, text("Dates are updated automatically. Third-party reports, forecasts and calculated estimates can differ from Pakistan's official sighting decision.",
            13, false, secondary), 15)
    }

    private fun actionRow(iconId: Int, title: String, action: () -> Unit): View {
        val row = row()
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(2), dp(10), 0, dp(10))
        row.addView(icon(iconId, title, primary), LinearLayout.LayoutParams(dp(30), dp(30)))
        row.addView(weightedText(title, 14, true, onSurface))
        row.addView(text(chevron(), 24, false, secondary))
        row.setOnClickListener { action() }
        return row
    }

    private fun compactSurfaceRow(emblem: String, heading: String, sub: String, action: () -> Unit): LinearLayout {
        val row = row()
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(13), dp(12), dp(13), dp(12))
        val mark = text(emblem, 21, false, primary)
        mark.gravity = Gravity.CENTER
        row.addView(mark, LinearLayout.LayoutParams(dp(40), dp(44)))
        val details = column()
        details.addView(text(heading, 15, true, onSurface))
        details.addView(text(sub, 12, false, secondary))
        row.addView(details, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(text(chevron(), 24, false, secondary))
        row.setOnClickListener { action() }
        return row
    }

    private fun openSettings(language: Boolean) {
        val intent = Intent(this, SettingsActivity::class.java)
        if (language) intent.putExtra("open_language_dialog", true)
        startActivity(intent)
    }

    private fun checkUpdates() {
        if (UpdateManager.isChecking()) {
            toast("Update check already running")
            return
        }
        UpdateManager.check(this, true) { _, message ->
            toast(message)
            if (!isFinishing && !isDestroyed) render()
        }
        toast("Checking GitHub releases")
    }

    private fun showMonthDetails(item: LunarEngine.Month) {
        val message = UiText.tr(this, "Month: ") + UiText.month(this, item) +
            "\n" + UiText.tr(this, "First civil day (Pakistan): ") + UiText.date(this, item.start, true) +
            "\n" + UiText.expectedHijriDate(this, item, item.start) +
            "\n\n" + UiText.tr(this, "First Thursday: ") + UiText.date(this, item.thursday(), true) +
            "\n" + UiText.expectedHijriDate(this, item, item.thursday()) +
            "\n" + UiText.tr(this, "Reminder: ") + UiText.reminderTimeText(this) + " PKT" +
            "\n\n" + UiText.tr(this, "Source: ") + LunarEngine.basisDescription(item.basis) +
            dependsOnSightingNote(item) +
            "\n\n" + UiText.tr(this, "Forecasts and third-party reports may differ from the official Ruet-e-Hilal decision.")
        AlertDialog.Builder(this).setTitle(UiText.tr(this, "Lunar month details"))
            .setMessage(message).setPositiveButton(UiText.tr(this, "Close"), null).show()
    }

    private fun calendarDisclaimer() {
        AlertDialog.Builder(this).setTitle(UiText.tr(this, "Calendar source information"))
            .setMessage(UiText.tr(this, "Official Pakistani moon sighting is determined by the Central Ruet-e-Hilal Committee. The app uses third-party reported dates when available and PMD forecasts or calculated estimates otherwise. It does not claim a government-approved date feed."))
            .setPositiveButton(UiText.tr(this, "Close"), null).show()
    }

    private fun sourceLabel(basis: LunarEngine.Basis): String = when (basis) {
        LunarEngine.Basis.LIVE_REPORTED_CONFIRMED -> UiText.tr(this, "Third-party feed reports confirmation")
        LunarEngine.Basis.PMD_FORECAST -> UiText.tr(this, "PMD forecast · not confirmed")
        else -> UiText.tr(this, "Calendar estimate · not confirmed")
    }

    /** A calculated hint for a sighting-dependent month; never a claim about the decision. */
    private fun sightingOutlook(item: LunarEngine.Month): String {
        val evening = ReminderPlanner.decisionEvening(item.hijriYear, item.hijriMonth, ReminderStore.confirmedStarts(this))
        val label = when (CrescentVisibility.estimate(evening).outlook) {
            CrescentVisibility.Outlook.LIKELY -> R.string.sighting_likely
            CrescentVisibility.Outlook.UNLIKELY -> R.string.sighting_unlikely
            else -> R.string.sighting_borderline
        }
        return str(R.string.sighting_outlook, UiText.date(this, evening, false), str(label))
    }

    private fun dependsOnSightingNote(item: LunarEngine.Month): String {
        val other = ReminderPlanner.otherThursday(item.hijriYear, item.hijriMonth, ReminderStore.confirmedStarts(this))
            ?: return ""
        return "\n\n" + str(R.string.depends_on_sighting, UiText.date(this, other, true)) + "\n" + sightingOutlook(item)
    }

    /** Source label, plus a second line when the first Thursday still depends on the sighting. */
    private fun sourceLine(item: LunarEngine.Month): String {
        val label = sourceLabel(item.basis)
        val other = ReminderPlanner.otherThursday(item.hijriYear, item.hijriMonth, ReminderStore.confirmedStarts(this))
            ?: return label
        return label + "\n" + str(R.string.depends_on_sighting, UiText.date(this, other, false)) + "\n" + sightingOutlook(item)
    }

    private fun months(n: Int): List<LunarEngine.Month> =
        // Same months and dates the reminder planner uses, so screens and alerts agree.
        ReminderPlanner.upcomingMonths(Instant.now(), ReminderStore.confirmedStarts(this), n, ReminderStore.reminderTime(this))

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && !ReminderNotifications.permitted(this)) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        }
    }

    override fun onRequestPermissionsResult(code: Int, names: Array<String>, grants: IntArray) {
        super.onRequestPermissionsResult(code, names, grants)
        if (code == REQUEST_NOTIFICATIONS) render()
    }

    private fun open(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (ex: RuntimeException) {
            toast("A browser is required")
        }
    }

    private fun chevron(): String = if (AppLanguage.isUrdu(this)) "‹" else "›"

    private fun toast(s: String) {
        Toast.makeText(this, UiText.tr(this, s), Toast.LENGTH_LONG).show()
    }

    private fun dp(n: Int): Int = (n * resources.displayMetrics.density + .5f).toInt()

    private fun color(attr: Int, fallback: Long): Int = MaterialColors.getColor(this, attr, fallback.toInt())

    private fun column(): LinearLayout {
        val l = LinearLayout(this)
        l.orientation = LinearLayout.VERTICAL
        return l
    }

    private fun row(): LinearLayout {
        val l = LinearLayout(this)
        l.orientation = LinearLayout.HORIZONTAL
        return l
    }

    private fun text(value: String, size: Int, strong: Boolean, tint: Int): TextView {
        val t = TextView(this)
        t.text = UiText.tr(this, value)
        t.textSize = size.toFloat()
        t.setTextColor(tint)
        if (strong) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        return t
    }

    private fun weightedText(value: String, size: Int, strong: Boolean, tint: Int): TextView {
        val t = text(value, size, strong, tint)
        t.layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        return t
    }

    private fun icon(drawable: Int, label: String, tint: Int): ImageView {
        val img = ImageView(this)
        img.setImageResource(drawable)
        img.setColorFilter(tint)
        img.scaleType = ImageView.ScaleType.CENTER_INSIDE
        img.contentDescription = UiText.tr(this, label)
        img.setPadding(dp(4), dp(4), dp(4), dp(4))
        return img
    }

    private fun card(color: Int, radiusDp: Int): MaterialCardView {
        val c = MaterialCardView(this)
        c.cardElevation = 0f
        c.radius = dp(radiusDp).toFloat()
        c.setCardBackgroundColor(color)
        return c
    }

    private fun card(child: View, color: Int, radiusDp: Int): MaterialCardView {
        val c = card(color, radiusDp)
        c.addView(child)
        return c
    }

    private fun round(color: Int, radiusDp: Int): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.setColor(color)
        drawable.cornerRadius = dp(radiusDp).toFloat()
        return drawable
    }

    private fun add(parent: LinearLayout, child: View, topDp: Int) {
        val p = LinearLayout.LayoutParams(-1, -2)
        p.topMargin = dp(topDp)
        parent.addView(child, p)
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 44
        private const val TAB_HOME = 0
        private const val TAB_MONTHS = 1
        private const val TAB_UPDATES = 2
    }
}
