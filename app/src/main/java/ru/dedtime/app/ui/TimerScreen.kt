package ru.dedtime.app.ui

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ru.dedtime.app.MainActivity
import ru.dedtime.app.MainVm
import ru.dedtime.app.domain.Rules
import ru.dedtime.app.domain.activeMs
import ru.dedtime.app.domain.sessionBaseXp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimerScreen(vm: MainVm) {
    val cats by vm.categories.collectAsStateWithLifecycle()
    val active by vm.active.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    val now = rememberNow()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selected by rememberSaveable { mutableLongStateOf(-1L) }
    var note by rememberSaveable { mutableStateOf("") }
    var savedXp by remember { mutableLongStateOf(-1L) }
    var addCat by remember { mutableStateOf(false) }

    LaunchedEffect(cats, active) {
        val a = active
        if (a != null) selected = a.categoryId
        else if (selected < 0 || cats.none { it.id == selected }) {
            selected = vm.repo.lastCategoryId() ?: cats.firstOrNull()?.id ?: -1L
        }
    }

    val s = active
    val ms = s?.activeMs(now) ?: 0L
    val cur = cats.firstOrNull { it.id == selected }
    val color = dc.category(cur)
    val inBlock = ms % Rules.BLOCK_MS
    val left = Rules.BLOCK_MS - inBlock

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle("Таймер")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            cats.forEach { c ->
                Chip(c.name, c.id == selected, dc.category(c)) {
                    selected = c.id
                    if (s != null && s.categoryId != c.id) {
                        // Смена категории во время сессии: закрываем текущую и начинаем новую
                        vm.launch { vm.repo.startTimer(c.id, note) }
                    }
                }
            }
            Chip("+ Своя", false) { addCat = true }
        }

        if (s == null) {
            Field(note, { note = it.take(60) }, "Что делаешь", placeholder = "ТАУ: ЛАЧХ и запасы устойчивости")
        } else if (s.note.isNotBlank()) {
            Text(s.note, style = T.body(16.sp, FontWeight.Bold))
        }

        Box(Modifier.size(260.dp).align(Alignment.CenterHorizontally), contentAlignment = Alignment.Center) {
            val track = dc.line
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 16.dp.toPx()
                val d = size.minDimension - stroke
                val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
                drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(stroke))
                if (inBlock > 0) drawArc(color, -90f, 360f * inBlock / Rules.BLOCK_MS, false, tl, Size(d, d), style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text((cur?.name ?: "Фокус").uppercase(), style = T.label())
                Text(fmtClock(ms), style = T.display(40.sp))
                Text(
                    if (s?.pausedAt != null) "пауза" else "до бонуса ${left / 60_000}:${((left / 1000) % 60).toString().padStart(2, '0')}",
                    style = T.muted(13.sp),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox("+${sessionBaseXp(ms)}", "XP за сессию", Modifier.weight(1f), valueColor = dc.accent)
            StatBox("${ms / Rules.BLOCK_MS}", "блоков по 25 мин", Modifier.weight(1f))
            StatBox(fmtMin(state?.todayFocusMin ?: 0), "сегодня всего", Modifier.weight(1f), valueColor = dc.secondary)
        }
        state?.let { st ->
            if (st.doubleMinutesLeft > 0) Text("Двойной XP: осталось ${st.doubleMinutesLeft} мин фокуса", style = T.body(13.sp, FontWeight.Bold, dc.accent))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val label = when {
                s == null -> "Старт"
                s.pausedAt == null -> "Пауза"
                else -> "Продолжить"
            }
            BigButton(label, {
                savedXp = -1
                if (s == null) {
                    (context as? MainActivity)?.ensureNotificationPermission()
                    if (selected > 0) scope.launch { vm.repo.startTimer(selected, note); note = "" }
                } else vm.launch { vm.repo.togglePause() }
            }, Modifier.weight(1f), enabled = s != null || selected > 0)
            OutlineButton("Стоп", { scope.launch { savedXp = vm.repo.stopTimer() } }, Modifier.width(120.dp), height = 56.dp, enabled = s != null)
        }
        if (savedXp >= 0) {
            Text(
                if (savedXp > 0) "Сессия сохранена: +$savedXp XP в статистику" else "Сессия короче минуты не сохраняется",
                style = T.body(13.sp, FontWeight.Bold, dc.secondary),
            )
        }
        Text("Если таймер идёт больше 3 часов, дед спросит «Ты тут?». Без ответа 10 минут — пауза.", style = T.muted(12.sp))
    }

    if (addCat) CategoryDialog(onDismiss = { addCat = false }) { name, hex ->
        scope.launch { selected = vm.repo.addCategory(name, hex) }; addCat = false
    }
}

private val palette = listOf("accent", "#1D4ED8", "#F59E68", "#93B4F5", "#15803D", "#7E22CE", "#BE185D", "#8A877F")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var hex by remember { mutableStateOf("#15803D") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dc.bg,
        title = { Text("Своя категория", style = T.display(20.sp)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Field(name, { name = it.take(16) }, "Название", placeholder = "Английский")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    palette.forEach { h ->
                        val c = if (h == "accent") dc.accent else Color(android.graphics.Color.parseColor(h))
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(c)
                                .border(3.dp, if (hex == h) dc.ink else Color.Transparent, CircleShape)
                                .clickable(role = Role.RadioButton) { hex = h },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(name.trim(), hex) }) {
                Text("Добавить", style = T.body(15.sp, FontWeight.ExtraBold, dc.accent))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", style = T.body(15.sp, FontWeight.Bold, dc.muted)) } },
    )
}

@Suppress("unused")
private fun keep(a: Activity?) = a
