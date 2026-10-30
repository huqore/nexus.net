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
    sheetBg = Color(0xF2110F22),
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
    sheetBg = Color(0xF2181C22),
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
    sheetBg = Color(0xF215161C),
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
    sheetBg = Color(0xF207050E),
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
    sheetBg = Color(0xF20C1C13),
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
    sheetBg = Color(0xF20A1930),
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
    sheetBg = Color(0xF216090E),
)

fun AppTheme.palette(): NexusColorPalette = when (this) {
    AppTheme.BASE -> BaseThemeColors
    AppTheme.AMOLED_BLACK -> AmoledBlackThemeColors
    AppTheme.GREEN_SCREEN -> GreenScreenThemeColors
    AppTheme.BLUE_SCREEN -> BlueScreenThemeColors
    AppTheme.RED_SCREEN -> RedScreenThemeColors
    AppTheme.WHITE_SCREEN -> WhiteScreenThemeColors
    AppTheme.HOME_SCREEN -> HomeScreenFallbackColors
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
    NX.palette = if (appTheme == AppTheme.HOME_SCREEN) rememberDynamicPalette(ctx) else appTheme.palette()
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
