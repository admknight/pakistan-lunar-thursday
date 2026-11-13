package com.scopewise.lunarthursday

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Picks the next day on which observances should be announced. Separate from
 * the first-Thursday reminder, which keeps its own alarm.
 */
object EventReminderPlanner {
    private const val LOOKAHEAD = 400

    class Batch internal constructor(
        /** The Pakistan civil day the notification is shown on. */
        @JvmField val notifyDate: LocalDate,
        /** Everything announced that day, in file order, one entry per event. */
        @JvmField val items: List<IslamicEvents.Occurrence>
    ) {
        fun triggerAt(chosenTime: LocalTime): Instant =
            ZonedDateTime.of(notifyDate, chosenTime, LunarEngine.PAKISTAN).toInstant()
    }

    /** An observance held the night before its listed day is announced a day earlier. */
    @JvmStatic
    fun notifyDate(occurrence: IslamicEvents.Occurrence): LocalDate =
        if (occurrence.event.eve) occurrence.date.minusDays(1) else occurrence.date

    /** Everything to announce on [date], for the chosen sets and the events with reminders on. */
    @JvmStatic
    fun onDate(events: List<IslamicEvents.Event>, date: LocalDate, confirmedStarts: Map<String, LocalDate>,
               enabledSets: Set<String>, remindIds: Set<String>): List<IslamicEvents.Occurrence> {
        val wanted = events.filter { it.id in remindIds }
        val seen = HashSet<String>()
        return IslamicEvents.upcoming(wanted, date, confirmedStarts, enabledSets, LOOKAHEAD)
            .filter { notifyDate(it) == date && seen.add(it.event.id) }
    }

    /** The next batch whose time is strictly after [now], or null when nothing is due. */
    @JvmStatic
    fun next(events: List<IslamicEvents.Event>, now: Instant, confirmedStarts: Map<String, LocalDate>,
             enabledSets: Set<String>, remindIds: Set<String>, chosenTime: LocalTime): Batch? {
        val today = now.atZone(LunarEngine.PAKISTAN).toLocalDate()
        val wanted = events.filter { it.id in remindIds }
        val day = IslamicEvents.upcoming(wanted, today, confirmedStarts, enabledSets, LOOKAHEAD)
            .map { notifyDate(it) }
            .filter { ZonedDateTime.of(it, chosenTime, LunarEngine.PAKISTAN).toInstant().isAfter(now) }
            .minOrNull() ?: return null
        return Batch(day, onDate(events, day, confirmedStarts, enabledSets, remindIds))
    }
}
