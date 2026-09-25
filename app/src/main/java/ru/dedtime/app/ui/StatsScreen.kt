package ru.dedtime.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.dedtime.app.MainVm
import ru.dedtime.app.data.Category
import ru.dedtime.app.domain.Engine
import ru.dedtime.app.domain.GameState
import java.time.DayOfWeek
import java.time.format.TextStyle as JTextStyle
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

private val ru = Locale("ru")

@Composable
fun StatsScreen(vm: MainVm, onShare: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val cats by vm.categories.collectAsStateWithLifecycle()
    val st = state ?: return
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenTitle("Статистика")
        DayCard(st, cats, onShare)
        HoursCard(st, cats)
        XpRangeCard(st)
        HeatmapCard(st)
    }
}

@Composable
fun Donut(parts: List<Pair<Color, Int>>, modifier: Modifier, track: Color, stroke: Float = 40f) {
    val total = parts.sumOf { it.second }
    Canvas(modifier) {
        val sw = stroke
        val d = size.minDimension - sw
        val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
        drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(sw))
        if (total > 0) {
            var start = -90f
            parts.forEach { (c, v) ->
                val sweep = 360f * v / total
                if (sweep > 0) drawArc(c, start, (sweep - 1.5f).coerceAtLeast(0.5f), false, tl, Size(d, d), style = Stroke(sw))
                start += sweep
            }
        }
    }
}

@Composable
private fun DayCard(st: GameState, cats: List<Category>, onShare: () -> Unit) {
    val today = st.todayFocus
    val parts = cats.map { dc.category(it) to (today[it.id] ?: 0) }
    DCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Куда ушёл день", style = T.body(15.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f))
            LinkText("Поделиться", onShare)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                Donut(parts, Modifier.fillMaxSize(), dc.line, stroke = 52f)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Engine.minutesLabel(st.todayFocusMin), style = T.display(18.sp))
                    Text("в фокусе", style = T.muted(12.sp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                cats.forEach { c ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(dc.category(c)))
                        Text(c.name, style = T.body(14.sp), modifier = Modifier.weight(1f), maxLines = 1)
                        Text(Engine.minutesLabel(today[c.id] ?: 0), style = T.body(14.sp, FontWeight.ExtraBold))
                    }
                }
            }
        }
    }
}

private data class Bar(val label: String, val byCat: Map<Long, Int>, val highlight: Boolean)

@Composable
private fun HoursCard(st: GameState, cats: List<Category>) {
    var period by rememberSaveable { mutableStateOf("week") }
    val bars: List<Bar>
    val title: String
    val note: String
    when (period) {
        "week" -> {
            val days = Engine.lastDays(st.today, 7)
            bars = days.mapIndexed { i, d -> Bar(d.dayOfWeek.getDisplayName(JTextStyle.SHORT, ru).replaceFirstChar { it.uppercase() }, st.focus[d] ?: emptyMap(), i == 6) }
            title = "Неделя"
            val studyId = cats.firstOrNull { it.name == "Учёба" }?.id
            val study = days.sumOf { st.focus[it]?.get(studyId) ?: 0 }
            note = "Учёба за неделю: ${Engine.minutesLabel(study)}"
        }
        "month" -> {
            val days = Engine.lastDays(st.today, 30)
            bars = days.mapIndexed { i, d ->
                Bar(if (i == 0 || d.dayOfMonth % 5 == 0) d.dayOfMonth.toString() else "", st.focus[d] ?: emptyMap(), i == 29)
            }
            title = "Последние 30 дней"
            val total = days.sumOf { st.focusMin(it) }
            val best = days.maxOfOrNull { st.focusMin(it) } ?: 0
            note = "В среднем ${Engine.minutesLabel(total / 30)} в день · лучший день ${Engine.minutesLabel(best)}"
        }
        else -> {
            val months = Engine.lastMonths(st.today, 12)
            bars = months.mapIndexed { i, ym ->
                Bar(ym.month.getDisplayName(JTextStyle.NARROW_STANDALONE, ru).uppercase(), Engine.sumFocus(st, Engine.daysOf(ym)), i == 11)
            }
            title = "Последние 12 месяцев"
            val best = months.maxByOrNull { Engine.sumFocus(st, Engine.daysOf(it)).values.sum() }
            note = if (best != null) "Лучший месяц: ${best.month.getDisplayName(JTextStyle.FULL_STANDALONE, ru)}" else ""
        }
    }
    val totals = bars.map { it.byCat.values.sum() }
    val periodMin = totals.sum()
    val maxH = (totals.maxOrNull() ?: 0) / 60.0
    val step = if (period == "year") 10 else 2
    val top = (ceil(maxH / step).toInt() * step).coerceAtLeast(step)
    val colors = cats.associate { it.id to dc.category(it) }
    val fallback = dc.muted
    val ink = dc.ink
    val surface = dc.surface

    DCard {
        Segmented(listOf("week" to "Неделя", "month" to "Месяц", "year" to "Год"), period, { period = it })
        Row(verticalAlignment = Alignment.Bottom) {
            Text(title, style = T.body(15.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f))
            Text("${periodMin / 60} ч · +${fmtNum(periodMin.toLong())} XP", style = T.body(13.sp, FontWeight.ExtraBold, dc.accent))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.height(170.dp).width(30.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
                Text("${top}ч", style = T.muted(11.sp)); Text("${top / 2}ч", style = T.muted(11.sp)); Text("0", style = T.muted(11.sp))
            }
            Column(Modifier.weight(1f)) {
                Canvas(Modifier.fillMaxWidth().height(170.dp)) {
                    val n = bars.size
                    val gap = if (n > 12) 2.dp.toPx() else 6.dp.toPx()
                    val w = ((size.width - gap * (n - 1)) / n).coerceAtMost(30.dp.toPx())
                    val totalW = w * n + gap * (n - 1)
                    val x0 = (size.width - totalW) / 2
                    val scale = size.height / (top * 60f)
                    bars.forEachIndexed { i, b ->
                        var y = size.height
                        val x = x0 + i * (w + gap)
                        cats.forEach { c ->
                            val m = b.byCat[c.id] ?: 0
                            if (m > 0) {
                                val h = m * scale
                                drawRect(colors[c.id] ?: fallback, Offset(x, y - h), Size(w, h))
                                drawRect(surface, Offset(x, y - h), Size(w, if (n > 12) 1f else 2f))
                                y -= h
                            }
                        }
                        // категории из архива
                        val other = b.byCat.filterKeys { k -> cats.none { it.id == k } }.values.sum()
                        if (other > 0) { val h = other * scale; drawRect(fallback, Offset(x, y - h), Size(w, h)) }
                    }
                    drawRect(ink, Offset(0f, size.height - 2.dp.toPx()), Size(size.width, 2.dp.toPx()))
                }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    bars.forEach { b ->
                        Text(
                            b.label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1,
                            style = T.body(if (bars.size > 12) 9.sp else 12.sp, if (b.highlight) FontWeight.ExtraBold else FontWeight.SemiBold, if (b.highlight) dc.ink else dc.muted),
                        )
                    }
                }
            }
        }
        if (note.isNotEmpty()) Text(note, style = T.body(13.sp, FontWeight.Bold, dc.secondary))
    }
}

@Composable
private fun XpRangeCard(st: GameState) {
    val days = Engine.lastDays(st.today, 7)
    val per = days.map { st.dayXp[it] ?: 0L }
    val min = per.minOrNull() ?: 0L
    val max = per.maxOrNull() ?: 0L
    val avg = if (per.isEmpty()) 0L else (per.sum().toDouble() / per.size).roundToInt().toLong()
    val today = per.last()
    val top = (max * 1.1).coerceAtLeast(10.0).toFloat()
    val track = dc.line; val range = dc.accentTint; val sec = dc.secondary; val acc = dc.accent; val surf = dc.surface
    DCard {
        Row {
            Text("XP в день", style = T.body(15.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f))
            Text("за 7 дней", style = T.muted())
        }
        Canvas(Modifier.fillMaxWidth().height(34.dp)) {
            val cy = size.height / 2
            val bh = 10.dp.toPx()
            fun x(v: Long) = size.width * (v / top)
            drawRoundRect(track, Offset(0f, cy - bh / 2), Size(size.width, bh), CornerRadius(bh / 2))
            drawRoundRect(range, Offset(x(min), cy - bh / 2), Size((x(max) - x(min)).coerceAtLeast(bh), bh), CornerRadius(bh / 2))
            drawRoundRect(sec, Offset(x(avg) - 2.dp.toPx(), cy - 12.dp.toPx()), Size(4.dp.toPx(), 24.dp.toPx()), CornerRadius(2.dp.toPx()))
            drawCircle(surf, 11.dp.toPx(), Offset(x(today), cy))
            drawCircle(acc, 8.dp.toPx(), Offset(x(today), cy))
        }
        Row {
            listOf(Triple(min, "минимум", dc.ink), Triple(avg, "среднее", dc.secondary), Triple(max, "максимум", dc.ink), Triple(today, "сегодня", dc.accent)).forEach { (v, l, c) ->
                Column(Modifier.weight(1f)) {
                    Text(fmtNum(v), style = T.display(16.sp).copy(color = c))
                    Text(l, style = T.muted(12.sp))
                }
            }
        }
        val diff = today - avg
        Text(if (diff >= 0) "Сегодня на ${fmtNum(diff)} XP выше среднего" else "До среднего осталось ${fmtNum(-diff)} XP", style = T.muted())
    }
}

@Composable
private fun HeatmapCard(st: GameState) {
    // 26 недель, колонка = неделя с понедельника
    val lastMonday = st.today.with(DayOfWeek.MONDAY)
    val start = lastMonday.minusWeeks(25)
    val shades = listOf(dc.line, dc.accent.copy(alpha = 0.3f), dc.accent.copy(alpha = 0.55f), dc.accent, dc.accent.copy(red = dc.accent.red * 0.6f, green = dc.accent.green * 0.6f, blue = dc.accent.blue * 0.6f))
    fun level(m: Int) = when { m <= 0 -> 0; m < 30 -> 1; m < 90 -> 2; m < 180 -> 3; else -> 4 }
    val today = st.today
    val focus = st.focus
    DCard {
        Row {
            Text("Полгода", style = T.body(15.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f))
            Text("серия ${st.streak} · рекорд ${st.bestStreak}", style = T.muted())
        }
        Canvas(Modifier.fillMaxWidth().height(110.dp)) {
            val gap = 3.dp.toPx()
            val cell = ((size.width - gap * 25) / 26).coerceAtMost((size.height - gap * 6) / 7)
            for (w in 0 until 26) for (dow in 0 until 7) {
                val d = start.plusWeeks(w.toLong()).plusDays(dow.toLong())
                if (d.isAfter(today)) continue
                val m = focus[d]?.values?.sum() ?: 0
                drawRoundRect(shades[level(m)], Offset(w * (cell + gap), dow * (cell + gap)), Size(cell, cell), CornerRadius(2.dp.toPx()))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("меньше", style = T.muted(12.sp))
            shades.forEach { Box(Modifier.size(10.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp)).background(it)) }
            Text("больше", style = T.muted(12.sp))
        }
    }
}
