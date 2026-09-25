package ru.dedtime.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.dedtime.app.BuildConfig
import ru.dedtime.app.R

private const val TG = "https://t.me/andreew_work"
private const val MAIL = "goland555@yandex.ru"
private const val GITHUB = "https://github.com/AndrewGolubcsov"

private fun open(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Нет приложения, чтобы открыть это", Toast.LENGTH_SHORT).show()
    }
}

private fun mail(context: Context, subject: String, body: String) {
    val uri = Uri.parse("mailto:$MAIL?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}")
    open(context, Intent(Intent.ACTION_SENDTO, uri))
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val version = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenTitle("О приложении", onBack = onBack)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LogoTile(96.dp)
            Wordmark(30)
            Text("дед знает, сколько у тебя времени", style = T.muted(14.sp), textAlign = TextAlign.Center)
            Text("Версия $version", style = T.body(13.sp, FontWeight.Bold, dc.muted))
        }

        DCard {
            Text("Что нового", style = T.body(15.sp, FontWeight.ExtraBold))
            listOf(
                "Первый выпуск: привычки, таймер по категориям, статистика",
                "Магазин, свои награды и 12 ачивок",
                "Резервные копии и 7 виджетов",
            ).forEach { Text("• $it", style = T.body(14.sp, FontWeight.Medium)) }
        }

        DCard {
            Text("Разработчик — Андрей", style = T.body(15.sp, FontWeight.ExtraBold))
            ContactRow(R.drawable.ic_telegram, "Telegram", "@andreew_work") { open(context, Intent(Intent.ACTION_VIEW, Uri.parse(TG))) }
            HorizontalDivider(color = dc.line)
            ContactRow(R.drawable.ic_mail, "Почта", MAIL) { mail(context, "Дедтайм", "") }
            HorizontalDivider(color = dc.line)
            ContactRow(R.drawable.ic_github, "GitHub", "github.com/AndrewGolubcsov") { open(context, Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB))) }
        }

        BigButton("Сообщить об ошибке", {
            mail(
                context, "Дедтайм: ошибка",
                "Что случилось:\n\n\nЧто ожидал:\n\n\n---\nВерсия: $version\nТелефон: ${Build.MANUFACTURER} ${Build.MODEL}\nAndroid: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            )
        }, Modifier.fillMaxWidth(), icon = R.drawable.ic_bug)
        OutlineButton("Предложить идею", { mail(context, "Дедтайм: идея", "") }, Modifier.fillMaxWidth())
        Text(
            "Все данные хранятся только на этом телефоне. Никаких аккаунтов и серверов — для переноса используй резервную копию.",
            style = T.muted(12.sp), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ContactRow(icon: Int, title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DIcon(icon, dc.secondary, 22.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = T.muted(12.sp))
            Text(value, style = T.body(15.sp, FontWeight.Bold))
        }
        DIcon(R.drawable.ic_chevron, dc.muted, 18.dp)
    }
}
