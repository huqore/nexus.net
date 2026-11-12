package net.nexus.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.sin

private class AshParticle(
    val x: Float, val y: Float,
    val speed: Float, val drift: Float,
    val swayAmp: Float, val swayFreq: Float, val phase: Float,
    val size: Float, val spin: Float,
    val ember: Boolean, val tone: Int,
)

private fun generateAsh(count: Int): List<AshParticle> {
    val r = java.util.Random(4242)
    return List(count) {
        val ember = r.nextFloat() < 0.42f
        AshParticle(
            x = r.nextFloat(), y = r.nextFloat(),
            speed = if (ember) 18f + r.nextFloat() * 26f else 28f + r.nextFloat() * 34f, // dp/s, ash falls a bit faster
            drift = -10f + r.nextFloat() * 20f,
            swayAmp = 8f + r.nextFloat() * 18f,
            swayFreq = 0.4f + r.nextFloat() * 0.9f,
            phase = r.nextFloat() * 6.28f,
            size = if (ember) 3.2f + r.nextFloat() * 3.6f else 3.5f + r.nextFloat() * 5.5f,
            spin = -90f + r.nextFloat() * 180f,
            ember = ember,
            tone = r.nextInt(3),
        )
    }
}

/**
 * Ash & embers: dark ash flakes drifting down mixed with small glowing red-orange embers that
 * flicker as they fall (custom-theme particle option).
 */
@Composable
fun AshEmbersOverlay(modifier: Modifier = Modifier) {
    val density = LocalDensity.current.density
    val particles = remember { generateAsh(46) }
    val timeMs by rememberInfiniteTransition(label = "ashAnimation").animateFloat(
        initialValue = 0f, targetValue = 120_000f,
        animationSpec = infiniteRepeatable(tween(120_000, easing = LinearEasing), RepeatMode.Restart),
        label = "ashTime",
    )
    val ashTones = remember { listOf(Color(0xFF57534E), Color(0xFF78716C), Color(0xFFA8A29E)) }
    val emberCore = remember { listOf(Color(0xFFFFB347), Color(0xFFFF7A1A), Color(0xFFFF4D1A)) }

    Canvas(modifier.fillMaxSize()) {
        val t = timeMs / 1000f
        val w = size.width
        val h = size.height
        val totalH = h + 80f * density
        for (p in particles) {
            val y = (p.y * totalH + p.speed * density * t) % totalH - 40f * density
            val sway = sin(t * p.swayFreq + p.phase) * p.swayAmp * density
            val rawX = (p.x * w + p.drift * density * t + sway) % w
            val x = if (rawX < 0) rawX + w else rawX
            val c = Offset(x, y)
            if (p.ember) {
                // Flicker like a cooling coal.
                val flicker = 0.55f + 0.45f * ((sin(t * (3f + p.swayFreq * 4f) + p.phase) + 1f) / 2f)
                val core = emberCore[p.tone]
                val r = p.size * density
                drawCircle(
                    Brush.radialGradient(listOf(core.copy(alpha = 0.6f * flicker), Color.Transparent), center = c, radius = r * 3.6f),
                    radius = r * 3.6f, center = c,
                )
                drawCircle(core.copy(alpha = 0.95f * flicker), radius = r * 0.55f, center = c)
                drawCircle(Color(0xFFFFE8C2).copy(alpha = 0.7f * flicker), radius = r * 0.22f, center = c)
            } else {
                // Irregular ash flake (rotated rectangle with a slightly lighter edge).
                val s = p.size * density
                rotate(degrees = (p.spin * t + p.phase * 57f) % 360f, pivot = c) {
                    drawRect(
                        ashTones[p.tone].copy(alpha = 0.75f),
                        topLeft = Offset(x - s / 2, y - s / 3),
                        size = Size(s, s * 0.66f),
                    )
                    drawRect(
                        Color(0xFF1C1917).copy(alpha = 0.55f),
                        topLeft = Offset(x - s / 2, y - s / 3),
                        size = Size(s, s * 0.12f),
                    )
                }
            }
        }
    }
}
