package ru.dedtime.app.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.dedtime.app.BuildConfig
import ru.dedtime.app.MainVm
import ru.dedtime.app.R
import ru.dedtime.app.data.BackupPreview
import ru.dedtime.app.data.Profile
import ru.dedtime.app.data.Repository
import ru.dedtime.app.work.Work

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: MainVm, profile: Profile, onBack: () -> Unit, onAbout: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val s = settings ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var lastFile by remember { mutableStateOf<Pair<Uri, String>?>(null) }
    var preview by remember { mutableStateOf<BackupPreview?>(null) }
    var editProfile by remember { mutableStateOf(false) }

    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val text = vm.repo.exportJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
                }
                vm.updateSettings { it.copy(lastBackupAt = System.currentTimeMillis()) }
                lastFile = uri to "${text.toByteArray().size / 1024 + 1} КБ"
            } catch (e: Exception) {
                Toast.makeText(context, "Не удалось сохранить: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                }
                preview = vm.repo.parseBackup(text)
            } catch (e: IllegalArgumentException) {
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }
    val tree = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        vm.updateSettings { it.copy(autoBackup = true, backupTree = uri.toString()) }
        Work.setAutoBackup(context, true)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenTitle("Настройки", onBack = onBack)

        DCard(onClick = { editProfile = true }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Art(avatarRes(profile.avatarId), 48.dp)
                Column(Modifier.weight(1f)) {
                    Text(profile.name, style = T.body(16.sp, FontWeight.ExtraBold))
                    Text("Имя и напарник", style = T.muted())
                }
                DIcon(R.drawable.ic_chevron, dc.muted, 20.dp)
            }
        }

        SectionLabel("Предпросмотр")
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(ded.radius)).background(dc.surface)
                .border(2.dp, dc.accent, RoundedCornerShape(ded.radius))
                .padding(horizontal = 14.dp, vertical = if (ded.compact) 8.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape((ded.radius - 2.dp).coerceAtLeast(4.dp))).background(dc.accent), contentAlignment = Alignment.Center) {
                DIcon(R.drawable.ic_check, Color.White, 18.dp)
            }
            Column(Modifier.weight(1f)) {
                Text("Прочитать 15 страниц", style = T.body(16.sp, FontWeight.Bold))
                Text("серия 5 дней", style = T.muted(12.sp))
            }
            Text("+30 XP", style = T.body(14.sp, FontWeight.ExtraBold, dc.accent))
        }

        SectionLabel("Оформление")
        DCard {
            Text("Тема", style = T.body(14.sp, FontWeight.ExtraBold))
            Segmented(listOf("light" to "Светлая", "warm" to "Тёплая", "dark" to "Тёмная", "system" to "Система"), s.theme, { t -> vm.updateSettings { it.copy(theme = t) } })
            Text("Акцент", style = T.body(14.sp, FontWeight.ExtraBold))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Accents.forEach { (hex, name) ->
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(Color(hex.toInt()))
                            .border(4.dp, if (s.accent == hex) dc.ink else dc.surface, CircleShape)
                            .clickable(role = Role.RadioButton, onClickLabel = name) { vm.updateSettings { it.copy(accent = hex) } },
                    )
                }
            }
            Text("Шрифт", style = T.body(14.sp, FontWeight.ExtraBold))
            Segmented(listOf("manrope" to "Manrope", "onest" to "Onest", "serif" to "Засечки"), s.font, { f -> vm.updateSettings { it.copy(font = f) } })
            Text("Размер текста", style = T.body(14.sp, FontWeight.ExtraBold))
            Segmented(listOf(0.9f to "Мельче", 1f to "Обычный", 1.15f to "Крупнее"), s.textScale, { v -> vm.updateSettings { it.copy(textScale = v) } })
            Text("Углы", style = T.body(14.sp, FontWeight.ExtraBold))
            Segmented(listOf(4 to "Острые", 14 to "Мягкие", 24 to "Круглые"), s.radius, { v -> vm.updateSettings { it.copy(radius = v) } })
            ToggleRow("Анимация очков", "всплывающие +XP", s.anim) { v -> vm.updateSettings { it.copy(anim = v) } }
            ToggleRow("Компактные карточки", "больше привычек на экране", s.compact) { v -> vm.updateSettings { it.copy(compact = v) } }
        }

        SectionLabel("Данные")
        DCard {
            BigButton("Создать резервную копию", { create.launch(Repository.backupFileName()) }, Modifier.fillMaxWidth(), color = dc.accent, textColor = Color.White, icon = R.drawable.ic_download, height = 52.dp)
            lastFile?.let { (uri, size) ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(ded.radius)).background(dc.seg).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DIcon(R.drawable.ic_file, dc.ink, 28.dp)
                    Column(Modifier.weight(1f)) {
                        Text(Repository.backupFileName(), style = T.body(14.sp, FontWeight.Bold), maxLines = 1)
                        Text("$size · сохранено", style = T.muted(12.sp))
                    }
                    BigButton("Отправить", {
                        val send = Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_STREAM, uri)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        context.startActivity(Intent.createChooser(send, "Отправить копию"))
                    }, height = 44.dp)
                }
            }
            OutlineButton("Восстановить из файла", { open.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, Modifier.fillMaxWidth(), height = 48.dp)
            ToggleRow(
                "Автокопия раз в неделю",
                "последняя: " + if (s.lastBackupAt > 0) formatDay(s.lastBackupAt) else "ещё не было",
                s.autoBackup,
            ) { on ->
                if (on && s.backupTree == null) tree.launch(null)
                else {
                    vm.updateSettings { it.copy(autoBackup = on) }
                    Work.setAutoBackup(context, on)
                }
            }
            if (s.autoBackup) Text("Копии складываются в выбранную папку, хранятся последние 4.", style = T.muted(12.sp))
        }

        DCard(onClick = onAbout) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("О приложении", style = T.body(16.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f))
                Text(BuildConfig.VERSION_NAME, style = T.muted())
                Spacer(Modifier.size(8.dp))
                DIcon(R.drawable.ic_chevron, dc.ink, 20.dp)
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    preview?.let { p ->
        AlertDialog(
            onDismissRequest = { preview = null },
            containerColor = dc.bg,
            title = { Text("Восстановить копию?", style = T.display(18.sp)) },
            text = {
                Text(
                    "${p.name}, копия от ${formatDateTime(p.createdAt)}.\nУровень ${p.level}, серия ${p.streak} дн., ${p.sessions} сессий, ${fmtNum(p.coins)} монет.\n\nТекущие данные на телефоне будут заменены.",
                    style = T.body(15.sp),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    preview = null
                    scope.launch {
                        vm.repo.restore(p.file)
                        Toast.makeText(context, "Восстановлено. С возвращением, ${p.name}!", Toast.LENGTH_LONG).show()
                    }
                }) { Text("Восстановить", style = T.body(15.sp, FontWeight.ExtraBold, dc.accent)) }
            },
            dismissButton = { TextButton(onClick = { preview = null }) { Text("Отмена", style = T.body(15.sp, FontWeight.Bold, dc.muted)) } },
        )
    }

    if (editProfile) {
        var name by remember { mutableStateOf(profile.name) }
        var avatar by remember { mutableStateOf(profile.avatarId) }
        AlertDialog(
            onDismissRequest = { editProfile = false },
            containerColor = dc.bg,
            title = { Text("Профиль", style = T.display(18.sp)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Field(name, { if (it.length <= 20) name = it }, "Имя")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Avatars.forEach { (id, title, _) ->
                            Box(
                                Modifier.size(64.dp).clip(RoundedCornerShape(14.dp))
                                    .background(if (avatar == id) dc.accentTint else dc.surface)
                                    .border(if (avatar == id) 3.dp else 1.dp, if (avatar == id) dc.accent else dc.line, RoundedCornerShape(14.dp))
                                    .clickable(role = Role.RadioButton, onClickLabel = title) { avatar = id },
                                contentAlignment = Alignment.Center,
                            ) { Art(avatarRes(id), 48.dp, description = title) }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.launch { vm.repo.updateProfile(name.trim(), avatar) }; editProfile = false }) {
                    Text("Сохранить", style = T.body(15.sp, FontWeight.ExtraBold, dc.accent))
                }
            },
            dismissButton = { TextButton(onClick = { editProfile = false }) { Text("Отмена", style = T.body(15.sp, FontWeight.Bold, dc.muted)) } },
        )
    }
}
