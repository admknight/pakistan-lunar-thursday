package com.scopewise.lunarthursday

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors

/** Identity, transparency and three preserved opt-in easter eggs. */
class AboutActivity : LocalizedActivity() {
    private var primary = 0
    private var surface = 0
    private var surfaceVariant = 0
    private var primaryContainer = 0
    private var onSurface = 0
    private var secondary = 0
    private var onContainer = 0
    private var versionTaps = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivityIfAvailable(this)
        primary = color(androidx.appcompat.R.attr.colorPrimary, 0xFF136F53)
        surface = color(com.google.android.material.R.attr.colorSurface, 0xFFFFFFFF)
        surfaceVariant = color(com.google.android.material.R.attr.colorSurfaceVariant, 0xFFEAF1EB)
        primaryContainer = color(com.google.android.material.R.attr.colorPrimaryContainer, 0xFFD5F0DD)
        onSurface = color(com.google.android.material.R.attr.colorOnSurface, 0xFF192D26)
        secondary = color(com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF4D6459)
        onContainer = color(com.google.android.material.R.attr.colorOnPrimaryContainer, 0xFF002B1A)
        render()
    }

    private fun render() {
        val root = column()
        root.setBackgroundColor(surface)
        if (Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                view.setPadding(0, bars.top, 0, bars.bottom)
                insets
            }
        } else {
            root.fitsSystemWindows = true
        }
        setContentView(root)

        val toolbar = row()
        toolbar.gravity = Gravity.CENTER_VERTICAL
        toolbar.setPadding(dp(14), 0, dp(18), 0)
        val back = ImageView(this)
        back.setImageResource(R.drawable.ic_ui_back)
        back.setColorFilter(primary)
        if (AppLanguage.isUrdu(this)) back.scaleX = -1f
        back.setPadding(dp(11), dp(11), dp(11), dp(11))
        back.contentDescription = UiText.tr(this, "Back")
        back.setOnClickListener { finish() }
        toolbar.addView(back, LinearLayout.LayoutParams(dp(48), dp(48)))
        toolbar.addView(text("About / Developer", 19, true, onSurface))
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(62)))

        val scroll = ScrollView(this)
        scroll.isFillViewport = true
        val body = column()
        body.setPadding(dp(18), dp(9), dp(18), dp(36))
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val logo = ImageView(this)
        logo.setImageResource(R.mipmap.ic_launcher)
        logo.contentDescription = UiText.tr(this, "Hilal Thursday")
        val logoParams = LinearLayout.LayoutParams(dp(108), dp(108))
        logoParams.gravity = Gravity.CENTER_HORIZONTAL
        body.addView(logo, logoParams)
        logo.setOnLongClickListener {
            constellation()
            true
        }

        val name = text(getString(R.string.app_name), 28, true, onSurface)
        name.gravity = Gravity.CENTER
        add(body, name, 10)
        val tagline = text("🌙 A gentle reminder, every lunar month.", 15, false, secondary)
        tagline.gravity = Gravity.CENTER
        add(body, tagline, 6)
        val detail = text(UiText.reminderTimeText(this) + " Pakistan time · English & Urdu", 12, false, secondary)
        detail.gravity = Gravity.CENTER
        add(body, detail, 7)

        val developer = column()
        developer.setPadding(dp(17), dp(17), dp(17), dp(17))
        add(developer, text("👨‍💻  Developer", 16, true, onContainer), 0)
        add(developer, text("admknight", 21, true, onContainer), 7)
        add(developer, text("Made for Pakistan, open to everyone", 13, false, onContainer), 5)
        add(body, card(developer, primaryContainer), 22)

        val list = column()
        list.setPadding(dp(13), dp(6), dp(13), dp(6))
        list.addView(linkRow("🔒", "Privacy", "Optional weather location · no ads or accounts") {
            showInfo("Privacy", "No account, ads, or analytics. Location is requested only when you enable weather visuals, and only while the app is in use. Approximate coordinates are rounded, sent to Open-Meteo, and never stored. The Hijri-date feed and GitHub release checks also contact external services. Each provider can see the connecting IP address.")
        })
        divider(list)
        list.addView(linkRow("📅", "Calendar sources & accuracy", "How Islamic month dates are determined") {
            showInfo("Calendar sources & accuracy", "Official Pakistani moon sighting is determined by the Central Ruet-e-Hilal Committee. The app uses third-party reported dates when available and PMD forecasts or calculated estimates otherwise. It does not claim a government-approved date feed.")
        })
        divider(list)
        list.addView(linkRow("✨", "What's new", "Weather visuals and in-app updates") {
            showInfo("What's new", "Hilal Thursday v1.9.1 fixes the extra crescent, shows today's Hijri date with an optional city-based Maghrib transition, and lets you choose the monthly reminder time in Pakistan time. All three signature alarm sounds remain available.")
        })
        divider(list)
        list.addView(linkRow("⌘", "Open source & feedback", "Code, issues and contributions") { open(PROJECT) })
        divider(list)
        list.addView(linkRow("📜", "MIT License", "Read the project license") { open("$PROJECT/blob/main/LICENSE") })
        add(body, card(list, surfaceVariant), 16)

        val version = column()
        version.gravity = Gravity.CENTER
        val versionText = text(UiText.tr(this, "Version ") + UpdateManager.installed(this), 15, true, secondary)
        versionText.gravity = Gravity.CENTER
        add(version, versionText, 0)
        add(version, text("✨ Made with care under the same moon.", 12, false, secondary), 9)
        version.setOnClickListener {
            if (++versionTaps == 5) {
                versionTaps = 0
                AlertDialog.Builder(this).setTitle(UiText.tr(this, "🌟 Five stars"))
                    .setMessage(UiText.tr(this, "A tiny secret from the developer: some dates need patience, but a gentle reminder can still brighten the day.\n\n🌙  ✦  ✧  ✦  🌙"))
                    .setPositiveButton(UiText.tr(this, "Shine on"), null).show()
            }
        }
        add(body, version, 25)
    }

    private fun constellation() {
        AlertDialog.Builder(this).setTitle(UiText.tr(this, "✦ Constellation unlocked ✦"))
            .setMessage(UiText.tr(this, "       ✦           ·\n  ·       ☾         ✧\n       ✧       ✦\n\nThe night sky has no update button, but your reminder app does."))
            .setPositiveButton(UiText.tr(this, "Back to Earth"), null).show()
    }

    private fun linkRow(glyph: String, title: String, subtitle: String, action: () -> Unit): LinearLayout {
        val row = row()
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(3), dp(13), dp(3), dp(13))
        val emoji = text(glyph, 21, false, primary)
        emoji.gravity = Gravity.CENTER
        row.addView(emoji, LinearLayout.LayoutParams(dp(45), dp(45)))
        val words = column()
        words.addView(text(title, 15, true, onSurface))
        words.addView(text(subtitle, 12, false, secondary))
        row.addView(words, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(text(if (AppLanguage.isUrdu(this)) "‹" else "›", 24, false, secondary))
        row.isClickable = true
        row.isFocusable = true
        row.setOnClickListener { action() }
        row.contentDescription = UiText.tr(this, title)
        return row
    }

    private fun divider(parent: LinearLayout) {
        val line = View(this)
        line.setBackgroundColor((secondary and 0x00ffffff) or 0x22000000)
        val p = LinearLayout.LayoutParams(-1, dp(1))
        p.marginStart = dp(48)
        parent.addView(line, p)
    }

    private fun showInfo(title: String, detail: String) {
        AlertDialog.Builder(this).setTitle(UiText.tr(this, title))
            .setMessage(UiText.tr(this, detail))
            .setPositiveButton(UiText.tr(this, "Close"), null).show()
    }

    private fun open(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: RuntimeException) {
            Toast.makeText(this, UiText.tr(this, "A browser is required"), Toast.LENGTH_LONG).show()
        }
    }

    private fun column(): LinearLayout {
        val v = LinearLayout(this)
        v.orientation = LinearLayout.VERTICAL
        return v
    }

    private fun row(): LinearLayout {
        val v = LinearLayout(this)
        v.orientation = LinearLayout.HORIZONTAL
        return v
    }

    private fun text(value: String, size: Int, strong: Boolean, tint: Int): TextView {
        val t = TextView(this)
        t.text = UiText.tr(this, value)
        t.textSize = size.toFloat()
        t.setTextColor(tint)
        if (strong) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        return t
    }

    private fun card(view: View, fill: Int): MaterialCardView {
        val card = MaterialCardView(this)
        card.cardElevation = 0f
        card.radius = dp(23).toFloat()
        card.setCardBackgroundColor(fill)
        card.addView(view)
        return card
    }

    private fun color(attr: Int, fallback: Long): Int = MaterialColors.getColor(this, attr, fallback.toInt())

    private fun dp(n: Int): Int = (resources.displayMetrics.density * n + 0.5f).toInt()

    private fun add(parent: LinearLayout, child: View, topDp: Int) {
        val p = LinearLayout.LayoutParams(-1, -2)
        p.topMargin = dp(topDp)
        parent.addView(child, p)
    }

    companion object {
        private const val PROJECT = "https://github.com/admknight/pakistan-lunar-thursday"
    }
}
