package net.nexus.app.ui

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import net.nexus.app.vpn.VpnStatus
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.tanh

/**
 * Connect button of the Liquid Glass style (Android 13+): a drop of liquid glass, tuned live with
 * the owner on a prototype. It always flows a little; tap = quick squeeze, pops ~2% bigger, settles;
 * turning ON also sends a glass shock wave across the screen ([ShockWave]); connected = M3 "cookie"
 * bumps swell one after another as a wave runs round the drop; dragging makes it flow towards the
 * finger; tilting the phone moves the liquid and the light.
 */
object DropTuning {
    const val REFRACTION = 0.79f   // edge lensing
    const val DISPERSION = 0.75f   // rainbow fringe at the edge
    const val SPIKES = 1.0f        // cookie bump height
    const val LOBES = 14f          // number of bumps
    const val WAVE_SPEED = 1.275f  // bump wave, rad/s
    const val IDLE = 1.0f          // flow at rest
    const val DAMPING = 34f        // "Текучесть 0": calm, no jelly wobble
    const val STIFFNESS = 170f
    const val PULL_DP = 21f        // how far the centre follows the finger
    const val STRETCH = 0.14f      // how far the near side reaches for it
}

private class Spring(var x: Float) { var v = 0f; var to = x
    fun step(k: Float, z: Float, dt: Float) { v += ((to - x) * k - v * z) * dt; x += v * dt }
}

/** Tap pulse: squeeze in 90 ms -> out to 1.02 -> settle; total 0.6 s. */
internal fun dropPulse(a: Float): Float {
    fun ease(x: Float) = x.coerceIn(0f, 1f).let { it * it * (3 - 2 * it) }
    return when {
        a < 0f || a >= 0.60f -> 1f
        a < 0.09f -> 1f - 0.10f * ease(a / 0.09f)
        a < 0.26f -> 0.90f + 0.12f * ease((a - 0.09f) / 0.17f)
        else -> 1.02f - 0.02f * ease((a - 0.26f) / 0.34f)
    }
}

private class DropPhysics {
    val scale = Spring(1f); val glow = Spring(0f); val cookie = Spring(0f); val on = Spring(0f)
    val stretch = Spring(0f); val ox = Spring(0f); val oy = Spring(0f); val tx = Spring(0f); val ty = Spring(0f)
    // shaking the phone: the liquid lags behind and sways back a few times (underdamped, small)
    val shx = Spring(0f); val shy = Spring(0f)
    var ang = 0f
    var t = 0f
    var pulseAt = -99f
    var waveDue = false

    fun step(dt: Float) {
        t += dt
        val k = DropTuning.STIFFNESS; val z = DropTuning.DAMPING
        scale.step(k * 1.2f, z, dt); glow.step(60f, 14f, dt)
        stretch.step(k * 0.8f, z, dt); ox.step(k * 0.7f, z, dt); oy.step(k * 0.7f, z, dt)
        cookie.step(60f, 10f, dt); on.step(30f, 11f, dt)
        tx.step(30f, 10f, dt); ty.step(30f, 10f, dt)
        shx.step(110f, 5f, dt); shy.step(110f, 5f, dt)
        val pa = t - pulseAt
        if (pa < 0.6f) { scale.x = dropPulse(pa); scale.v = 0f }
    }

    /** Something is visibly moving beyond the slow idle flow: draw every frame. */
    fun busy(): Boolean = t - pulseAt < 0.7f ||
        listOf(scale, glow, cookie, on, stretch, ox, oy, shx, shy).any { abs(it.v) > 0.002f || abs(it.to - it.x) > 0.002f }
}

@Composable
fun LiquidDropButton(
    status: VpnStatus,
    buttonSize: Dp,
    iconTint: Color,
    onClick: () -> Unit,
) {
    val connected = status == VpnStatus.CONNECTED
    val connecting = status == VpnStatus.CONNECTING
    val phys = remember { DropPhysics() }
    phys.cookie.to = if (connected) 1f else if (connecting) 0.6f else 0f
    phys.on.to = if (connected) 1f else 0f
    val currentStatus by rememberUpdatedState(status)
    val click by rememberUpdatedState(onClick)
    val shader = remember { DropShader() }
    val layer = rememberGraphicsLayer()
    val coordsRef = remember { arrayOfNulls<LayoutCoordinates>(1) }
    var frame by remember { mutableLongStateOf(0L) }

    // Shake: linear acceleration (gravity removed) kicks the liquid the other way; tiny on purpose
    val ctx = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.DisposableEffect(ctx) {
        val sm = ctx.getSystemService(android.content.Context.SENSOR_SERVICE) as? android.hardware.SensorManager
        val sensor = sm?.getDefaultSensor(android.hardware.Sensor.TYPE_LINEAR_ACCELERATION)
        val dp = ctx.resources.displayMetrics.density
        val listener = object : android.hardware.SensorEventListener {
            override fun onSensorChanged(e: android.hardware.SensorEvent) {
                val ax = e.values[0]; val ay = e.values[1]          // device x right, y up (m/s^2)
                if (abs(ax) < 1.2f && abs(ay) < 1.2f) return       // hand tremor / noise
                val k = 5f * dp
                phys.shx.v = (phys.shx.v - ax * k).coerceIn(-90f * dp, 90f * dp)
                phys.shy.v = (phys.shy.v + ay * k).coerceIn(-90f * dp, 90f * dp)  // screen y points down
            }
            override fun onAccuracyChanged(s: android.hardware.Sensor?, a: Int) = Unit
        }
        if (sensor != null) sm.registerListener(listener, sensor, android.hardware.SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm?.unregisterListener(listener) }
    }

    // Physics clock: full rate while something moves, ~30 fps for the slow flow at rest.
    // withFrameNanos suspends when nothing is drawn (app in background): no work then.
    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1e9f).coerceIn(0f, 0.05f); last = now
            phys.tx.to = GlassLight.x.coerceIn(-1f, 1f)
            phys.ty.to = (0.6f - GlassLight.y).coerceIn(0f, 0.6f)   // upright phone: liquid settles down
            repeat(4) { phys.step(dt / 4f) }
            if (phys.waveDue && phys.t - phys.pulseAt >= 0.09f) {
                phys.waveDue = false
                coordsRef[0]?.takeIf { it.isAttached }?.let { c ->
                    ShockWave.fire(c.positionInRoot() + Offset(c.size.width / 2f, c.size.height / 2f))
                }
            }
            frame = now
            if (!phys.busy()) delay(33)
        }
    }

    val canvas = buttonSize + 96.dp // room for bumps, stretch, drift and the shadow
    Box(
        Modifier
            .size(buttonSize)
            .semantics(mergeDescendants = true) { role = Role.Button; onClick { click(); true } }
            .pointerInput(Unit) {
                val r = (buttonSize.toPx() * 0.42f)
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val c = Offset(size.width / 2f, size.height / 2f)
                    if ((down.position - c).getDistance() > r * 1.2f) return@awaitEachGesture
                    val t0 = System.currentTimeMillis()
                    var moved = false
                    phys.glow.to = 1f
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                        if (!ch.pressed) break
                        val d = ch.position - c
                        if ((ch.position - down.position).getDistance() > 8.dp.toPx()) moved = true
                        val dist = hypot(d.x, d.y)
                        val pull = DropTuning.PULL_DP.dp.toPx() * tanh(dist / 120.dp.toPx())
                        if (dist > 0f) { phys.ox.to = d.x / dist * pull; phys.oy.to = d.y / dist * pull }
                        phys.stretch.to = DropTuning.STRETCH * tanh(dist / 160.dp.toPx())
                        if (dist > 4.dp.toPx()) phys.ang = atan2(d.y, d.x)
                        ch.consume()
                    }
                    phys.glow.to = 0f; phys.stretch.to = 0f; phys.ox.to = 0f; phys.oy.to = 0f
                    if (!moved && System.currentTimeMillis() - t0 < 450) {
                        phys.pulseAt = phys.t
                        // turning ON: the drop fires a glass shock wave as it pops back out
                        phys.waveDue = currentStatus == VpnStatus.DISCONNECTED || currentStatus == VpnStatus.ERROR
                        click()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.requiredSize(canvas).onGloballyPositioned { coordsRef[0] = it }) {
            frame // redraw on every physics tick
            val src = LiquidBackdrop.background.layer ?: return@Canvas
            val off = rootOffset(coordsRef[0], LiquidBackdrop.background.coords)
            val dp = density
            val effect = runCatching {
                shader.effect(
                    cx = size.width / 2f + phys.ox.x + phys.tx.x * 6f * dp + phys.shx.x.coerceIn(-8f * dp, 8f * dp),
                    cy = size.height / 2f + phys.oy.x + phys.ty.x * 6f * dp + phys.shy.x.coerceIn(-8f * dp, 8f * dp),
                    r = (buttonSize.toPx() / 2f) * 0.84f, phys = phys, px = dp, light = NX.isLightTheme,
                    tint = NX.green,
                )
            }.getOrElse { LiquidBackdrop.disabled = true; return@Canvas }
            layer.renderEffect = effect.asComposeRenderEffect()
            layer.record(IntSize(size.width.toInt(), size.height.toInt())) {
                translate(-off.x, -off.y) { drawLayer(src) }
            }
            drawLayer(layer)
        }
        // The icon sits deeper inside: it follows the surface a bit less (parallax)
        Icon(
            Icons.Filled.PowerSettingsNew, null, tint = iconTint,
            modifier = Modifier.size(84.dp).graphicsLayer {
                frame
                translationX = (phys.ox.x + phys.shx.x.coerceIn(-8f * density, 8f * density)) * 0.75f + phys.tx.x * 3f * density
                translationY = (phys.oy.x + phys.shy.x.coerceIn(-8f * density, 8f * density)) * 0.75f + phys.ty.x * 3f * density
                scaleX = phys.scale.x; scaleY = phys.scale.x
            },
        )
    }
}

private fun rootOffset(child: LayoutCoordinates?, parent: LayoutCoordinates?): Offset {
    if (child == null || parent == null || !child.isAttached || !parent.isAttached) return Offset.Zero
    return child.positionInRoot() - parent.positionInRoot()
}

// ---------------------------------------------------------------------------------------------

private const val DROP_AGSL = """
uniform shader content;
uniform float2 uC;
uniform float2 uTilt;
uniform float3 uTint;
uniform float uR, uT, uCookie, uSpike, uLobes, uSpeed, uIdle, uStretch, uAng, uScale, uOn, uGlow, uRefr, uDisp, uPx, uLightTheme;

float rad(float a) {
  float r = 1.0 + uIdle * (0.014 * sin(2.0 * a + uT * 0.9) + 0.010 * sin(3.0 * a - uT * 0.7)) + uIdle * 0.008 * sin(uT * 1.4);
  float wave = pow(0.5 + 0.5 * cos(a - uT * uSpeed), 2.2);
  r += uCookie * uSpike * 0.09 * (0.25 + 0.75 * wave) * (0.5 + 0.5 * cos(uLobes * (a - uT * 0.15)));
  float c = cos(a - uAng); float f = max(c, 0.0);
  r += uStretch * (f * f + (c * c - 0.5) * 0.45);
  r += 0.035 * (cos(a) * uTilt.x + sin(a) * uTilt.y);
  return uR * uScale * r;
}
float sdf(float2 p) { return length(p) - rad(atan(p.y, p.x)); }
float3 bg(float2 q) { half4 c = content.eval(q); return c.a < 0.02 ? float3(0.0) : float3(c.rgb) / float(c.a); }

half4 main(float2 xy) {
  float2 p = xy - uC;
  float d = sdf(p);
  float s = sdf(p - float2(-uTilt.x * 6.0 * uPx, 16.0 * uPx));
  float shadowA = 0.30 * (1.0 - smoothstep(-20.0 * uPx, 34.0 * uPx, s)) * (1.0 - 0.6 * uLightTheme);
  if (d > 1.5) return half4(0.0, 0.0, 0.0, half(shadowA));

  float e = 0.75;
  float2 n = normalize(float2(sdf(p + float2(e, 0.0)) - sdf(p - float2(e, 0.0)), sdf(p + float2(0.0, e)) - sdf(p - float2(0.0, e))));
  // flat in the middle, curved at the rim: the refraction lives at the edge
  float x = clamp(-d / (uR * 0.45), 0.0, 1.0);
  float bend = pow(1.0 - x, 3.0);
  float2 off = -n * bend * uR * 0.45 * uRefr - p * 0.05;
  float ca = 0.10 * uDisp * bend;
  float3 g = float3(bg(xy + off * (1.0 + ca)).r, bg(xy + off).g, bg(xy + off * (1.0 - ca)).b);

  float2 L = normalize(float2(-0.45 - uTilt.x * 0.5, -0.75 - uTilt.y * 0.5));
  float lit = max(dot(n, L), 0.0); float back = max(dot(n, -L), 0.0);
  float rim = exp(d / (1.4 * uPx));
  float band = exp(d / (uR * 0.10));
  g += rim * (0.10 + 0.90 * pow(lit, 1.4)) * (0.85 + 0.4 * uGlow);
  g += rim * pow(back, 2.0) * 0.35;
  g += band * (0.05 + 0.14 * pow(lit, 2.0)) * (1.0 + uGlow);
  g += 0.015 + uGlow * 0.045;
  g = mix(g, g * (0.85 + 0.3 * uTint), uOn * 0.3);
  // light themes: white rims vanish on white, so the far edge gets a fine darker line instead
  g *= 1.0 - uLightTheme * (rim * 0.25 * (0.4 + back) + band * 0.05);

  float a = 1.0 - smoothstep(-0.75, 0.75, d);
  float outA = a + shadowA * (1.0 - a);
  return half4(half3(clamp(g, 0.0, 1.0) * a), half(outA));
}
"""

private class DropShader {
    private val impl: Any? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Impl() else null

    fun effect(cx: Float, cy: Float, r: Float, phys: DropPhysics, px: Float, light: Boolean, tint: Color): RenderEffect {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        return (impl as Impl).effect(cx, cy, r, phys, px, light, tint)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private class Impl {
        private val s = RuntimeShader(DROP_AGSL)
        fun effect(cx: Float, cy: Float, r: Float, p: DropPhysics, px: Float, light: Boolean, tint: Color): RenderEffect {
            s.setFloatUniform("uC", cx, cy)
            s.setFloatUniform("uTilt", p.tx.x, p.ty.x)
            s.setFloatUniform("uTint", tint.red, tint.green, tint.blue)
            s.setFloatUniform("uR", r)
            s.setFloatUniform("uT", p.t)
            s.setFloatUniform("uCookie", p.cookie.x.coerceAtLeast(0f))
            s.setFloatUniform("uSpike", DropTuning.SPIKES)
            s.setFloatUniform("uLobes", DropTuning.LOBES)
            s.setFloatUniform("uSpeed", DropTuning.WAVE_SPEED)
            s.setFloatUniform("uIdle", DropTuning.IDLE)
            // while not dragged, a shake stretches the drop a touch along the sway
            val shake = kotlin.math.hypot(p.shx.x, p.shy.x)
            val dragging = p.stretch.x > 0.005f
            s.setFloatUniform("uStretch", p.stretch.x + if (dragging) 0f else (shake / r * 0.5f).coerceAtMost(0.06f))
            s.setFloatUniform("uAng", if (dragging || shake < 0.5f) p.ang else kotlin.math.atan2(p.shy.x, p.shx.x))
            s.setFloatUniform("uScale", p.scale.x)
            s.setFloatUniform("uOn", p.on.x)
            s.setFloatUniform("uGlow", p.glow.x)
            s.setFloatUniform("uRefr", DropTuning.REFRACTION)
            s.setFloatUniform("uDisp", DropTuning.DISPERSION)
            s.setFloatUniform("uPx", px)
            s.setFloatUniform("uLightTheme", if (light) 1f else 0f)
            return RenderEffect.createRuntimeShaderEffect(s, "content")
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Shock wave

/** A glass shock wave rolling out from [origin] (root px) across the whole screen. */
object ShockWave {
    internal var origin = Offset.Zero
    internal var fired by mutableLongStateOf(0L)
    fun fire(at: Offset) { origin = at; fired = System.nanoTime() }
}

private const val WAVE_AGSL = """
uniform shader content;
uniform float2 uC;
uniform float uAge, uR, uPx;

half4 main(float2 xy) {
  float2 p = xy - uC;
  float dist = length(p);
  float front = uR + 820.0 * uPx * (1.0 - exp(-uAge * 2.4));   // bursts out fast, then slows
  float fade = 1.0 - smoothstep(0.25, 1.7, uAge);
  float w = (46.0 + uAge * 70.0) * uPx;
  float u = (dist - front) / w;
  float u2 = (dist - front + w * 2.6) / (w * 0.8);             // faint echo behind it
  float prof = u * exp(-u * u) * 2.3 - u2 * exp(-u2 * u2) * 0.5;
  float flash = 0.04 * exp(-uAge * 7.0);
  if (abs(prof) * fade < 0.002) { half4 c = content.eval(xy); return half4(c.rgb + half(flash) * c.a, c.a); }
  float2 dir = p / max(dist, 1.0);
  float2 warp = -dir * prof * 26.0 * uPx * fade;
  float ca = 0.15 * abs(prof) * fade;
  half4 cg = content.eval(xy + warp);
  float3 col = float3(content.eval(xy + warp * (1.0 + ca)).r, cg.g, content.eval(xy + warp * (1.0 - ca)).b);
  float2 L = normalize(float2(-0.45, -0.75));
  float crest = (exp(-u * u * 3.0) + 0.45 * exp(-u2 * u2 * 3.0)) * fade * (0.55 + 0.45 * max(dot(dir, L), 0.0));
  col += (crest * 0.08 + flash) * float(cg.a);
  return half4(half3(col), cg.a);
}
"""

/** Screen-wide post effect for [ShockWave]; free (no effect at all) when no wave is running. */
fun Modifier.shockWave(): Modifier = composed {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@composed this
    val wave = remember { WaveShader() }
    var age by remember { mutableFloatStateOf(-1f) }
    val fired = ShockWave.fired
    LaunchedEffect(fired) {
        if (fired == 0L) return@LaunchedEffect
        val t0 = withFrameNanos { it }
        while (true) {
            val a = (withFrameNanos { it } - t0) / 1e9f
            if (a > 1.8f) { age = -1f; break }
            age = a
        }
    }
    this.graphicsLayer {
        val a = age
        renderEffect = if (a >= 0f) runCatching {
            wave.effect(ShockWave.origin, a, 96.dp.toPx(), density).asComposeRenderEffect()
        }.getOrNull() else null
    }
}

private class WaveShader {
    private val impl: Any? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Impl() else null
    fun effect(c: Offset, age: Float, r: Float, px: Float): RenderEffect {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        return (impl as Impl).effect(c, age, r, px)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private class Impl {
        private val s = RuntimeShader(WAVE_AGSL)
        fun effect(c: Offset, age: Float, r: Float, px: Float): RenderEffect {
            s.setFloatUniform("uC", c.x, c.y)
            s.setFloatUniform("uAge", age)
            s.setFloatUniform("uR", r)
            s.setFloatUniform("uPx", px)
            return RenderEffect.createRuntimeShaderEffect(s, "content")
        }
    }
}

