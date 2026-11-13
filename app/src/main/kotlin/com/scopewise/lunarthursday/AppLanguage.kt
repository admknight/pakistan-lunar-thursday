package com.scopewise.lunarthursday

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/** Per-app language selection: system-native on 33+, app-owned fallback on 26-32. */
object AppLanguage {
    private const val PREF_KEY = "ui_language"

    @JvmStatic
    fun wrap(original: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return original
        val selected = ReminderStore.prefs(original).getString(PREF_KEY, "")
        if (selected.isNullOrEmpty()) return original
        val locale = Locale.forLanguageTag(selected)
        val configuration = Configuration(original.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return original.createConfigurationContext(configuration)
    }

    @JvmStatic
    fun choice(context: Context): String {
        if (Build.VERSION.SDK_INT >= 33) {
            val lm = context.getSystemService(LocaleManager::class.java)
            if (lm != null && !lm.applicationLocales.isEmpty) {
                return if (lm.applicationLocales.get(0).language == "ur") "ur" else "en"
            }
            return ""
        }
        return ReminderStore.prefs(context).getString(PREF_KEY, "") ?: ""
    }

    @JvmStatic
    fun select(context: Context, language: String?) {
        val chosen = if (language == "ur" || language == "en") language else ""
        if (Build.VERSION.SDK_INT >= 33) {
            val lm = context.getSystemService(LocaleManager::class.java)
            lm?.applicationLocales =
                if (chosen.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(chosen)
        } else {
            ReminderStore.prefs(context).edit().putString(PREF_KEY, chosen).apply()
        }
    }

    @JvmStatic
    fun locale(context: Context): Locale {
        val conf = wrap(context).resources.configuration
        return if (conf.locales.isEmpty) Locale.ENGLISH else conf.locales.get(0)
    }

    @JvmStatic
    fun isUrdu(context: Context): Boolean = "ur" == locale(context).language
}
