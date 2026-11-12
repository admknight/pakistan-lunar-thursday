package com.scopewise.lunarthursday;

import android.app.Activity;
import android.content.Context;

/** Keep Android 8-12 views in the chosen app language; Android 13+ uses LocaleManager. */
public abstract class LocalizedActivity extends Activity {
    @Override protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppLanguage.wrap(newBase));
    }
}
