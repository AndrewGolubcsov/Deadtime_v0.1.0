package ru.dedtime.app.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.dedtime.app.MainVm
import ru.dedtime.app.R
import ru.dedtime.app.data.BackupPreview
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun Wordmark(size: Int) {
    Text(
        buildAnnotatedString {
            append("Дед")
            withStyle(SpanStyle(color = dc.accent)) { append("тайм") }
        },
        style = T.display(size.sp),
    )
}

@Composable
fun OnboardingScreen(vm: MainVm) {
    var step by rememberSaveable { mutableStateOf("start") }
    var name by rememberSaveable { mutableStateOf("") }
    var avatar by rememberSaveable { mutableStateOf("pelmen") }
    var preview by remember { mutableStateOf<BackupPreview?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                }
                preview = vm.repo.parseBackup(text); error = null
            } catch (e: IllegalArgumentException) {
                preview = null; error = e.message
            }
        }
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        when (step) {
            "start" -> {
                Spacer(Modifier.height(40.dp))
                LogoTile(112.dp)
                Wordmark(34)
                Text("Привет! Начнём?", style = T.display(28.sp))
                Text("Привычки, таймер и очки за каждую минуту, потраченную с толком.", style = T.muted(15.sp))
                Spacer(Modifier.height(80.dp))
                BigButton("Я новенький", { step = "new" }, Modifier.fillMaxWidth())
                OutlineButton("Восстановить из копии", { step = "restore" }, Modifier.fillMaxWidth(), height = 56.dp)
            }
            "new" -> {
                ScreenTitle("Как тебя звать?", onBack = { step = "start" })
                Field(name, { if (it.length <= 20) name = it }, "Имя", placeholder = "Андрей")
                Text("Выбери напарника", style = T.body(13.sp, FontWeight.ExtraBold, dc.muted))
                Avatars.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { (id, title, _) ->
                            val on = avatar == id
                            Column(
                                Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                                    .background(if (on) dc.accentTint else dc.surface)
                                    .border(if (on) 3.dp else 2.dp, if (on) dc.accent else dc.line, RoundedCornerShape(16.dp))
                                    .clickable(role = Role.RadioButton) { avatar = id }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Art(avatarRes(id), 52.dp, description = title)
                                Text(title, style = T.body(12.sp, FontWeight.ExtraBold))
                            }
                        }
                    }
                }
                DCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Art(avatarRes(avatar), 44.dp)
                        val a = Avatars.first { it.first == avatar }
                        Text("${a.second}. ${a.third}", style = T.body(14.sp))
                    }
                }
                BigButton("Готово", { step = "ready" }, Modifier.fillMaxWidth())
            }
            "ready" -> {
                Spacer(Modifier.height(60.dp))
                Box(
                    Modifier.align(Alignment.CenterHorizontally).size(160.dp).clip(CircleShape).background(dc.accentTint),
                    contentAlignment = Alignment.Center,
                ) { Art(avatarRes(avatar), 110.dp) }
                Text("Поехали, ${name.trim().ifEmpty { "Друг" }}!", style = T.display(26.sp), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text(
                    "Первая привычка уже добавлена: «Выпить стакан воды». Отметь её и получи первые 10 XP.",
                    style = T.muted(15.sp), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(60.dp))
                BigButton("На главный экран", { vm.launch { vm.repo.createProfile(name.trim(), avatar) } }, Modifier.fillMaxWidth(), color = dc.accent, textColor = androidx.compose.ui.graphics.Color.White)
            }
            "restore" -> {
                ScreenTitle("Восстановление", onBack = { step = "start"; preview = null; error = null })
                Text("Выбери файл копии: из загрузок, Telegram или облачного диска.", style = T.muted(15.sp))
                OutlineButton(if (preview == null) "Выбрать файл копии" else "Выбрать другой файл", { picker.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, Modifier.fillMaxWidth(), height = 64.dp)
                error?.let { Text(it, style = T.body(14.sp, FontWeight.Bold, dc.accent)) }
                preview?.let { p ->
                    DCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Art(avatarRes(p.avatarId), 44.dp)
                            Column {
                                Text(p.name, style = T.body(16.sp, FontWeight.ExtraBold))
                                Text("копия от ${formatDateTime(p.createdAt)}", style = T.muted())
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatBox("${p.level}", "уровень", Modifier.weight(1f), bg = dc.bg)
                            StatBox("${p.streak} дн.", "серия", Modifier.weight(1f), bg = dc.bg)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatBox("${p.sessions}", "сессий таймера", Modifier.weight(1f), bg = dc.bg)
                            StatBox(fmtNum(p.coins), "монет", Modifier.weight(1f), bg = dc.bg)
                        }
                        Text("Файл проверен, данные целые", style = T.body(13.sp, FontWeight.Bold, dc.secondary))
                    }
                    BigButton("Восстановить", {
                        vm.launch {
                            vm.repo.restore(p.file)
                            Toast.makeText(context, "С возвращением, ${p.name}!", Toast.LENGTH_LONG).show()
                        }
                    }, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

fun formatDateTime(millis: Long): String =
    DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale("ru"))
        .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

fun formatDay(millis: Long): String =
    DateTimeFormatter.ofPattern("d MMMM, HH:mm", Locale("ru"))
        .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

@Suppress("unused")
private val keepIcon = R.drawable.ic_file
