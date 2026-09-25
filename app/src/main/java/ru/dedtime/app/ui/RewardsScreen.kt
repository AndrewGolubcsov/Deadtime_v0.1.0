package ru.dedtime.app.ui

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ru.dedtime.app.MainVm
import ru.dedtime.app.R
import ru.dedtime.app.data.Reward

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RewardsScreen(vm: MainVm, onShare: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val rewards by vm.rewards.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf<Reward?>(null) }
    var removing by remember { mutableStateOf<Reward?>(null) }
    var adding by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val st = state ?: return
    val builtIn = rewards.filter { !it.isCustom }
    val custom = rewards.filter { it.isCustom }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenTitle("Награды") {
            Row(
                Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFF17171A)).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                DIcon(R.drawable.ic_coin, Color(0xFFF5C451), 18.dp)
                Text(fmtNum(st.coins), style = T.body(15.sp, FontWeight.ExtraBold, Color.White))
            }
        }

        Text("Магазин", style = T.display(17.sp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            builtIn.forEach { r ->
                DCard(Modifier.weight(1f)) {
                    DIcon(if (r.code == "freeze") R.drawable.ic_snow else R.drawable.ic_bolt, if (r.code == "freeze") dc.secondary else dc.accent, 28.dp)
                    Text(r.title, style = T.body(15.sp, FontWeight.ExtraBold))
                    Text(
                        if (r.code == "freeze") "Пропуск дня без потери серии" + (if (st.freezesLeft > 0) " · в запасе ${st.freezesLeft}" else "")
                        else "Минуты фокуса считаются ×2" + (if (st.doubleMinutesLeft > 0) " · осталось ${st.doubleMinutesLeft} мин" else ""),
                        style = T.muted(12.sp),
                    )
                    OutlineButton("${r.price} монет", { confirm = r }, Modifier.fillMaxWidth(), height = 44.dp, enabled = st.coins >= r.price)
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(ded.radius + 4.dp)).background(Color(0xFF17171A)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Свои награды", style = T.body(15.sp, FontWeight.ExtraBold, Color.White), modifier = Modifier.weight(1f))
                LinkText("+ Создать", { adding = true }, Color(0xFFF5C451))
            }
            if (custom.isEmpty()) Text("Придумай, чем себя порадовать: серия сериала, пицца, новая игра.", style = T.body(13.sp, FontWeight.Medium, Color(0xFFD6D3CC)))
            custom.forEach { r ->
                Row(
                    Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp)
                        .combinedClickable(onClick = { if (st.coins >= r.price) { confirm = r } else { Toast.makeText(context, "Не хватает монет", Toast.LENGTH_SHORT).show() } }, onLongClick = { removing = r }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(r.title, style = T.body(14.sp, FontWeight.SemiBold, Color.White), modifier = Modifier.weight(1f))
                    Text("${r.price}", style = T.body(14.sp, FontWeight.ExtraBold, if (st.coins >= r.price) Color(0xFFF5C451) else Color(0xFF8A877F)))
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ачивки", style = T.display(17.sp), modifier = Modifier.weight(1f))
            Text("${st.achievements.count { it.unlocked }} из ${st.achievements.size}", style = T.body(14.sp, FontWeight.ExtraBold, dc.muted))
            Spacer(Modifier.height(1.dp).padding(start = 8.dp))
            LinkText("  Поделиться", onShare)
        }
        st.achievements.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { a ->
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(ded.radius + 2.dp)).background(if (a.unlocked) dc.surface else dc.seg).padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Art(achievementRes(a.def.id), 56.dp, locked = !a.unlocked, description = a.def.title)
                        Text(a.def.title, style = T.body(13.sp, FontWeight.ExtraBold), textAlign = TextAlign.Center, maxLines = 2)
                        Text(a.def.description, style = T.muted(11.sp), textAlign = TextAlign.Center, minLines = 2, maxLines = 2)
                        if (a.unlocked) Text("получено", style = T.body(11.sp, FontWeight.ExtraBold, dc.secondary))
                        else {
                            ProgressBar(a.current.toFloat() / a.target, dc.muted, height = 5.dp)
                            Text("${a.current}/${a.target}", style = T.muted(11.sp))
                        }
                    }
                }
                repeat(3 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    confirm?.let { r ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            containerColor = dc.bg,
            title = { Text(r.title, style = T.display(18.sp)) },
            text = { Text("Потратить ${r.price} монет? Останется ${fmtNum(st.coins - r.price)}.", style = T.body(15.sp)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    scope.launch {
                        val ok = vm.repo.buy(r)
                        Toast.makeText(context, if (ok) "Куплено! Заслужил." else "Не хватает монет", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Купить", style = T.body(15.sp, FontWeight.ExtraBold, dc.accent)) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Отмена", style = T.body(15.sp, FontWeight.Bold, dc.muted)) } },
        )
    }
    removing?.let { r ->
        AlertDialog(
            onDismissRequest = { removing = null },
            containerColor = dc.bg,
            title = { Text("Удалить награду?", style = T.display(18.sp)) },
            text = { Text("«${r.title}» пропадёт из магазина. Прошлые покупки останутся.", style = T.body(15.sp)) },
            confirmButton = { TextButton(onClick = { vm.launch { vm.repo.deleteCustomReward(r.id) }; removing = null }) { Text("Удалить", style = T.body(15.sp, FontWeight.ExtraBold, dc.accent)) } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Отмена", style = T.body(15.sp, FontWeight.Bold, dc.muted)) } },
        )
    }
    if (adding) {
        var title by remember { mutableStateOf("") }
        var price by remember { mutableStateOf("150") }
        AlertDialog(
            onDismissRequest = { adding = false },
            containerColor = dc.bg,
            title = { Text("Своя награда", style = T.display(18.sp)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Field(title, { title = it.take(40) }, "Что получишь", placeholder = "Серия любимого сериала")
                    Field(price, { price = it.filter(Char::isDigit).take(5) }, "Цена в монетах", keyboardNumber = true)
                }
            },
            confirmButton = {
                TextButton(enabled = title.isNotBlank() && (price.toIntOrNull() ?: 0) > 0, onClick = {
                    vm.launch { vm.repo.addCustomReward(title.trim(), price.toInt()) }; adding = false
                }) { Text("Добавить", style = T.body(15.sp, FontWeight.ExtraBold, dc.accent)) }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Отмена", style = T.body(15.sp, FontWeight.Bold, dc.muted)) } },
        )
    }
}
