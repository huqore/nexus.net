package net.nexus.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

enum class Tab { CONNECT, SERVERS, SETTINGS }

/** Colour tokens sampled from the purple reference design. */
object NX {
    val bg = Color(0xFF0A0914)
    val card = Color(0xFF15132A)
    val cardBorder = Color(0xFF2B2650)
    val chip = Color(0xFF1E1A3D)
    val iconBtn = Color(0xFF1F1B3A)
    val accent = Color(0xFF8B5CF6)
    val accentDim = Color(0xFF251C4D)
    val accentLight = Color(0xFFB79CFF)
    val text = Color(0xFFF1EFFA)
    val muted = Color(0xFF8C88A8)
    val muted2 = Color(0xFFB0ACC8)
    val yellow = Color(0xFFE5C14A)
    val green = Color(0xFF3DDC97)
    val red = Color(0xFFFF5C6C)
    val purple = Color(0xFFA78BFA)
}

@Composable
fun NexusTheme(fontScale: Float = 0.85f, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val customDensity = Density(
        density = density.density,
        fontScale = density.fontScale * fontScale
    )
    CompositionLocalProvider(LocalDensity provides customDensity) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = NX.accent,
                onPrimary = Color.White,
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
