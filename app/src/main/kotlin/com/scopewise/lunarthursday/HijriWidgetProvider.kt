package com.scopewise.lunarthursday

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import java.time.Instant

/**
 * Home-screen widget: today's Hijri date in Pakistan and the coming first
 * Thursday. It reads the same stored dates as the app and makes no network
 * request of its own.
 */
class HijriWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, widgetIds: IntArray) {
        for (id in widgetIds) render(context, manager, id)
    }

    companion object {
        /** Redraw every placed widget; safe to call when none exists. */
        @JvmStatic
        fun refreshAll(context: Context) {
            try {
                val manager = AppWidgetManager.getInstance(context) ?: return
                val ids = manager.getAppWidgetIds(ComponentName(context, HijriWidgetProvider::class.java))
                for (id in ids) render(context, manager, id)
            } catch (ignored: RuntimeException) {
                // A widget must never break scheduling.
            }
        }

        private fun render(context: Context, manager: AppWidgetManager, id: Int) {
            try {
                manager.updateAppWidget(id, build(context))
            } catch (ignored: RuntimeException) {
                // Keep whatever the launcher already shows.
            }
        }

        private fun build(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_hijri)
            val localized = AppLanguage.wrap(context)
            val locale = AppLanguage.locale(context)
            val now = Instant.now()
            val confirmed = ReminderStore.confirmedStarts(context)

            val clock = HijriClock.current(context, now)
            val hijri = LunarEngine.expectedOnDate(clock.hijriReferenceDate, confirmed)
            views.setTextViewText(R.id.widget_hijri,
                if (hijri == null) localized.getString(R.string.app_name)
                else String.format(locale, "%d", hijri.day) + " " + UiText.month(context, hijri.month))
            views.setTextViewText(R.id.widget_gregorian, UiText.date(context, clock.todayGregorian, true))

            val next = WidgetContent.next(now, confirmed)
            if (next == null) {
                views.setViewVisibility(R.id.widget_next_block, View.GONE)
            } else {
                views.setViewVisibility(R.id.widget_next_block, View.VISIBLE)
                views.setTextViewText(R.id.widget_next_label, localized.getString(R.string.widget_next_label))
                views.setTextViewText(R.id.widget_next_date,
                    UiText.date(context, next.thursday, false) + " · " + UiText.month(context, next.month))
                val countdown = when (next.daysLeft) {
                    0 -> localized.getString(R.string.widget_today)
                    1 -> localized.getString(R.string.widget_tomorrow)
                    else -> localized.getString(R.string.widget_in_days, String.format(locale, "%d", next.daysLeft))
                }
                val other = next.otherThursday
                views.setTextViewText(R.id.widget_countdown,
                    if (other == null) countdown
                    else countdown + " · " + localized.getString(
                        R.string.widget_may_be_instead, UiText.date(context, other, false)))
            }

            // Next observance from the sets chosen in Settings; hidden when there is none.
            val observance = EventsStore.upcoming(context, 1).firstOrNull()
            if (observance == null) {
                views.setViewVisibility(R.id.widget_observance, View.GONE)
            } else {
                views.setViewVisibility(R.id.widget_observance, View.VISIBLE)
                views.setTextViewText(R.id.widget_observance,
                    EventsStore.name(context, observance.event) + " · " + UiText.date(context, observance.date, false))
            }

            val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, open)
            return views
        }
    }
}
