package com.scopewise.lunarthursday

import android.app.Activity
import android.content.Context

/** Keep Android 8-12 views in the chosen app language; Android 13+ uses LocaleManager. */
abstract class LocalizedActivity : Activity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }
}
