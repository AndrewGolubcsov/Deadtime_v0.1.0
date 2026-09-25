package ru.dedtime.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.dedtime.app.R
import ru.dedtime.app.data.Category
import ru.dedtime.app.data.UiSettings

val Unbounded = FontFamily(
    Font(R.font.unbounded, FontWeight.Medium),
    Font(R.font.unbounded, FontWeight.Bold),
)
val Manrope = FontFamily(
    Font(R.font.manrope, FontWeight.Normal),
    Font(R.font.manrope, FontWeight.Medium),
    Font(R.font.manrope, FontWeight.SemiBold),
    Font(R.font.manrope, FontWeight.Bold),
    Font(R.font.manrope, FontWeight.ExtraBold),
)
val Onest = FontFamily(
    Font(R.font.onest, FontWeight.Normal),
    Font(R.font.onest, FontWeight.Medium),
    Font(R.font.onest, FontWeight.SemiBold),
    Font(R.font.onest, FontWeight.Bold),
    Font(R.font.onest, FontWeight.ExtraBold),
)
val PtSerif = FontFamily(
    Font(R.font.ptserif_regular, FontWeight.Normal),
    Font(R.font.ptserif_bold, FontWeight.Bold),
)

@Immutable
data class DedColors(
    val bg: Color,
    val surface: Color,
    val ink: Color,
    val onInk: Color,
    val muted: Color,
    val line: Color,
    val seg: Color,
    val accent: Color,
    val accentTint: Color,
    val secondary: Color,
    val secondaryTint: Color,
    val isDark: Boolean,
)

@Immutable
data class Ded(
    val c: DedColors,
    val body: FontFamily,
    val radius: Dp,
    val compact: Boolean,
    val anim: Boolean,
)

val LocalDed = staticCompositionLocalOf {
    Ded(lightColors(Color(0xFFC2410C), warm = false), Manrope, 14.dp, compact = false, anim = true)
}

val ded: Ded @Composable get() = LocalDed.current
val dc: DedColors @Composable get() = LocalDed.current.c

val Accents = listOf(
    0xFFC2410C to "Оранжевый", 0xFF1D4ED8 to "Синий", 0xFF15803D to "Зелёный",
    0xFF7E22CE to "Фиолетовый", 0xFFBE185D to "Малиновый", 0xFF0F766E to "Бирюзовый",
)

fun lightColors(accent: Color, warm: Boolean) = DedColors(
    bg = if (warm) Color(0xFFEFE3D0) else Color(0xFFF3EFE6),
    surface = Color.White,
    ink = Color(0xFF17171A),
    onInk = Color.White,
    muted = Color(0xFF5B5A57),
    line = Color(0xFFE4DFD3),
    seg = Color(0xFFEDE8DC),
    accent = accent,
    accentTint = Color(0xFFFBE3D6).takeIf { accent.value == Color(0xFFC2410C).value } ?: accent.copy(alpha = 0.14f).over(Color.White),
    secondary = Color(0xFF1D4ED8),
    secondaryTint = Color(0xFFDCE6FB),
    isDark = false,
)

fun darkColors(accent: Color) = DedColors(
    bg = Color(0xFF141416),
    surface = Color(0xFF222226),
    ink = Color(0xFFF3EFE6),
    onInk = Color(0xFF141416),
    muted = Color(0xFFA9A59C),
    line = Color(0xFF34343A),
    seg = Color(0xFF2C2C31),
    accent = if (accent.value == Color(0xFFC2410C).value) Color(0xFFF59E68) else accent,
    accentTint = accent.copy(alpha = 0.22f).over(Color(0xFF222226)),
    secondary = Color(0xFF93B4F5),
    secondaryTint = Color(0xFF93B4F5).copy(alpha = 0.2f).over(Color(0xFF222226)),
    isDark = true,
)

/** Смешивание полупрозрачного цвета с фоном. */
fun Color.over(bg: Color): Color {
    val a = alpha
    return Color(red * a + bg.red * (1 - a), green * a + bg.green * (1 - a), blue * a + bg.blue * (1 - a), 1f)
}

fun DedColors.category(c: Category?): Color =
    if (c == null || c.colorHex == "accent") accent
    else runCatching { Color(android.graphics.Color.parseColor(c.colorHex)) }.getOrDefault(accent)

@Composable
fun DedtimeTheme(settings: UiSettings, content: @Composable () -> Unit) {
    val accent = Color(settings.accent.toInt())
    val dark = when (settings.theme) {
        "dark" -> true
        "light", "warm" -> false
        else -> isSystemInDarkTheme()
    }
    val colors = if (dark) darkColors(accent) else lightColors(accent, warm = settings.theme == "warm")
    val body = when (settings.font) {
        "onest" -> Onest
        "serif" -> PtSerif
        else -> Manrope
    }
    val scheme = if (dark) darkColorScheme(
        primary = colors.accent, onPrimary = Color.White, background = colors.bg, surface = colors.surface,
        onSurface = colors.ink, onBackground = colors.ink, surfaceVariant = colors.seg, outline = colors.line,
        secondary = colors.secondary,
    ) else lightColorScheme(
        primary = colors.accent, onPrimary = Color.White, background = colors.bg, surface = colors.surface,
        onSurface = colors.ink, onBackground = colors.ink, surfaceVariant = colors.seg, outline = colors.line,
        secondary = colors.secondary,
    )
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDed provides Ded(colors, body, settings.radius.dp, settings.compact, settings.anim),
        LocalDensity provides Density(density.density, density.fontScale * settings.textScale),
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = MaterialTheme.typography.copy(
                bodyLarge = TextStyle(fontFamily = body, fontSize = 16.sp),
                bodyMedium = TextStyle(fontFamily = body, fontSize = 14.sp),
                labelLarge = TextStyle(fontFamily = body, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                titleLarge = TextStyle(fontFamily = Unbounded, fontSize = 20.sp, fontWeight = FontWeight.Bold),
            ),
            content = content,
        )
    }
}

/** Текстовые стили приложения. */
object T {
    @Composable
    fun display(size: TextUnit = 24.sp) = TextStyle(fontFamily = Unbounded, fontWeight = FontWeight.Bold, fontSize = size, color = dc.ink)

    @Composable
    fun body(size: TextUnit = 15.sp, weight: FontWeight = FontWeight.SemiBold, color: Color = dc.ink) =
        TextStyle(fontFamily = ded.body, fontWeight = weight, fontSize = size, color = color)

    @Composable
    fun muted(size: TextUnit = 13.sp) = body(size, FontWeight.SemiBold, dc.muted)

    @Composable
    fun label() = TextStyle(fontFamily = ded.body, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = dc.muted, letterSpacing = 0.8.sp)
}
