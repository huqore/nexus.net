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
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.nexus.app.data.Server
import net.nexus.app.data.Store
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
    // Exact real geographic centers of each country on a real globe
    const val GERMANY_LAT = 51.16f
    const val GERMANY_LON = 10.45f

    const val NETHERLANDS_LAT = 52.13f
    const val NETHERLANDS_LON = 5.29f

    const val POLAND_LAT = 52.07f
    const val POLAND_LON = 19.48f

    const val SWEDEN_LAT = 60.13f
    const val SWEDEN_LON = 15.00f

    const val FINLAND_LAT = 64.00f
    const val FINLAND_LON = 26.00f

    const val RUSSIA_WL_LAT = 55.75f
    const val RUSSIA_WL_LON = 37.62f
}

// 826 authentic land points
private val CONTINENT_POINTS: FloatArray = floatArrayOf(
    -51.4f, -70.4f,
    -51.4f, -58.8f,
    -47.8f, -72.8f,
    -47.8f, -67.5f,
    -44.2f, -69.5f,
    -44.2f, 171.5f,
    -40.6f, -70.9f,
    -40.6f, -66.2f,
    -40.6f, 175.6f,
    -37.0f, -71.8f,
    -37.0f, -67.3f,
    -37.0f, -62.8f,
    -37.0f, -58.3f,
    -37.0f, 140.0f,
    -37.0f, 144.6f,
    -37.0f, 149.1f,
    -33.4f, -67.9f,
    -33.4f, -63.6f,
    -33.4f, -59.3f,
    -33.4f, -54.9f,
    -33.4f, 18.4f,
    -33.4f, 22.7f,
    -33.4f, 27.0f,
    -33.4f, 117.5f,
    -33.4f, 121.9f,
    -33.4f, 139.1f,
    -33.4f, 143.4f,
    -33.4f, 147.7f,
    -29.8f, -68.0f,
    -29.8f, -63.8f,
    -29.8f, -59.7f,
    -29.8f, -55.5f,
    -29.8f, -51.4f,
    -29.8f, 19.1f,
    -29.8f, 23.3f,
    -29.8f, 27.4f,
    -29.8f, 118.7f,
    -29.8f, 122.8f,
    -29.8f, 127.0f,
    -29.8f, 131.1f,
    -29.8f, 135.3f,
    -29.8f, 139.4f,
    -29.8f, 143.6f,
    -29.8f, 147.7f,
    -29.8f, 151.9f,
    -26.2f, -67.7f,
    -26.2f, -63.6f,
    -26.2f, -59.6f,
    -26.2f, -55.6f,
    -26.2f, -51.6f,
    -26.2f, 16.6f,
    -26.2f, 20.6f,
    -26.2f, 24.6f,
    -26.2f, 28.6f,
    -26.2f, 32.6f,
    -26.2f, 116.9f,
    -26.2f, 120.9f,
    -26.2f, 124.9f,
    -26.2f, 128.9f,
    -26.2f, 133.0f,
    -26.2f, 137.0f,
    -26.2f, 141.0f,
    -26.2f, 145.0f,
    -26.2f, 149.0f,
    -26.2f, 153.0f,
    -22.6f, -66.9f,
    -22.6f, -63.0f,
    -22.6f, -59.1f,
    -22.6f, -55.2f,
    -22.6f, -51.3f,
    -22.6f, -47.4f,
    -22.6f, -43.5f,
    -22.6f, 15.0f,
    -22.6f, 18.9f,
    -22.6f, 22.8f,
    -22.6f, 26.7f,
    -22.6f, 30.6f,
    -22.6f, 34.5f,
    -22.6f, 46.2f,
    -22.6f, 116.4f,
    -22.6f, 120.3f,
    -22.6f, 124.2f,
    -22.6f, 128.1f,
    -22.6f, 132.0f,
    -22.6f, 135.9f,
    -22.6f, 139.8f,
    -22.6f, 143.7f,
    -22.6f, 147.6f,
    -19.0f, -69.6f,
    -19.0f, -65.8f,
    -19.0f, -62.0f,
    -19.0f, -58.2f,
    -19.0f, -54.4f,
    -19.0f, -50.5f,
    -19.0f, -46.7f,
    -19.0f, -42.9f,
    -19.0f, 14.2f,
    -19.0f, 18.0f,
    -19.0f, 21.8f,
    -19.0f, 25.6f,
    -19.0f, 29.4f,
    -19.0f, 33.2f,
    -19.0f, 44.6f,
    -19.0f, 48.4f,
    -19.0f, 124.6f,
    -19.0f, 128.4f,
    -19.0f, 132.2f,
    -19.0f, 136.0f,
    -19.0f, 139.8f,
    -19.0f, 143.6f,
    -15.4f, -71.7f,
    -15.4f, -68.0f,
    -15.4f, -64.2f,
    -15.4f, -60.5f,
    -15.4f, -56.8f,
    -15.4f, -53.0f,
    -15.4f, -49.3f,
    -15.4f, -45.6f,
    -15.4f, -41.8f,
    -15.4f, 14.2f,
    -15.4f, 17.9f,
    -15.4f, 21.6f,
    -15.4f, 25.4f,
    -15.4f, 29.1f,
    -15.4f, 32.8f,
    -15.4f, 36.6f,
    -15.4f, 40.3f,
    -15.4f, 47.8f,
    -15.4f, 126.2f,
    -15.4f, 129.9f,
    -15.4f, 133.7f,
    -15.4f, 144.9f,
    -11.8f, -77.0f,
    -11.8f, -73.3f,
    -11.8f, -69.7f,
    -11.8f, -66.0f,
    -11.8f, -62.3f,
    -11.8f, -58.6f,
    -11.8f, -55.0f,
    -11.8f, -51.3f,
    -11.8f, -47.6f,
    -11.8f, -43.9f,
    -11.8f, -40.2f,
    -11.8f, 14.9f,
    -11.8f, 18.6f,
    -11.8f, 22.3f,
    -11.8f, 26.0f,
    -11.8f, 29.6f,
    -11.8f, 33.3f,
    -11.8f, 37.0f,
    -8.2f, -78.2f,
    -8.2f, -74.5f,
    -8.2f, -70.9f,
    -8.2f, -67.2f,
    -8.2f, -63.6f,
    -8.2f, -60.0f,
    -8.2f, -56.3f,
    -8.2f, -52.7f,
    -8.2f, -49.1f,
    -8.2f, -45.4f,
    -8.2f, -41.8f,
    -8.2f, -38.1f,
    -8.2f, 16.4f,
    -8.2f, 20.0f,
    -8.2f, 23.7f,
    -8.2f, 27.3f,
    -8.2f, 31.0f,
    -8.2f, 34.6f,
    -8.2f, 38.2f,
    -8.2f, 111.0f,
    -8.2f, 114.6f,
    -8.2f, 140.1f,
    -8.2f, 147.3f,
    -4.6f, -78.9f,
    -4.6f, -75.3f,
    -4.6f, -71.7f,
    -4.6f, -68.0f,
    -4.6f, -64.4f,
    -4.6f, -60.8f,
    -4.6f, -57.2f,
    -4.6f, -53.6f,
    -4.6f, -50.0f,
    -4.6f, -46.4f,
    -4.6f, -42.8f,
    -4.6f, -39.1f,
    -4.6f, 15.0f,
    -4.6f, 18.6f,
    -4.6f, 22.3f,
    -4.6f, 25.9f,
    -4.6f, 29.5f,
    -4.6f, 33.1f,
    -4.6f, 36.7f,
    -4.6f, 105.3f,
    -4.6f, 119.8f,
    -4.6f, 137.8f,
    -4.6f, 141.4f,
    -4.6f, 145.0f,
    -4.6f, 152.3f,
    -1.0f, -79.2f,
    -1.0f, -75.6f,
    -1.0f, -72.0f,
    -1.0f, -68.4f,
    -1.0f, -64.8f,
    -1.0f, -61.2f,
    -1.0f, -57.6f,
    -1.0f, -54.0f,
    -1.0f, -50.4f,
    -1.0f, -46.8f,
    -1.0f, 10.8f,
    -1.0f, 14.4f,
    -1.0f, 18.0f,
    -1.0f, 21.6f,
    -1.0f, 25.2f,
    -1.0f, 28.8f,
    -1.0f, 32.4f,
    -1.0f, 36.0f,
    -1.0f, 39.6f,
    -1.0f, 100.8f,
    -1.0f, 111.6f,
    -1.0f, 115.2f,
    -1.0f, 122.4f,
    -1.0f, 133.2f,
    2.6f, -75.5f,
    2.6f, -71.9f,
    2.6f, -68.3f,
    2.6f, -64.7f,
    2.6f, -61.1f,
    2.6f, -57.5f,
    2.6f, -53.9f,
    2.6f, 11.0f,
    2.6f, 14.6f,
    2.6f, 18.2f,
    2.6f, 21.8f,
    2.6f, 25.4f,
    2.6f, 29.0f,
    2.6f, 32.6f,
    2.6f, 36.2f,
    2.6f, 39.8f,
    2.6f, 43.4f,
    2.6f, 111.9f,
    2.6f, 115.5f,
    6.2f, -75.0f,
    6.2f, -71.4f,
    6.2f, -67.7f,
    6.2f, -64.1f,
    6.2f, -60.5f,
    6.2f, -9.8f,
    6.2f, -6.2f,
    6.2f, -2.6f,
    6.2f, 1.1f,
    6.2f, 4.7f,
    6.2f, 8.3f,
    6.2f, 11.9f,
    6.2f, 15.5f,
    6.2f, 19.2f,
    6.2f, 22.8f,
    6.2f, 26.4f,
    6.2f, 30.0f,
    6.2f, 33.6f,
    6.2f, 37.3f,
    6.2f, 40.9f,
    6.2f, 44.5f,
    6.2f, 48.1f,
    6.2f, 80.7f,
    6.2f, 116.9f,
    9.8f, -85.0f,
    9.8f, -74.1f,
    9.8f, -70.4f,
    9.8f, -66.7f,
    9.8f, -63.1f,
    9.8f, -11.9f,
    9.8f, -8.3f,
    9.8f, -4.6f,
    9.8f, -1.0f,
    9.8f, 2.7f,
    9.8f, 6.3f,
    9.8f, 10.0f,
    9.8f, 13.6f,
    9.8f, 17.3f,
    9.8f, 20.9f,
    9.8f, 24.6f,
    9.8f, 28.2f,
    9.8f, 31.9f,
    9.8f, 35.5f,
    9.8f, 39.2f,
    9.8f, 42.9f,
    9.8f, 46.5f,
    9.8f, 50.2f,
    9.8f, 105.0f,
    13.4f, -87.5f,
    13.4f, -83.8f,
    13.4f, -13.5f,
    13.4f, -9.8f,
    13.4f, -6.1f,
    13.4f, -2.4f,
    13.4f, 1.3f,
    13.4f, 5.0f,
    13.4f, 8.7f,
    13.4f, 12.4f,
    13.4f, 16.1f,
    13.4f, 19.8f,
    13.4f, 23.5f,
    13.4f, 27.2f,
    13.4f, 30.9f,
    13.4f, 34.6f,
    13.4f, 38.3f,
    13.4f, 42.0f,
    13.4f, 45.7f,
    13.4f, 75.4f,
    13.4f, 79.1f,
    13.4f, 101.3f,
    13.4f, 105.0f,
    13.4f, 108.7f,
    13.4f, 123.5f,
    17.0f, -97.2f,
    17.0f, -93.4f,
    17.0f, -89.7f,
    17.0f, -14.4f,
    17.0f, -10.6f,
    17.0f, -6.8f,
    17.0f, -3.1f,
    17.0f, 0.7f,
    17.0f, 4.5f,
    17.0f, 8.2f,
    17.0f, 12.0f,
    17.0f, 15.8f,
    17.0f, 19.5f,
    17.0f, 23.3f,
    17.0f, 27.0f,
    17.0f, 30.8f,
    17.0f, 34.6f,
    17.0f, 38.3f,
    17.0f, 45.9f,
    17.0f, 49.6f,
    17.0f, 53.4f,
    17.0f, 76.0f,
    17.0f, 79.7f,
    17.0f, 94.8f,
    17.0f, 98.6f,
    17.0f, 102.3f,
    17.0f, 106.1f,
    17.0f, 121.2f,
    20.6f, -103.1f,
    20.6f, -99.2f,
    20.6f, -87.7f,
    20.6f, -76.2f,
    20.6f, -14.6f,
    20.6f, -10.8f,
    20.6f, -6.9f,
    20.6f, -3.1f,
    20.6f, 0.8f,
    20.6f, 4.6f,
    20.6f, 8.4f,
    20.6f, 12.3f,
    20.6f, 16.1f,
    20.6f, 20.0f,
    20.6f, 23.8f,
    20.6f, 27.7f,
    20.6f, 31.5f,
    20.6f, 35.4f,
    20.6f, 43.1f,
    20.6f, 46.9f,
    20.6f, 50.8f,
    20.6f, 54.6f,
    20.6f, 58.4f,
    20.6f, 73.8f,
    20.6f, 77.7f,
    20.6f, 81.5f,
    20.6f, 85.4f,
    20.6f, 93.1f,
    20.6f, 96.9f,
    20.6f, 100.8f,
    20.6f, 104.6f,
    24.2f, -105.0f,
    24.2f, -101.1f,
    24.2f, -14.2f,
    24.2f, -10.3f,
    24.2f, -6.3f,
    24.2f, -2.4f,
    24.2f, 1.6f,
    24.2f, 5.5f,
    24.2f, 9.4f,
    24.2f, 13.4f,
    24.2f, 17.3f,
    24.2f, 21.3f,
    24.2f, 25.2f,
    24.2f, 29.2f,
    24.2f, 33.1f,
    24.2f, 41.0f,
    24.2f, 45.0f,
    24.2f, 48.9f,
    24.2f, 56.8f,
    24.2f, 68.7f,
    24.2f, 72.6f,
    24.2f, 76.5f,
    24.2f, 80.5f,
    24.2f, 84.4f,
    24.2f, 88.4f,
    24.2f, 92.3f,
    24.2f, 96.3f,
    24.2f, 100.2f,
    24.2f, 104.2f,
    24.2f, 108.1f,
    24.2f, 112.1f,
    24.2f, 116.0f,
    27.8f, -114.9f,
    27.8f, -106.7f,
    27.8f, -102.7f,
    27.8f, -98.6f,
    27.8f, -82.3f,
    27.8f, -9.1f,
    27.8f, -5.0f,
    27.8f, -0.9f,
    27.8f, 3.1f,
    27.8f, 7.2f,
    27.8f, 11.3f,
    27.8f, 15.3f,
    27.8f, 19.4f,
    27.8f, 23.5f,
    27.8f, 27.6f,
    27.8f, 31.6f,
    27.8f, 35.7f,
    27.8f, 39.8f,
    27.8f, 43.8f,
    27.8f, 47.9f,
    27.8f, 56.0f,
    27.8f, 60.1f,
    27.8f, 64.2f,
    27.8f, 68.3f,
    27.8f, 72.3f,
    27.8f, 76.4f,
    27.8f, 80.5f,
    27.8f, 84.5f,
    27.8f, 88.6f,
    27.8f, 92.7f,
    27.8f, 96.7f,
    27.8f, 100.8f,
    27.8f, 104.9f,
    27.8f, 109.0f,
    27.8f, 113.0f,
    27.8f, 117.1f,
    31.4f, -112.5f,
    31.4f, -108.3f,
    31.4f, -104.1f,
    31.4f, -99.9f,
    31.4f, -95.6f,
    31.4f, -91.4f,
    31.4f, -87.2f,
    31.4f, -83.0f,
    31.4f, -7.1f,
    31.4f, -2.9f,
    31.4f, 1.4f,
    31.4f, 5.6f,
    31.4f, 9.8f,
    31.4f, 14.0f,
    31.4f, 22.4f,
    31.4f, 26.7f,
    31.4f, 30.9f,
    31.4f, 35.1f,
    31.4f, 39.3f,
    31.4f, 43.5f,
    31.4f, 47.8f,
    31.4f, 52.0f,
    31.4f, 56.2f,
    31.4f, 60.4f,
    31.4f, 64.6f,
    31.4f, 68.8f,
    31.4f, 73.1f,
    31.4f, 77.3f,
    31.4f, 81.5f,
    31.4f, 85.7f,
    31.4f, 89.9f,
    31.4f, 94.1f,
    31.4f, 98.4f,
    31.4f, 102.6f,
    31.4f, 106.8f,
    31.4f, 111.0f,
    31.4f, 115.2f,
    31.4f, 119.5f,
    35.0f, -118.5f,
    35.0f, -114.1f,
    35.0f, -109.7f,
    35.0f, -105.3f,
    35.0f, -100.9f,
    35.0f, -96.5f,
    35.0f, -92.1f,
    35.0f, -87.7f,
    35.0f, -83.3f,
    35.0f, -78.9f,
    35.0f, -4.2f,
    35.0f, 0.2f,
    35.0f, 4.6f,
    35.0f, 9.0f,
    35.0f, 39.7f,
    35.0f, 44.1f,
    35.0f, 48.5f,
    35.0f, 52.9f,
    35.0f, 57.3f,
    35.0f, 61.7f,
    35.0f, 66.1f,
    35.0f, 70.5f,
    35.0f, 74.9f,
    35.0f, 79.3f,
    35.0f, 83.7f,
    35.0f, 88.1f,
    35.0f, 92.5f,
    35.0f, 96.9f,
    35.0f, 101.3f,
    35.0f, 105.7f,
    35.0f, 110.1f,
    35.0f, 114.5f,
    35.0f, 118.8f,
    35.0f, 127.6f,
    35.0f, 136.4f,
    38.6f, -120.1f,
    38.6f, -115.5f,
    38.6f, -110.9f,
    38.6f, -106.3f,
    38.6f, -101.7f,
    38.6f, -97.1f,
    38.6f, -92.5f,
    38.6f, -87.9f,
    38.6f, -83.3f,
    38.6f, -78.7f,
    38.6f, -5.0f,
    38.6f, -0.4f,
    38.6f, 22.7f,
    38.6f, 27.3f,
    38.6f, 31.9f,
    38.6f, 36.5f,
    38.6f, 41.1f,
    38.6f, 45.7f,
    38.6f, 54.9f,
    38.6f, 59.5f,
    38.6f, 64.1f,
    38.6f, 68.7f,
    38.6f, 73.4f,
    38.6f, 78.0f,
    38.6f, 82.6f,
    38.6f, 87.2f,
    38.6f, 91.8f,
    38.6f, 96.4f,
    38.6f, 101.0f,
    38.6f, 105.6f,
    38.6f, 110.2f,
    38.6f, 114.8f,
    42.2f, -121.7f,
    42.2f, -116.8f,
    42.2f, -112.0f,
    42.2f, -107.1f,
    42.2f, -102.2f,
    42.2f, -97.4f,
    42.2f, -92.5f,
    42.2f, -87.7f,
    42.2f, -82.8f,
    42.2f, -77.9f,
    42.2f, -73.1f,
    42.2f, -5.1f,
    42.2f, -0.2f,
    42.2f, 9.5f,
    42.2f, 14.4f,
    42.2f, 19.2f,
    42.2f, 24.1f,
    42.2f, 43.5f,
    42.2f, 53.3f,
    42.2f, 58.1f,
    42.2f, 63.0f,
    42.2f, 67.8f,
    42.2f, 72.7f,
    42.2f, 77.6f,
    42.2f, 82.4f,
    42.2f, 87.3f,
    42.2f, 92.1f,
    42.2f, 97.0f,
    42.2f, 101.9f,
    42.2f, 106.7f,
    42.2f, 111.6f,
    42.2f, 116.4f,
    42.2f, 121.3f,
    42.2f, 126.2f,
    45.8f, -123.2f,
    45.8f, -118.0f,
    45.8f, -112.9f,
    45.8f, -107.7f,
    45.8f, -102.5f,
    45.8f, -97.4f,
    45.8f, -92.2f,
    45.8f, -87.1f,
    45.8f, -81.9f,
    45.8f, -76.7f,
    45.8f, -71.6f,
    45.8f, -66.4f,
    45.8f, -61.2f,
    45.8f, 0.7f,
    45.8f, 5.9f,
    45.8f, 11.1f,
    45.8f, 16.2f,
    45.8f, 21.4f,
    45.8f, 26.6f,
    45.8f, 42.0f,
    45.8f, 47.2f,
    45.8f, 57.5f,
    45.8f, 62.7f,
    45.8f, 67.9f,
    45.8f, 73.0f,
    45.8f, 78.2f,
    45.8f, 83.4f,
    45.8f, 88.5f,
    45.8f, 93.7f,
    45.8f, 98.8f,
    45.8f, 104.0f,
    45.8f, 109.2f,
    45.8f, 114.3f,
    45.8f, 119.5f,
    45.8f, 124.7f,
    45.8f, 129.8f,
    45.8f, 135.0f,
    49.4f, -124.7f,
    49.4f, -119.1f,
    49.4f, -113.6f,
    49.4f, -108.1f,
    49.4f, -102.6f,
    49.4f, -97.0f,
    49.4f, -91.5f,
    49.4f, -86.0f,
    49.4f, -80.4f,
    49.4f, -74.9f,
    49.4f, -69.4f,
    49.4f, 2.6f,
    49.4f, 8.1f,
    49.4f, 13.6f,
    49.4f, 19.1f,
    49.4f, 24.7f,
    49.4f, 30.2f,
    49.4f, 35.7f,
    49.4f, 41.3f,
    49.4f, 46.8f,
    49.4f, 52.3f,
    49.4f, 57.9f,
    49.4f, 63.4f,
    49.4f, 68.9f,
    49.4f, 74.5f,
    49.4f, 80.0f,
    49.4f, 85.5f,
    49.4f, 91.1f,
    49.4f, 96.6f,
    49.4f, 102.1f,
    49.4f, 107.7f,
    49.4f, 113.2f,
    49.4f, 118.7f,
    49.4f, 124.3f,
    49.4f, 129.8f,
    49.4f, 135.3f,
    53.0f, -126.2f,
    53.0f, -120.2f,
    53.0f, -114.2f,
    53.0f, -108.2f,
    53.0f, -102.2f,
    53.0f, -96.3f,
    53.0f, -90.3f,
    53.0f, -84.3f,
    53.0f, -78.3f,
    53.0f, -72.3f,
    53.0f, -66.3f,
    53.0f, -60.4f,
    53.0f, -6.5f,
    53.0f, -0.5f,
    53.0f, 5.4f,
    53.0f, 11.4f,
    53.0f, 17.4f,
    53.0f, 23.4f,
    53.0f, 29.4f,
    53.0f, 35.3f,
    53.0f, 41.3f,
    53.0f, 47.3f,
    53.0f, 53.3f,
    53.0f, 59.3f,
    53.0f, 65.3f,
    53.0f, 71.2f,
    53.0f, 77.2f,
    53.0f, 83.2f,
    53.0f, 89.2f,
    53.0f, 95.2f,
    53.0f, 101.1f,
    53.0f, 107.1f,
    53.0f, 113.1f,
    53.0f, 119.1f,
    53.0f, 125.1f,
    53.0f, 131.1f,
    53.0f, 137.0f,
    53.0f, 143.0f,
    56.6f, -127.7f,
    56.6f, -121.1f,
    56.6f, -114.6f,
    56.6f, -108.1f,
    56.6f, -101.5f,
    56.6f, -95.0f,
    56.6f, -88.4f,
    56.6f, -75.4f,
    56.6f, -68.8f,
    56.6f, -62.3f,
    56.6f, -3.4f,
    56.6f, 9.7f,
    56.6f, 16.2f,
    56.6f, 22.7f,
    56.6f, 29.3f,
    56.6f, 35.8f,
    56.6f, 42.4f,
    56.6f, 48.9f,
    56.6f, 55.4f,
    56.6f, 62.0f,
    56.6f, 68.5f,
    56.6f, 75.0f,
    56.6f, 81.6f,
    56.6f, 88.1f,
    56.6f, 94.7f,
    56.6f, 101.2f,
    56.6f, 107.7f,
    56.6f, 114.3f,
    56.6f, 120.8f,
    56.6f, 127.4f,
    56.6f, 133.9f,
    56.6f, 160.1f,
    60.2f, -158.3f,
    60.2f, -151.0f,
    60.2f, -143.8f,
    60.2f, -136.5f,
    60.2f, -129.3f,
    60.2f, -122.0f,
    60.2f, -114.8f,
    60.2f, -107.6f,
    60.2f, -100.3f,
    60.2f, -71.3f,
    60.2f, 8.3f,
    60.2f, 15.6f,
    60.2f, 30.1f,
    60.2f, 37.3f,
    60.2f, 44.6f,
    60.2f, 51.8f,
    60.2f, 59.0f,
    60.2f, 66.3f,
    60.2f, 73.5f,
    60.2f, 80.8f,
    60.2f, 88.0f,
    60.2f, 95.3f,
    60.2f, 102.5f,
    60.2f, 109.8f,
    60.2f, 117.0f,
    60.2f, 124.2f,
    60.2f, 131.5f,
    60.2f, 138.7f,
    60.2f, 146.0f,
    60.2f, 153.2f,
    63.8f, -155.5f,
    63.8f, -147.4f,
    63.8f, -139.2f,
    63.8f, -131.1f,
    63.8f, -122.9f,
    63.8f, -114.8f,
    63.8f, -106.6f,
    63.8f, -98.5f,
    63.8f, -90.3f,
    63.8f, -82.2f,
    63.8f, -65.8f,
    63.8f, -49.5f,
    63.8f, 15.7f,
    63.8f, 23.8f,
    63.8f, 32.0f,
    63.8f, 40.2f,
    63.8f, 48.3f,
    63.8f, 56.5f,
    63.8f, 64.6f,
    63.8f, 72.8f,
    63.8f, 80.9f,
    63.8f, 89.1f,
    63.8f, 97.2f,
    63.8f, 105.4f,
    63.8f, 113.5f,
    63.8f, 121.7f,
    63.8f, 129.8f,
    63.8f, 138.0f,
    63.8f, 146.2f,
    63.8f, 154.3f,
    63.8f, 162.5f,
    63.8f, 170.6f,
    67.4f, -161.3f,
    67.4f, -151.9f,
    67.4f, -142.5f,
    67.4f, -133.2f,
    67.4f, -123.8f,
    67.4f, -114.4f,
    67.4f, -105.1f,
    67.4f, -86.3f,
    67.4f, -77.0f,
    67.4f, -67.6f,
    67.4f, -48.9f,
    67.4f, -39.5f,
    67.4f, 16.7f,
    67.4f, 26.1f,
    67.4f, 35.5f,
    67.4f, 44.8f,
    67.4f, 54.2f,
    67.4f, 63.6f,
    67.4f, 82.3f,
    67.4f, 91.7f,
    67.4f, 101.0f,
    67.4f, 110.4f,
    67.4f, 119.8f,
    67.4f, 129.1f,
    67.4f, 138.5f,
    67.4f, 147.9f,
    67.4f, 157.2f,
    67.4f, 166.6f,
    67.4f, 176.0f,
    71.0f, -113.7f,
    71.0f, -80.5f,
    71.0f, -47.3f,
    71.0f, -36.3f,
    71.0f, 74.3f,
    71.0f, 85.4f,
    71.0f, 96.4f,
    71.0f, 107.5f,
    71.0f, 118.6f,
    71.0f, 129.6f,
    71.0f, 140.7f,
    71.0f, 151.7f
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

    for (s in allServers) {
        val isWl = Store.isWlBypass(s)
        val n = s.name.lowercase()
        when {
            isWl || n.contains("bypass") || n.contains("wl") || n.contains("\u0440\u043E\u0441\u0441\u0438\u044F") || n.contains("russia") -> {
                wlBypassServers.add(s)
            }
            n.contains("\u0433\u0435\u0440\u043C\u0430\u043D") || n.contains("germany") || s.name.contains("\uD83C\uDDE9\uD83C\uDDEA") || Regex("""\\bde\\b""").containsMatchIn(n) -> {
                germanyServers.add(s)
            }
            n.contains("\u0444\u0438\u043D\u043B\u044F\u043D\u0434") || n.contains("finland") || s.name.contains("\uD83C\uDDEB\uD83C\uDDEE") || Regex("""\\bfi\\b""").containsMatchIn(n) -> {
                finlandServers.add(s)
            }
            n.contains("\u043D\u0438\u0434\u0435\u0440\u043B\u0430\u043D\u0434") || n.contains("netherlands") || n.contains("amsterdam") || s.name.contains("\uD83C\uDDF3\uD83C\uDDF1") || Regex("""\\bnl\\b""").containsMatchIn(n) -> {
                netherlandsServers.add(s)
            }
            n.contains("\u0448\u0432\u0435\u0446") || n.contains("sweden") || n.contains("stockholm") || s.name.contains("\uD83C\uDDF8\uD83C\uDDEA") || Regex("""\\bse\\b""").containsMatchIn(n) -> {
                swedenServers.add(s)
            }
            n.contains("\u043F\u043E\u043B\u044C\u0448") || n.contains("poland") || n.contains("warsaw") || s.name.contains("\uD83C\uDDF5\uD83C\uDDF1") || Regex("""\\bpl\\b""").containsMatchIn(n) -> {
                polandServers.add(s)
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

    fun isNodeSelected(list: List<Server>): Boolean {
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
            countryName = "\u0413\u0435\u0440\u043C\u0430\u043D\u0438\u044F",
            flag = "",
            lat = GlobeCoordinates.GERMANY_LAT,
            lon = GlobeCoordinates.GERMANY_LON,
            isWlBypass = false,
            servers = germanyServers,
            bestServer = deBest,
            bestPing = bestPing(germanyServers),
            isSelected = isNodeSelected(germanyServers)
        )
    )

    // Finland
    val fiBest = bestOf(finlandServers)
    nodes.add(
        GlobeNode(
            id = "NODE_FI",
            countryCode = "FI",
            countryName = "\u0424\u0438\u043D\u043B\u044F\u043D\u0434\u0438\u044F",
            flag = "",
            lat = GlobeCoordinates.FINLAND_LAT,
            lon = GlobeCoordinates.FINLAND_LON,
            isWlBypass = false,
            servers = finlandServers,
            bestServer = fiBest,
            bestPing = bestPing(finlandServers),
            isSelected = isNodeSelected(finlandServers)
        )
    )

    // Netherlands
    val nlBest = bestOf(netherlandsServers)
    nodes.add(
        GlobeNode(
            id = "NODE_NL",
            countryCode = "NL",
            countryName = "\u041D\u0438\u0434\u0435\u0440\u043B\u0430\u043D\u0434\u044B",
            flag = "",
            lat = GlobeCoordinates.NETHERLANDS_LAT,
            lon = GlobeCoordinates.NETHERLANDS_LON,
            isWlBypass = false,
            servers = netherlandsServers,
            bestServer = nlBest,
            bestPing = bestPing(netherlandsServers),
            isSelected = isNodeSelected(netherlandsServers)
        )
    )

    // Sweden
    val seBest = bestOf(swedenServers)
    nodes.add(
        GlobeNode(
            id = "NODE_SE",
            countryCode = "SE",
            countryName = "\u0428\u0432\u0435\u0446\u0438\u044F",
            flag = "",
            lat = GlobeCoordinates.SWEDEN_LAT,
            lon = GlobeCoordinates.SWEDEN_LON,
            isWlBypass = false,
            servers = swedenServers,
            bestServer = seBest,
            bestPing = bestPing(swedenServers),
            isSelected = isNodeSelected(swedenServers)
        )
    )

    // Poland
    val plBest = bestOf(polandServers)
    nodes.add(
        GlobeNode(
            id = "NODE_PL",
            countryCode = "PL",
            countryName = "\u041F\u043E\u043B\u044C\u0448\u0430",
            flag = "",
            lat = GlobeCoordinates.POLAND_LAT,
            lon = GlobeCoordinates.POLAND_LON,
            isWlBypass = false,
            servers = polandServers,
            bestServer = plBest,
            bestPing = bestPing(polandServers),
            isSelected = isNodeSelected(polandServers)
        )
    )

    // Whitelist Bypass (Russia) - Visually highlighted in gold with distinct amber styling
    val wlBest = bestOf(wlBypassServers)
    nodes.add(
        GlobeNode(
            id = "NODE_WL_BYPASS",
            countryCode = "WL",
            countryName = "WL Bypass",
            flag = "",
            lat = GlobeCoordinates.RUSSIA_WL_LAT,
            lon = GlobeCoordinates.RUSSIA_WL_LON,
            isWlBypass = true,
            servers = wlBypassServers,
            bestServer = wlBest,
            bestPing = bestPing(wlBypassServers),
            isSelected = isNodeSelected(wlBypassServers)
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
    val ctx = LocalContext.current

    // Natural realistic globe view centered on European & Eurasian servers
    val yawAnim = remember { Animatable(Math.toRadians(18.0).toFloat()) }
    val pitchAnim = remember { Animatable(Math.toRadians(-52.0).toFloat()) }
    val zoomAnim = remember { Animatable(1.75f) }

    var isUserInteracting by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(0L) }

    // Smoothly turn and zoom into target location on selection
    LaunchedEffect(targetLocation) {
        targetLocation?.let { (lat, lon) ->
            val targetYaw = Math.toRadians(lon.toDouble()).toFloat()
            val targetPitch = (-Math.toRadians(lat.toDouble()) * 0.88).toFloat().coerceIn(-1.1f, 1.1f)
            launch {
                yawAnim.animateTo(
                    targetYaw,
                    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
                )
            }
            launch {
                pitchAnim.animateTo(
                    targetPitch,
                    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
                )
            }
            // Smoothly zoom in close to the country
            launch {
                zoomAnim.animateTo(
                    2.15f,
                    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    // Auto-return to home state after 1.5 seconds of user inactivity
    LaunchedEffect(lastInteractionTime, targetLocation) {
        if (lastInteractionTime > 0L) {
            delay(1500)
            isUserInteracting = false
            val (homeLat, homeLon) = targetLocation ?: Pair(54.0f, 18.0f)
            val homeYaw = Math.toRadians(homeLon.toDouble()).toFloat()
            val homePitch = (-Math.toRadians(homeLat.toDouble()) * 0.88).toFloat().coerceIn(-1.1f, 1.1f)
            launch {
                yawAnim.animateTo(homeYaw, tween(850, easing = FastOutSlowInEasing))
            }
            launch {
                pitchAnim.animateTo(homePitch, tween(850, easing = FastOutSlowInEasing))
            }
            launch {
                zoomAnim.animateTo(1.75f, tween(850, easing = FastOutSlowInEasing))
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

    // Theme-based colors (no hardcoded yellow!)
    val accentColor = NX.accent
    val accentLight = NX.accentLight
    val cardColor = NX.card
    val bgColor = NX.bg
    val borderColor = NX.cardBorder
    val wlThemeColor = NX.accentLight
    val wlThemeDim = NX.accentDim

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures(
                    panZoomLock = false,
                    onGesture = { _, pan, zoom, _ ->
                        isUserInteracting = true
                        lastInteractionTime = System.currentTimeMillis()
                        scope.launch {
                            val newYaw = yawAnim.value - pan.x * 0.0035f
                            val newPitch = (pitchAnim.value - pan.y * 0.0035f).coerceIn(-1.15f, 1.15f)
                            yawAnim.snapTo(newYaw)
                            pitchAnim.snapTo(newPitch)

                            val newZoom = (zoomAnim.value * zoom).coerceIn(0.9f, 4.5f)
                            zoomAnim.snapTo(newZoom)
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
            globeRadius = min(size.width, size.height) * 0.50f * zoomAnim.value
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

            // 5. Authentic continent dot matrix (clean sci-fi radar map)
            var idx = 0
            val len = CONTINENT_POINTS.size
            while (idx < len) {
                val cLat = CONTINENT_POINTS[idx]
                val cLon = CONTINENT_POINTS[idx + 1]
                idx += 2

                val (dx, dy, dz) = projectGlobePoint(cLat, cLon, currentYaw, currentPitch, r)
                if (dz > 0f) {
                    val depthFactor = (dz / r).coerceIn(0f, 1f)
                    val dotAlpha = (0.28f + 0.52f * depthFactor).coerceIn(0.18f, 0.88f)
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
                    val baseColor = if (node.isWlBypass) wlThemeColor else accentLight

                    // Pulsing radar ripple waves (compact and crisp)
                    val maxWaveR = 14.dp.toPx()
                    val waveR1 = 4.dp.toPx() + maxWaveR * radarPulse
                    val waveA1 = (0.50f * (1f - radarPulse) * depth).coerceIn(0f, 1f)
                    drawCircle(
                        color = baseColor.copy(alpha = waveA1),
                        radius = waveR1,
                        center = nodeCenter,
                        style = Stroke(width = 1.4.dp.toPx())
                    )

                    val p2 = (radarPulse + 0.5f) % 1f
                    val waveR2 = 4.dp.toPx() + maxWaveR * p2
                    val waveA2 = (0.50f * (1f - p2) * depth).coerceIn(0f, 1f)
                    drawCircle(
                        color = baseColor.copy(alpha = waveA2),
                        radius = waveR2,
                        center = nodeCenter,
                        style = Stroke(width = 1.2.dp.toPx())
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

        // 7. Clean Country Labels (Text only, smart directional offsets to avoid overlap)
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
                        accentLight
                    } else {
                        if (isWl) wlThemeDim.copy(alpha = 0.85f) else borderColor.copy(alpha = 0.65f)
                    }
                    val badgeBg = if (node.isSelected) {
                        if (isWl) wlThemeDim.copy(alpha = 0.85f) else accentColor.copy(alpha = 0.35f)
                    } else {
                        if (isWl) wlThemeDim.copy(alpha = 0.30f) else cardColor.copy(alpha = 0.88f)
                    }

                    // Radial directional offsets from cluster center so labels NEVER overlap
                    val (dirOffsetX, dirOffsetY) = when (node.countryCode) {
                        "NL" -> Pair((-54).dp, (-4).dp)   // Netherlands: West
                        "DE" -> Pair(0.dp, 34.dp)         // Germany: South
                        "PL" -> Pair(26.dp, 32.dp)        // Poland: South-East
                        "SE" -> Pair((-18).dp, (-34).dp)  // Sweden: North / Up-Left
                        "FI" -> Pair(34.dp, (-30).dp)     // Finland: North-East / Up-Right
                        "WL" -> Pair(52.dp, 0.dp)         // WL Bypass: East
                        else -> Pair(0.dp, (-22).dp)
                    }

                    val interactionSource = remember { MutableInteractionSource() }

                    Box(
                        modifier = Modifier
                            .layout { measurable, constraints ->
                                val p = measurable.measure(constraints)
                                layout(p.width, p.height) {
                                    val px = (nodeX + dirOffsetX.toPx() - p.width / 2f).roundToInt()
                                    val py = (nodeY + dirOffsetY.toPx() - p.height / 2f).roundToInt()
                                    p.placeRelative(px, py)
                                }
                            }
                            .graphicsLayer {
                                this.alpha = alpha
                                scaleX = 0.85f + 0.15f * depth
                                scaleY = 0.85f + 0.15f * depth
                            }
                            .clip(RoundedCornerShape(10.dp))
                            .background(badgeBg)
                            .border(1.dp, badgeBorder, RoundedCornerShape(10.dp))
                            .clickable(interactionSource = interactionSource, indication = null) {
                                HapticManager.trigger(ctx, net.nexus.app.data.HapticIntensity.LIGHT, HapticType.CLICK)
                                onNodeClick(node)
                            }
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = node.countryName,
                                fontSize = 11.sp,
                                fontWeight = if (node.isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (node.isSelected || isWl) accentLight else NX.text
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
                                        .size(5.dp)
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
