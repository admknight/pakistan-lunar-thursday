package com.scopewise.lunarthursday;

import android.app.LocaleManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

/** Per-app language selection: system-native on 33+, app-owned fallback on 26-32. */
final class AppLanguage {
    private static final String PREF_KEY = "ui_language";
    private AppLanguage() { }

    static Context wrap(Context original) {
        if (Build.VERSION.SDK_INT >= 33) return original;
        String selected = ReminderStore.prefs(original).getString(PREF_KEY, "");
        if (selected == null || selected.isEmpty()) return original;
        Locale locale = Locale.forLanguageTag(selected);
        Configuration configuration = new Configuration(original.getResources().getConfiguration());
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);
        return original.createConfigurationContext(configuration);
    }

    static String choice(Context context) {
        if (Build.VERSION.SDK_INT >= 33) {
            LocaleManager lm = context.getSystemService(LocaleManager.class);
            if (lm != null && !lm.getApplicationLocales().isEmpty()) {
                return lm.getApplicationLocales().get(0).getLanguage().equals("ur") ? "ur" : "en";
            }
            return "";
        }
        return ReminderStore.prefs(context).getString(PREF_KEY, "");
    }

    static void select(Context context, String language) {
        if (!"ur".equals(language) && !"en".equals(language)) language = "";
        if (Build.VERSION.SDK_INT >= 33) {
            LocaleManager lm = context.getSystemService(LocaleManager.class);
            if (lm != null) lm.setApplicationLocales(
                    language.isEmpty() ? LocaleList.getEmptyLocaleList()
                            : LocaleList.forLanguageTags(language));
        } else {
            ReminderStore.prefs(context).edit().putString(PREF_KEY, language).apply();
        }
    }

    static Locale locale(Context context) {
        Context translated = wrap(context);
        Configuration conf = translated.getResources().getConfiguration();
        Locale locale = conf.getLocales().isEmpty() ? Locale.ENGLISH : conf.getLocales().get(0);
        return locale;
    }

    static boolean isUrdu(Context context) { return "ur".equals(locale(context).getLanguage()); }
}
