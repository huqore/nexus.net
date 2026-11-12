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
import android.app.WallpaperManager
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember

import androidx.compose.ui.unit.Density
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import net.nexus.app.R
import net.nexus.app.data.AppFont
import net.nexus.app.data.AppTheme
import net.nexus.app.data.DesignStyle
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.luminance

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

fun extractDynamicWallpaperPalette(context: Context): NexusColorPalette {
    var wpPrimaryColor: Color? = null
    var wpSecondaryColor: Color? = null
    var wpTertiaryColor: Color? = null

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        runCatching {
            val wm = context.getSystemService(Context.WALLPAPER_SERVICE) as? WallpaperManager
                ?: WallpaperManager.getInstance(context)
            val colors = wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
            colors?.primaryColor?.toArgb()?.let { wpPrimaryColor = Color(it) }
            colors?.secondaryColor?.toArgb()?.let { wpSecondaryColor = Color(it) }
            colors?.tertiaryColor?.toArgb()?.let { wpTertiaryColor = Color(it) }
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val dyn = runCatching { dynamicDarkColorScheme(context) }.getOrNull()
        if (dyn != null) {
            val seedColor = wpPrimaryColor ?: dyn.primary
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(seedColor.toArgb(), hsv)
            val hue = hsv[0]
            val sat = hsv[1]

            if (sat >= 0.08f) {
                val vibrantAccent = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat.coerceIn(0.55f, 0.95f), 0.90f)))
                val accentLight = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, (sat * 0.4f).coerceIn(0.15f, 0.4f), 0.98f)))
                val accentDim = vibrantAccent.copy(alpha = 0.28f)
                val cardBorder = vibrantAccent.copy(alpha = 0.35f)

                val bg = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.35f, 0.065f)))
                val card = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.30f, 0.11f)))
                val chip = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.26f, 0.16f)))
                val btnTop = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.30f, 0.20f)))
                val btnBottom = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.30f, 0.08f)))
                val sheet = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.32f, 0.08f))).copy(alpha = 0.97f)

                return NexusColorPalette(
                    bg = bg,
                    card = card,
                    cardBorder = cardBorder,
                    chip = chip,
                    iconBtn = chip,
                    accent = vibrantAccent,
                    accentDim = accentDim,
                    accentLight = accentLight,
                    text = Color(0xFFF8FAFC),
                    muted = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.22f, 0.65f))),
                    muted2 = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.25f, 0.85f))),
                    yellow = Color(0xFFFBBF24),
                    green = wpSecondaryColor ?: dyn.secondary,
                    red = Color(0xFFFB7185),
                    purple = wpTertiaryColor ?: dyn.tertiary,
                    btnGradTop = btnTop,
                    btnGradBottom = btnBottom,
                    sheetBg = sheet
                )
            } else {
                return NexusColorPalette(
                    bg = Color(0xFF0D0E12),
                    card = Color(0xFF16171E),
                    cardBorder = dyn.outlineVariant.copy(alpha = 0.4f),
                    chip = Color(0xFF22242D),
                    iconBtn = Color(0xFF22242D),
                    accent = dyn.primary,
                    accentDim = dyn.primaryContainer.copy(alpha = 0.4f),
                    accentLight = dyn.inversePrimary,
                    text = dyn.onSurface,
                    muted = dyn.onSurfaceVariant,
                    muted2 = dyn.onSurfaceVariant.copy(alpha = 0.8f),
                    yellow = Color(0xFFFBBF24),
                    green = Color(0xFF34D399),
                    red = Color(0xFFFB7185),
                    purple = dyn.tertiary,
                    btnGradTop = Color(0xFF242732),
                    btnGradBottom = Color(0xFF121318),
                    sheetBg = Color(0xFD121318)
                )
            }
        }
    }

    if (wpPrimaryColor != null) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(wpPrimaryColor!!.toArgb(), hsv)
        val hue = hsv[0]
        val sat = hsv[1]
        val vibrantAccent = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat.coerceIn(0.55f, 0.95f), 0.90f)))
        val accentLight = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, (sat * 0.4f).coerceIn(0.15f, 0.4f), 0.98f)))
        val bg = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.35f, 0.065f)))
        val card = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.30f, 0.11f)))
        val chip = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.26f, 0.16f)))
        return NexusColorPalette(
            bg = bg,
            card = card,
            cardBorder = vibrantAccent.copy(alpha = 0.35f),
            chip = chip,
            iconBtn = chip,
            accent = vibrantAccent,
            accentDim = vibrantAccent.copy(alpha = 0.28f),
            accentLight = accentLight,
            text = Color(0xFFF8FAFC),
            muted = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.22f, 0.65f))),
            muted2 = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.25f, 0.85f))),
            yellow = Color(0xFFFBBF24),
            green = wpSecondaryColor ?: Color(0xFF34D399),
            red = Color(0xFFFB7185),
            purple = wpTertiaryColor ?: Color(0xFFA78BFA),
            btnGradTop = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.30f, 0.20f))),
            btnGradBottom = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.30f, 0.08f))),
            sheetBg = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.32f, 0.08f))).copy(alpha = 0.97f)
        )
    }

    return HomeScreenFallbackColors
}

@Composable
fun rememberDynamicPalette(context: Context): NexusColorPalette {
    return remember(context) {
        extractDynamicWallpaperPalette(context)
    }
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
    bg = Color(0xFF130910),
    card = Color(0xFF1F0E1A),
    cardBorder = Color(0xFF4D1C3E),
    chip = Color(0xFF2D1226),
    iconBtn = Color(0xFF2D1226),
    accent = Color(0xFFFF5E88),       // Cherry blossom rose pink
    accentDim = Color(0xFF4D1635),
    accentLight = Color(0xFFFFCCD5),  // Soft petal beige-pink
    text = Color(0xFFFFF5F7),         // Creamy petal white
    muted = Color(0xFFA8869B),
    muted2 = Color(0xFFFCE7DF),       // Petal beige cream
    yellow = Color(0xFFFDE047),
    green = Color(0xFF4ADE80),
    red = Color(0xFFE11D48),          // Deep petal crimson
    purple = Color(0xFFFB7185),
    btnGradTop = Color(0xFF38142C),
    btnGradBottom = Color(0xFF160813),
    sheetBg = Color(0xFD180B15),
)

val WinterBaseThemeColors = NexusColorPalette(
    bg = Color(0xFF060A13),
    card = Color(0xFF0B1424),
    cardBorder = Color(0xFF1B2E52),
    chip = Color(0xFF12203B),
    iconBtn = Color(0xFF12203B),
    accent = Color(0xFF38BDF8),       // Sky / ice cyan
    accentDim = Color(0xFF0F2D54),
    accentLight = Color(0xFFF0F9FF),  // Crisp arctic white
    text = Color(0xFFF0F9FF),         // Arctic white
    muted = Color(0xFF7E94B4),
    muted2 = Color(0xFFBAE6FD),       // Ice light blue
    yellow = Color(0xFFFDE047),
    green = Color(0xFF34D399),
    red = Color(0xFFFF5C6C),
    purple = Color(0xFF0284C7),       // Deep ocean blue
    btnGradTop = Color(0xFF132546),
    btnGradBottom = Color(0xFF070E1A),
    sheetBg = Color(0xFD08101D),
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

@Composable
fun rememberSecretRgbPalette(): NexusColorPalette {
    val transition = rememberInfiniteTransition(label = "secretRgbWave")
    val hue by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "secretRgbHue"
    )

    val accent = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1.0f, 1.0f)))
    val accentLight = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 45f) % 360f, 0.85f, 1.0f)))
    val accentDim = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1.0f, 0.45f)))
    val cardBorder = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 180f) % 360f, 0.95f, 0.85f))).copy(alpha = 0.75f)

    return RgbBaseThemeColors.copy(
        bg = Color(0xFF07070C),
        card = Color(0xFF0F0F18),
        accent = accent,
        accentLight = accentLight,
        accentDim = accentDim,
        cardBorder = cardBorder,
        purple = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 60f) % 360f, 1f, 1f))),
        green = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 120f) % 360f, 1f, 1f))),
        yellow = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 180f) % 360f, 1f, 1f))),
        red = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 240f) % 360f, 1f, 1f)))
    )
}

/** Live colours of the user's custom theme (pushed from Settings by NexusTheme). */
object CustomThemeColors {
    var accent by mutableStateOf(Color(0xFF8B5CF6))
    var bg by mutableStateOf(Color(0xFF0A0812))
}

/** Builds a full palette from just an accent and a background colour (dark or light). */
fun customPalette(accent: Color, bg: Color): NexusColorPalette {
    val light = bg.luminance() > 0.5f
    val ink = if (light) Color.Black else Color.White
    val text = if (light) Color(0xFF16141C) else Color(0xFFF2F0F7)
    fun l(a: Color, b: Color, t: Float) = androidx.compose.ui.graphics.lerp(a, b, t)
    val card = l(l(bg, ink, if (light) 0.035f else 0.055f), accent, 0.06f)
    val chip = l(l(bg, ink, if (light) 0.07f else 0.10f), accent, 0.10f)
    return NexusColorPalette(
        bg = bg,
        card = card,
        cardBorder = accent.copy(alpha = if (light) 0.40f else 0.35f),
        chip = chip,
        iconBtn = chip,
        accent = accent,
        accentDim = l(bg, accent, if (light) 0.22f else 0.30f),
        accentLight = if (light) l(accent, Color.Black, 0.25f) else l(accent, Color.White, 0.55f),
        text = text,
        muted = l(text, bg, 0.45f),
        muted2 = l(text, accent, 0.35f),
        purple = l(accent, ink, 0.2f),
        btnGradTop = l(bg, accent, if (light) 0.10f else 0.18f),
        btnGradBottom = l(bg, ink, if (light) 0.02f else 0.03f),
        sheetBg = l(bg, ink, 0.03f).copy(alpha = 0.97f),
    )
}

/** AMOLED toggle: same theme on a pure-black background, cards only faintly lifted. */
fun amoledVariant(p: NexusColorPalette): NexusColorPalette {
    fun k(c: Color, t: Float) = androidx.compose.ui.graphics.lerp(Color.Black, c, t)
    return p.copy(
        bg = Color.Black,
        card = k(p.card, 0.55f),
        chip = k(p.chip, 0.75f),
        iconBtn = k(p.iconBtn, 0.75f),
        accentDim = k(p.accentDim, 0.8f),
        btnGradTop = k(p.btnGradTop, 0.7f),
        btnGradBottom = Color.Black,
        sheetBg = Color.Black.copy(alpha = 0.97f),
    )
}

/** Light-mode toggle: a light variant of the theme built from its accent. */
fun lightVariant(p: NexusColorPalette): NexusColorPalette {
    val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(p.accent.toArgb(), it) }
    val bg = Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], 0.05f, 0.985f)))
    // Bright accents (yellow, cyan, white-ish) get darker so they stay readable on white.
    val accent = if (p.accent.luminance() > 0.45f) androidx.compose.ui.graphics.lerp(p.accent, Color.Black, 0.32f) else p.accent
    return customPalette(accent, bg).copy(
        yellow = Color(0xFFB7791F),
        green = Color(0xFF15803D),
        red = Color(0xFFDC2626),
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
    AppTheme.SECRET_RGB -> RgbBaseThemeColors
    AppTheme.CUSTOM -> customPalette(CustomThemeColors.accent, CustomThemeColors.bg)
}

@Composable
fun rememberThemePalette(theme: AppTheme, context: Context): NexusColorPalette {
    return when (theme) {
        AppTheme.HOME_SCREEN -> rememberDynamicPalette(context)
        AppTheme.RGB -> rememberRgbPalette()
        AppTheme.SECRET_RGB -> rememberSecretRgbPalette()
        AppTheme.CUSTOM -> {
            val a = CustomThemeColors.accent
            val b = CustomThemeColors.bg
            remember(a, b) { customPalette(a, b) }
        }
        else -> theme.palette()
    }
}

/** Dynamic colour tokens responding to the active theme */
object NX {
    var palette by mutableStateOf(BaseThemeColors)

    /** Visual language (Fluent / Material 3 / Liquid Glass) layered on top of the colour theme. */
    var style by mutableStateOf(DesignStyle.FLUENT)

    /** MD3 tonal roles for the current palette; recomputed only when the palette changes. */
    private var tonesFor: NexusColorPalette? = null
    private var tonesCache: Md3Tones? = null
    /** Set by NexusTheme for Material You (wallpaper dynamic colour); null = derive from palette. */
    var md3Override: Md3Tones? by mutableStateOf(null)

    val md3: Md3Tones
        get() {
            md3Override?.let { return it }
            val p = palette
            if (tonesFor !== p || tonesCache == null) {
                tonesCache = md3TonesFor(p)
                tonesFor = p
            }
            return tonesCache!!
        }

    val isLightTheme: Boolean get() = palette.bg.luminance() > 0.5f

    val bg: Color get() = when (style) {
        DesignStyle.MATERIAL3 -> md3.surface
        else -> palette.bg
    }
    val card: Color get() = when (style) {
        DesignStyle.FLUENT -> palette.card
        DesignStyle.MATERIAL3 -> md3.surfaceContainerLow
        DesignStyle.LIQUID_GLASS -> Glass.fill(isLightTheme, 0)
    }
    val cardBorder: Color get() = when (style) {
        DesignStyle.FLUENT -> palette.cardBorder
        DesignStyle.MATERIAL3 -> md3.outlineVariant
        DesignStyle.LIQUID_GLASS -> Color.White.copy(alpha = if (isLightTheme) 0.7f else 0.16f)
    }
    val chip: Color get() = when (style) {
        DesignStyle.FLUENT -> palette.chip
        DesignStyle.MATERIAL3 -> md3.surfaceContainerHigh
        DesignStyle.LIQUID_GLASS -> Glass.fill(isLightTheme, 1)
    }
    val iconBtn: Color get() = when (style) {
        DesignStyle.FLUENT -> palette.iconBtn
        DesignStyle.MATERIAL3 -> md3.surfaceContainerHighest
        DesignStyle.LIQUID_GLASS -> Glass.fill(isLightTheme, 2)
    }
    val accent: Color get() = if (style == DesignStyle.MATERIAL3) md3.primary else palette.accent

    /** Fill/outline of a selected list item (server row, option) in the current style. */
    val selectedFill: Color get() = when (style) {
        DesignStyle.FLUENT -> palette.accentDim
        DesignStyle.MATERIAL3 -> md3.secondaryContainer
        DesignStyle.LIQUID_GLASS -> Color.White.copy(alpha = if (isLightTheme) 0.75f else 0.15f)
    }
    val selectedBorder: Color get() = when (style) {
        DesignStyle.FLUENT -> palette.accent.copy(alpha = 0.45f)
        DesignStyle.MATERIAL3 -> Color.Transparent
        DesignStyle.LIQUID_GLASS -> Color.White.copy(alpha = if (isLightTheme) 0.9f else 0.38f)
    }
    /** Content on an accent-filled control (M3: onPrimary; other styles: white as before). */
    val onAccent: Color get() = if (style == DesignStyle.MATERIAL3) md3.onPrimary else Color.White
    val accentDim: Color get() = if (style == DesignStyle.MATERIAL3) md3.secondaryContainer else palette.accentDim
    val accentLight: Color get() = if (style == DesignStyle.MATERIAL3) md3.primary else palette.accentLight
    val text: Color get() = if (style == DesignStyle.MATERIAL3) md3.onSurface else palette.text
    val muted: Color get() = when {
        style == DesignStyle.MATERIAL3 -> md3.onSurfaceVariant
        // Over saturated glass the palette greys lose contrast; use translucent text colour instead.
        style == DesignStyle.LIQUID_GLASS && !isLightTheme -> palette.text.copy(alpha = 0.66f)
        else -> palette.muted
    }
    val muted2: Color get() = when {
        style == DesignStyle.MATERIAL3 -> md3.onSurfaceVariant
        style == DesignStyle.LIQUID_GLASS && !isLightTheme -> palette.text.copy(alpha = 0.82f)
        else -> palette.muted2
    }
    val yellow: Color get() = palette.yellow
    val green: Color get() = palette.green
    val red: Color get() = palette.red
    val purple: Color get() = palette.purple
    val btnGradTop: Color get() = when (style) {
        DesignStyle.FLUENT -> palette.btnGradTop
        DesignStyle.MATERIAL3 -> md3.surfaceContainerHigh
        DesignStyle.LIQUID_GLASS -> Glass.fill(isLightTheme, 3)
    }
    val btnGradBottom: Color get() = when (style) {
        DesignStyle.FLUENT -> palette.btnGradBottom
        DesignStyle.MATERIAL3 -> md3.surfaceContainerHigh
        DesignStyle.LIQUID_GLASS -> Glass.fill(isLightTheme, 0)
    }
    val sheetBg: Color get() = when (style) {
        DesignStyle.FLUENT -> palette.sheetBg
        DesignStyle.MATERIAL3 -> md3.surfaceContainerLow
        DesignStyle.LIQUID_GLASS -> palette.bg.copy(alpha = 0.74f)
    }
}

val RussoOneFamily = FontFamily(
    Font(R.font.russo_one, FontWeight.Normal),
    Font(R.font.russo_one, FontWeight.Medium),
    Font(R.font.russo_one, FontWeight.SemiBold),
    Font(R.font.russo_one, FontWeight.Bold)
)

private fun createTypography(fontFamily: FontFamily): Typography {
    val d = Typography()
    return Typography(
        displayLarge = d.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = d.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = d.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = d.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = d.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = d.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = d.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = d.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = d.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = d.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = d.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = d.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = d.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = d.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = d.labelSmall.copy(fontFamily = fontFamily),
    )
}

@Composable
fun NexusTheme(
    fontScale: Float = 0.85f,
    appTheme: AppTheme = AppTheme.BASE,
    appFont: AppFont = AppFont.RUSSO_ONE,
    designStyle: DesignStyle = DesignStyle.FLUENT,
    customAccent: Int = 0xFF8B5CF6.toInt(),
    customBg: Int = 0xFF0A0812.toInt(),
    amoled: Boolean = false,
    lightMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val ctx = LocalContext.current
    CustomThemeColors.accent = Color(customAccent)
    CustomThemeColors.bg = Color(customBg)
    val basePalette = rememberThemePalette(appTheme, ctx)
    NX.palette = when {
        lightMode -> remember(basePalette) { lightVariant(basePalette) }
        amoled -> remember(basePalette) { amoledVariant(basePalette) }
        else -> basePalette
    }
    // Status-bar icons must be dark on the light theme.
    val view = androidx.compose.ui.platform.LocalView.current
    val lightBars = NX.palette.bg.luminance() > 0.5f
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = lightBars
                isAppearanceLightNavigationBars = lightBars
            }
        }
    }
    NX.style = designStyle
    // Material You: with the wallpaper colour theme on Android 12+, use the system dynamic scheme.
    val dynamicScheme = if (designStyle == DesignStyle.MATERIAL3 && appTheme == AppTheme.HOME_SCREEN &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    ) remember(ctx) { runCatching { dynamicDarkColorScheme(ctx) }.getOrNull() } else null
    NX.md3Override = dynamicScheme?.let { md3TonesFrom(it, light = false) }
    val density = LocalDensity.current
    val customDensity = Density(
        density = density.density,
        fontScale = density.fontScale * fontScale
    )
    val activeFontFamily = remember(appFont) {
        when (appFont) {
            AppFont.RUSSO_ONE -> RussoOneFamily
            AppFont.SYSTEM -> FontFamily.Default
        }
    }
    val typography = remember(activeFontFamily) {
        createTypography(activeFontFamily)
    }

    CompositionLocalProvider(
        LocalDensity provides customDensity,
        LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = activeFontFamily)
    ) {
        val scheme = if (designStyle == DesignStyle.MATERIAL3) NX.md3.toColorScheme() else darkColorScheme(
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
        MaterialTheme(colorScheme = scheme, typography = typography) {
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = activeFontFamily)
            ) {
                // ONE stable tree for every style: content() is always called from the same place,
                // so switching style/theme keeps the current tab and settings sub-screen.
                val glass = designStyle == DesignStyle.LIQUID_GLASS
                val haze = remember { dev.chrisbanes.haze.HazeState() }
                if (glass) GlassLightSensor()
                CompositionLocalProvider(LocalGlassHaze provides (if (glass) haze else null)) {
                    Box(Modifier.fillMaxSize()) {
                        if (glass) GlassBackdrop(Modifier.glassSource(zIndex = 0f))
                        Surface(
                            color = if (glass) Color.Transparent else NX.bg,
                            contentColor = NX.text,
                            modifier = Modifier.fillMaxSize(),
                        ) { content() }
                    }
                }
            }
        }
    }
}
