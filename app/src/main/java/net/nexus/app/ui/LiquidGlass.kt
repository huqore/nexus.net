package net.nexus.app.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlin.math.abs

/**
 * Where the light comes from, driven by the device's gravity sensor (like iOS Liquid Glass:
 * tilt the phone and the speculars slide across every glass surface).
 * x: -1 (light from the left) .. 1 (right); y: 0 = light from the top edge .. 1 = from the bottom.
 * Read only inside draw lambdas, so a change redraws the glass without recomposition.
 */
object GlassLight {
    var x by mutableFloatStateOf(-0.35f)
    var y by mutableFloatStateOf(0.0f)
}

/** Registers the gravity sensor while the app is resumed (Liquid Glass style only). */
@Composable
fun GlassLightSensor() {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    DisposableEffect(ctx, owner) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var sx = GlassLight.x
        var sy = GlassLight.y
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val g = SensorManager.GRAVITY_EARTH
                // Phone upright (gravity along -y of screen) -> light from the top edge.
                val tx = (-e.values[0] / g * 1.2f).coerceIn(-1f, 1f)
                val ty = (1f - (e.values[1] / g)).coerceIn(0f, 1f) * 0.6f
                sx += (tx - sx) * 0.12f
                sy += (ty - sy) * 0.12f
                if (abs(sx - GlassLight.x) > 0.004f) GlassLight.x = sx
                if (abs(sy - GlassLight.y) > 0.004f) GlassLight.y = sy
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
        }
        fun start() { if (sensor != null) sm?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI) }
        fun stop() { sm?.unregisterListener(listener) }
        val obs = LifecycleEventObserver { _, ev ->
            when (ev) {
                Lifecycle.Event.ON_RESUME -> start()
                Lifecycle.Event.ON_PAUSE -> stop()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(obs)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) start()
        onDispose {
            owner.lifecycle.removeObserver(obs)
            stop()
        }
    }
}

/**
 * Liquid Glass (iOS-like, CLEAR not frosted): almost no fill, the background stays sharp.
 * The glass reads through optics only, all following the device tilt (GlassLight):
 *  1. lens volume — a soft dark inner shadow on the side away from the light;
 *  2. specular hotspot — a small bright reflection hugging the lit edge;
 *  after the content:
 *  3. bright thin rim on the lit side fading round the shape;
 *  4. dark refraction rim on the far side (edge of a thick lens);
 *  5. faint chromatic fringe (dispersion).
 */
fun Modifier.liquidGlassLayers(shape: Shape, width: Dp, light: Boolean): Modifier = this.drawWithContent {
    val lx = GlassLight.x
    val ly = GlassLight.y
    val w = size.width
    val h = size.height
    val outline = shape.createOutline(size, layoutDirection, this)
    val litPoint = Offset(w * (0.5f + lx * 0.55f), h * ly)
    val farPoint = Offset(w * (0.5f - lx * 0.55f), h * (1f - ly * 0.4f))

    // 1. lens volume: darker towards the far edge
    drawRect(
        Brush.linearGradient(
            0f to Color.Transparent,
            0.6f to Color.Transparent,
            1f to Color.Black.copy(alpha = if (light) 0.06f else 0.22f),
            start = litPoint, end = farPoint,
        )
    )
    // 2. specular hotspot on the lit edge
    val hot = Offset(litPoint.x, (litPoint.y).coerceAtMost(h * 0.25f))
    val hotR = size.minDimension * 0.55f
    drawCircle(
        Brush.radialGradient(
            listOf(Color.White.copy(alpha = if (light) 0.35f else 0.16f), Color.White.copy(alpha = 0f)),
            center = hot, radius = hotR,
        ),
        radius = hotR, center = hot,
    )

    drawContent()

    val px = width.toPx()
    // 3. lit rim (crisp, bright towards the light)
    val litRim = Brush.linearGradient(
        0f to Color.White.copy(alpha = if (light) 1f else 0.95f),
        0.35f to Color.White.copy(alpha = if (light) 0.55f else 0.28f),
        0.75f to Color.White.copy(alpha = 0.04f),
        1f to Color.White.copy(alpha = if (light) 0.30f else 0.12f),
        start = litPoint, end = farPoint,
    )
    // 4. dark refraction rim on the far side, slightly inside
    val darkRim = Brush.linearGradient(
        0f to Color.Transparent,
        0.55f to Color.Transparent,
        1f to Color.Black.copy(alpha = if (light) 0.18f else 0.45f),
        start = litPoint, end = farPoint,
    )
    drawOutline(outline, darkRim, style = Stroke(width = px * 3.5f))
    // 5. dispersion
    translate(left = -lx * 0.9f, top = -0.7f) {
        drawOutline(outline, Color(0xFF7DD3FC).copy(alpha = if (light) 0.20f else 0.16f), style = Stroke(width = px))
    }
    translate(left = lx * 0.9f, top = 0.7f) {
        drawOutline(outline, Color(0xFFF0ABFC).copy(alpha = if (light) 0.16f else 0.12f), style = Stroke(width = px))
    }
    drawOutline(outline, litRim, style = Stroke(width = px * 1.3f))
}

private fun Int.dp(scope: androidx.compose.ui.unit.Density): Float = with(scope) { this@dp.toFloat() * density }
