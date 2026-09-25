package ru.dedtime.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import ru.dedtime.app.MainVm
import ru.dedtime.app.R
import ru.dedtime.app.data.Habit
import ru.dedtime.app.data.Profile
import ru.dedtime.app.domain.Days
import ru.dedtime.app.domain.Engine
import ru.dedtime.app.domain.activeMs
import ru.dedtime.app.domain.sessionBaseXp
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun rememberNow(periodMs: Long = 1000): Long {
    val now by produceState(System.currentTimeMillis()) {
        while (true) { value = System.currentTimeMillis(); delay(periodMs) }
    }
    return now
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodayScreen(vm: MainVm, profile: Profile, onSettings: () -> Unit, onTimer: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val habits by vm.habits.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val active by vm.active.collectAsStateWithLifecycle()
    val cats by vm.categories.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Habit?>(null) }
    var adding by remember { mutableStateOf(false) }
    val now = rememberNow()
    val st = state ?: return
    val todayKey = Days.key(st.today)
    val values = logs.filter { it.date == todayKey }.associate { it.habitId to it.value }

    // Анимация «+XP»
    var lastXp by remember { mutableLongStateOf(-1L) }
    var gained by remember { mutableLongStateOf(0L) }
    val anim = ded.anim
    LaunchedEffect(st.xp) {
        if (lastXp >= 0 && st.xp > lastXp && anim) { gained = st.xp - lastXp; delay(1400); gained = 0 }
        lastXp = st.xp
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (ded.compact) 10.dp else 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("ru")).format(st.today).replaceFirstChar { it.uppercase() }, style = T.muted(14.sp))
                    Text("Привет, ${profile.name}", style = T.display(24.sp))
                }
                Box(
                    Modifier.size(44.dp).clip(CircleShape).border(2.dp, dc.ink, CircleShape).background(dc.surface)
                        .clickable(role = Role.Button, onClick = onSettings),
                    contentAlignment = Alignment.Center,
                ) { DIcon(R.drawable.ic_gear, size = 22.dp, description = "Настройки") }
            }

            // Уровень
            val span = (st.levelCeil - st.levelFloor).coerceAtLeast(1)
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(ded.radius + 6.dp)).background(Color(0xFF17171A)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Уровень ${st.level}", style = T.display(20.sp).copy(color = Color.White), modifier = Modifier.weight(1f))
                    Text("${fmtNum(st.xp)} / ${fmtNum(st.levelCeil)} XP", style = T.body(14.sp, FontWeight.SemiBold, Color(0xFFD6D3CC)))
                }
                ProgressBar((st.xp - st.levelFloor).toFloat() / span, dc.accent, track = Color(0xFF3A3A3F), height = 12.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MiniStat(R.drawable.ic_flame, Color(0xFFF59E68), "${st.streak} ${plural(st.streak.toLong(), "день", "дня", "дней")}", "серия", Modifier.weight(1f))
                    MiniStat(R.drawable.ic_coin, Color(0xFFF5C451), fmtNum(st.coins), "монет", Modifier.weight(1f))
                }
            }

            // Задание дня
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(ded.radius + 2.dp)).background(dc.secondaryTint).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DIcon(R.drawable.ic_gift, dc.secondary, 28.dp)
                Column(Modifier.weight(1f)) {
                    Text("Задание дня" + if (st.quest.done) " · выполнено" else "", style = T.body(15.sp, FontWeight.ExtraBold))
                    Text("${st.quest.title} → +50 XP, +10 монет", style = T.body(13.sp, FontWeight.SemiBold, dc.secondary))
                }
                Text(st.quest.progress, style = T.body(13.sp, FontWeight.ExtraBold, dc.secondary))
            }

            // Идущий таймер
            active?.let { s ->
                val cat = cats.firstOrNull { it.id == s.categoryId }
                val ms = s.activeMs(now)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(ded.radius + 2.dp)).background(dc.surface)
                        .border(2.dp, dc.accent, RoundedCornerShape(ded.radius + 2.dp)).clickable(onClick = onTimer).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(dc.category(cat)))
                    Column(Modifier.weight(1f)) {
                        Text(listOfNotNull(cat?.name, s.note.takeIf { it.isNotBlank() }).joinToString(" · "), style = T.body(15.sp, FontWeight.ExtraBold), maxLines = 1)
                        Text((if (s.pausedAt != null) "пауза" else "таймер идёт") + " · +${sessionBaseXp(ms)} XP", style = T.muted(12.sp))
                    }
                    Text(fmtClock(ms), style = T.display(18.sp).copy(color = dc.accent))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Привычки", style = T.display(17.sp), modifier = Modifier.weight(1f))
                LinkText("+ Добавить", { adding = true })
            }
            if (habits.isEmpty()) {
                DCard { Text("Пока пусто. Добавь первую привычку — дед проследит.", style = T.muted(14.sp)) }
            }
            habits.forEach { h ->
                val v = values[h.id] ?: 0
                val done = Engine.habitDone(h, v)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(ded.radius)).background(dc.surface)
                        .combinedClickable(
                            onClick = { if (h.kind == Habit.KIND_CHECK) { vm.launch { vm.repo.tapHabit(h.id) } } else { editing = h } },
                            onLongClick = { editing = h },
                        )
                        .padding(horizontal = 12.dp, vertical = if (ded.compact) 6.dp else 10.dp)
                        .alpha(if (done) 0.7f else 1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val box = Modifier.size(44.dp).clip(RoundedCornerShape((ded.radius - 2.dp).coerceAtLeast(4.dp)))
                    if (h.kind == Habit.KIND_CHECK) {
                        Box(
                            (if (done) box.background(dc.ink) else box.border(2.dp, dc.ink, RoundedCornerShape((ded.radius - 2.dp).coerceAtLeast(4.dp))))
                                .clickable(role = Role.Checkbox) { vm.launch { vm.repo.tapHabit(h.id) } },
                            contentAlignment = Alignment.Center,
                        ) { if (done) DIcon(R.drawable.ic_check, dc.onInk, 20.dp, "Отмечено") }
                    } else {
                        Box(
                            box.border(2.dp, dc.ink, RoundedCornerShape((ded.radius - 2.dp).coerceAtLeast(4.dp)))
                                .background(if (done) dc.ink else Color.Transparent)
                                .clickable(role = Role.Button) { vm.launch { vm.repo.tapHabit(h.id) } },
                            contentAlignment = Alignment.Center,
                        ) { DIcon(R.drawable.ic_plus, if (done) dc.onInk else dc.ink, 20.dp, "Добавить") }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val title = if (h.kind == Habit.KIND_COUNTER) "${h.title} $v / ${h.target}" else h.title
                        Text(title, style = T.body(15.sp, FontWeight.Bold).copy(textDecoration = if (done) TextDecoration.LineThrough else null))
                        if (h.kind == Habit.KIND_COUNTER) ProgressBar(v.toFloat() / h.target, dc.secondary, height = 6.dp)
                    }
                    Text("+${h.xp} XP", style = T.body(14.sp, FontWeight.ExtraBold, if (done) dc.muted else dc.accent))
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        AnimatedVisibility(
            visible = gained > 0,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp),
        ) {
            Text(
                "+$gained XP",
                style = T.display(22.sp).copy(color = Color.White),
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(dc.accent).padding(horizontal = 18.dp, vertical = 8.dp),
            )
        }
    }

    if (adding) HabitDialog(null, onDismiss = { adding = false }) { title, kind, target, xp ->
        vm.launch { vm.repo.addHabit(title, kind, target, xp) }; adding = false
    }
    editing?.let { h ->
        HabitDialog(h, onDismiss = { editing = null }, onArchive = { vm.launch { vm.repo.archiveHabit(h) }; editing = null },
            onMinus = if (h.kind == Habit.KIND_COUNTER) ({ vm.launch { vm.repo.decHabit(h.id) } }) else null,
        ) { title, kind, target, xp ->
            vm.launch { vm.repo.updateHabit(h.copy(title = title, kind = kind, target = target, xp = xp)) }; editing = null
        }
    }
}

@Composable
private fun MiniStat(icon: Int, tint: Color, value: String, label: String, modifier: Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF2A2A2F)).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DIcon(icon, tint, 22.dp)
        Column {
            Text(value, style = T.body(17.sp, FontWeight.ExtraBold, Color.White))
            Text(label, style = T.body(12.sp, FontWeight.SemiBold, Color(0xFFD6D3CC)))
        }
    }
}

@Composable
fun HabitDialog(
    habit: Habit?,
    onDismiss: () -> Unit,
    onArchive: (() -> Unit)? = null,
    onMinus: (() -> Unit)? = null,
    onSave: (String, String, Int, Int) -> Unit,
) {
    var title by remember { mutableStateOf(habit?.title ?: "") }
    var kind by remember { mutableStateOf(habit?.kind ?: Habit.KIND_CHECK) }
    var target by remember { mutableStateOf((habit?.target ?: 8).toString()) }
    var xp by remember { mutableStateOf((habit?.xp ?: 20).toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dc.bg,
        title = { Text(if (habit == null) "Новая привычка" else "Привычка", style = T.display(20.sp)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Field(title, { title = it }, "Название", placeholder = "Зарядка 10 минут")
                Segmented(listOf(Habit.KIND_CHECK to "Галочка", Habit.KIND_COUNTER to "Счётчик"), kind, { kind = it })
                if (kind == Habit.KIND_COUNTER) Field(target, { target = it.filter(Char::isDigit).take(3) }, "Цель в день", keyboardNumber = true)
                Field(xp, { xp = it.filter(Char::isDigit).take(3) }, "XP за выполнение", keyboardNumber = true)
                if (onMinus != null) OutlineButton("−1 к счётчику сегодня", onMinus, Modifier.fillMaxWidth(), height = 44.dp)
                if (onArchive != null) LinkText("Убрать в архив", onArchive, dc.accent)
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = { onSave(title.trim(), kind, target.toIntOrNull()?.coerceAtLeast(1) ?: 1, xp.toIntOrNull()?.coerceIn(1, 500) ?: 20) },
            ) { Text("Сохранить", style = T.body(15.sp, FontWeight.ExtraBold, dc.accent)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", style = T.body(15.sp, FontWeight.Bold, dc.muted)) } },
    )
}

@Suppress("unused")
private val keepWidth = Modifier.width(1.dp)

@Suppress("unused")
private val keepBorder = BorderStroke(1.dp, Color.Black)
