package net.nexus.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** One star: position 0..1, size 0..1, own twinkle speed and phase. */
private class Star(val x: Float, val y: Float, val s: Float, val speed: Float, val phase: Float)

/** Seconds between shooting stars and how long one flies. */
internal const val SHOOTING_EVERY_S = 10f
internal const val SHOOTING_FLIGHT_S = 0.9f

/**
 * Twinkling star field with a shooting star every [SHOOTING_EVERY_S] seconds.
 * Redraws at ~15 fps while only twinkling (slow, the eye can't tell, and every backdrop frame makes
 * the glass re-render) and at full frame rate only while a shooting star is in the air.
 * withFrameNanos suspends while nothing is drawn, so it costs nothing in the background.
 */
@Composable
fun StarField(modifier: Modifier = Modifier) {
    val stars = remember {
        val r = java.util.Random(42)
        List(90) { Star(r.nextFloat(), r.nextFloat(), r.nextFloat(), 0.6f + r.nextFloat() * 1.6f, r.nextFloat() * 2f * PI.toFloat()) }
    }
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = System.nanoTime()
        while (true) {
            val now = withFrameNanos { it }
            t = ((now - start) / 1_000_000_000.0).toFloat()
            if (shootingProgress(t) < 0f) delay(66)
        }
    }
    Canvas(modifier.fillMaxSize()) {
        val unit = 1.dp.toPx()
        for (st in stars) {
            val tw = 0.5f + 0.5f * sin(t * st.speed + st.phase)          // 0..1
            val a = (0.10f + st.s * 0.42f) * (0.35f + 0.65f * tw)
            val r = (0.55f + st.s * 0.95f) * unit
            val c = Offset(st.x * size.width, st.y * size.height)
            if (st.s > 0.85f) {
                // the brightest few get a soft halo when they flare up
                drawCircle(Color.White.copy(alpha = a * 0.25f * tw), radius = r * 3.2f, center = c)
            }
            drawCircle(Color.White.copy(alpha = a), radius = r, center = c)
        }

        val p = shootingProgress(t)
        if (p >= 0f) {
            // each flight gets its own start point and angle from its index
            val n = (t / SHOOTING_EVERY_S).toInt()
            val rnd = java.util.Random(1000L + n)
            val fromLeft = rnd.nextBoolean()
            val ang = ((18f + rnd.nextFloat() * 22f) * PI / 180f).toFloat()
            val dir = Offset(if (fromLeft) cos(ang) else -cos(ang), sin(ang))
            val start = Offset(
                size.width * (if (fromLeft) 0.05f + rnd.nextFloat() * 0.45f else 0.5f + rnd.nextFloat() * 0.45f),
                size.height * (0.05f + rnd.nextFloat() * 0.35f),
            )
            val travel = size.minDimension * 0.75f
            val ease = 1f - (1f - p) * (1f - p)                          // fast in, slowing out
            val head = start + dir * (travel * ease)
            val fade = if (p < 0.15f) p / 0.15f else (1f - p) / 0.85f      // flare in, fade out
            val tail = head - dir * (110.dp.toPx() * (0.4f + 0.6f * fade))
            drawLine(
                Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.75f * fade)), start = tail, end = head),
                start = tail, end = head, strokeWidth = 1.6f * unit, cap = StrokeCap.Round,
            )
            drawCircle(Color.White.copy(alpha = 0.35f * fade), radius = 4f * unit, center = head)
            drawCircle(Color.White.copy(alpha = 0.95f * fade), radius = 1.4f * unit, center = head)
        }
    }
}

/** 0..1 while a shooting star is flying (the first starts 3 s in), -1 otherwise. */
internal fun shootingProgress(t: Float): Float {
    val local = (t - 3f).let { if (it < 0f) return -1f else it % SHOOTING_EVERY_S }
    return if (local < SHOOTING_FLIGHT_S) local / SHOOTING_FLIGHT_S else -1f
}
