package com.scopewise.lunarthursday

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors

/** Offline original tone selection plus stock Android system alarm-tone picker. */
class AlarmToneActivity : LocalizedActivity() {
    private var surface = 0
    private var onSurface = 0
    private var secondary = 0
    private var primary = 0
    private var container = 0
    private var onContainer = 0
    private var preview: MediaPlayer? = null
    private var playing = ""

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        DynamicColors.applyToActivityIfAvailable(this)
        applyColors()
        render()
    }

    private fun applyColors() {
        surface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, 0xFFFFFFFF.toInt())
        onSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, 0xFF1B211E.toInt())
        secondary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF4F5752.toInt())
        primary = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary, 0xFF136F53.toInt())
        container = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimaryContainer, 0xFFBCEAD3.toInt())
        onContainer = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimaryContainer, 0xFF002114.toInt())
    }

    private fun render() {
        val scroll = ScrollView(this)
        scroll.isFillViewport = true
        scroll.setBackgroundColor(surface)
        val page = column()
        page.setPadding(dp(20), dp(26), dp(20), dp(38))
        scroll.addView(page)
        setContentView(scroll)

        add(page, actionText("‹   " + getString(R.string.tone_back)) { finish() }, 0)
        add(page, text(getString(R.string.tone_heading), 26, true, onSurface), 14)
        add(page, text(getString(R.string.tone_intro), 14, false, secondary), 8)
        add(page, text(getString(R.string.tone_signature_section), 16, true, onSurface), 20)

        add(page, toneCard(AlarmTonePolicy.NOOR, "🌙", R.string.tone_noor, R.string.tone_noor_desc), 12)
        add(page, toneCard(AlarmTonePolicy.SUKOON, "✨", R.string.tone_sukoon, R.string.tone_sukoon_desc), 9)
        add(page, toneCard(AlarmTonePolicy.NIDA, "🕌", R.string.tone_nida, R.string.tone_nida_desc), 9)
        add(page, text(getString(R.string.tone_phone_section), 16, true, onSurface), 20)
        add(page, toneCard(AlarmTonePolicy.PHONE, "📱", R.string.tone_phone, R.string.tone_phone_desc), 12)
        add(page, actionText(getString(R.string.tone_stop)) {
            stopPreview()
            render()
        }, 14)
        add(page, text(getString(R.string.tone_footer), 12, false, secondary), 16)
    }

    private fun toneCard(id: String, symbol: String, titleId: Int, descriptionId: Int): View {
        val selected = id == AlarmTonePlayer.selection(this)
        val active = id == playing
        val row = LinearLayout(this)
        row.gravity = Gravity.CENTER_VERTICAL
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(dp(14), dp(13), dp(11), dp(13))
        val emoji = text(symbol, 25, false, onSurface)
        emoji.gravity = Gravity.CENTER
        row.addView(emoji, LinearLayout.LayoutParams(dp(44), dp(54)))
        val copy = column()
        add(copy, text(getString(titleId), 16, true, if (selected) onContainer else onSurface), 0)
        add(copy, text(getString(descriptionId), 12, false, if (selected) onContainer else secondary), 4)
        row.addView(copy, LinearLayout.LayoutParams(0, -2, 1f))

        val chosen = text(if (selected) "✓" else "○", 23, true, if (selected) primary else secondary)
        chosen.gravity = Gravity.CENTER
        chosen.contentDescription = getString(if (selected) R.string.tone_selected else R.string.tone_select)
        chosen.setOnClickListener { select(id) }
        row.addView(chosen, LinearLayout.LayoutParams(dp(34), dp(48)))
        val play = text(if (active) "■" else "▶", 22, true, primary)
        play.gravity = Gravity.CENTER
        play.contentDescription = getString(if (active) R.string.tone_stop else R.string.tone_preview)
        play.setOnClickListener { togglePreview(id) }
        row.addView(play, LinearLayout.LayoutParams(dp(40), dp(48)))
        val card = MaterialCardView(this)
        card.radius = dp(20).toFloat()
        card.setCardBackgroundColor(if (selected) container else surface)
        card.strokeColor = if (selected) primary else secondary
        card.strokeWidth = dp(if (selected) 2 else 1)
        card.cardElevation = 0f
        card.addView(row)
        card.isClickable = true
        card.isFocusable = true
        card.setOnClickListener { select(id) }
        return card
    }

    private fun select(id: String) {
        if (AlarmTonePolicy.PHONE == id) {
            choosePhoneAlarm()
            return
        }
        AlarmTonePlayer.save(this, id, null)
        render()
    }

    private fun togglePreview(id: String) {
        if (id == playing) {
            stopPreview()
            render()
            return
        }
        stopPreview()
        val candidate = AlarmTonePlayer.prepare(this, id, false)
        if (candidate == null) {
            Toast.makeText(this, getString(R.string.tone_cannot_play), Toast.LENGTH_SHORT).show()
            return
        }
        try {
            candidate.setVolume(0.72f, 0.72f)
            candidate.setOnCompletionListener {
                stopPreview()
                if (!isFinishing && !isDestroyed) render()
            }
            candidate.start()
            preview = candidate
            playing = id
            render()
        } catch (failure: RuntimeException) {
            candidate.release()
            playing = ""
            Toast.makeText(this, getString(R.string.tone_cannot_play), Toast.LENGTH_SHORT).show()
        }
    }

    @Suppress("DEPRECATION")
    private fun choosePhoneAlarm() {
        stopPreview()
        val picker = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.tone_phone_picker))
            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                AlarmTonePlayer.selectedPhoneUri(this) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
        try {
            startActivityForResult(picker, PICK_PHONE_ALARM)
        } catch (noPicker: RuntimeException) {
            Toast.makeText(this, getString(R.string.tone_picker_unavailable), Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Uses the platform activity-result callback")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PICK_PHONE_ALARM || resultCode != Activity.RESULT_OK || data == null) return
        // Silent sound is intentionally unavailable in this picker.
        val uri = data.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) ?: return
        val scheme = uri.scheme
        if ("content" != scheme && "android.resource" != scheme) {
            Toast.makeText(this, getString(R.string.tone_picker_unavailable), Toast.LENGTH_LONG).show()
            return
        }
        // Some Android media pickers grant durable read access; gracefully handle ones that do not.
        try {
            if ((data.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) != 0) {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (ignored: SecurityException) {
        }
        AlarmTonePlayer.save(this, AlarmTonePolicy.PHONE, uri)
        render()
    }

    private fun stopPreview() {
        playing = ""
        val current = preview ?: return
        try {
            if (current.isPlaying) current.stop()
        } catch (ignored: RuntimeException) {
        }
        current.release()
        preview = null
    }

    override fun onPause() {
        stopPreview()
        super.onPause()
    }

    override fun onDestroy() {
        stopPreview()
        super.onDestroy()
    }

    private fun column(): LinearLayout {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        return box
    }

    private fun text(value: String, sp: Int, strong: Boolean, color: Int): TextView {
        val t = TextView(this)
        t.text = value
        t.textSize = sp.toFloat()
        t.setTextColor(color)
        if (strong) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        return t
    }

    private fun actionText(value: String, action: () -> Unit): TextView {
        val button = text(value, 15, true, primary)
        button.setPadding(dp(7), dp(9), dp(7), dp(9))
        button.isClickable = true
        button.isFocusable = true
        button.setOnClickListener { action() }
        return button
    }

    private fun add(parent: LinearLayout, child: View, margin: Int) {
        val p = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        p.topMargin = dp(margin)
        parent.addView(child, p)
    }

    private fun dp(n: Int): Int = (n * resources.displayMetrics.density + .5f).toInt()

    companion object {
        private const val PICK_PHONE_ALARM = 193
    }
}
