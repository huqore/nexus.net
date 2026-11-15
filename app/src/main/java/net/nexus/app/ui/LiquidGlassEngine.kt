package net.nexus.app.ui

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Real Liquid Glass (iOS 26 style) on Android 13+.
 *
 * The backdrop is recorded into GraphicsLayers ([LiquidBackdrop.background]: the app background,
 * [LiquidBackdrop.scene]: background + screen content). Every glass element re-draws the part of
 * the backdrop behind it (with a margin) through a RenderEffect chain:
 *   blur (light) -> AGSL lens: rounded-rect SDF, content bent and magnified towards the edges,
 *   chromatic dispersion, vibrancy, adaptive tint, specular rim lit from [GlassLight].
 * Because a GraphicsLayer is a RenderNode reference, an animating backdrop updates the glass
 * without re-recording it; only moving the element (scroll) re-records its sample.
 */
object LiquidBackdrop {
    val background = BackdropHolder()
    val scene = BackdropHolder()
    /** The tab bar itself (glass + icons + labels): what the selection drop refracts. */
    val dock = BackdropHolder()

    /** RuntimeShader (AGSL) needs Android 13. Older devices keep the drawn glass (LiquidGlass.kt). */
    val supported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !disabled

    /** Set when the platform refused the shader (e.g. a test renderer): fall back for good. */
    @Volatile internal var disabled = false
}

class BackdropHolder {
    internal var layer by mutableStateOf<GraphicsLayer?>(null)
    internal var coords: LayoutCoordinates? = null
}

/** Look of one kind of glass. Distances in dp. */
data class GlassSpec(
    val blur: Float,          // backdrop blur radius
    val band: Float,          // width of the curved rim where light bends
    val strength: Float,      // how far the backdrop is pulled at the very edge
    val dispersion: Float,    // 0..1 chromatic split of the bent light
    val tintDark: Float,      // dark-theme tint (black) for legibility
    val tintLight: Float,     // light-theme tint (white)
    val rim: Float,           // specular rim intensity
    val saturation: Float = 1.25f,
    val darkTintWhite: Boolean = false, // dark themes: lighten instead of darken (glass buttons)
) {
    companion object {
        /** Cards, rows, dialogs: regular glass, legible text on top. */
        val Card = GlassSpec(blur = 0f, band = 18f, strength = 12f, dispersion = 0.10f, tintDark = 0.24f, tintLight = 0.16f, rim = 0.9f)
        /** Floating tab bar: clearer, refracts what scrolls under it. */
        val Bar = GlassSpec(blur = 3f, band = 24f, strength = 16f, dispersion = 0.14f, tintDark = 0.20f, tintLight = 0.14f, rim = 1.0f)
        /** Round connect button: a thick lens. */
        val Lens = GlassSpec(blur = 0f, band = 70f, strength = 30f, dispersion = 0.16f, tintDark = 0.10f, tintLight = 0.08f, rim = 1.2f, saturation = 1.3f, darkTintWhite = true)
        /** The sliding tab indicator: a clear magnifier. */
        val Drop = GlassSpec(blur = 0f, band = 16f, strength = 12f, dispersion = 0.14f, tintDark = 0.06f, tintLight = 0.10f, rim = 1.2f, saturation = 1.3f, darkTintWhite = true)
    }
}

// ---------------------------------------------------------------------------------------------
// Sources

/** Records this content (the app background) as the glass backdrop. */
fun Modifier.liquidBackgroundSource(): Modifier = this then BackdropSourceElement(LiquidBackdrop.background, includeBackground = false)

/** Records this content (the tab bar with its icons) for the drop that floats over it. */
fun Modifier.liquidDockSource(): Modifier = this then BackdropSourceElement(LiquidBackdrop.dock, includeBackground = false)

/** Records background + this content (the screen) for glass that floats above it (tab bar). */
fun Modifier.liquidSceneSource(): Modifier = this then BackdropSourceElement(LiquidBackdrop.scene, includeBackground = true)

private data class BackdropSourceElement(val holder: BackdropHolder, val includeBackground: Boolean) :
    ModifierNodeElement<BackdropSourceNode>() {
    override fun create() = BackdropSourceNode(holder, includeBackground)
    override fun update(node: BackdropSourceNode) { node.holder = holder; node.includeBackground = includeBackground }
    override fun InspectorInfo.inspectableProperties() { name = "liquidBackdropSource" }
}

private class BackdropSourceNode(var holder: BackdropHolder, var includeBackground: Boolean) :
    Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {
    private var layer: GraphicsLayer? = null

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) { holder.coords = coordinates }

    override fun ContentDrawScope.draw() {
        if (!LiquidBackdrop.supported || NX.style != net.nexus.app.data.DesignStyle.LIQUID_GLASS) {
            drawContent(); return
        }
        val l = layer ?: requireGraphicsContext().createGraphicsLayer().also { layer = it; holder.layer = it }
        val bg = LiquidBackdrop.background
        val bgLayer = if (includeBackground) bg.layer else null
        val bgOff = if (bgLayer != null) relativeOffset(holder.coords, bg.coords) else Offset.Zero
        l.record {
            if (bgLayer != null) translate(-bgOff.x, -bgOff.y) { drawLayer(bgLayer) }
            this@draw.drawContent()
        }
        drawLayer(l)
    }

    override fun onDetach() {
        layer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        if (holder.layer === layer) holder.layer = null
        layer = null
    }
}

// ---------------------------------------------------------------------------------------------
// Glass

/** Real refracting glass of [shape] sampling [source]. No-op (returns this) when unsupported. */
fun Modifier.liquidGlass(
    shape: Shape,
    spec: GlassSpec = GlassSpec.Card,
    source: BackdropHolder = LiquidBackdrop.background,
): Modifier = this then LiquidGlassElement(shape, spec, source)

private data class LiquidGlassElement(val shape: Shape, val spec: GlassSpec, val source: BackdropHolder) :
    ModifierNodeElement<LiquidGlassNode>() {
    override fun create() = LiquidGlassNode(shape, spec, source)
    override fun update(node: LiquidGlassNode) {
        node.shape = shape; node.spec = spec; node.source = source
        node.invalidateDraw()
    }
    override fun InspectorInfo.inspectableProperties() { name = "liquidGlass" }
}

private class LiquidGlassNode(var shape: Shape, var spec: GlassSpec, var source: BackdropHolder) :
    Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {
    private var layer: GraphicsLayer? = null
    private var coords: LayoutCoordinates? = null
    private var lens: LensShader? = null
    private var effectKey: List<Any>? = null
    private var cachedEffect: androidx.compose.ui.graphics.RenderEffect? = null

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        coords = coordinates
        invalidateDraw() // moved (scroll, animation): sample the backdrop at the new place
    }

    override fun ContentDrawScope.draw() {
        val src = source.layer
        if (src == null || !LiquidBackdrop.supported || size.minDimension <= 0f) {
            drawContent(); return
        }
        val l = layer ?: requireGraphicsContext().createGraphicsLayer().also { layer = it }
        val s = spec
        val pad = ceil((s.blur * 3f + 3f).dp.toPx()).toInt() // blur margin; the lens samples inwards
        val off = relativeOffset(coords, source.coords)
        val outline = shape.createOutline(size, layoutDirection, this)
        val radius = when (outline) {
            is Outline.Rounded -> outline.roundRect.topLeftCornerRadius.x
            is Outline.Rectangle -> 0f
            is Outline.Generic -> min(size.width, size.height) / 2f
        }
        val w = ceil(size.width).toInt() + pad * 2
        val h = ceil(size.height).toInt() + pad * 2

        // Light is quantised so tiny sensor jitter doesn't rebuild every glass effect.
        val qx = kotlin.math.round(GlassLight.x * 50f) / 50f
        val qy = kotlin.math.round(GlassLight.y * 50f) / 50f
        val key = listOf(size.width, size.height, radius, pad, s, qx, qy, NX.isLightTheme, density)
        val effect = cachedEffect.takeIf { key == effectKey } ?: try {
            val shader = lens ?: LensShader().also { lens = it }
            shader.effect(
                pad = pad.toFloat(), w = size.width, h = size.height, radius = radius,
                band = s.band.dp.toPx(), strength = s.strength.dp.toPx(), dispersion = s.dispersion,
                lightX = qx, lightY = qy, light = NX.isLightTheme,
                tintDark = s.tintDark, tintLight = s.tintLight, rim = s.rim, saturation = s.saturation,
                blurPx = s.blur.dp.toPx(), density = density, darkTintWhite = s.darkTintWhite,
            ).asComposeRenderEffect().also { cachedEffect = it; effectKey = key }
        } catch (t: Throwable) {
            LiquidBackdrop.disabled = true // renderer can't run AGSL: drawn glass from now on
            drawContent(); return
        }
        if (l.renderEffect !== effect) l.renderEffect = effect
        l.topLeft = IntOffset(-pad, -pad)
        l.record(IntSize(w, h)) {
            translate(pad - off.x, pad - off.y) { drawLayer(src) }
        }
        drawLayer(l)
        drawContent()
    }

    override fun onDetach() {
        layer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        layer = null
        coords = null
    }
}

private fun relativeOffset(child: LayoutCoordinates?, parent: LayoutCoordinates?): Offset {
    if (child == null || parent == null || !child.isAttached || !parent.isAttached) return Offset.Zero
    return child.positionInRoot() - parent.positionInRoot()
}

// ---------------------------------------------------------------------------------------------
// AGSL lens

private const val LENS_AGSL = """
uniform shader content;
uniform float2 uOrigin;     // top-left of the glass inside the sampled layer
uniform float2 uSize;       // glass size
uniform float uRadius;
uniform float uBand;        // curved rim width
uniform float uStrength;    // how far inside the rim samples
uniform float uDispersion;
uniform float2 uLight;      // direction the light comes FROM (unit, screen space)
uniform float4 uTint;       // rgb + amount
uniform float uRim;
uniform float uSaturation;
uniform float uPx;          // 1dp in px
uniform float uLightTheme;  // 1 on light themes

float sdRoundRect(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + float2(r);
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

// Straight (un-premultiplied) colour: blurred edges and anything not drawn must never read as dark.
float3 straight(float2 at) {
    half4 c = content.eval(at);
    if (c.a < 0.02) return uTint.rgb;
    return float3(c.rgb) / float(c.a);
}

half4 main(float2 xy) {
    float2 hs = uSize * 0.5;
    float2 p = xy - uOrigin - hs;
    float r = min(uRadius, min(hs.x, hs.y));
    float d = sdRoundRect(p, hs, r);                 // < 0 inside the glass
    if (d > 1.0) return half4(0.0);

    float e = 0.75;
    float2 g = float2(sdRoundRect(p + float2(e, 0.0), hs, r) - sdRoundRect(p - float2(e, 0.0), hs, r),
                      sdRoundRect(p + float2(0.0, e), hs, r) - sdRoundRect(p - float2(0.0, e), hs, r));
    float2 n = g / max(length(g), 1e-4);             // outward surface normal

    // Convex lens: flat in the middle, the rim samples from further inside, so what is under
    // the edge is magnified and bent (Liquid Glass). Samples never leave the glass.
    float t = clamp(1.0 + d / uBand, 0.0, 1.0);      // 0 deep inside .. 1 at the edge
    float curve = t * t * t;
    float2 bend = -n * curve * uStrength;

    float3 col;
    if (uDispersion > 0.001) {
        col.r = straight(xy + bend * (1.0 + 0.2 * uDispersion)).r;
        col.g = straight(xy + bend).g;
        col.b = straight(xy + bend * (1.0 - 0.2 * uDispersion)).b;
    } else {
        col = straight(xy + bend);
    }

    float luma = dot(col, float3(0.2126, 0.7152, 0.0722));
    col = mix(float3(luma), col, uSaturation);
    col = mix(col, uTint.rgb, uTint.a);

    // Specular rim: a crisp bright line on the edge, strongest where the surface faces the light,
    // a softer reflection on the opposite side and a gentle glow just inside the rim.
    float facing = dot(n, -uLight);
    float lit = pow(clamp(facing, 0.0, 1.0), 2.0);
    float back = pow(clamp(-facing, 0.0, 1.0), 2.0) * 0.5;
    float line = clamp(1.0 + d / (1.2 * uPx), 0.0, 1.0);
    float glow = clamp(1.0 + d / (8.0 * uPx), 0.0, 1.0);
    float rimA = (line * (0.30 + 0.70 * (lit + back)) + glow * glow * 0.20 * (lit + back)) * uRim;
    col = col + (1.0 - col) * clamp(rimA, 0.0, 1.0);

    // Dark themes: a whisper of shade inside the rim on the side away from the light.
    col *= 1.0 - (1.0 - uLightTheme) * curve * 0.10 * clamp(-facing * 0.5 + 0.5, 0.0, 1.0);
    // Light themes: white-on-white rims vanish, so the edge is drawn the way iOS light glass reads:
    // a fine darker line on the shadow side and a soft refraction shade just inside it.
    float away = clamp(-facing * 0.5 + 0.5, 0.0, 1.0);
    col *= 1.0 - uLightTheme * (line * 0.26 * away + glow * glow * 0.07 * away + curve * 0.05);

    float alpha = clamp(0.5 - d, 0.0, 1.0);          // anti-aliased edge
    return half4(half3(clamp(col, 0.0, 1.0)) * half(alpha), half(alpha));
}
"""

private class LensShader {
    private val impl: Any = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Impl() else Unit

    fun effect(
        pad: Float, w: Float, h: Float, radius: Float, band: Float, strength: Float, dispersion: Float,
        lightX: Float, lightY: Float, light: Boolean, tintDark: Float, tintLight: Float, rim: Float,
        saturation: Float, blurPx: Float, density: Float, darkTintWhite: Boolean = false,
    ): RenderEffect {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        return (impl as Impl).effect(pad, w, h, radius, band, strength, dispersion, lightX, lightY, light, tintDark, tintLight, rim, saturation, blurPx, density, darkTintWhite)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private class Impl {
        private val shader = RuntimeShader(LENS_AGSL)

        fun effect(
            pad: Float, w: Float, h: Float, radius: Float, band: Float, strength: Float, dispersion: Float,
            lightX: Float, lightY: Float, light: Boolean, tintDark: Float, tintLight: Float, rim: Float,
            saturation: Float, blurPx: Float, density: Float, darkTintWhite: Boolean,
        ): RenderEffect {
            // GlassLight: x -1..1 (left..right), y 0 (top) .. 1 (bottom) -> direction light comes from
            var lx = lightX
            var ly = -1f + lightY * 2f
            val len = sqrt(lx * lx + ly * ly).let { if (it < 1e-3f) 1f else it }
            lx /= len; ly /= len
            shader.setFloatUniform("uOrigin", pad, pad)
            shader.setFloatUniform("uSize", w, h)
            shader.setFloatUniform("uRadius", radius)
            shader.setFloatUniform("uBand", max(band, 1f))
            shader.setFloatUniform("uStrength", strength)
            shader.setFloatUniform("uDispersion", dispersion)
            shader.setFloatUniform("uLight", lx, ly)
            if (light) shader.setFloatUniform("uTint", 1f, 1f, 1f, tintLight)
            else if (darkTintWhite) shader.setFloatUniform("uTint", 1f, 1f, 1f, tintDark)
            else shader.setFloatUniform("uTint", 0f, 0f, 0f, tintDark)
            shader.setFloatUniform("uRim", if (light) rim * 0.85f else rim)
            shader.setFloatUniform("uSaturation", saturation)
            shader.setFloatUniform("uPx", density)
            shader.setFloatUniform("uLightTheme", if (light) 1f else 0f)
            val lens = RenderEffect.createRuntimeShaderEffect(shader, "content")
            return if (blurPx >= 0.5f) {
                RenderEffect.createChainEffect(lens, RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP))
            } else lens
        }
    }
}
