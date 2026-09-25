package ru.dedtime.app.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.dedtime.app.MainVm
import ru.dedtime.app.R
import ru.dedtime.app.data.Category
import ru.dedtime.app.data.Profile
import ru.dedtime.app.domain.AchievementState
import ru.dedtime.app.domain.Engine
import ru.dedtime.app.domain.GameState
import java.io.File
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class CardStyle(val bg: Color, val ink: Color, val muted: Color, val panel: Color)

private val styles = mapOf(
    "dark" to CardStyle(Color(0xFF17171A), Color.White, Color(0xFFD6D3CC), Color(0xFF2A2A2F)),
    "light" to CardStyle(Color(0xFFF3EFE6), Color(0xFF17171A), Color(0xFF5B5A57), Color.White),
    "peach" to CardStyle(Color(0xFFFBE3D6), Color(0xFF17171A), Color(0xFF5B5A57), Color(0xFFFFF4EC)),
)

@Composable
fun ShareScreen(vm: MainVm, profile: Profile, mode: String, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val cats by vm.categories.collectAsStateWithLifecycle()
    val st = state ?: return
    var kind by rememberSaveable { mutableStateOf(mode) }
    var bg by rememberSaveable { mutableStateOf("dark") }
    val unlocked = st.achievements.filter { it.unlocked }.sortedByDescending { it.unlockedAt }
    var achId by rememberSaveable { mutableStateOf(unlocked.firstOrNull()?.def?.id) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    val style = styles.getValue(bg)
    val accent = dc.accent

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenTitle("Поделиться", onBack = onBack)
        Segmented(listOf("day" to "Итог дня", "ach" to "Ачивка"), kind, { kind = it })

        Box(
            Modifier.align(Alignment.CenterHorizontally)
                .drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                },
        ) {
            Column(
                Modifier.width(300.dp).height(533.dp).clip(RoundedCornerShape(28.dp)).background(style.bg).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LogoTile(40.dp)
                    Text(
                        buildAnnotatedString { append("Дед"); withStyle(SpanStyle(color = accent)) { append("тайм") } },
                        style = T.display(18.sp).copy(color = style.ink),
                    )
                }
                if (kind == "day") DayContent(st, cats, profile, style, accent)
                else AchContent(unlocked.firstOrNull { it.def.id == achId }, profile, style, accent)
            }
        }

        if (kind == "ach") {
            if (unlocked.isEmpty()) Text("Пока нет полученных ачивок. Первая — за первую сессию таймера.", style = T.muted())
            else Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                unlocked.forEach { a ->
                    Box(
                        Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(dc.surface)
                            .border(if (a.def.id == achId) 3.dp else 1.dp, if (a.def.id == achId) dc.accent else dc.line, RoundedCornerShape(14.dp))
                            .clickable(role = Role.RadioButton, onClickLabel = a.def.title) { achId = a.def.id },
                        contentAlignment = Alignment.Center,
                    ) { Art(achievementRes(a.def.id), 48.dp, description = a.def.title) }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Фон", style = T.body(14.sp, FontWeight.ExtraBold))
            styles.forEach { (k, v) ->
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(v.bg)
                        .border(if (bg == k) 3.dp else 1.dp, if (bg == k) dc.accent else dc.line, CircleShape)
                        .clickable(role = Role.RadioButton) { bg = k },
                )
            }
        }

        val canShare = kind == "day" || unlocked.isNotEmpty()
        BigButton("Поделиться", {
            scope.launch {
                val file = render(context, layer) ?: return@launch
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(Intent.createChooser(send, "Поделиться"))
            }
        }, Modifier.fillMaxWidth(), icon = R.drawable.ic_share, enabled = canShare)
        OutlineButton("Сохранить в галерею", {
            scope.launch {
                val file = render(context, layer) ?: return@launch
                val ok = saveToGallery(context, file)
                Toast.makeText(context, if (ok) "Сохранено в галерею" else "Не получилось сохранить", Toast.LENGTH_SHORT).show()
            }
        }, Modifier.fillMaxWidth(), enabled = canShare)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ColumnScope.DayContent(st: GameState, cats: List<Category>, profile: Profile, s: CardStyle, accent: Color) {
    Text(
        DateTimeFormatter.ofPattern("d MMMM", Locale("ru")).format(st.today) + " · итог дня",
        style = T.body(13.sp, FontWeight.Bold, s.muted),
    )
    Text(Engine.minutesLabel(st.todayFocusMin), style = T.display(44.sp).copy(color = s.ink))
    Text("в фокусе", style = T.body(14.sp, FontWeight.Bold, s.muted))
    val parts = cats.map { dc.category(it) to (st.todayFocus[it.id] ?: 0) }.filter { it.second > 0 }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Donut(parts, Modifier.size(96.dp), s.panel, stroke = 30f)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            cats.filter { (st.todayFocus[it.id] ?: 0) > 0 }.take(4).forEach { c ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(dc.category(c)))
                    Text("${c.name} ${Engine.minutesLabel(st.todayFocus[c.id] ?: 0)}", style = T.body(12.sp, FontWeight.Bold, s.ink))
                }
            }
        }
    }
    Spacer(Modifier.weight(1f))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill("+${fmtNum(st.dayXp[st.today] ?: 0L)}", "XP", s, accent, Modifier.weight(1f))
        Pill("${st.streak}", "дней серии", s, s.ink, Modifier.weight(1f))
        Pill("${st.todayHabitsDone}/${st.habitsTotal}", "привычек", s, s.ink, Modifier.weight(1f))
    }
    Footer(profile, s)
}

@Composable
private fun ColumnScope.AchContent(a: AchievementState?, profile: Profile, s: CardStyle, accent: Color) {
    Spacer(Modifier.weight(0.6f))
    if (a == null) {
        Text("Ачивок пока нет", style = T.display(20.sp).copy(color = s.ink))
    } else {
        Box(
            Modifier.align(Alignment.CenterHorizontally).size(170.dp).clip(CircleShape).background(s.panel),
            contentAlignment = Alignment.Center,
        ) { Art(achievementRes(a.def.id), 130.dp) }
        Text("Новая ачивка".uppercase(), style = T.body(12.sp, FontWeight.ExtraBold, accent), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(a.def.title, style = T.display(26.sp).copy(color = s.ink), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(a.def.description, style = T.body(14.sp, FontWeight.Bold, s.muted), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
    Spacer(Modifier.weight(1f))
    Footer(profile, s)
}

@Composable
private fun Pill(value: String, label: String, s: CardStyle, color: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(s.panel).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = T.display(15.sp).copy(color = color), maxLines = 1)
        Text(label, style = T.body(10.sp, FontWeight.Bold, s.muted), maxLines = 1)
    }
}

@Composable
private fun Footer(profile: Profile, s: CardStyle) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Art(avatarRes(profile.avatarId), 28.dp)
        Text(profile.name, style = T.body(13.sp, FontWeight.ExtraBold, s.ink), modifier = Modifier.weight(1f))
        Text("дед засчитал", style = T.body(11.sp, FontWeight.Bold, s.muted))
    }
}

private suspend fun render(context: Context, layer: GraphicsLayer): File? = try {
    val bmp = layer.toImageBitmap().asAndroidBitmap()
    withContext(Dispatchers.IO) {
        val scaled = Bitmap.createScaledBitmap(bmp, 1080, 1920, true)
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val f = File(dir, "dedtime_${System.currentTimeMillis()}.png")
        FileOutputStream(f).use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
        f
    }
} catch (e: Exception) {
    Toast.makeText(context, "Не удалось сделать картинку", Toast.LENGTH_SHORT).show()
    null
}

private suspend fun saveToGallery(context: Context, file: File): Boolean = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT < 29) {
        // На Android 8–9 нужен доступ к памяти — проще отдать через «Поделиться»
        withContext(Dispatchers.Main) {
            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
            val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(Intent.createChooser(send, "Сохранить"))
        }
        return@withContext true
    }
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Dedtime")
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext false
    context.contentResolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } } != null
}
