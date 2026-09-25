package ru.dedtime.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.dedtime.app.R

// ——— Картинки ———

val Avatars = listOf(
    Triple("pelmen", "Пельмень", "Мягкий снаружи, упорный внутри."),
    Triple("cactus", "Кактус", "Не поливай — просто занимайся."),
    Triple("sock", "Носок", "Потерял пару, но не мотивацию."),
    Triple("toaster", "Тостер", "Разогревается за 25 минут."),
    Triple("shroom", "Гриб", "Растёт в темноте, как твои дедлайны."),
    Triple("rock", "Камень", "Невозмутим к пропущенным дням."),
)

@DrawableRes
fun avatarRes(id: String): Int = when (id) {
    "pelmen" -> R.drawable.avatar_pelmen
    "cactus" -> R.drawable.avatar_cactus
    "sock" -> R.drawable.avatar_sock
    "shroom" -> R.drawable.avatar_shroom
    "rock" -> R.drawable.avatar_rock
    else -> R.drawable.avatar_toaster
}

@DrawableRes
fun achievementRes(id: String): Int = when (id) {
    "first_pancake" -> R.drawable.ach_first_pancake
    "early_bird" -> R.drawable.ach_early_bird
    "night_owl" -> R.drawable.ach_night_owl
    "week" -> R.drawable.ach_week
    "month_fire" -> R.drawable.ach_month_fire
    "deep_dive" -> R.drawable.ach_deep_dive
    "bookworm" -> R.drawable.ach_bookworm
    "iron" -> R.drawable.ach_iron
    "phoenix" -> R.drawable.ach_phoenix
    "miser" -> R.drawable.ach_miser
    "shopaholic" -> R.drawable.ach_shopaholic
    else -> R.drawable.ach_sunday
}

val grayscale: ColorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@Composable
fun Art(@DrawableRes res: Int, size: Dp, modifier: Modifier = Modifier, locked: Boolean = false, description: String? = null) {
    Image(
        painter = painterResource(res),
        contentDescription = description,
        modifier = modifier.size(size).alpha(if (locked) 0.45f else 1f),
        colorFilter = if (locked) grayscale else null,
    )
}

@Composable
fun LogoTile(size: Dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.28f)).background(Color(0xFF17171A)),
        contentAlignment = Alignment.Center,
    ) { Art(R.drawable.logo_ded, size * 0.82f, description = "Дедтайм") }
}

@Composable
fun DIcon(@DrawableRes res: Int, tint: Color = dc.ink, size: Dp = 22.dp, description: String? = null) {
    Icon(painterResource(res), contentDescription = description, tint = tint, modifier = Modifier.size(size))
}

// ——— Контейнеры ———

@Composable
fun DCard(
    modifier: Modifier = Modifier,
    color: Color = dc.surface,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    padding: Dp = if (ded.compact) 12.dp else 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(ded.radius + 4.dp)
    var m = modifier.fillMaxWidth().clip(shape).background(color)
    if (border != null) m = m.border(border, shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m.padding(padding), verticalArrangement = Arrangement.spacedBy(if (ded.compact) 8.dp else 12.dp), content = content)
}

@Composable
fun SectionLabel(text: String) {
    Text(text.uppercase(), style = T.label(), modifier = Modifier.padding(top = 6.dp))
}

@Composable
fun ScreenTitle(text: String, onBack: (() -> Unit)? = null, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (onBack != null) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack, role = Role.Button),
                contentAlignment = Alignment.CenterStart,
            ) { DIcon(R.drawable.ic_back, description = "Назад") }
        }
        Text(text, style = T.display(24.sp), modifier = Modifier.weight(1f))
        trailing()
    }
}

// ——— Кнопки ———

@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = dc.ink,
    textColor: Color = dc.onInk,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
    height: Dp = 56.dp,
) {
    Row(
        modifier
            .defaultMinSize(minHeight = height)
            .clip(RoundedCornerShape(ded.radius + 2.dp))
            .background(if (enabled) color else dc.muted.copy(alpha = 0.5f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            DIcon(icon, textColor, 20.dp); Spacer(Modifier.width(8.dp))
        }
        Text(text, style = T.body(16.sp, androidx.compose.ui.text.font.FontWeight.ExtraBold, textColor), textAlign = TextAlign.Center)
    }
}

@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 52.dp, enabled: Boolean = true) {
    Box(
        modifier
            .defaultMinSize(minHeight = height)
            .clip(RoundedCornerShape(ded.radius + 2.dp))
            .border(2.dp, if (enabled) dc.ink else dc.line, RoundedCornerShape(ded.radius + 2.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = T.body(15.sp, androidx.compose.ui.text.font.FontWeight.ExtraBold, if (enabled) dc.ink else dc.muted))
    }
}

@Composable
fun LinkText(text: String, onClick: () -> Unit, color: Color = dc.secondary) {
    Box(Modifier.defaultMinSize(minHeight = 44.dp).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, style = T.body(14.sp, androidx.compose.ui.text.font.FontWeight.ExtraBold, color))
    }
}

@Composable
fun <K> Segmented(options: List<Pair<K, String>>, selected: K, onSelect: (K) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(ded.radius)).background(dc.seg).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (k, label) ->
            val on = k == selected
            Box(
                Modifier.weight(1f).height(40.dp)
                    .clip(RoundedCornerShape((ded.radius - 4.dp).coerceAtLeast(2.dp)))
                    .background(if (on) dc.ink else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(k) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = T.body(14.sp, androidx.compose.ui.text.font.FontWeight.ExtraBold, if (on) dc.onInk else dc.ink), maxLines = 1)
            }
        }
    }
}

@Composable
fun Chip(text: String, selected: Boolean, dot: Color? = null, onClick: () -> Unit) {
    Row(
        Modifier.height(44.dp).clip(RoundedCornerShape(22.dp))
            .background(if (selected) dc.ink else Color.Transparent)
            .border(2.dp, if (selected) dc.ink else dc.line, RoundedCornerShape(22.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(dot)); Spacer(Modifier.width(8.dp))
        }
        Text(text, style = T.body(14.sp, androidx.compose.ui.text.font.FontWeight.ExtraBold, if (selected) dc.onInk else dc.ink))
    }
}

@Composable
fun ToggleRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = T.body(15.sp))
            if (subtitle != null) Text(subtitle, style = T.muted(12.sp))
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = dc.accent, checkedThumbColor = Color.White),
        )
    }
}

@Composable
fun ProgressBar(fraction: Float, color: Color, modifier: Modifier = Modifier, track: Color = dc.line, height: Dp = 8.dp) {
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2)).background(track)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(height).clip(RoundedCornerShape(height / 2)).background(color))
    }
}

@Composable
fun Field(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, placeholder: String = "", singleLine: Boolean = true, keyboardNumber: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = dc.muted) },
        singleLine = singleLine,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ded.radius),
        keyboardOptions = if (keyboardNumber) androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) else androidx.compose.foundation.text.KeyboardOptions.Default,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = dc.ink, unfocusedBorderColor = dc.line,
            focusedContainerColor = dc.surface, unfocusedContainerColor = dc.surface,
            focusedLabelColor = dc.ink, cursorColor = dc.accent,
        ),
    )
}

@Composable
fun StatBox(value: String, label: String, modifier: Modifier = Modifier, valueColor: Color = dc.ink, bg: Color = dc.surface) {
    Column(
        modifier.clip(RoundedCornerShape(ded.radius)).background(bg).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = T.display(17.sp).copy(color = valueColor), maxLines = 1)
        Text(label, style = T.muted(12.sp), textAlign = TextAlign.Center)
    }
}

fun plural(n: Long, one: String, few: String, many: String): String {
    val m10 = n % 10; val m100 = n % 100
    return when {
        m10 == 1L && m100 != 11L -> one
        m10 in 2L..4L && m100 !in 12L..14L -> few
        else -> many
    }
}

fun fmtNum(n: Long): String = "%,d".format(n).replace(',', ' ')

fun fmtMin(min: Int): String = if (min >= 60) "${min / 60} ч ${(min % 60).toString().padStart(2, '0')}" else "$min мин"

fun fmtClock(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d:%02d".format(s / 3600, (s / 60) % 60, s % 60)
}

@Suppress("unused")
val unusedSp = 0.sp
