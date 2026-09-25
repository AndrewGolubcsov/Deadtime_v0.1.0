package ru.dedtime.app.widget

import android.content.Context
import android.content.res.Configuration
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider as FixedColor
import ru.dedtime.app.MainActivity
import ru.dedtime.app.R
import ru.dedtime.app.app
import ru.dedtime.app.data.Category
import ru.dedtime.app.data.Habit
import ru.dedtime.app.domain.Days
import ru.dedtime.app.domain.Engine
import ru.dedtime.app.domain.activeMs
import ru.dedtime.app.domain.sessionBaseXp

val HabitKey = ActionParameters.Key<Long>("habit_id")
val StartCategoryKey = ActionParameters.Key<Long>(MainActivity.EXTRA_START_CATEGORY)

private object WC {
    val bg = ColorProvider(day = Color(0xFFF3EFE6), night = Color(0xFF222226))
    val ink = ColorProvider(day = Color(0xFF17171A), night = Color(0xFFF3EFE6))
    val muted = ColorProvider(day = Color(0xFF5B5A57), night = Color(0xFFA9A59C))
    val chip = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF2C2C31))
    val dark = FixedColor(Color(0xFF17171A))
    val cream = FixedColor(Color(0xFFF3EFE6))
    val flame = FixedColor(Color(0xFFF59E68))
}

private suspend fun accentOf(context: Context): Color = Color(context.app.settings.current().accent.toInt())

private fun catColor(c: Category?, accent: Color): Color =
    if (c == null || c.colorHex == "accent") accent
    else runCatching { Color(android.graphics.Color.parseColor(c.colorHex)) }.getOrDefault(accent)

private fun isNight(context: Context) =
    (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

private val openApp get() = actionStartActivity<MainActivity>()

@Composable
private fun Card(modifier: GlanceModifier = GlanceModifier, bg: androidx.glance.unit.ColorProvider = WC.bg, content: @Composable () -> Unit) {
    Box(
        modifier = modifier.fillMaxSize().background(bg).cornerRadius(24.dp).padding(14.dp),
        content = content,
    )
}

private fun style(size: Int, bold: Boolean = true, color: androidx.glance.unit.ColorProvider = WC.ink) =
    TextStyle(color = color, fontSize = size.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)

// ——— Таймер 2×2 ———

class TimerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = context.app.repo
        val s = repo.active()
        val cats = repo.categoriesNow()
        val accent = accentOf(context)
        val lastCat = repo.lastCategoryId()
        val now = System.currentTimeMillis()
        val night = isNight(context)
        provideContent {
            Card(GlanceModifier.clickable(actionStartActivity<MainActivity>(actionParametersOf()))) {
                Column(GlanceModifier.fillMaxSize()) {
                    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        val cat = cats.firstOrNull { it.id == (s?.categoryId ?: lastCat) }
                        Box(GlanceModifier.size(10.dp).cornerRadius(5.dp).background(catColor(cat, accent))) {}
                        Spacer(GlanceModifier.width(6.dp))
                        Text(cat?.name ?: "Фокус", style = style(13))
                        Spacer(GlanceModifier.defaultWeight())
                        Image(ImageProvider(R.drawable.logo_ded), "Дед", GlanceModifier.size(30.dp))
                    }
                    Spacer(GlanceModifier.defaultWeight())
                    if (s != null) {
                        val ms = s.activeMs(now)
                        val rv = RemoteViews(context.packageName, R.layout.widget_chrono).apply {
                            setChronometer(R.id.chrono, SystemClock.elapsedRealtime() - ms, null, s.pausedAt == null)
                            setTextColor(R.id.chrono, if (night) 0xFFF3EFE6.toInt() else 0xFF17171A.toInt())
                        }
                        AndroidRemoteViews(rv)
                    } else {
                        Text("0:00:00", style = style(26))
                    }
                    Spacer(GlanceModifier.defaultWeight())
                    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (s != null) "+${sessionBaseXp(s.activeMs(now))} XP" else "не запущен",
                            style = style(12, color = FixedColor(accent)),
                        )
                        Spacer(GlanceModifier.defaultWeight())
                        val action = if (s != null) actionRunCallback<ToggleTimerAction>()
                        else actionStartActivity<MainActivity>(actionParametersOf(StartCategoryKey to (lastCat ?: 1L)))
                        Box(
                            GlanceModifier.size(44.dp).cornerRadius(22.dp).background(WC.ink).clickable(action),
                            contentAlignment = Alignment.Center,
                        ) {
                            val icon = if (s != null && s.pausedAt == null) R.drawable.ic_pause else R.drawable.ic_play
                            Image(ImageProvider(icon), if (s?.pausedAt == null && s != null) "Пауза" else "Старт",
                                GlanceModifier.size(18.dp), colorFilter = ColorFilter.tint(WC.bg))
                        }
                    }
                }
            }
        }
    }
}

class ToggleTimerAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.app.repo.togglePause()
    }
}

// ——— Серия ———

class StreakWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val st = context.app.repo.snapshot()
        val left = (st.habitsTotal - st.todayHabitsDone).coerceAtLeast(0)
        val note = when {
            st.todayFocusMin >= 25 || (st.habitsTotal > 0 && st.todayHabitsDone * 2 >= st.habitsTotal) -> "День засчитан. Дед доволен"
            left > 0 -> "Отметь ещё $left — и огонь не погаснет"
            else -> "25 минут фокуса спасут серию"
        }
        provideContent {
            Card(GlanceModifier.clickable(openApp), bg = WC.dark) {
                Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Image(ImageProvider(R.drawable.ic_flame), null, GlanceModifier.size(38.dp), colorFilter = ColorFilter.tint(WC.flame))
                    Spacer(GlanceModifier.width(12.dp))
                    Column {
                        Text("${st.streak} дн.", style = style(24, color = WC.cream))
                        Text(note, style = style(11, color = WC.flame), maxLines = 2)
                    }
                }
            }
        }
    }
}

// ——— Привычки на сегодня ———

class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = context.app.repo
        val habits = repo.habitsNow().take(4)
        val date = Days.key(Days.today())
        val values = habits.associate { it.id to repo.logValue(it.id, date) }
        val accent = accentOf(context)
        val done = habits.count { Engine.habitDone(it, values[it.id] ?: 0) }
        provideContent {
            Card {
                Column(GlanceModifier.fillMaxSize()) {
                    Row(GlanceModifier.fillMaxWidth().clickable(openApp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Сегодня", style = style(15))
                        Spacer(GlanceModifier.defaultWeight())
                        Text("$done из ${habits.size}", style = style(13, color = WC.muted))
                    }
                    Spacer(GlanceModifier.height(6.dp))
                    if (habits.isEmpty()) {
                        Text("Добавь привычку в приложении", style = style(13, bold = false, color = WC.muted))
                    }
                    habits.forEach { h -> HabitRow(h, values[h.id] ?: 0, accent) }
                }
            }
        }
    }
}

@Composable
private fun HabitRow(h: Habit, value: Int, accent: Color) {
    val done = Engine.habitDone(h, value)
    Row(
        GlanceModifier.fillMaxWidth().height(40.dp)
            .clickable(actionRunCallback<TapHabitAction>(actionParametersOf(HabitKey to h.id))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            GlanceModifier.size(24.dp).cornerRadius(7.dp).background(if (done) FixedColor(accent) else WC.chip),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Image(ImageProvider(R.drawable.ic_check), "Готово", GlanceModifier.size(14.dp), colorFilter = ColorFilter.tint(FixedColor(Color.White)))
            else if (h.kind == Habit.KIND_COUNTER) Text("+", style = style(14))
        }
        Spacer(GlanceModifier.width(10.dp))
        val label = if (h.kind == Habit.KIND_COUNTER) "${h.title} $value/${h.target}" else h.title
        Text(label, style = style(13, color = if (done) WC.muted else WC.ink), maxLines = 1)
    }
}

class TapHabitAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[HabitKey] ?: return
        context.app.repo.tapHabit(id)
    }
}

// ——— Быстрый старт 4×1 ———

class QuickWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val cats = context.app.repo.categoriesNow().take(4)
        val accent = accentOf(context)
        provideContent {
            Box(GlanceModifier.fillMaxSize().background(WC.bg).cornerRadius(24.dp).padding(8.dp)) {
                Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    cats.forEachIndexed { i, c ->
                        if (i > 0) Spacer(GlanceModifier.width(6.dp))
                        Row(
                            GlanceModifier.defaultWeight().height(44.dp).cornerRadius(16.dp).background(WC.chip)
                                .clickable(actionStartActivity<MainActivity>(actionParametersOf(StartCategoryKey to c.id))),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(GlanceModifier.size(8.dp).cornerRadius(4.dp).background(catColor(c, accent))) {}
                            Spacer(GlanceModifier.width(5.dp))
                            Text(c.name, style = style(12), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

// ——— Неделя 4×2 ———

class WeekWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val st = context.app.repo.snapshot()
        val days = Engine.lastDays(st.today, 7)
        val mins = days.map { st.focusMin(it) }
        val max = (mins.maxOrNull() ?: 0).coerceAtLeast(60)
        val total = mins.sum()
        val xp = days.sumOf { st.dayXp[it] ?: 0L }
        val accent = accentOf(context)
        val letters = listOf("П", "В", "С", "Ч", "П", "С", "В")
        provideContent {
            Card(GlanceModifier.clickable(openApp)) {
                Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Bottom) {
                    Column {
                        Text("Неделя", style = style(12, color = WC.muted))
                        Text("${total / 60} ч", style = style(22))
                        Text("+$xp XP", style = style(12, color = FixedColor(accent)))
                    }
                    Spacer(GlanceModifier.width(14.dp))
                    Row(GlanceModifier.defaultWeight(), verticalAlignment = Alignment.Bottom) {
                        days.forEachIndexed { i, d ->
                            Column(GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                                val h = (mins[i] * 70 / max).coerceAtLeast(3)
                                Box(
                                    GlanceModifier.width(14.dp).height(h.dp).cornerRadius(4.dp)
                                        .background(if (i == 6) FixedColor(accent) else FixedColor(Color(0xFFC9C3B5))),
                                ) {}
                                Text(letters[d.dayOfWeek.value - 1], style = style(10, color = WC.muted))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ——— Дед 1×1 ———

class DedWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val st = context.app.repo.snapshot()
        val m = st.todayFocusMin
        provideContent {
            Box(
                GlanceModifier.fillMaxSize().background(WC.dark).cornerRadius(24.dp).padding(6.dp).clickable(openApp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(ImageProvider(R.drawable.logo_ded), "Дедтайм", GlanceModifier.size(52.dp))
                    Text("${m / 60}:${(m % 60).toString().padStart(2, '0')}", style = style(14, color = WC.cream))
                }
            }
        }
    }
}

// ——— Дед торопит 2×1 ———

class NudgeWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val st = context.app.repo.snapshot()
        val left = (120 - st.todayFocusMin).coerceAtLeast(0)
        val text = if (left > 0) "Ещё ${Engine.minutesLabel(left)} мин до 2 часов фокуса. Дед ждёт." else "Цель дня взята. Дед доволен."
        provideContent {
            Card(GlanceModifier.clickable(openApp), bg = FixedColor(Color(0xFFFBE3D6))) {
                Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Image(ImageProvider(R.drawable.logo_ded), "Дед", GlanceModifier.size(56.dp))
                    Spacer(GlanceModifier.width(10.dp))
                    Text(text, style = style(13, color = WC.dark), maxLines = 3)
                }
            }
        }
    }
}

class TimerWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = TimerWidget() }
class StreakWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = StreakWidget() }
class TodayWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = TodayWidget() }
class QuickWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = QuickWidget() }
class WeekWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = WeekWidget() }
class DedWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = DedWidget() }
class NudgeWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = NudgeWidget() }
