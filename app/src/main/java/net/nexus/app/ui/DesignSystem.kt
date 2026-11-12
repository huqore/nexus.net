package net.nexus.app.ui

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.nexus.app.data.DesignStyle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.cos
import kotlin.math.sin

/**
 * Material 3 tonal roles derived from the active colour theme's accent hue.
 * Dark themes get MD3 dark tones (surface ≈ T6…T22, primary ≈ T80), light themes the light set.
 */
@Immutable
data class Md3Tones(
    val isLight: Boolean,
    val surface: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val outline: Color,
    val outlineVariant: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
)

private fun hsv(h: Float, s: Float, v: Float, a: Float = 1f): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(((h % 360f) + 360f) % 360f, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f)))).copy(alpha = a)

private fun hueOf(c: Color): Float {
    val out = FloatArray(3)
    android.graphics.Color.colorToHSV(c.toArgb(), out)
    return out[0]
}

fun md3TonesFor(p: NexusColorPalette): Md3Tones {
    val h = hueOf(p.accent)
    val light = p.bg.luminance() > 0.5f
    val amoled = !light && p.bg.luminance() < 0.0005f
    if (amoled) return Md3Tones(
        isLight = false,
        surface = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = hsv(h, 0.25f, 0.05f),
        surfaceContainer = hsv(h, 0.26f, 0.085f),
        surfaceContainerHigh = hsv(h, 0.26f, 0.125f),
        surfaceContainerHighest = hsv(h, 0.24f, 0.165f),
        primary = hsv(h, 0.32f, 0.99f),
        onPrimary = hsv(h, 0.75f, 0.30f),
        primaryContainer = hsv(h, 0.70f, 0.50f),
        onPrimaryContainer = hsv(h, 0.12f, 1.0f),
        secondaryContainer = hsv(h, 0.45f, 0.30f),
        onSecondaryContainer = hsv(h, 0.10f, 0.95f),
        tertiary = hsv(h + 60f, 0.30f, 0.92f),
        outline = hsv(h, 0.08f, 0.50f),
        outlineVariant = hsv(h, 0.12f, 0.22f),
        onSurface = hsv(h, 0.05f, 0.92f),
        onSurfaceVariant = hsv(h, 0.08f, 0.80f),
    )
    return if (!light) Md3Tones(
        isLight = false,
        surface = hsv(h, 0.32f, 0.05f),
        surfaceContainerLowest = hsv(h, 0.30f, 0.045f),
        surfaceContainerLow = hsv(h, 0.30f, 0.10f),
        surfaceContainer = hsv(h, 0.30f, 0.145f),
        surfaceContainerHigh = hsv(h, 0.30f, 0.195f),
        surfaceContainerHighest = hsv(h, 0.28f, 0.245f),
        primary = hsv(h, 0.32f, 0.99f),
        onPrimary = hsv(h, 0.75f, 0.30f),
        primaryContainer = hsv(h, 0.70f, 0.50f),
        onPrimaryContainer = hsv(h, 0.12f, 1.0f),
        secondaryContainer = hsv(h, 0.45f, 0.36f),
        onSecondaryContainer = hsv(h, 0.10f, 0.95f),
        tertiary = hsv(h + 60f, 0.30f, 0.92f),
        outline = hsv(h, 0.08f, 0.58f),
        outlineVariant = hsv(h, 0.10f, 0.29f),
        onSurface = hsv(h, 0.05f, 0.92f),
        onSurfaceVariant = hsv(h, 0.08f, 0.80f),
    ) else Md3Tones(
        isLight = true,
        surface = hsv(h, 0.03f, 0.99f),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = hsv(h, 0.04f, 0.97f),
        surfaceContainer = hsv(h, 0.05f, 0.95f),
        surfaceContainerHigh = hsv(h, 0.06f, 0.93f),
        surfaceContainerHighest = hsv(h, 0.07f, 0.90f),
        primary = hsv(h, 0.65f, 0.55f),
        onPrimary = Color.White,
        primaryContainer = hsv(h, 0.18f, 0.98f),
        onPrimaryContainer = hsv(h, 0.80f, 0.22f),
        secondaryContainer = hsv(h, 0.12f, 0.93f),
        onSecondaryContainer = hsv(h, 0.55f, 0.20f),
        tertiary = hsv(h + 60f, 0.55f, 0.50f),
        outline = hsv(h, 0.07f, 0.47f),
        outlineVariant = hsv(h, 0.08f, 0.80f),
        onSurface = hsv(h, 0.10f, 0.11f),
        onSurfaceVariant = hsv(h, 0.08f, 0.30f),
    )
}

/** Material You: tones straight from a (wallpaper-based) dynamic ColorScheme. */
fun md3TonesFrom(cs: ColorScheme, light: Boolean): Md3Tones = Md3Tones(
    isLight = light,
    surface = cs.surface,
    surfaceContainerLowest = cs.surfaceContainerLowest,
    surfaceContainerLow = cs.surfaceContainerLow,
    surfaceContainer = cs.surfaceContainer,
    surfaceContainerHigh = cs.surfaceContainerHigh,
    surfaceContainerHighest = cs.surfaceContainerHighest,
    primary = cs.primary,
    onPrimary = cs.onPrimary,
    primaryContainer = cs.primaryContainer,
    onPrimaryContainer = cs.onPrimaryContainer,
    secondaryContainer = cs.secondaryContainer,
    onSecondaryContainer = cs.onSecondaryContainer,
    tertiary = cs.tertiary,
    outline = cs.outline,
    outlineVariant = cs.outlineVariant,
    onSurface = cs.onSurface,
    onSurfaceVariant = cs.onSurfaceVariant,
)

fun Md3Tones.toColorScheme(): ColorScheme = if (isLight) lightColorScheme(
    primary = primary, onPrimary = onPrimary,
    primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
    secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    background = surface, onBackground = onSurface,
    surface = surface, onSurface = onSurface,
    surfaceVariant = surfaceContainerHighest, onSurfaceVariant = onSurfaceVariant,
    surfaceContainerLowest = surfaceContainerLowest, surfaceContainerLow = surfaceContainerLow,
    surfaceContainer = surfaceContainer, surfaceContainerHigh = surfaceContainerHigh,
    surfaceContainerHighest = surfaceContainerHighest,
    outline = outline, outlineVariant = outlineVariant,
) else darkColorScheme(
    primary = primary, onPrimary = onPrimary,
    primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
    secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    background = surface, onBackground = onSurface,
    surface = surface, onSurface = onSurface,
    surfaceVariant = surfaceContainerHighest, onSurfaceVariant = onSurfaceVariant,
    surfaceContainerLowest = surfaceContainerLowest, surfaceContainerLow = surfaceContainerLow,
    surfaceContainer = surfaceContainer, surfaceContainerHigh = surfaceContainerHigh,
    surfaceContainerHighest = surfaceContainerHighest,
    outline = outline, outlineVariant = outlineVariant,
)

/** Liquid Glass fills: translucent white on dark themes, frosted white on light ones. */
internal object Glass {
    // Clear glass: barely tinted, the background must stay visible (iOS Liquid Glass, not frosted).
    fun fill(light: Boolean, level: Int): Color = if (light) Color.White.copy(alpha = 0.16f + level * 0.06f)
    else Color.White.copy(alpha = 0.018f + level * 0.014f)

    /** Specular rim: bright where light hits (top-left), fading, with a faint return at bottom-right. */
    fun rim(light: Boolean): Brush = Brush.linearGradient(
        0f to Color.White.copy(alpha = if (light) 1f else 0.62f),
        0.35f to Color.White.copy(alpha = if (light) 0.45f else 0.10f),
        0.65f to Color.White.copy(alpha = if (light) 0.30f else 0.05f),
        1f to Color.White.copy(alpha = if (light) 0.85f else 0.26f),
        start = Offset.Zero, end = Offset.Infinite,
    )

    /** Fine grain so large glass areas don't look like flat plastic. */
    val noise: ImageBitmap by lazy {
        val n = 96
        val bmp = android.graphics.Bitmap.createBitmap(n, n, android.graphics.Bitmap.Config.ARGB_8888)
        val rnd = java.util.Random(7)
        for (y in 0 until n) for (x in 0 until n) {
            val v = rnd.nextInt(256)
            bmp.setPixel(x, y, android.graphics.Color.argb(255, v, v, v))
        }
        bmp.asImageBitmap()
    }
}

/** Haze state shared by the Liquid Glass backdrop/content (sources) and glass bars (effects). */
val LocalGlassHaze = staticCompositionLocalOf<HazeState?> { null }

/** Marks content that frosted glass bars should blur (no-op outside Liquid Glass). */
@Composable
fun Modifier.glassSource(zIndex: Float = 0f): Modifier {
    val haze = LocalGlassHaze.current
    return if (haze != null && NX.style == DesignStyle.LIQUID_GLASS) this.then(Modifier.hazeSource(haze, zIndex = zIndex)) else this
}

/**
 * Real backdrop blur (Android 12+) of everything behind this element, iOS-style:
 * strong blur, light tint, a little grain. Older Android gets a denser tint instead.
 */
@Composable
fun Modifier.glassBlur(): Modifier {
    val haze = LocalGlassHaze.current ?: return this
    if (NX.style != DesignStyle.LIQUID_GLASS) return this
    val light = NX.isLightTheme
    val base = NX.palette.bg
    return this.then(
        Modifier.hazeEffect(
            state = haze,
            style = HazeStyle(
                backgroundColor = base,
                tints = listOf(HazeTint(if (light) Color.White.copy(alpha = 0.30f) else base.copy(alpha = 0.22f))),
                blurRadius = 20.dp,
                noiseFactor = 0.08f,
                fallbackTint = HazeTint(base.copy(alpha = 0.86f)),
            ),
        )
    )
}

/**
 * Card outline that follows the design style:
 * Fluent — the classic 1dp tinted border; Material 3 — none (tonal elevation instead);
 * Liquid Glass — layered glass: depth gradient, top-left specular bloom, bottom caustic,
 * fine grain under the content, and a directional specular rim.
 */
fun Modifier.nxBorder(shape: Shape, width: Dp = 1.dp, color: Color = NX.cardBorder): Modifier = when (NX.style) {
    DesignStyle.FLUENT -> this.border(width, color, shape)
    // Material You: same geometry as Fluent; 1dp outlineVariant contour on tonal containers.
    DesignStyle.MATERIAL3 -> this.border(width, NX.md3.outlineVariant, shape)
    // Liquid Glass: six live layers, speculars follow device tilt (see LiquidGlass.kt).
    DesignStyle.LIQUID_GLASS -> this.liquidGlassLayers(shape, width, NX.isLightTheme)
}

/**
 * Backdrop for Liquid Glass: deep space with slowly drifting saturated colour fields in the
 * theme's hues and a faint star field. Glass needs colour and detail behind it to read as glass.
 */
@Composable
fun GlassBackdrop(modifier: Modifier = Modifier) {
    val p = NX.palette
    val h = hueOf(p.accent)
    val light = NX.isLightTheme
    val t by rememberInfiniteTransition(label = "glassBackdrop").animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(40_000, easing = LinearEasing), RepeatMode.Restart),
        label = "glassPhase",
    )
    // Darker, calmer field built ONLY from the theme accent (two depths of it), so the colour
    // source is obvious: blue theme -> blue glow, red -> red, etc.
    val a = if (light) 0.40f else 0.42f
    val blobs = remember(p, light) {
        listOf(
            Triple(p.accent.copy(alpha = a), Offset(0.18f, 0.10f), 0.62f),
            Triple(p.accent.copy(alpha = a * 0.55f), Offset(0.90f, 0.42f), 0.55f),
            Triple(p.accentDim.copy(alpha = if (light) 0.35f else 0.9f), Offset(0.25f, 0.80f), 0.70f),
        )
    }
    val stars = remember {
        val r = java.util.Random(42)
        List(90) { Triple(r.nextFloat(), r.nextFloat(), r.nextFloat()) }
    }
    Canvas(modifier.fillMaxSize()) {
        drawRect(if (light) p.bg else if (p.bg.luminance() < 0.0005f) Color.Black else hsv(h, 0.40f, 0.035f))
        blobs.forEachIndexed { i, (color, base, rr) ->
            val cx = (base.x + 0.08f * sin(t + i * 2.1f)) * size.width
            val cy = (base.y + 0.06f * cos(t * 0.8f + i * 1.3f)) * size.height
            val radius = rr * size.maxDimension * 0.62f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(color, color.copy(alpha = color.alpha * 0.35f), Color.Transparent),
                    center = Offset(cx, cy), radius = radius,
                ),
                radius = radius, center = Offset(cx, cy),
            )
        }
        if (!light) {
            stars.forEach { (x, y, s) ->
                drawCircle(
                    Color.White.copy(alpha = 0.15f + s * 0.45f),
                    radius = 0.6f + s * 1.2f,
                    center = Offset(x * size.width, y * size.height),
                )
            }
            // Veil keeps white text readable over saturated colour.
            drawRect(Color.Black.copy(alpha = 0.48f))
        }
    }
}

/**
 * Corner shape for a card/container in the current style.
 * Material 3: "medium" corner radius (12dp), the spec default for cards.
 * Liquid Glass: slightly softer than Fluent (closer to iOS continuous corners).
 * Fluent: unchanged.
 */
fun nxCardShape(fluent: Dp): androidx.compose.foundation.shape.RoundedCornerShape =
    androidx.compose.foundation.shape.RoundedCornerShape(
        when (NX.style) {
            DesignStyle.MATERIAL3 -> fluent
            DesignStyle.LIQUID_GLASS -> fluent + 4.dp
            DesignStyle.FLUENT -> fluent
        }
    )

/**
 * Switch colours per style. Material 3 uses the stock M3 tokens of the M3 colour scheme
 * (checked: primary track + onPrimary handle; unchecked: surfaceContainerHighest track,
 * outline border/handle). Liquid Glass: frosted track. Fluent: unchanged.
 */
@Composable
fun nxSwitchColors(fluentUncheckedTrack: Color): androidx.compose.material3.SwitchColors = when (NX.style) {
    DesignStyle.MATERIAL3 -> androidx.compose.material3.SwitchDefaults.colors()
    DesignStyle.LIQUID_GLASS -> androidx.compose.material3.SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = NX.accent.copy(alpha = 0.85f),
        checkedBorderColor = Color.White.copy(alpha = 0.35f),
        uncheckedThumbColor = Color.White.copy(alpha = if (NX.isLightTheme) 1f else 0.75f),
        uncheckedTrackColor = Glass.fill(NX.isLightTheme, 2),
        uncheckedBorderColor = Color.White.copy(alpha = if (NX.isLightTheme) 0.9f else 0.25f),
    )
    DesignStyle.FLUENT -> androidx.compose.material3.SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = NX.accent,
        uncheckedThumbColor = NX.muted,
        uncheckedTrackColor = fluentUncheckedTrack,
    )
}

/**
 * Dialog container per style.
 * Material 3: surfaceContainerHigh, 28dp ("extra-large") corners, no outline (dialog spec).
 * Liquid Glass: dialogs live in their own window (no backdrop blur), so use a dense frosted fill
 * plus the glass rim. Fluent: unchanged (card colour + tinted border).
 */
fun Modifier.nxDialogSurface(shape: Shape): Modifier = when (NX.style) {
    DesignStyle.MATERIAL3 -> {
        val s = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
        this.clip(s).background(NX.md3.surfaceContainerHigh)
    }
    DesignStyle.LIQUID_GLASS -> this.clip(shape)
        .background(NX.palette.bg.copy(alpha = if (NX.isLightTheme) 0.90f else 0.86f))
        .background(Glass.fill(NX.isLightTheme, 1))
        .nxBorder(shape)
    DesignStyle.FLUENT -> this.clip(shape).background(NX.card).nxBorder(shape)
}

/** True inside an M3 Expressive segmented list group: rows draw their own segment container. */
val LocalSegmentedList = staticCompositionLocalOf { false }

/** Gap between list rows: 2dp segments in M3 Expressive, the classic spacing elsewhere. */
fun nxListGap(fluent: Dp): Dp = if (NX.style == DesignStyle.MATERIAL3) 2.dp else fluent

/**
 * M3 Expressive segmented list item container: 4dp inner corners (the group clip gives the
 * 16-20dp outer ones); a selected item morphs to 16dp all round on primaryContainer.
 */
@Composable
fun Modifier.m3Segment(selected: Boolean = false): Modifier {
    val radius by androidx.compose.animation.core.animateDpAsState(
        if (selected) 16.dp else 4.dp,
        androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "segmentMorph",
    )
    val bg by androidx.compose.animation.animateColorAsState(
        if (selected) NX.md3.primaryContainer else NX.md3.surfaceContainerHigh, label = "segmentBg",
    )
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(radius)
    return this.clip(shape).background(bg, shape)
}

/** Button shape: M3 Expressive buttons are fully rounded pills; other styles keep their radius. */
fun nxButtonShape(fluent: Dp): androidx.compose.foundation.shape.RoundedCornerShape =
    androidx.compose.foundation.shape.RoundedCornerShape(fluent)
