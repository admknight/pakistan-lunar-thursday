package com.scopewise.lunarthursday

import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.DynamicColors

/** Optional large-format alarm controls; also accessible from notification actions. */
class AlarmActivity : LocalizedActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        DynamicColors.applyToActivityIfAvailable(this)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(0x00080000 or 0x00200000) // SHOW_WHEN_LOCKED | TURN_SCREEN_ON
        }
        val test = intent.getBooleanExtra(AlarmRingingService.EXTRA_TEST, false)
        val label = UiText.monthLabel(this,
            intent.getStringExtra(AlarmRingingService.EXTRA_LABEL) ?: "Hilal Thursday")
        val body = LinearLayout(this)
        body.orientation = LinearLayout.VERTICAL
        body.gravity = Gravity.CENTER
        body.setPadding(dp(32), dp(48), dp(32), dp(40))
        val title = TextView(this)
        title.text = UiText.tr(this, if (test) "Test alarm" else "Hilal Thursday alarm")
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        title.textSize = 28f
        title.gravity = Gravity.CENTER
        body.addView(title)
        val caption = TextView(this)
        caption.text = label + "\n" + UiText.tr(this, "11:00 AM Pakistan time")
        caption.textSize = 18f
        caption.gravity = Gravity.CENTER
        val cap = LinearLayout.LayoutParams(-1, -2)
        cap.topMargin = dp(18)
        body.addView(caption, cap)
        val dismiss = MaterialButton(this)
        dismiss.text = UiText.tr(this, "Dismiss alarm")
        dismiss.isAllCaps = false
        dismiss.setOnClickListener { command(AlarmRingingService.ACTION_DISMISS) }
        val buttons = LinearLayout.LayoutParams(-1, dp(56))
        buttons.topMargin = dp(28)
        body.addView(dismiss, buttons)
        if (!test) {
            val snooze = MaterialButton(this)
            snooze.text = UiText.tr(this, "Snooze 10 minutes")
            snooze.isAllCaps = false
            snooze.setOnClickListener { command(AlarmRingingService.ACTION_SNOOZE) }
            val row = LinearLayout.LayoutParams(-1, dp(56))
            row.topMargin = dp(12)
            body.addView(snooze, row)
        }
        setContentView(body, ViewGroup.LayoutParams(-1, -1))
    }

    private fun command(action: String) {
        AlarmRingingService.command(this, action)
        finish()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + .5f).toInt()
}
