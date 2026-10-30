package net.nexus.app.ui

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.nexus.app.data.Server
import net.nexus.app.data.Store
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

data class GlobeNode(
    val id: String,
    val countryCode: String,
    val countryName: String,
    val flag: String,
    val lat: Float,
    val lon: Float,
    val isWlBypass: Boolean,
    val servers: List<Server> = emptyList(),
    val bestServer: Server? = null,
    val bestPing: Int = -1,
    val isSelected: Boolean = false,
)

object GlobeCoordinates {
    const val GERMANY_LAT = 50.11f
    const val GERMANY_LON = 8.68f

    const val FINLAND_LAT = 60.17f
    const val FINLAND_LON = 24.94f

    const val NETHERLANDS_LAT = 52.37f
    const val NETHERLANDS_LON = 4.90f

    const val SWEDEN_LAT = 59.33f
    const val SWEDEN_LON = 18.07f

    const val POLAND_LAT = 52.23f
    const val POLAND_LON = 21.01f

    const val RUSSIA_WL_LAT = 55.75f
    const val RUSSIA_WL_LON = 37.62f
}

private val CONTINENT_POINTS: FloatArray = floatArrayOf(
    36.0f, -9.0f,
    36.0f, -5.0f,
    36.0f, -1.0f,
    36.0f, 3.0f,
    36.0f, 7.0f,
    36.0f, 11.0f,
    36.0f, 15.0f,
    36.0f, 19.0f,
    36.0f, 23.0f,
    40.0f, -9.0f,
    40.0f, -5.0f,
    40.0f, -1.0f,
    40.0f, 3.0f,
    40.0f, 7.0f,
    40.0f, 11.0f,
    40.0f, 15.0f,
    40.0f, 19.0f,
    40.0f, 23.0f,
    44.0f, -9.0f,
    44.0f, -5.0f,
    44.0f, -1.0f,
    44.0f, 3.0f,
    44.0f, 7.0f,
    44.0f, 11.0f,
    44.0f, 15.0f,
    44.0f, 19.0f,
    44.0f, 23.0f,
    48.0f, -9.0f,
    48.0f, -5.0f,
    48.0f, -1.0f,
    48.0f, 3.0f,
    48.0f, 7.0f,
    48.0f, 11.0f,
    48.0f, 15.0f,
    48.0f, 19.0f,
    48.0f, 23.0f,
    52.0f, -9.0f,
    52.0f, -5.0f,
    52.0f, -1.0f,
    52.0f, 3.0f,
    52.0f, 7.0f,
    52.0f, 11.0f,
    52.0f, 15.0f,
    52.0f, 19.0f,
    52.0f, 23.0f,
    56.0f, -9.0f,
    56.0f, -5.0f,
    56.0f, -1.0f,
    56.0f, 3.0f,
    56.0f, 7.0f,
    56.0f, 11.0f,
    56.0f, 15.0f,
    56.0f, 19.0f,
    56.0f, 23.0f,
    56.0f, 5.0f,
    56.0f, 9.0f,
    56.0f, 13.0f,
    56.0f, 17.0f,
    56.0f, 21.0f,
    56.0f, 25.0f,
    56.0f, 29.0f,
    60.0f, 5.0f,
    60.0f, 9.0f,
    60.0f, 13.0f,
    60.0f, 17.0f,
    60.0f, 21.0f,
    60.0f, 25.0f,
    60.0f, 29.0f,
    64.0f, 5.0f,
    64.0f, 9.0f,
    64.0f, 13.0f,
    64.0f, 17.0f,
    64.0f, 21.0f,
    64.0f, 25.0f,
    64.0f, 29.0f,
    68.0f, 5.0f,
    68.0f, 9.0f,
    68.0f, 13.0f,
    68.0f, 17.0f,
    68.0f, 21.0f,
    68.0f, 25.0f,
    68.0f, 29.0f,
    46.0f, 25.0f,
    46.0f, 29.5f,
    46.0f, 34.0f,
    46.0f, 38.5f,
    46.0f, 43.0f,
    46.0f, 47.5f,
    46.0f, 52.0f,
    50.5f, 25.0f,
    50.5f, 29.5f,
    50.5f, 34.0f,
    50.5f, 38.5f,
    50.5f, 43.0f,
    50.5f, 47.5f,
    50.5f, 52.0f,
    55.0f, 25.0f,
    55.0f, 29.5f,
    55.0f, 34.0f,
    55.0f, 38.5f,
    55.0f, 43.0f,
    55.0f, 47.5f,
    55.0f, 52.0f,
    59.5f, 25.0f,
    59.5f, 29.5f,
    59.5f, 34.0f,
    59.5f, 38.5f,
    59.5f, 43.0f,
    59.5f, 47.5f,
    59.5f, 52.0f,
    64.0f, 25.0f,
    64.0f, 29.5f,
    64.0f, 34.0f,
    64.0f, 38.5f,
    64.0f, 43.0f,
    64.0f, 47.5f,
    64.0f, 52.0f,
    52.0f, 55.0f,
    52.0f, 63.0f,
    52.0f, 71.0f,
    52.0f, 79.0f,
    52.0f, 87.0f,
    52.0f, 95.0f,
    52.0f, 103.0f,
    52.0f, 111.0f,
    52.0f, 119.0f,
    52.0f, 127.0f,
    52.0f, 135.0f,
    60.0f, 55.0f,
    60.0f, 63.0f,
    60.0f, 71.0f,
    60.0f, 79.0f,
    60.0f, 87.0f,
    60.0f, 95.0f,
    60.0f, 103.0f,
    60.0f, 111.0f,
    60.0f, 119.0f,
    60.0f, 127.0f,
    60.0f, 135.0f,
    68.0f, 55.0f,
    68.0f, 63.0f,
    68.0f, 71.0f,
    68.0f, 79.0f,
    68.0f, 87.0f,
    68.0f, 95.0f,
    68.0f, 103.0f,
    68.0f, 111.0f,
    68.0f, 119.0f,
    68.0f, 127.0f,
    68.0f, 135.0f,
    22.0f, 100.0f,
    22.0f, 106.0f,
    22.0f, 112.0f,
    22.0f, 118.0f,
    22.0f, 124.0f,
    22.0f, 130.0f,
    22.0f, 136.0f,
    22.0f, 142.0f,
    28.0f, 100.0f,
    28.0f, 106.0f,
    28.0f, 112.0f,
    28.0f, 118.0f,
    28.0f, 124.0f,
    28.0f, 130.0f,
    28.0f, 136.0f,
    28.0f, 142.0f,
    34.0f, 100.0f,
    34.0f, 106.0f,
    34.0f, 112.0f,
    34.0f, 118.0f,
    34.0f, 124.0f,
    34.0f, 130.0f,
    34.0f, 136.0f,
    34.0f, 142.0f,
    40.0f, 100.0f,
    40.0f, 106.0f,
    40.0f, 112.0f,
    40.0f, 118.0f,
    40.0f, 124.0f,
    40.0f, 130.0f,
    40.0f, 136.0f,
    40.0f, 142.0f,
    8.0f, 68.0f,
    8.0f, 73.5f,
    8.0f, 79.0f,
    8.0f, 84.5f,
    8.0f, 90.0f,
    13.5f, 68.0f,
    13.5f, 73.5f,
    13.5f, 79.0f,
    13.5f, 84.5f,
    13.5f, 90.0f,
    19.0f, 68.0f,
    19.0f, 73.5f,
    19.0f, 79.0f,
    19.0f, 84.5f,
    19.0f, 90.0f,
    24.5f, 68.0f,
    24.5f, 73.5f,
    24.5f, 79.0f,
    24.5f, 84.5f,
    24.5f, 90.0f,
    30.0f, 68.0f,
    30.0f, 73.5f,
    30.0f, 79.0f,
    30.0f, 84.5f,
    30.0f, 90.0f,
    -8.0f, 95.0f,
    -8.0f, 101.0f,
    -8.0f, 107.0f,
    -8.0f, 113.0f,
    -8.0f, 119.0f,
    -8.0f, 125.0f,
    -2.0f, 95.0f,
    -2.0f, 101.0f,
    -2.0f, 107.0f,
    -2.0f, 113.0f,
    -2.0f, 119.0f,
    -2.0f, 125.0f,
    4.0f, 95.0f,
    4.0f, 101.0f,
    4.0f, 107.0f,
    4.0f, 113.0f,
    4.0f, 119.0f,
    4.0f, 125.0f,
    10.0f, 95.0f,
    10.0f, 101.0f,
    10.0f, 107.0f,
    10.0f, 113.0f,
    10.0f, 119.0f,
    10.0f, 125.0f,
    16.0f, 95.0f,
    16.0f, 101.0f,
    16.0f, 107.0f,
    16.0f, 113.0f,
    16.0f, 119.0f,
    16.0f, 125.0f,
    15.0f, 35.0f,
    15.0f, 40.5f,
    15.0f, 46.0f,
    15.0f, 51.5f,
    15.0f, 57.0f,
    20.5f, 35.0f,
    20.5f, 40.5f,
    20.5f, 46.0f,
    20.5f, 51.5f,
    20.5f, 57.0f,
    26.0f, 35.0f,
    26.0f, 40.5f,
    26.0f, 46.0f,
    26.0f, 51.5f,
    26.0f, 57.0f,
    31.5f, 35.0f,
    31.5f, 40.5f,
    31.5f, 46.0f,
    31.5f, 51.5f,
    31.5f, 57.0f,
    37.0f, 35.0f,
    37.0f, 40.5f,
    37.0f, 46.0f,
    37.0f, 51.5f,
    37.0f, 57.0f,
    20.0f, -15.0f,
    20.0f, -9.0f,
    20.0f, -3.0f,
    20.0f, 3.0f,
    20.0f, 9.0f,
    20.0f, 15.0f,
    20.0f, 21.0f,
    20.0f, 27.0f,
    20.0f, 33.0f,
    26.0f, -15.0f,
    26.0f, -9.0f,
    26.0f, -3.0f,
    26.0f, 3.0f,
    26.0f, 9.0f,
    26.0f, 15.0f,
    26.0f, 21.0f,
    26.0f, 27.0f,
    26.0f, 33.0f,
    32.0f, -15.0f,
    32.0f, -9.0f,
    32.0f, -3.0f,
    32.0f, 3.0f,
    32.0f, 9.0f,
    32.0f, 15.0f,
    32.0f, 21.0f,
    32.0f, 27.0f,
    32.0f, 33.0f,
    -34.0f, 10.0f,
    -34.0f, 17.0f,
    -34.0f, 24.0f,
    -34.0f, 31.0f,
    -34.0f, 38.0f,
    -27.0f, 10.0f,
    -27.0f, 17.0f,
    -27.0f, 24.0f,
    -27.0f, 31.0f,
    -27.0f, 38.0f,
    -20.0f, 10.0f,
    -20.0f, 17.0f,
    -20.0f, 24.0f,
    -20.0f, 31.0f,
    -20.0f, 38.0f,
    -13.0f, 10.0f,
    -13.0f, 17.0f,
    -13.0f, 24.0f,
    -13.0f, 31.0f,
    -13.0f, 38.0f,
    -6.0f, 10.0f,
    -6.0f, 17.0f,
    -6.0f, 24.0f,
    -6.0f, 31.0f,
    -6.0f, 38.0f,
    1.0f, 10.0f,
    1.0f, 17.0f,
    1.0f, 24.0f,
    1.0f, 31.0f,
    1.0f, 38.0f,
    8.0f, 10.0f,
    8.0f, 17.0f,
    8.0f, 24.0f,
    8.0f, 31.0f,
    8.0f, 38.0f,
    15.0f, 10.0f,
    15.0f, 17.0f,
    15.0f, 24.0f,
    15.0f, 31.0f,
    15.0f, 38.0f,
    26.0f, -125.0f,
    26.0f, -118.0f,
    26.0f, -111.0f,
    26.0f, -104.0f,
    26.0f, -97.0f,
    26.0f, -90.0f,
    26.0f, -83.0f,
    26.0f, -76.0f,
    26.0f, -69.0f,
    33.0f, -125.0f,
    33.0f, -118.0f,
    33.0f, -111.0f,
    33.0f, -104.0f,
    33.0f, -97.0f,
    33.0f, -90.0f,
    33.0f, -83.0f,
    33.0f, -76.0f,
    33.0f, -69.0f,
    40.0f, -125.0f,
    40.0f, -118.0f,
    40.0f, -111.0f,
    40.0f, -104.0f,
    40.0f, -97.0f,
    40.0f, -90.0f,
    40.0f, -83.0f,
    40.0f, -76.0f,
    40.0f, -69.0f,
    47.0f, -125.0f,
    47.0f, -118.0f,
    47.0f, -111.0f,
    47.0f, -104.0f,
    47.0f, -97.0f,
    47.0f, -90.0f,
    47.0f, -83.0f,
    47.0f, -76.0f,
    47.0f, -69.0f,
    54.0f, -125.0f,
    54.0f, -118.0f,
    54.0f, -111.0f,
    54.0f, -104.0f,
    54.0f, -97.0f,
    54.0f, -90.0f,
    54.0f, -83.0f,
    54.0f, -76.0f,
    54.0f, -69.0f,
    14.0f, -105.0f,
    14.0f, -100.0f,
    14.0f, -95.0f,
    14.0f, -90.0f,
    14.0f, -85.0f,
    19.0f, -105.0f,
    19.0f, -100.0f,
    19.0f, -95.0f,
    19.0f, -90.0f,
    19.0f, -85.0f,
    24.0f, -105.0f,
    24.0f, -100.0f,
    24.0f, -95.0f,
    24.0f, -90.0f,
    24.0f, -85.0f,
    -52.0f, -78.0f,
    -52.0f, -71.0f,
    -52.0f, -64.0f,
    -52.0f, -57.0f,
    -52.0f, -50.0f,
    -52.0f, -43.0f,
    -52.0f, -36.0f,
    -45.0f, -78.0f,
    -45.0f, -71.0f,
    -45.0f, -64.0f,
    -45.0f, -57.0f,
    -45.0f, -50.0f,
    -45.0f, -43.0f,
    -45.0f, -36.0f,
    -38.0f, -78.0f,
    -38.0f, -71.0f,
    -38.0f, -64.0f,
    -38.0f, -57.0f,
    -38.0f, -50.0f,
    -38.0f, -43.0f,
    -38.0f, -36.0f,
    -31.0f, -78.0f,
    -31.0f, -71.0f,
    -31.0f, -64.0f,
    -31.0f, -57.0f,
    -31.0f, -50.0f,
    -31.0f, -43.0f,
    -31.0f, -36.0f,
    -24.0f, -78.0f,
    -24.0f, -71.0f,
    -24.0f, -64.0f,
    -24.0f, -57.0f,
    -24.0f, -50.0f,
    -24.0f, -43.0f,
    -24.0f, -36.0f,
    -17.0f, -78.0f,
    -17.0f, -71.0f,
    -17.0f, -64.0f,
    -17.0f, -57.0f,
    -17.0f, -50.0f,
    -17.0f, -43.0f,
    -17.0f, -36.0f,
    -10.0f, -78.0f,
    -10.0f, -71.0f,
    -10.0f, -64.0f,
    -10.0f, -57.0f,
    -10.0f, -50.0f,
    -10.0f, -43.0f,
    -10.0f, -36.0f,
    -3.0f, -78.0f,
    -3.0f, -71.0f,
    -3.0f, -64.0f,
    -3.0f, -57.0f,
    -3.0f, -50.0f,
    -3.0f, -43.0f,
    -3.0f, -36.0f,
    4.0f, -78.0f,
    4.0f, -71.0f,
    4.0f, -64.0f,
    4.0f, -57.0f,
    4.0f, -50.0f,
    4.0f, -43.0f,
    4.0f, -36.0f,
    -42.0f, 115.0f,
    -42.0f, 123.0f,
    -42.0f, 131.0f,
    -42.0f, 139.0f,
    -42.0f, 147.0f,
    -42.0f, 155.0f,
    -42.0f, 163.0f,
    -42.0f, 171.0f,
    -34.0f, 115.0f,
    -34.0f, 123.0f,
    -34.0f, 131.0f,
    -34.0f, 139.0f,
    -34.0f, 147.0f,
    -34.0f, 155.0f,
    -34.0f, 163.0f,
    -34.0f, 171.0f,
    -26.0f, 115.0f,
    -26.0f, 123.0f,
    -26.0f, 131.0f,
    -26.0f, 139.0f,
    -26.0f, 147.0f,
    -26.0f, 155.0f,
    -26.0f, 163.0f,
    -26.0f, 171.0f,
    -18.0f, 115.0f,
    -18.0f, 123.0f,
    -18.0f, 131.0f,
    -18.0f, 139.0f,
    -18.0f, 147.0f,
    -18.0f, 155.0f,
    -18.0f, 163.0f,
    -18.0f, 171.0f
)

/** 3D Spherical projection helper */
fun projectGlobePoint(
    latDeg: Float,
    lonDeg: Float,
    yawRad: Float,
    pitchRad: Float,
    radius: Float
): Triple<Float, Float, Float> {
    val latRad = Math.toRadians(latDeg.toDouble()).toFloat()
    val lonRad = Math.toRadians(lonDeg.toDouble()).toFloat()

    val x0 = radius * cos(latRad) * sin(lonRad - yawRad)
    val y0 = -radius * sin(latRad)
    val z0 = radius * cos(latRad) * cos(lonRad - yawRad)

    val y1 = y0 * cos(pitchRad) - z0 * sin(pitchRad)
    val z1 = y0 * sin(pitchRad) + z0 * cos(pitchRad)
    val x1 = x0

    return Triple(x1, y1, z1)
}

/** Builds the list of location nodes from available servers */
fun buildGlobeNodes(
    allServers: List<Server>,
    selectedServerId: String?,
    bestServer: Server?
): List<GlobeNode> {
    val wlBypassServers = mutableListOf<Server>()
    val germanyServers = mutableListOf<Server>()
    val finlandServers = mutableListOf<Server>()
    val netherlandsServers = mutableListOf<Server>()
    val swedenServers = mutableListOf<Server>()
    val polandServers = mutableListOf<Server>()
    val otherServers = mutableMapOf<String, MutableList<Server>>()

    for (s in allServers) {
        val isWl = Store.isWlBypass(s)
        val n = s.name.lowercase()
        when {
            isWl || n.contains("bypass") || n.contains("wl") || n.contains("??????") || n.contains("russia") -> {
                wlBypassServers.add(s)
            }
            n.contains("??????") || n.contains("germany") || s.name.contains("????") || Regex("""\bde\b""").containsMatchIn(n) -> {
                germanyServers.add(s)
            }
            n.contains("???????") || n.contains("finland") || s.name.contains("????") || Regex("""\bfi\b""").containsMatchIn(n) -> {
                finlandServers.add(s)
            }
            n.contains("?????????") || n.contains("netherlands") || n.contains("amsterdam") || s.name.contains("????") || Regex("""\bnl\b""").containsMatchIn(n) -> {
                netherlandsServers.add(s)
            }
            n.contains("?????") || n.contains("sweden") || n.contains("stockholm") || s.name.contains("????") || Regex("""\bse\b""").containsMatchIn(n) -> {
                swedenServers.add(s)
            }
            n.contains("?????") || n.contains("poland") || n.contains("warsaw") || s.name.contains("????") || Regex("""\bpl\b""").containsMatchIn(n) -> {
                polandServers.add(s)
            }
            else -> {
                val flag = flagFor(s.name)
                otherServers.getOrPut(flag) { mutableListOf() }.add(s)
            }
        }
    }

    fun bestOf(list: List<Server>): Server? {
        val withPing = list.filter { it.pingMs > 0 }.minByOrNull { it.pingMs }
        return withPing ?: list.firstOrNull()
    }

    fun bestPing(list: List<Server>): Int {
        val p = list.filter { it.pingMs > 0 }.minOfOrNull { it.pingMs }
        return p ?: -1
    }

    fun isNodeSelected(list: List<Server>, fallbackId: String): Boolean {
        if (selectedServerId == Store.AUTO_SERVER_ID) {
            return bestServer != null && list.any { it.id == bestServer.id }
        }
        return list.any { it.id == selectedServerId }
    }

    val nodes = mutableListOf<GlobeNode>()

    // Germany
    val deBest = bestOf(germanyServers)
    nodes.add(
        GlobeNode(
            id = "NODE_DE",
            countryCode = "DE",
            countryName = "????????",
            flag = "????",
            lat = GlobeCoordinates.GERMANY_LAT,
            lon = GlobeCoordinates.GERMANY_LON,
            isWlBypass = false,
            servers = germanyServers,
            bestServer = deBest,
            bestPing = bestPing(germanyServers),
            isSelected = isNodeSelected(germanyServers, "NODE_DE")
        )
    )

    // Finland
    val fiBest = bestOf(finlandServers)
    nodes.add(
        GlobeNode(
            id = "NODE_FI",
            countryCode = "FI",
            countryName = "?????????",
            flag = "????",
            lat = GlobeCoordinates.FINLAND_LAT,
            lon = GlobeCoordinates.FINLAND_LON,
            isWlBypass = false,
            servers = finlandServers,
            bestServer = fiBest,
            bestPing = bestPing(finlandServers),
            isSelected = isNodeSelected(finlandServers, "NODE_FI")
        )
    )

    // Netherlands
    val nlBest = bestOf(netherlandsServers)
    nodes.add(
        GlobeNode(
            id = "NODE_NL",
            countryCode = "NL",
            countryName = "??????????",
            flag = "????",
            lat = GlobeCoordinates.NETHERLANDS_LAT,
            lon = GlobeCoordinates.NETHERLANDS_LON,
            isWlBypass = false,
            servers = netherlandsServers,
            bestServer = nlBest,
            bestPing = bestPing(netherlandsServers),
            isSelected = isNodeSelected(netherlandsServers, "NODE_NL")
        )
    )

    // Sweden
    val seBest = bestOf(swedenServers)
    nodes.add(
        GlobeNode(
            id = "NODE_SE",
            countryCode = "SE",
            countryName = "??????",
            flag = "????",
            lat = GlobeCoordinates.SWEDEN_LAT,
            lon = GlobeCoordinates.SWEDEN_LON,
            isWlBypass = false,
            servers = swedenServers,
            bestServer = seBest,
            bestPing = bestPing(swedenServers),
            isSelected = isNodeSelected(swedenServers, "NODE_SE")
        )
    )

    // Poland
    val plBest = bestOf(polandServers)
    nodes.add(
        GlobeNode(
            id = "NODE_PL",
            countryCode = "PL",
            countryName = "??????",
            flag = "????",
            lat = GlobeCoordinates.POLAND_LAT,
            lon = GlobeCoordinates.POLAND_LON,
            isWlBypass = false,
            servers = polandServers,
            bestServer = plBest,
            bestPing = bestPing(polandServers),
            isSelected = isNodeSelected(polandServers, "NODE_PL")
        )
    )

    // Whitelist Bypass (Russia) - Visually highlighted with shield ??? and distinct amber color
    val wlBest = bestOf(wlBypassServers)
    nodes.add(
        GlobeNode(
            id = "NODE_WL_BYPASS",
            countryCode = "WL",
            countryName = "Whitelist Bypass",
            flag = "???",
            lat = GlobeCoordinates.RUSSIA_WL_LAT,
            lon = GlobeCoordinates.RUSSIA_WL_LON,
            isWlBypass = true,
            servers = wlBypassServers,
            bestServer = wlBest,
            bestPing = bestPing(wlBypassServers),
            isSelected = isNodeSelected(wlBypassServers, "NODE_WL_BYPASS")
        )
    )

    return nodes
}

@Composable
fun NexusGlobe3D(
    modifier: Modifier = Modifier,
    nodes: List<GlobeNode>,
    targetLocation: Pair<Float, Float>? = null,
    onNodeClick: (GlobeNode) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val ctx = LocalContext.current

    val yawAnim = remember { Animatable(Math.toRadians(15.0).toFloat()) }
    val pitchAnim = remember { Animatable(Math.toRadians(-25.0).toFloat()) }

    var isUserInteracting by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableFloatStateOf(0f) }

    // Smoothly turn to target location (e.g. on Auto-select or server click)
    LaunchedEffect(targetLocation) {
        targetLocation?.let { (lat, lon) ->
            val targetYaw = Math.toRadians(lon.toDouble()).toFloat()
            val targetPitch = (-Math.toRadians(lat.toDouble()) * 0.45).toFloat().coerceIn(-0.85f, 0.85f)
            launch {
                yawAnim.animateTo(
                    targetYaw,
                    animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
                )
            }
            launch {
                pitchAnim.animateTo(
                    targetPitch,
                    animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    // Subtle background auto-rotation when idle
    LaunchedEffect(isUserInteracting) {
        if (!isUserInteracting) {
            delay(1200)
            while (isActive && !isUserInteracting) {
                val current = yawAnim.value + 0.0035f
                yawAnim.snapTo(current)
                delay(16)
            }
        }
    }

    // Radar pulsing ring transition
    val infiniteTransition = rememberInfiniteTransition(label = "globeRadar")
    val radarPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseProgress"
    )

    val currentYaw = yawAnim.value
    val currentPitch = pitchAnim.value

    // Distinct theme colors
    val accentColor = NX.accent
    val accentLight = NX.accentLight
    val cardColor = NX.card
    val bgColor = NX.bg
    val borderColor = NX.cardBorder
    val wlGoldColor = Color(0xFFFFB300)
    val wlGoldLight = Color(0xFFFFD54F)

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isUserInteracting = true
                    },
                    onDragEnd = {
                        isUserInteracting = false
                    },
                    onDragCancel = {
                        isUserInteracting = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            val newYaw = yawAnim.value - dragAmount.x * 0.007f
                            val newPitch = (pitchAnim.value - dragAmount.y * 0.007f).coerceIn(-0.95f, 0.95f)
                            yawAnim.snapTo(newYaw)
                            pitchAnim.snapTo(newPitch)
                        }
                    }
                )
            }
    ) {
        var canvasWidth by remember { mutableFloatStateOf(0f) }
        var canvasHeight by remember { mutableFloatStateOf(0f) }
        var globeRadius by remember { mutableFloatStateOf(0f) }
        var centerX by remember { mutableFloatStateOf(0f) }
        var centerY by remember { mutableFloatStateOf(0f) }

        Canvas(modifier = Modifier.fillMaxSize()) {
            canvasWidth = size.width
            canvasHeight = size.height
            globeRadius = min(size.width, size.height) * 0.44f
            centerX = size.width / 2f
            centerY = size.height / 2f

            val r = globeRadius
            val cx = centerX
            val cy = centerY

            // 1. Outer atmospheric halo glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.28f),
                        accentColor.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = r * 1.25f
                ),
                radius = r * 1.25f,
                center = Offset(cx, cy)
            )

            // 2. 3D Planet sphere base
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        cardColor.copy(alpha = 0.92f),
                        bgColor.copy(alpha = 0.98f)
                    ),
                    center = Offset(cx - r * 0.25f, cy - r * 0.28f),
                    radius = r * 1.25f
                ),
                radius = r,
                center = Offset(cx, cy)
            )

            // 3. Globe perimeter edge stroke
            drawCircle(
                color = accentColor.copy(alpha = 0.40f),
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 4. Parallels & Meridians (Lat/Lon grid)
            val parallels = floatArrayOf(-60f, -30f, 0f, 30f, 60f)
            for (pLat in parallels) {
                val step = 8
                var prevPt: Offset? = null
                var prevZ = 0f
                for (lon in 0..360 step step) {
                    val (px, py, pz) = projectGlobePoint(pLat, lon.toFloat(), currentYaw, currentPitch, r)
                    val currPt = Offset(cx + px, cy + py)
                    if (prevPt != null && (prevZ > 0f || pz > 0f)) {
                        val alpha = if (prevZ > 0f && pz > 0f) 0.18f else 0.06f
                        drawLine(
                            color = borderColor.copy(alpha = alpha),
                            start = prevPt,
                            end = currPt,
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    prevPt = currPt
                    prevZ = pz
                }
            }

            val meridians = floatArrayOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f)
            for (mLon in meridians) {
                val step = 6
                var prevPt: Offset? = null
                var prevZ = 0f
                for (lat in -90..90 step step) {
                    val (px, py, pz) = projectGlobePoint(lat.toFloat(), mLon, currentYaw, currentPitch, r)
                    val currPt = Offset(cx + px, cy + py)
                    if (prevPt != null && (prevZ > 0f || pz > 0f)) {
                        val alpha = if (prevZ > 0f && pz > 0f) 0.18f else 0.06f
                        drawLine(
                            color = borderColor.copy(alpha = alpha),
                            start = prevPt,
                            end = currPt,
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    prevPt = currPt
                    prevZ = pz
                }
            }

            // 5. Continent point matrix
            var idx = 0
            val len = CONTINENT_POINTS.size
            while (idx < len) {
                val cLat = CONTINENT_POINTS[idx]
                val cLon = CONTINENT_POINTS[idx + 1]
                idx += 2

                val (dx, dy, dz) = projectGlobePoint(cLat, cLon, currentYaw, currentPitch, r)
                if (dz > 0f) {
                    val depthFactor = (dz / r).coerceIn(0f, 1f)
                    val dotAlpha = (0.25f + 0.50f * depthFactor).coerceIn(0.15f, 0.85f)
                    val dotRadius = (1.4f + 1.2f * depthFactor).dp.toPx()
                    drawCircle(
                        color = accentColor.copy(alpha = dotAlpha),
                        radius = dotRadius,
                        center = Offset(cx + dx, cy + dy)
                    )
                } else {
                    drawCircle(
                        color = borderColor.copy(alpha = 0.06f),
                        radius = 0.9.dp.toPx(),
                        center = Offset(cx + dx, cy + dy)
                    )
                }
            }

            // 6. Server Location Nodes: Sonar Waves and Beacons
            for (node in nodes) {
                val (nx, ny, nz) = projectGlobePoint(node.lat, node.lon, currentYaw, currentPitch, r)
                if (nz > 0f) {
                    val depth = (nz / r).coerceIn(0f, 1f)
                    val nodeCenter = Offset(cx + nx, cy + ny)
                    val baseColor = if (node.isWlBypass) wlGoldColor else accentLight

                    // Pulsing radar ripple waves
                    val maxWaveR = 24.dp.toPx()
                    val waveR1 = 6.dp.toPx() + maxWaveR * radarPulse
                    val waveA1 = (0.65f * (1f - radarPulse) * depth).coerceIn(0f, 1f)
                    drawCircle(
                        color = baseColor.copy(alpha = waveA1),
                        radius = waveR1,
                        center = nodeCenter,
                        style = Stroke(width = 1.6.dp.toPx())
                    )

                    val p2 = (radarPulse + 0.5f) % 1f
                    val waveR2 = 6.dp.toPx() + maxWaveR * p2
                    val waveA2 = (0.65f * (1f - p2) * depth).coerceIn(0f, 1f)
                    drawCircle(
                        color = baseColor.copy(alpha = waveA2),
                        radius = waveR2,
                        center = nodeCenter,
                        style = Stroke(width = 1.4.dp.toPx())
                    )

                    // Vertical 3D light pillar / beacon if node is selected
                    if (node.isSelected) {
                        val beamHeight = 44.dp.toPx()
                        val beamPath = Path().apply {
                            moveTo(nodeCenter.x - 3.dp.toPx(), nodeCenter.y)
                            lineTo(nodeCenter.x + 3.dp.toPx(), nodeCenter.y)
                            lineTo(nodeCenter.x + 8.dp.toPx(), nodeCenter.y - beamHeight)
                            lineTo(nodeCenter.x - 8.dp.toPx(), nodeCenter.y - beamHeight)
                            close()
                        }
                        drawPath(
                            path = beamPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    baseColor.copy(alpha = 0.55f * depth)
                                ),
                                startY = nodeCenter.y - beamHeight,
                                endY = nodeCenter.y
                            )
                        )
                    }

                    // Node core
                    val coreR = if (node.isSelected) 5.5.dp.toPx() else 4.2.dp.toPx()
                    drawCircle(
                        color = baseColor.copy(alpha = (0.95f * depth).coerceIn(0.2f, 1f)),
                        radius = coreR,
                        center = nodeCenter
                    )
                    drawCircle(
                        color = Color.White,
                        radius = coreR * 0.45f,
                        center = nodeCenter
                    )
                }
            }
        }

        // 7. Interactive Floating Badges (Compose overlay with crisp typography and click targets)
        if (globeRadius > 0f) {
            val r = globeRadius
            val cx = centerX
            val cy = centerY

            for (node in nodes) {
                val (nx, ny, nz) = projectGlobePoint(node.lat, node.lon, currentYaw, currentPitch, r)
                if (nz > 0.08f * r) {
                    val depth = (nz / r).coerceIn(0f, 1f)
                    val alpha = ((nz / r - 0.08f) / 0.18f).coerceIn(0f, 1f)
                    val nodeX = cx + nx
                    val nodeY = cy + ny

                    val isWl = node.isWlBypass
                    val badgeBorder = if (node.isSelected) {
                        if (isWl) wlGoldColor else accentLight
                    } else {
                        borderColor.copy(alpha = 0.65f)
                    }
                    val badgeBg = if (node.isSelected) {
                        if (isWl) Color(0x33FFA000) else accentColor.copy(alpha = 0.35f)
                    } else {
                        cardColor.copy(alpha = 0.88f)
                    }

                    val interactionSource = remember { MutableInteractionSource() }

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = nodeX - 44.dp.toPx()
                                translationY = nodeY - 36.dp.toPx()
                                this.alpha = alpha
                                scaleX = 0.82f + 0.18f * depth
                                scaleY = 0.82f + 0.18f * depth
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .background(badgeBg)
                            .border(1.dp, badgeBorder, RoundedCornerShape(12.dp))
                            .clickable(interactionSource = interactionSource, indication = null) {
                                HapticManager.trigger(ctx, net.nexus.app.data.HapticIntensity.LIGHT, HapticType.CLICK)
                                onNodeClick(node)
                            }
                            .padding(horizontal = 7.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = node.flag,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = node.countryCode,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isWl) wlGoldLight else Color.White
                            )
                            if (node.bestPing > 0) {
                                Spacer(Modifier.width(4.dp))
                                val pingColor = when {
                                    node.bestPing < 100 -> Color(0xFF3DDC97)
                                    node.bestPing < 200 -> Color(0xFFE5C14A)
                                    else -> Color(0xFFFF5C6C)
                                }
                                Box(
                                    Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(pingColor)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
