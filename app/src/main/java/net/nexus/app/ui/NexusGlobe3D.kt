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
    // Authentic geographic coordinates
    const val NETHERLANDS_LAT = 52.37f
    const val NETHERLANDS_LON = 4.90f

    const val GERMANY_LAT = 51.50f
    const val GERMANY_LON = 10.50f

    const val POLAND_LAT = 52.10f
    const val POLAND_LON = 19.50f

    const val SWEDEN_LAT = 59.33f
    const val SWEDEN_LON = 18.06f

    const val FINLAND_LAT = 61.92f
    const val FINLAND_LON = 25.74f

    const val RUSSIA_WL_LAT = 55.75f
    const val RUSSIA_WL_LON = 37.62f
}


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

    // Natural zoomed-in 3D globe perspective focusing strictly on Europe & Western Russia
    val yawAnim = remember { Animatable(Math.toRadians(19.0).toFloat()) }
    val pitchAnim = remember { Animatable(Math.toRadians(-54.0).toFloat()) }
    val zoomAnim = remember { Animatable(3.5f) }

    var isUserInteracting by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(0L) }

    // Smoothly turn and zoom into target location on selection
    LaunchedEffect(targetLocation) {
        targetLocation?.let { (lat, lon) ->
            val targetYaw = Math.toRadians(lon.toDouble()).toFloat()
            val targetPitch = (-Math.toRadians(lat.toDouble())).toFloat().coerceIn(-1.35f, 1.35f)
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
                    4.2f,
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
            val homeYaw = targetLocation?.let { Math.toRadians(it.second.toDouble()).toFloat() } ?: Math.toRadians(19.0).toFloat()
            val homePitch = targetLocation?.let { (-Math.toRadians(it.first.toDouble())).toFloat().coerceIn(-1.35f, 1.35f) } ?: Math.toRadians(-54.0).toFloat()
            val homeZoom = if (targetLocation != null) 4.0f else 3.5f
            launch {
                yawAnim.animateTo(homeYaw, tween(850, easing = FastOutSlowInEasing))
            }
            launch {
                pitchAnim.animateTo(homePitch, tween(850, easing = FastOutSlowInEasing))
            }
            launch {
                zoomAnim.animateTo(homeZoom, tween(850, easing = FastOutSlowInEasing))
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
                            val newPitch = (pitchAnim.value - pan.y * 0.0035f).coerceIn(-1.45f, 1.45f)
                            yawAnim.snapTo(newYaw)
                            pitchAnim.snapTo(newPitch)

                            val newZoom = (zoomAnim.value * zoom).coerceIn(1.2f, 6.0f)
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

            // 4. Parallels & Meridians (Subtle geographic coordinate grid)
            val parallels = floatArrayOf(30f, 45f, 60f, 75f)
            for (pLat in parallels) {
                val step = 4
                var prevPt: Offset? = null
                var prevZ = 0f
                for (lon in -30..90 step step) {
                    val (px, py, pz) = projectGlobePoint(pLat, lon.toFloat(), currentYaw, currentPitch, r)
                    val currPt = Offset(cx + px, cy + py)
                    if (prevPt != null && prevZ > 0.05f * r && pz > 0.05f * r) {
                        drawLine(
                            color = borderColor.copy(alpha = 0.12f),
                            start = prevPt,
                            end = currPt,
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    prevPt = currPt
                    prevZ = pz
                }
            }

            val meridians = floatArrayOf(-15f, 0f, 15f, 30f, 45f, 60f)
            for (mLon in meridians) {
                val step = 3
                var prevPt: Offset? = null
                var prevZ = 0f
                for (lat in 20..85 step step) {
                    val (px, py, pz) = projectGlobePoint(lat.toFloat(), mLon, currentYaw, currentPitch, r)
                    val currPt = Offset(cx + px, cy + py)
                    if (prevPt != null && prevZ > 0.05f * r && pz > 0.05f * r) {
                        drawLine(
                            color = borderColor.copy(alpha = 0.12f),
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
            val len = GLOBE_LAND_POINTS.size
            while (idx < len) {
                val cLat = GLOBE_LAND_POINTS[idx]
                val cLon = GLOBE_LAND_POINTS[idx + 1]
                idx += 2

                val (dx, dy, dz) = projectGlobePoint(cLat, cLon, currentYaw, currentPitch, r)
                if (dz > 0.02f * r) {
                    val depthFactor = (dz / r).coerceIn(0f, 1f)
                    val dotAlpha = (0.35f + 0.55f * depthFactor).coerceIn(0.2f, 0.90f)
                    val dotRadius = (1.5f + 1.3f * depthFactor).dp.toPx()
                    drawCircle(
                        color = accentColor.copy(alpha = dotAlpha),
                        radius = dotRadius,
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

                    // Clean non-overlapping directional offsets:
                    // Wide separation across whole screen: Netherlands left, Germany down-left, Poland down-right, Sweden up-left, Finland up-right, WL Bypass right
                    val (dirOffsetX, dirOffsetY) = when (node.countryCode) {
                        "NL" -> Pair((-44).dp, 0.dp)       // Netherlands: Left
                        "DE" -> Pair((-8).dp, 28.dp)       // Germany: Down-Left
                        "PL" -> Pair(16.dp, 28.dp)        // Poland: Down-Right
                        "SE" -> Pair((-36).dp, (-14).dp)   // Sweden: Up-Left
                        "FI" -> Pair(36.dp, (-12).dp)      // Finland: Up-Right
                        "WL" -> Pair(38.dp, 0.dp)          // WL Bypass: Right
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
