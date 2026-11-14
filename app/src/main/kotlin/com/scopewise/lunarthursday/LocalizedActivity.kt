package com.scopewise.lunarthursday

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.View
import android.view.WindowInsets

/** Keep Android 8-12 views in the chosen app language; Android 13+ uses LocaleManager. */
abstract class LocalizedActivity : Activity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    /** Adds the status-bar and navigation-bar heights to a page's own padding (Android 15 draws edge to edge). */
    protected fun padForSystemBars(page: View) {
        if (Build.VERSION.SDK_INT < 30) return
        val left = page.paddingLeft
        val top = page.paddingTop
        val right = page.paddingRight
        val bottom = page.paddingBottom
        page.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            view.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom)
            insets
        }
    }
}
