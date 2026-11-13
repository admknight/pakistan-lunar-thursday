package com.scopewise.lunarthursday

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** A plain counter with an optional target. It keeps its count between visits. */
class TasbihActivity : LocalizedActivity() {
    private val targets = intArrayOf(0, 33, 34, 100)
    private var count = 0
    private var target = 33
    private var countView: TextView? = null
    private var targetView: Button? = null

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val settings = ReminderStore.prefs(this)
        count = settings.getInt(COUNT, 0).coerceAtLeast(0)
        target = settings.getInt(TARGET, 33).let { if (it in targets) it else 33 }

        val page = LinearLayout(this)
        page.orientation = LinearLayout.VERTICAL
        page.gravity = Gravity.CENTER
        val pad = (20 * resources.displayMetrics.density).toInt()
        page.setPadding(pad, pad, pad, pad)

        val title = TextView(this)
        title.text = getString(R.string.tasbih_title)
        title.textSize = 20f
        title.gravity = Gravity.CENTER
        page.addView(title)

        val number = TextView(this)
        number.textSize = 96f
        number.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        number.gravity = Gravity.CENTER
        page.addView(number, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        countView = number

        val hint = TextView(this)
        hint.text = getString(R.string.tasbih_hint)
        hint.gravity = Gravity.CENTER
        page.addView(hint)

        val targetButton = Button(this)
        targetButton.setOnClickListener {
            target = targets[(targets.indexOf(target) + 1) % targets.size]
            save()
            show()
        }
        page.addView(targetButton)
        targetView = targetButton

        val reset = Button(this)
        reset.text = getString(R.string.tasbih_reset)
        reset.setOnClickListener {
            count = 0
            save()
            show()
        }
        page.addView(reset)

        // Tapping the number, or anywhere on the page, counts one.
        val tap = { view: android.view.View ->
            count += 1
            if (target > 0 && count % target == 0) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            save()
            show()
        }
        page.setOnClickListener(tap)
        number.setOnClickListener(tap)
        setContentView(page)
        show()
    }

    private fun show() {
        val locale = AppLanguage.locale(this)
        countView?.text = String.format(locale, "%d", count)
        targetView?.text = if (target == 0) getString(R.string.tasbih_no_target)
        else getString(R.string.tasbih_target, String.format(locale, "%d", target))
    }

    private fun save() {
        ReminderStore.prefs(this).edit().putInt(COUNT, count).putInt(TARGET, target).apply()
    }

    companion object {
        const val COUNT = "tasbih_count"
        const val TARGET = "tasbih_target"
    }
}
