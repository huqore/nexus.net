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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

private data class EffectParticle(
    val xRatio: Float,
    val yRatio: Float,
    val speedY: Float,
    val driftSpeedX: Float,
    val swayAmplitude: Float,
    val swayFrequency: Float,
    val phase: Float,
    val sizeDp: Float,
    val rotationSpeed: Float,
    val colorIndex: Int,
    val alpha: Float,
    val isCrystal: Boolean = false
)

/**
 * Falling sakura petals effect on the home screen.
 * Soft pink-red-beige petals drifting diagonally down with gentle wavy flutter.
 */
@Composable
fun SakuraPetalsOverlay(modifier: Modifier = Modifier) {
    val density = LocalDensity.current.density
    val particles = remember { generateSakuraParticles(count = 26) }

    val transition = rememberInfiniteTransition(label = "sakuraAnimation")
    val timeMs by transition.animateFloat(
        initialValue = 0f,
        targetValue = 60_000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sakuraTime"
    )

    val petalColors = remember {
        listOf(
            Color(0xFFFF85A1), // Sakura pink
            Color(0xFFFB7185), // Rose blossom
            Color(0xFFF43F5E), // Crimson rose red
            Color(0xFFFFF0ED), // Cream petal beige
            Color(0xFFFFCCD5), // Soft blush beige-pink
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val t = timeMs / 1000f
        val w = size.width
        val h = size.height
        val totalH = h + 120f * density

        for (p in particles) {
            val rawY = (p.yRatio * totalH + p.speedY * density * t) % totalH
            val y = rawY - 60f * density

            val wave = sin(t * p.swayFrequency + p.phase) * (p.swayAmplitude * density)
            val rawX = (p.xRatio * w + p.driftSpeedX * density * t + wave) % w
            val x = if (rawX < 0) rawX + w else rawX

            val rotation = (p.rotationSpeed * t + p.phase * 57.3f) % 360f
            val flutterScaleX = cos(t * p.swayFrequency * 1.4f + p.phase).absoluteValue.coerceIn(0.25f, 1f)

            val color = petalColors[p.colorIndex % petalColors.size].copy(alpha = p.alpha)
            val petalW = p.sizeDp * density * 0.55f
            val petalH = p.sizeDp * density * 1.15f

            rotate(degrees = rotation, pivot = Offset(x, y)) {
                scale(scaleX = flutterScaleX, scaleY = 1f, pivot = Offset(x, y)) {
                    drawSakuraPetal(center = Offset(x, y), width = petalW, height = petalH, color = color)
                }
            }
        }
    }
}

private fun DrawScope.drawSakuraPetal(center: Offset, width: Float, height: Float, color: Color) {
    val path = Path().apply {
        val halfW = width / 2f
        val halfH = height / 2f
        moveTo(center.x, center.y - halfH)
        cubicTo(
            center.x + halfW * 1.1f, center.y - halfH * 0.4f,
            center.x + halfW * 0.9f, center.y + halfH * 0.5f,
            center.x, center.y + halfH
        )
        cubicTo(
            center.x - halfW * 0.9f, center.y + halfH * 0.5f,
            center.x - halfW * 1.1f, center.y - halfH * 0.4f,
            center.x, center.y - halfH
        )
        close()
    }
    drawPath(path = path, color = color)
}

/**
 * Falling snowflakes effect on the home screen.
 * White-blue-cyan snowflakes and crystals drifting diagonally down with gentle sway.
 */
@Composable
fun WinterSnowOverlay(modifier: Modifier = Modifier) {
    val density = LocalDensity.current.density
    val particles = remember { generateWinterParticles(count = 32) }

    val transition = rememberInfiniteTransition(label = "winterAnimation")
    val timeMs by transition.animateFloat(
        initialValue = 0f,
        targetValue = 60_000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "winterTime"
    )

    val snowColors = remember {
        listOf(
            Color(0xFFFFFFFF), // Pure frosty white
            Color(0xFFF0F9FF), // Arctic ice white
            Color(0xFFBAE6FD), // Soft ice blue
            Color(0xFF38BDF8), // Cyan sky blue
            Color(0xFF7DD3FC), // Pale cyan
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val t = timeMs / 1000f
        val w = size.width
        val h = size.height
        val totalH = h + 100f * density

        for (p in particles) {
            val rawY = (p.yRatio * totalH + p.speedY * density * t) % totalH
            val y = rawY - 50f * density

            val wave = sin(t * p.swayFrequency + p.phase) * (p.swayAmplitude * density)
            val rawX = (p.xRatio * w + p.driftSpeedX * density * t + wave) % w
            val x = if (rawX < 0) rawX + w else rawX

            val color = snowColors[p.colorIndex % snowColors.size].copy(alpha = p.alpha)

            if (p.isCrystal) {
                val rotation = (p.rotationSpeed * t + p.phase * 57.3f) % 360f
                val radius = p.sizeDp * density * 0.5f
                rotate(degrees = rotation, pivot = Offset(x, y)) {
                    val strokeW = (1.2f * density).coerceAtLeast(1f)
                    // 3 crossing lines for 6-point snowflake star
                    for (deg in 0 until 180 step 60) {
                        val rad = Math.toRadians(deg.toDouble())
                        val cosVal = cos(rad).toFloat() * radius
                        val sinVal = sin(rad).toFloat() * radius
                        drawLine(
                            color = color,
                            start = Offset(x - cosVal, y - sinVal),
                            end = Offset(x + cosVal, y + sinVal),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                    }
                    drawCircle(color = color, radius = (1.2f * density).coerceAtLeast(1f), center = Offset(x, y))
                }
            } else {
                // Soft fluffy snow grain
                val radius = p.sizeDp * density * 0.32f
                drawCircle(color = color, radius = radius, center = Offset(x, y))
            }
        }
    }
}

private fun generateSakuraParticles(count: Int): List<EffectParticle> {
    val list = ArrayList<EffectParticle>(count)
    val rng = java.util.Random(1337)
    for (i in 0 until count) {
        list.add(
            EffectParticle(
                xRatio = rng.nextFloat(),
                yRatio = rng.nextFloat(),
                speedY = 65f + rng.nextFloat() * 70f,          // 65..135 dp/s
                driftSpeedX = 22f + rng.nextFloat() * 45f,      // 22..67 dp/s diagonal drift
                swayAmplitude = 16f + rng.nextFloat() * 24f,    // 16..40 dp wave sway
                swayFrequency = 1.0f + rng.nextFloat() * 1.6f,  // 1.0..2.6 rad/s
                phase = rng.nextFloat() * 6.28f,
                sizeDp = 11f + rng.nextFloat() * 6f,            // 11..17 dp medium size
                rotationSpeed = (30f + rng.nextFloat() * 80f) * if (rng.nextBoolean()) 1f else -1f,
                colorIndex = rng.nextInt(5),
                alpha = 0.50f + rng.nextFloat() * 0.35f
            )
        )
    }
    return list
}

private fun generateWinterParticles(count: Int): List<EffectParticle> {
    val list = ArrayList<EffectParticle>(count)
    val rng = java.util.Random(2026)
    for (i in 0 until count) {
        val isCrystal = i % 2 == 0
        list.add(
            EffectParticle(
                xRatio = rng.nextFloat(),
                yRatio = rng.nextFloat(),
                speedY = 55f + rng.nextFloat() * 65f,          // 55..120 dp/s
                driftSpeedX = 15f + rng.nextFloat() * 35f,      // 15..50 dp/s diagonal drift
                swayAmplitude = 12f + rng.nextFloat() * 20f,    // 12..32 dp wave sway
                swayFrequency = 0.9f + rng.nextFloat() * 1.5f,  // 0.9..2.4 rad/s
                phase = rng.nextFloat() * 6.28f,
                sizeDp = if (isCrystal) 10f + rng.nextFloat() * 5f else 6f + rng.nextFloat() * 5f,
                rotationSpeed = (25f + rng.nextFloat() * 50f) * if (rng.nextBoolean()) 1f else -1f,
                colorIndex = rng.nextInt(5),
                alpha = 0.40f + rng.nextFloat() * 0.45f,
                isCrystal = isCrystal
            )
        )
    }
    return list
}

