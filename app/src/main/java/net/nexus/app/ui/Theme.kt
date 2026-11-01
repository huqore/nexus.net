package net.nexus.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import android.os.Build
import android.content.Context
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember

import androidx.compose.ui.unit.Density
import net.nexus.app.data.AppTheme

enum class Tab { CONNECT, SERVERS, SETTINGS }

data class NexusColorPalette(
    val bg: Color,
    val card: Color,
    val cardBorder: Color,
    val chip: Color,
    val iconBtn: Color,
    val accent: Color,
    val accentDim: Color,
    val accentLight: Color,
    val text: Color,
    val muted: Color,
    val muted2: Color,
    val yellow: Color = Color(0xFFE5C14A),
    val green: Color = Color(0xFF3DDC97),
    val red: Color = Color(0xFFFF5C6C),
    val purple: Color = Color(0xFFA78BFA),
    val btnGradTop: Color = Color(0xFF1B1733),
    val btnGradBottom: Color = Color(0xFF120F22),
    val sheetBg: Color = Color(0xF2110F22),
)

val BaseThemeColors = NexusColorPalette(
    bg = Color(0xFF0A0914),
    card = Color(0xFF15132A),
    cardBorder = Color(0xFF2B2650),
    chip = Color(0xFF1E1A3D),
    iconBtn = Color(0xFF1F1B3A),
    accent = Color(0xFF8B5CF6),
    accentDim = Color(0xFF251C4D),
    accentLight = Color(0xFFB79CFF),
    text = Color(0xFFF1EFFA),
    muted = Color(0xFF8C88A8),
    muted2 = Color(0xFFB0ACC8),
    yellow = Color(0xFFE5C14A),
    green = Color(0xFF3DDC97),
    red = Color(0xFFFF5C6C),
    purple = Color(0xFFA78BFA),
    btnGradTop = Color(0xFF1B1733),
    btnGradBottom = Color(0xFF120F22),
    sheetBg = Color(0xFD110F22),
)

val HomeScreenFallbackColors = NexusColorPalette(
    bg = Color(0xFF0F1216),
    card = Color(0xFF181C22),
    cardBorder = Color(0xFF2C333D),
    chip = Color(0xFF222831),
    iconBtn = Color(0xFF222831),
    accent = Color(0xFF38BDF8),
    accentDim = Color(0xFF1E3A5F),
    accentLight = Color(0xFF7DD3FC),
    text = Color(0xFFF1F5F9),
    muted = Color(0xFF94A3B8),
    muted2 = Color(0xFFCBD5E1),
    yellow = Color(0xFFE5C14A),
    green = Color(0xFF3DDC97),
    red = Color(0xFFFF5C6C),
    purple = Color(0xFF38BDF8),
    btnGradTop = Color(0xFF252D37),
    btnGradBottom = Color(0xFF181C22),
    sheetBg = Color(0xFD181C22),
)

@Composable
fun rememberDynamicPalette(context: Context): NexusColorPalette {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val dyn = dynamicDarkColorScheme(context)
        return remember(dyn) {
            val bg = dyn.background
            val card = dyn.surface
            val primary = dyn.primary
            val cardBorder = dyn.outlineVariant.copy(alpha = 0.5f)
            NexusColorPalette(
                bg = bg,
                card = card,
                cardBorder = cardBorder,
                chip = dyn.surfaceVariant,
                iconBtn = dyn.surfaceVariant,
                accent = primary,
                accentDim = dyn.primaryContainer.copy(alpha = 0.5f),
                accentLight = dyn.inversePrimary,
                text = dyn.onSurface,
                muted = dyn.onSurfaceVariant,
                muted2 = dyn.onSurfaceVariant.copy(alpha = 0.8f),
                yellow = Color(0xFFE5C14A),
                green = Color(0xFF3DDC97),
                red = Color(0xFFFF5C6C),
                purple = primary,
                btnGradTop = dyn.surfaceContainerHigh,
                btnGradBottom = card,
                sheetBg = card.copy(alpha = 0.95f)
            )
        }
    }
    return HomeScreenFallbackColors
}

val WhiteScreenThemeColors = NexusColorPalette(
    bg = Color(0xFF0D0E12),
    card = Color(0xFF16171D),
    cardBorder = Color(0xFF2E313D),
    chip = Color(0xFF21232B),
    iconBtn = Color(0xFF21232B),
    accent = Color(0xFFE5E7EB),
    accentDim = Color(0xFF2D303C),
    accentLight = Color(0xFFFFFFFF),
    text = Color(0xFFF3F4F6),
    muted = Color(0xFF9CA3AF),
    muted2 = Color(0xFFD1D5DB),
    yellow = Color(0xFFE5C14A),
    green = Color(0xFF3DDC97),
    red = Color(0xFFFF5C6C),
    purple = Color(0xFFE5E7EB),
    btnGradTop = Color(0xFF282A36),
    btnGradBottom = Color(0xFF17181F),
    sheetBg = Color(0xFD15161C),
)

val AmoledBlackThemeColors = NexusColorPalette(
    bg = Color(0xFF000000),
    card = Color(0xFF0A0912),
    cardBorder = Color(0xFF1F1A3A),
    chip = Color(0xFF141126),
    iconBtn = Color(0xFF141126),
    accent = Color(0xFF8B5CF6),
    accentDim = Color(0xFF201642),
    accentLight = Color(0xFFB79CFF),
    text = Color(0xFFF5F3FF),
    muted = Color(0xFF827D9E),
    muted2 = Color(0xFFA9A4C4),
    yellow = Color(0xFFE5C14A),
    green = Color(0xFF3DDC97),
    red = Color(0xFFFF5C6C),
    purple = Color(0xFFA78BFA),
    btnGradTop = Color(0xFF151026),
    btnGradBottom = Color(0xFF07050E),
    sheetBg = Color(0xFD07050E),
)

val GreenScreenThemeColors = NexusColorPalette(
    bg = Color(0xFF07120C),
    card = Color(0xFF0E1F16),
    cardBorder = Color(0xFF193B2B),
    chip = Color(0xFF132A1F),
    iconBtn = Color(0xFF132A1F),
    accent = Color(0xFF34D399),
    accentDim = Color(0xFF123824),
    accentLight = Color(0xFF6EE7B7),
    text = Color(0xFFECFDF5),
    muted = Color(0xFF7D9E8D),
    muted2 = Color(0xFFA7F3D0),
    yellow = Color(0xFFE5C14A),
    green = Color(0xFF34D399),
    red = Color(0xFFFF5C6C),
    purple = Color(0xFF34D399),
    btnGradTop = Color(0xFF163826),
    btnGradBottom = Color(0xFF0B1F15),
    sheetBg = Color(0xFD0C1C13),
)

val BlueScreenThemeColors = NexusColorPalette(
    bg = Color(0xFF06101E),
    card = Color(0xFF0C1B33),
    cardBorder = Color(0xFF173566),
    chip = Color(0xFF122748),
    iconBtn = Color(0xFF122748),
    accent = Color(0xFF38BDF8),
    accentDim = Color(0xFF0E305C),
    accentLight = Color(0xFF7DD3FC),
    text = Color(0xFFF0F9FF),
    muted = Color(0xFF7A93B0),
    muted2 = Color(0xFFBAE6FD),
    yellow = Color(0xFFE5C14A),
    green = Color(0xFF3DDC97),
    red = Color(0xFFFF5C6C),
    purple = Color(0xFF38BDF8),
    btnGradTop = Color(0xFF132E59),
    btnGradBottom = Color(0xFF091B33),
    sheetBg = Color(0xFD0A1930),
)

val RedScreenThemeColors = NexusColorPalette(
    bg = Color(0xFF0F070A),
    card = Color(0xFF1B0E13),
    cardBorder = Color(0xFF3B1822),
    chip = Color(0xFF27131B),
    iconBtn = Color(0xFF27131B),
    accent = Color(0xFFF43F5E),
    accentDim = Color(0xFF3D121D),
    accentLight = Color(0xFFFB7185),
    text = Color(0xFFFFF1F2),
    muted = Color(0xFFFB7185).copy(alpha = 0.65f),
    muted2 = Color(0xFFFECDD3),
    yellow = Color(0xFFE5C14A),
    green = Color(0xFF3DDC97),
    red = Color(0xFFFF5C6C),
    purple = Color(0xFFF43F5E),
    btnGradTop = Color(0xFF2E111A),
    btnGradBottom = Color(0xFF14070B),
    sheetBg = Color(0xFD16090E),
)


val PinkScreenThemeColors = NexusColorPalette(
    bg = Color(0xFF140812),
    card = Color(0xFF220E1E),
    cardBorder = Color(0xFF451B3D),
    chip = Color(0xFF31142B),
    iconBtn = Color(0xFF31142B),
    accent = Color(0xFFEC4899),
    accentDim = Color(0xFF4C1637),
    accentLight = Color(0xFFF472B6),
    text = Color(0xFFFDF2F8),
    muted = Color(0xFFA27B96),
    muted2 = Color(0xFFFBCFE8),
    yellow = Color(0xFFFDE047),
    green = Color(0xFF4ADE80),
    red = Color(0xFFFB7185),
    purple = Color(0xFFC084FC),
    btnGradTop = Color(0xFF381532),
    btnGradBottom = Color(0xFF1B0918),
    sheetBg = Color(0xFD1A0A17),
)

val YellowScreenThemeColors = NexusColorPalette(
    bg = Color(0xFF120E05),
    card = Color(0xFF1F1709),
    cardBorder = Color(0xFF423213),
    chip = Color(0xFF2B200C),
    iconBtn = Color(0xFF2B200C),
    accent = Color(0xFFF59E0B),
    accentDim = Color(0xFF452B07),
    accentLight = Color(0xFFFBBF24),
    text = Color(0xFFFFFBEB),
    muted = Color(0xFFA6936E),
    muted2 = Color(0xFFFDE68A),
    yellow = Color(0xFFFBBF24),
    green = Color(0xFF34D399),
    red = Color(0xFFFB923C),
    purple = Color(0xFFF59E0B),
    btnGradTop = Color(0xFF38260B),
    btnGradBottom = Color(0xFF171005),
    sheetBg = Color(0xFD171106),
)

val SakuraBaseThemeColors = NexusColorPalette(
    bg = Color(0xFF120910),
    card = Color(0xFF1D0F1B),
    cardBorder = Color(0xFF3F1F39),
    chip = Color(0xFF2B1428),
    iconBtn = Color(0xFF2B1428),
    accent = Color(0xFFFF85A1),
    accentDim = Color(0xFF4A1837),
    accentLight = Color(0xFFFFCCD5),
    text = Color(0xFFFFF5F8),
    muted = Color(0xFFA8899E),
    muted2 = Color(0xFFFFDDE9),
    yellow = Color(0xFFFDE047),
    green = Color(0xFF4ADE80),
    red = Color(0xFFFF758F),
    purple = Color(0xFFE879F9),
    btnGradTop = Color(0xFF36142F),
    btnGradBottom = Color(0xFF160914),
    sheetBg = Color(0xFD170B16),
)

val WinterBaseThemeColors = NexusColorPalette(
    bg = Color(0xFF070B14),
    card = Color(0xFF0D1526),
    cardBorder = Color(0xFF1C2C4E),
    chip = Color(0xFF14203A),
    iconBtn = Color(0xFF14203A),
    accent = Color(0xFF38BDF8),
    accentDim = Color(0xFF112D55),
    accentLight = Color(0xFFE0F2FE),
    text = Color(0xFFF0F9FF),
    muted = Color(0xFF7B91B2),
    muted2 = Color(0xFFBAE6FD),
    yellow = Color(0xFFE5C14A),
    green = Color(0xFF34D399),
    red = Color(0xFFFF5C6C),
    purple = Color(0xFFA78BFA),
    btnGradTop = Color(0xFF152646),
    btnGradBottom = Color(0xFF091222),
    sheetBg = Color(0xFD091222),
)

val RgbBaseThemeColors = NexusColorPalette(
    bg = Color(0xFF0A0A0E),
    card = Color(0xFF14141C),
    cardBorder = Color(0xFF282836),
    chip = Color(0xFF1E1E2A),
    iconBtn = Color(0xFF1E1E2A),
    accent = Color(0xFFFF0055),
    accentDim = Color(0xFF3D0818),
    accentLight = Color(0xFFFF5599),
    text = Color(0xFFF8FAFC),
    muted = Color(0xFF94A3B8),
    muted2 = Color(0xFFE2E8F0),
    yellow = Color(0xFFFFCC00),
    green = Color(0xFF00FF88),
    red = Color(0xFFFF3366),
    purple = Color(0xFF9933FF),
    btnGradTop = Color(0xFF222230),
    btnGradBottom = Color(0xFF0E0E14),
    sheetBg = Color(0xFD0F0F16),
)

@Composable
fun rememberSakuraPalette(): NexusColorPalette {
    val transition = rememberInfiniteTransition(label = "sakuraShimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sakuraProgress"
    )

    val c1 = Color(0xFFFF85A1)
    val c2 = Color(0xFFF472B6)
    val c3 = Color(0xFFFFB3C6)

    val currentAccent = if (progress < 0.5f) {
        lerpColor(c1, c2, progress * 2f)
    } else {
        lerpColor(c2, c3, (progress - 0.5f) * 2f)
    }
    val currentLight = if (progress < 0.5f) {
        lerpColor(Color(0xFFFFCCD5), Color(0xFFFFE5EC), progress * 2f)
    } else {
        lerpColor(Color(0xFFFFE5EC), Color(0xFFFFCCD5), (progress - 0.5f) * 2f)
    }

    return SakuraBaseThemeColors.copy(
        accent = currentAccent,
        accentDim = currentAccent.copy(alpha = 0.28f),
        accentLight = currentLight,
        cardBorder = currentAccent.copy(alpha = 0.35f)
    )
}

@Composable
fun rememberWinterPalette(): NexusColorPalette {
    val transition = rememberInfiniteTransition(label = "winterShimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "winterProgress"
    )

    val cWhite = Color(0xFFE0F2FE)
    val cIceBlue = Color(0xFF38BDF8)
    val cViolet = Color(0xFFA78BFA)

    val currentAccent = if (progress < 0.5f) {
        lerpColor(cIceBlue, cWhite, progress * 2f)
    } else {
        lerpColor(cWhite, cViolet, (progress - 0.5f) * 2f)
    }
    val currentLight = if (progress < 0.5f) {
        lerpColor(Color(0xFFBAE6FD), Color(0xFFF0F9FF), progress * 2f)
    } else {
        lerpColor(Color(0xFFF0F9FF), Color(0xFFDDD6FE), (progress - 0.5f) * 2f)
    }

    return WinterBaseThemeColors.copy(
        accent = currentAccent,
        accentDim = currentAccent.copy(alpha = 0.25f),
        accentLight = currentLight,
        cardBorder = currentAccent.copy(alpha = 0.32f),
        purple = cViolet
    )
}

@Composable
fun rememberRgbPalette(): NexusColorPalette {
    val transition = rememberInfiniteTransition(label = "rgbWave")
    val hue by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rgbHue"
    )

    val accent = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.95f, 1.0f)))
    val accentLight = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 35f) % 360f, 0.70f, 1.0f)))
    val accentDim = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.90f, 0.35f)))
    val cardBorder = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 180f) % 360f, 0.75f, 0.55f))).copy(alpha = 0.55f)

    return RgbBaseThemeColors.copy(
        accent = accent,
        accentLight = accentLight,
        accentDim = accentDim,
        cardBorder = cardBorder,
        purple = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 60f) % 360f, 0.9f, 1f))),
        green = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 120f) % 360f, 0.9f, 1f))),
        yellow = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 180f) % 360f, 0.9f, 1f))),
        red = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 240f) % 360f, 0.9f, 1f)))
    )
}

private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * f,
        green = start.green + (end.green - start.green) * f,
        blue = start.blue + (end.blue - start.blue) * f,
        alpha = start.alpha + (end.alpha - start.alpha) * f
    )
}

fun AppTheme.palette(): NexusColorPalette = when (this) {
    AppTheme.BASE -> BaseThemeColors
    AppTheme.AMOLED_BLACK -> AmoledBlackThemeColors
    AppTheme.GREEN_SCREEN -> GreenScreenThemeColors
    AppTheme.BLUE_SCREEN -> BlueScreenThemeColors
    AppTheme.RED_SCREEN -> RedScreenThemeColors
    AppTheme.WHITE_SCREEN -> WhiteScreenThemeColors
    AppTheme.HOME_SCREEN -> HomeScreenFallbackColors
    AppTheme.PINK_SCREEN -> PinkScreenThemeColors
    AppTheme.YELLOW_SCREEN -> YellowScreenThemeColors
    AppTheme.SAKURA -> SakuraBaseThemeColors
    AppTheme.WINTER -> WinterBaseThemeColors
    AppTheme.RGB -> RgbBaseThemeColors
}

@Composable
fun rememberThemePalette(theme: AppTheme, context: Context): NexusColorPalette {
    return when (theme) {
        AppTheme.HOME_SCREEN -> rememberDynamicPalette(context)
        AppTheme.SAKURA -> rememberSakuraPalette()
        AppTheme.WINTER -> rememberWinterPalette()
        AppTheme.RGB -> rememberRgbPalette()
        else -> theme.palette()
    }
}

/** Dynamic colour tokens responding to the active theme */
object NX {
    var palette by mutableStateOf(BaseThemeColors)

    val bg: Color get() = palette.bg
    val card: Color get() = palette.card
    val cardBorder: Color get() = palette.cardBorder
    val chip: Color get() = palette.chip
    val iconBtn: Color get() = palette.iconBtn
    val accent: Color get() = palette.accent
    val accentDim: Color get() = palette.accentDim
    val accentLight: Color get() = palette.accentLight
    val text: Color get() = palette.text
    val muted: Color get() = palette.muted
    val muted2: Color get() = palette.muted2
    val yellow: Color get() = palette.yellow
    val green: Color get() = palette.green
    val red: Color get() = palette.red
    val purple: Color get() = palette.purple
    val btnGradTop: Color get() = palette.btnGradTop
    val btnGradBottom: Color get() = palette.btnGradBottom
    val sheetBg: Color get() = palette.sheetBg
}

@Composable
fun NexusTheme(
    fontScale: Float = 0.85f,
    appTheme: AppTheme = AppTheme.BASE,
    content: @Composable () -> Unit
) {
    val ctx = LocalContext.current
    NX.palette = rememberThemePalette(appTheme, ctx)
    val density = LocalDensity.current
    val customDensity = Density(
        density = density.density,
        fontScale = density.fontScale * fontScale
    )
    CompositionLocalProvider(LocalDensity provides customDensity) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = NX.accent,
                onPrimary = if (appTheme == AppTheme.WHITE_SCREEN) Color.Black else Color.White,
                background = NX.bg,
                onBackground = NX.text,
                surface = NX.card,
                onSurface = NX.text,
                surfaceVariant = NX.iconBtn,
                onSurfaceVariant = NX.muted,
                outline = NX.cardBorder,
            )
        ) {
            Surface(color = NX.bg, modifier = Modifier.fillMaxSize()) { content() }
        }
    }
}
