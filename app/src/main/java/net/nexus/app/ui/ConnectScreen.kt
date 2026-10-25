package net.nexus.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.nexus.app.MainViewModel
import net.nexus.app.R
import net.nexus.app.data.Server
import net.nexus.app.vpn.VpnState
import net.nexus.app.vpn.VpnStatus

/** Home screen: logo, power button, status, server card, speed + ping tiles. */
@Composable
fun ConnectScreen(vm: MainViewModel, onToggle: () -> Unit, onAddServers: () -> Unit) {
    val providers by vm.providers.collectAsState()
    val settings by vm.settings.collectAsState()
    val status by VpnState.status.collectAsState()
    val speed by vm.speedMbps.collectAsState()
    val speedTesting by vm.speedTesting.collectAsState()
    val pinging by vm.pinging.collectAsState()
    val locale = LocalConfiguration.current.locales[0]

    val selected = providers.flatMap { it.servers }.firstOrNull { it.id == settings.selectedServerId }
    val connected = status == VpnStatus.CONNECTED
    val connecting = status == VpnStatus.CONNECTING

    val mbpsUnit = stringResource(R.string.unit_mbps)
    val msUnit = stringResource(R.string.unit_ms)
    val tapText = stringResource(R.string.tap_to_test)
    val speedValue = speed?.let { String.format(locale, if (it < 10) "%.1f %s" else "%.0f %s", it, mbpsUnit) }
        ?: if (connected) tapText else "—"
    val pingMs = selected?.pingMs ?: -1
    val pingValue = if (pingMs > 0) "$pingMs $msUnit" else "—"

    val glowAlphaTarget = when {
        connected -> 0.35f
        connecting -> 0.22f
        else -> 0.12f
    }
    val glowAlpha by animateFloatAsState(glowAlphaTarget, tween(600, easing = FastOutSlowInEasing), label = "glowAlpha")
    val glowColor by animateColorAsState(
        when (status) {
            VpnStatus.CONNECTED -> NX.accent
            VpnStatus.CONNECTING -> NX.yellow
            VpnStatus.ERROR -> NX.red
            VpnStatus.DISCONNECTED -> NX.accent
        },
        tween(500),
        label = "glowColor"
    )

    Box(Modifier.fillMaxSize()) {
        // dynamic animated ambient glow behind the button
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height * 0.34f)
            val r = size.width * 0.95f
            drawCircle(
                Brush.radialGradient(
                    listOf(glowColor.copy(alpha = glowAlpha), Color.Transparent),
                    center = c, radius = r,
                ),
                radius = r, center = c,
            )
        }

        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))
            Header()
            Spacer(Modifier.weight(1f))
            PowerButton(status, onToggle)
            Spacer(Modifier.height(20.dp))
            StatusLabel(status)
            Spacer(Modifier.weight(1f))
            ServerCard(selected, onAddServers)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                InfoTile(
                    Modifier.weight(1f), Icons.Filled.BarChart, stringResource(R.string.speed_label),
                    speedValue, speedTesting, onClick = { vm.runSpeedTest() },
                )
                InfoTile(
                    Modifier.weight(1f), Icons.Filled.AccessTime, stringResource(R.string.ping_label),
                    pingValue, selected != null && selected.id in pinging, onClick = { vm.checkSelected() },
                )
            }
            Spacer(Modifier.height(112.dp)) // room for the bottom bar
        }
    }
}

@Composable
private fun Header() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("NEXUS", fontSize = 44.sp, fontWeight = FontWeight.Black, color = NX.text)
            Text(".net", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = NX.accentLight)
        }
        Spacer(Modifier.height(4.dp))
        Text("VPN", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NX.muted, letterSpacing = 10.sp)
    }
}

@Composable
private fun PowerButton(status: VpnStatus, onClick: () -> Unit) {
    val connected = status == VpnStatus.CONNECTED
    val connecting = status == VpnStatus.CONNECTING
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "powerButtonPress"
    )

    val spin = rememberInfiniteTransition(label = "spin")
    val angle by spin.animateFloat(
        0f, 360f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "angle",
    )
    val pulse by spin.animateFloat(
        0.82f, 1.05f, infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse",
    )
    val rippleProgress by spin.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "ripple",
    )

    val targetRingAlpha = when {
        connected -> 1f
        connecting -> 0.85f
        else -> 0.35f
    }
    val ringAlpha by animateFloatAsState(targetRingAlpha, tween(400), label = "ringAlpha")

    val ringColor by animateColorAsState(
        when (status) {
            VpnStatus.CONNECTED -> NX.accent
            VpnStatus.CONNECTING -> NX.yellow
            VpnStatus.ERROR -> NX.red
            VpnStatus.DISCONNECTED -> NX.accent
        },
        tween(400),
        label = "ringColor"
    )

    val iconTint by animateColorAsState(
        when (status) {
            VpnStatus.CONNECTED -> Color.White
            VpnStatus.CONNECTING -> NX.yellow
            VpnStatus.ERROR -> NX.red
            VpnStatus.DISCONNECTED -> NX.muted2
        },
        tween(350),
        label = "iconTint"
    )

    val iconScale by animateFloatAsState(
        if (connected) 1.06f else 1f,
        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "iconScale"
    )

    Box(
        Modifier
            .size(240.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val outer = size.minDimension / 2
            val stroke = 7.dp.toPx()
            val r = outer - 22.dp.toPx()

            // Outer breathing glow when connected
            if (connected) {
                drawCircle(
                    Brush.radialGradient(
                        listOf(ringColor.copy(alpha = 0.45f * pulse), Color.Transparent),
                        radius = outer * 1.05f
                    ),
                    radius = outer * 1.05f,
                )
            }

            // Radar ripple ring when connecting
            if (connecting) {
                val rippleR = r + (outer - r) * rippleProgress
                val rippleAlpha = ((1f - rippleProgress) * 0.45f).coerceIn(0f, 1f)
                drawCircle(
                    color = NX.yellow.copy(alpha = rippleAlpha),
                    radius = rippleR,
                    style = Stroke(3.dp.toPx()),
                )
            }

            // soft halo + crisp ring
            drawCircle(color = ringColor.copy(alpha = 0.22f * ringAlpha), radius = r, style = Stroke(stroke * 2.8f))
            drawCircle(color = ringColor.copy(alpha = ringAlpha), radius = r, style = Stroke(stroke))

            if (connecting) {
                rotate(angle) {
                    drawArc(
                        color = NX.accentLight, startAngle = 0f, sweepAngle = 100f, useCenter = false,
                        topLeft = Offset(center.x - r, center.y - r), size = Size(r * 2, r * 2),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
            drawCircle(
                Brush.verticalGradient(listOf(Color(0xFF1B1733), Color(0xFF120F22))),
                radius = r - stroke * 1.3f,
            )
        }
        Icon(
            Icons.Filled.PowerSettingsNew, null,
            tint = iconTint,
            modifier = Modifier
                .size(86.dp)
                .scale(iconScale),
        )
    }
}

@Composable
private fun StatusLabel(status: VpnStatus) {
    val (color, text) = when (status) {
        VpnStatus.CONNECTED -> NX.green to R.string.status_connected
        VpnStatus.CONNECTING -> NX.yellow to R.string.status_connecting
        VpnStatus.ERROR -> NX.red to R.string.status_error
        VpnStatus.DISCONNECTED -> NX.muted to R.string.status_disconnected
    }

    val animatedColor by animateColorAsState(color, tween(350), label = "statusColor")

    val dotPulseTransition = rememberInfiniteTransition(label = "dotPulse")
    val dotPulseScale by dotPulseTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(750, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dotPulseScale"
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(9.dp)
                .scale(if (status == VpnStatus.CONNECTING) dotPulseScale else 1f)
                .clip(CircleShape)
                .background(animatedColor)
        )
        Spacer(Modifier.width(10.dp))
        AnimatedContent(
            targetState = text,
            transitionSpec = {
                (slideInVertically(tween(260)) { height -> height / 2 } + fadeIn(tween(220)))
                    .togetherWith(slideOutVertically(tween(200)) { height -> -height / 2 } + fadeOut(tween(180)))
            },
            label = "statusTextTransition"
        ) { targetRes ->
            Text(stringResource(targetRes), color = animatedColor, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ServerCard(selected: Server?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "serverCardPress"
    )

    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(shape)
            .background(NX.card)
            .border(1.dp, NX.cardBorder, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(NX.iconBtn), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = selected?.name,
                transitionSpec = { fadeIn(tween(200)).togetherWith(fadeOut(tween(150))) },
                label = "flagTransition"
            ) { name ->
                Text(if (name != null) flagFor(name) else "🌐", fontSize = 22.sp)
            }
        }
        Spacer(Modifier.width(14.dp))
        AnimatedContent(
            targetState = selected?.name ?: stringResource(R.string.choose_server),
            transitionSpec = { (slideInVertically(tween(220)) { it / 3 } + fadeIn(tween(200))).togetherWith(slideOutVertically(tween(180)) { -it / 3 } + fadeOut(tween(150))) },
            modifier = Modifier.weight(1f),
            label = "serverNameTransition"
        ) { serverName ->
            Text(
                cleanServerName(serverName),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = NX.muted, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun InfoTile(
    modifier: Modifier, icon: ImageVector, label: String, value: String, loading: Boolean, onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "infoTilePress"
    )

    Row(
        modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(shape)
            .background(NX.card)
            .border(1.dp, NX.cardBorder, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = NX.accent, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, color = NX.muted, fontSize = 13.sp)
            Box(Modifier.height(26.dp), contentAlignment = Alignment.CenterStart) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = NX.accent)
                } else {
                    AnimatedContent(
                        targetState = value,
                        transitionSpec = {
                            (slideInVertically(tween(200)) { it / 2 } + fadeIn(tween(180)))
                                .togetherWith(slideOutVertically(tween(160)) { -it / 2 } + fadeOut(tween(140)))
                        },
                        label = "infoTileValue"
                    ) { targetVal ->
                        Text(targetVal, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

