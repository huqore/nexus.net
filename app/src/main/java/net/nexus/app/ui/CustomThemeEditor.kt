package net.nexus.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import net.nexus.app.R

private val AccentPresets = listOf(
    0xFF8B5CF6, 0xFF6366F1, 0xFF3B82F6, 0xFF0EA5E9, 0xFF14B8A6, 0xFF22C55E,
    0xFF84CC16, 0xFFEAB308, 0xFFF97316, 0xFFEF4444, 0xFFEC4899, 0xFFD946EF,
).map { Color(it) }

private enum class BgKind { DARK, AMOLED, LIGHT }

private fun hsvColor(h: Float, s: Float, v: Float) =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(h.coerceIn(0f, 359.9f), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))))

private fun bgFor(kind: BgKind, hue: Float): Color = when (kind) {
    BgKind.DARK -> hsvColor(hue, 0.38f, 0.055f)
    BgKind.AMOLED -> Color.Black
    BgKind.LIGHT -> hsvColor(hue, 0.04f, 0.98f)
}

/**
 * Custom theme editor: pick an accent (presets or hue/saturation/brightness) and a background
 * style; the live preview is built with the same customPalette() the app will use.
 */
@Composable
fun CustomThemeEditor(
    initialAccent: Int,
    initialBg: Int,
    initialParticles: net.nexus.app.data.CustomParticles = net.nexus.app.data.CustomParticles.NONE,
    onDismiss: () -> Unit,
    onApply: (accent: Int, bg: Int, particles: net.nexus.app.data.CustomParticles) -> Unit,
) {
    var particles by remember { androidx.compose.runtime.mutableStateOf(initialParticles) }
    val start = remember {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialAccent, it) }
    }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var sat by remember { mutableFloatStateOf(start[1].coerceAtLeast(0.35f)) }
    var value by remember { mutableFloatStateOf(start[2].coerceAtLeast(0.55f)) }
    var bgKind by remember {
        val c = Color(initialBg)
        mutableIntStateOf(
            when {
                c.red + c.green + c.blue < 0.02f -> BgKind.AMOLED.ordinal
                (0.299f * c.red + 0.587f * c.green + 0.114f * c.blue) > 0.5f -> BgKind.LIGHT.ordinal
                else -> BgKind.DARK.ordinal
            }
        )
    }

    val accent = hsvColor(hue, sat, value)
    val bg = bgFor(BgKind.entries[bgKind], hue)
    val p = remember(accent, bg) { customPalette(accent, bg) }
    // Readable text on the accent: dark on bright accents (yellow, lime), white otherwise.
    val onAccent = if (accent.luminance() > 0.55f) Color(0xFF14121A) else Color.White

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(NX.sheetBg.copy(alpha = 1f))
                .background(NX.card)
                .border(1.dp, NX.cardBorder, RoundedCornerShape(28.dp))
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.custom_theme_title), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NX.text)

            // Live preview built from the chosen colours.
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(p.bg)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(p.card)
                        .border(1.dp, p.cardBorder, RoundedCornerShape(14.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(p.accentDim), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(p.accent))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(stringResource(R.string.custom_preview_text), color = p.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("NEXUS.net", color = p.muted, fontSize = 11.sp)
                    }
                }
                Box(
                    Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(12.dp)).background(p.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.custom_apply),
                        color = onAccent,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    )
                }
            }

            Text(stringResource(R.string.custom_accent), color = NX.muted2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            // Presets
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AccentPresets.chunked(6).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        row.forEach { c ->
                            val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(c.toArgb(), it) }
                            val sel = kotlin.math.abs(hsv[0] - hue) < 2f && kotlin.math.abs(hsv[1] - sat) < 0.03f && kotlin.math.abs(hsv[2] - value) < 0.03f
                            Box(
                                Modifier.size(34.dp).clip(CircleShape).background(c)
                                    .border(if (sel) 3.dp else 0.dp, NX.text, CircleShape)
                                    .clickable { hue = hsv[0]; sat = hsv[1]; value = hsv[2] },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (sel) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Hue / saturation / brightness
            ColorSlider(stringResource(R.string.custom_hue), hue, 0f..359.9f,
                Brush.horizontalGradient((0..6).map { hsvColor(it * 60f, sat, value) })) { hue = it }
            ColorSlider(stringResource(R.string.custom_saturation), sat, 0.2f..1f,
                Brush.horizontalGradient(listOf(hsvColor(hue, 0.2f, value), hsvColor(hue, 1f, value)))) { sat = it }
            ColorSlider(stringResource(R.string.custom_brightness), value, 0.45f..1f,
                Brush.horizontalGradient(listOf(hsvColor(hue, sat, 0.45f), hsvColor(hue, sat, 1f)))) { value = it }

            Text(stringResource(R.string.custom_background), color = NX.muted2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    BgKind.DARK to R.string.custom_bg_dark,
                    BgKind.AMOLED to R.string.custom_bg_amoled,
                    BgKind.LIGHT to R.string.custom_bg_light,
                ).forEach { (kind, label) ->
                    val sel = bgKind == kind.ordinal
                    val swatch = bgFor(kind, hue)
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                            .border(if (sel) 2.dp else 1.dp, if (sel) NX.accent else NX.cardBorder, RoundedCornerShape(14.dp))
                            .clickable { bgKind = kind.ordinal }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(8.dp)).background(swatch)
                            .border(1.dp, NX.cardBorder.copy(alpha = 0.6f), RoundedCornerShape(8.dp)))
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(label), color = if (sel) NX.text else NX.muted, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }

            Text(stringResource(R.string.custom_particles), color = NX.muted2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    net.nexus.app.data.CustomParticles.NONE to R.string.particles_none,
                    net.nexus.app.data.CustomParticles.SAKURA to R.string.particles_sakura,
                    net.nexus.app.data.CustomParticles.SNOW to R.string.particles_snow,
                    net.nexus.app.data.CustomParticles.ASH to R.string.particles_ash,
                ).forEach { (kind, label) ->
                    val sel = particles == kind
                    Box(
                        Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(19.dp))
                            .background(if (sel) accent else NX.chip)
                            .clickable { particles = kind },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(label), color = if (sel) onAccent else NX.text, fontSize = 12.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(14.dp)).background(NX.chip).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) { Text(stringResource(R.string.custom_cancel), color = NX.text, fontWeight = FontWeight.SemiBold) }
                Box(
                    Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(14.dp)).background(accent)
                        .clickable { onApply(accent.toArgb(), bg.toArgb(), particles) },
                    contentAlignment = Alignment.Center,
                ) { Text(stringResource(R.string.custom_apply), color = onAccent, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun ColorSlider(label: String, v: Float, range: ClosedFloatingPointRange<Float>, track: Brush, onChange: (Float) -> Unit) {
    Column {
        Text(label, color = NX.muted, fontSize = 12.sp)
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(10.dp).clip(RoundedCornerShape(5.dp)).background(track))
            Slider(
                value = v, onValueChange = onChange, valueRange = range,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                ),
            )
        }
    }
}
