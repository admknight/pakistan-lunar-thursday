package com.scopewise.lunarthursday

import android.content.Context
import java.time.Instant

/** Loads the bundled observances and remembers which sets the user wants to see. */
object EventsStore {
    const val SHOW_SHARED = "events_show_shared"
    const val SHOW_A = "events_show_a"
    const val SHOW_B = "events_show_b"

    @Volatile private var cached: List<IslamicEvents.Event>? = null

    /** The bundled list; empty if the file cannot be read or is invalid. */
    @JvmStatic
    fun events(context: Context): List<IslamicEvents.Event> {
        cached?.let { return it }
        val loaded = try {
            context.applicationContext.assets.open("events.json").use { stream ->
                IslamicEvents.parse(String(stream.readBytes(), Charsets.UTF_8))
            }
        } catch (invalidOrMissing: Exception) {
            emptyList()
        }
        cached = loaded
        return loaded
    }

    private fun key(set: String): String? = when (set) {
        IslamicEvents.SET_SHARED -> SHOW_SHARED
        IslamicEvents.SET_A -> SHOW_A
        IslamicEvents.SET_B -> SHOW_B
        else -> null
    }

    /** Common observances are shown by default; the two further sets are opt-in. */
    @JvmStatic
    fun isShown(context: Context, set: String): Boolean {
        val key = key(set) ?: return false
        return ReminderStore.prefs(context).getBoolean(key, set == IslamicEvents.SET_SHARED)
    }

    @JvmStatic
    fun setShown(context: Context, set: String, shown: Boolean) {
        val key = key(set) ?: return
        ReminderStore.prefs(context).edit().putBoolean(key, shown).apply()
    }

    @JvmStatic
    fun enabledSets(context: Context): Set<String> =
        listOf(IslamicEvents.SET_SHARED, IslamicEvents.SET_A, IslamicEvents.SET_B)
            .filter { isShown(context, it) }.toSet()

    /** Upcoming observances for the chosen sets, from today's Pakistan civil date. */
    @JvmStatic
    fun upcoming(context: Context, limit: Int): List<IslamicEvents.Occurrence> {
        val today = Instant.now().atZone(LunarEngine.PAKISTAN).toLocalDate()
        return IslamicEvents.upcoming(events(context), today,
            ReminderStore.confirmedStarts(context), enabledSets(context), limit)
    }

    /** Urdu name when the app is in Urdu and one exists, otherwise English. */
    @JvmStatic
    fun name(context: Context, event: IslamicEvents.Event): String =
        if (AppLanguage.isUrdu(context) && event.nameUr != null) event.nameUr else event.nameEn
}
