package ru.dedtime.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.launch
import ru.dedtime.app.app

object WidgetUpdater {
    fun refresh(context: Context) {
        val app = context.app
        app.appScope.launch {
            runCatching { updateAllNow(app) }
        }
    }

    suspend fun updateAllNow(context: Context) {
        TimerWidget().updateAll(context)
        StreakWidget().updateAll(context)
        TodayWidget().updateAll(context)
        QuickWidget().updateAll(context)
        WeekWidget().updateAll(context)
        DedWidget().updateAll(context)
        NudgeWidget().updateAll(context)
    }
}
