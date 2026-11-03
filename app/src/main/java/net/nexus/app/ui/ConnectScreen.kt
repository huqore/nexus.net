package net.nexus.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import net.nexus.app.data.Store
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.runtime.mutableFloatStateOf
import kotlin.math.roundToInt
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import net.nexus.app.auth.SubscriptionNotifier
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.nexus.app.MainViewModel
import net.nexus.app.R
import net.nexus.app.data.Provider
import net.nexus.app.data.Server
import net.nexus.app.data.AppTheme
import net.nexus.app.vpn.VpnState
import net.nexus.app.vpn.VpnStatus

/** Home screen: logo, power button, status, server card, speed + ping tiles. */
@Composable
fun ConnectScreen(vm: MainViewModel, onToggle: () -> Unit, onAddServers: () -> Unit) {
    val providers by vm.providers.collectAsState()
    val settings by vm.settings.collectAsState()
    val status by VpnState.status.collectAsState()
    val activeId by VpnState.serverId.collectAsState()
    val speed by vm.speedMbps.collectAsState()
    val speedTesting by vm.speedTesting.collectAsState()
    val pinging by vm.pinging.collectAsState()
    val locale = LocalConfiguration.current.locales[0]

    val ctx = LocalContext.current
    val isAuto = settings.selectedServerId == Store.AUTO_SERVER_ID
    val selected = if (isAuto) {
        providers.flatMap { it.servers }.firstOrNull { it.id == activeId }
            ?: remember(providers) { Store(ctx).findBestServer() }
    } else {
        providers.flatMap { it.servers }.firstOrNull { it.id == settings.selectedServerId }
    }
    val connected = status == VpnStatus.CONNECTED
    val connecting = status == VpnStatus.CONNECTING

    val connectedAt by VpnState.connectedAt.collectAsState()

    var sessionDurationMs by remember { mutableStateOf(0L) }
    LaunchedEffect(connected, connectedAt) {
        if (connected && connectedAt > 0) {
            while (isActive) {
                sessionDurationMs = (System.currentTimeMillis() - connectedAt).coerceAtLeast(0L)
                delay(1000)
            }
        } else {
            sessionDurationMs = 0L
        }
    }

    val durationText = fmtDuration(sessionDurationMs)

    val prevStatus = remember { mutableStateOf(status) }
    LaunchedEffect(status) {
        if (prevStatus.value != status) {
            if (status == VpnStatus.CONNECTED) {
                SoundManager.playConnect(settings.appSounds)
            } else if (prevStatus.value == VpnStatus.CONNECTED && status == VpnStatus.DISCONNECTED) {
                SoundManager.playDisconnect(settings.appSounds)
            }
            prevStatus.value = status
        }
    }

    val tgUser by vm.telegramUser.collectAsState()
    val daysLeft: Long? = if (!tgUser?.expireAt.isNullOrBlank()) {
        SubscriptionNotifier.getDaysLeft(tgUser!!.expireAt)
    } else null
    var dismissedNotification by rememberSaveable { mutableStateOf(false) }
    var showInAppNotification by remember { mutableStateOf(false) }

    val isTester = tgUser?.cleanUsername.equals("huqore", ignoreCase = true)
    val showExpiryWarning = tgUser != null && (isTester || (daysLeft != null && daysLeft <= 3))

    LaunchedEffect(showExpiryWarning) {
        if (showExpiryWarning && !dismissedNotification) {
            delay(450)
            showInAppNotification = true
            SoundManager.playNotification(settings.appSounds)
        }
    }

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

        // Falling particles behind all elements on home screen
        when (settings.appTheme) {
            AppTheme.SAKURA -> SakuraPetalsOverlay()
            AppTheme.WINTER -> WinterSnowOverlay()
            AppTheme.SECRET_RGB -> SecretBunnyCatOverlay()
            else -> Unit
        }

        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))
            Header()
            Spacer(Modifier.weight(1f))
            PowerButton(status = status, isSecretRgb = settings.appTheme == AppTheme.SECRET_RGB) {
                HapticManager.trigger(ctx, settings.hapticIntensity, HapticType.TOGGLE)
                if (settings.appTheme == AppTheme.SECRET_RGB) {
                    SoundManager.playCatPaw(settings.appSounds)
                }
                onToggle()
            }
            Spacer(Modifier.height(20.dp))
            StatusLabel(status)
            AnimatedVisibility(
                visible = connected,
                enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(10.dp))
                    SessionTimerBadge(duration = durationText)
                }
            }
            Spacer(Modifier.weight(1f))
            ServerCard(selected, isAuto, connected) {
                HapticManager.trigger(ctx, settings.hapticIntensity, HapticType.CLICK)
                onAddServers()
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                InfoTile(
                    Modifier.weight(1f), Icons.Filled.BarChart, stringResource(R.string.speed_label),
                    speedValue, speedTesting, onClick = {
                        HapticManager.trigger(ctx, settings.hapticIntensity, HapticType.CLICK)
                        vm.runSpeedTest()
                    },
                )
                InfoTile(
                    Modifier.weight(1f), Icons.Filled.AccessTime, stringResource(R.string.ping_label),
                    pingValue, selected != null && selected.id in pinging, onClick = {
                        HapticManager.trigger(ctx, settings.hapticIntensity, HapticType.CLICK)
                        vm.checkSelected()
                    },
                )
            }

            Spacer(Modifier.height(112.dp)) // room for the bottom bar
        }

        // In-App Expiry Notification floating smoothly from top
        AnimatedVisibility(
            visible = showExpiryWarning && showInAppNotification && !dismissedNotification,
            enter = slideInVertically(
                initialOffsetY = { -it - 60 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { -it - 60 },
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            ) + fadeOut(tween(180)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            InAppSubscriptionNotification(
                daysLeft = if (isTester && (daysLeft == null || daysLeft > 3)) 2L else (daysLeft ?: 0L),
                isTester = isTester,
                onRenew = {
                    runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/NEXUSnet_robot")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        ctx.startActivity(intent)
                    }
                },
                onDismiss = {
                    dismissedNotification = true
                }
            )
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
private fun PowerButton(status: VpnStatus, isSecretRgb: Boolean = false, onClick: () -> Unit) {
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
    val breathePulse by spin.animateFloat(
        0.75f, 1.0f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathePulse",
    )
    val wave1 by spin.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "wave1",
    )
    val wave2 by spin.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2600, delayMillis = 1300, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "wave2",
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

            // Outer soft breathing glow and smooth expanding waves in active theme color
            if (connected) {
                drawCircle(
                    Brush.radialGradient(
                        listOf(NX.accent.copy(alpha = 0.40f * breathePulse), Color.Transparent),
                        radius = outer * 1.18f
                    ),
                    radius = outer * 1.18f,
                )

                // Soft wave 1 - expanding ripple in theme accent
                val rWave1 = r + (outer - r + 16.dp.toPx()) * wave1
                val alphaWave1 = (0.38f * (1f - wave1)).coerceIn(0f, 1f)
                drawCircle(
                    color = NX.accent.copy(alpha = alphaWave1),
                    radius = rWave1,
                    style = Stroke(3.5.dp.toPx()),
                )

                // Soft wave 2 - staggered second ripple in theme accentLight
                val rWave2 = r + (outer - r + 16.dp.toPx()) * wave2
                val alphaWave2 = (0.30f * (1f - wave2)).coerceIn(0f, 1f)
                drawCircle(
                    color = NX.accentLight.copy(alpha = alphaWave2),
                    radius = rWave2,
                    style = Stroke(2.5.dp.toPx()),
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

            // soft halo + crisp ring in active theme color
            val haloAlpha = if (connected) 0.30f * breathePulse else 0.18f * ringAlpha
            drawCircle(color = ringColor.copy(alpha = haloAlpha), radius = r, style = Stroke(stroke * 2.8f))
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
                Brush.verticalGradient(listOf(NX.btnGradTop, NX.btnGradBottom)),
                radius = r - stroke * 1.3f,
            )
            if (connected) {
                // Soft inner glow inside button in theme accent
                drawCircle(
                    Brush.radialGradient(
                        listOf(NX.accent.copy(alpha = 0.20f * breathePulse), Color.Transparent),
                        radius = (r - stroke * 1.3f) * 0.95f
                    ),
                    radius = (r - stroke * 1.3f) * 0.95f,
                )
            }
        }
        if (isSecretRgb) {
            Image(
                painter = painterResource(R.drawable.img_cat_power),
                contentDescription = "Secret Cat Button",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(178.dp)
                    .clip(CircleShape)
                    .scale(iconScale)
            )
        } else {
            Icon(
                Icons.Filled.PowerSettingsNew, null,
                tint = iconTint,
                modifier = Modifier
                    .size(86.dp)
                    .scale(iconScale),
            )
        }
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
private fun SessionTimerBadge(
    duration: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(NX.card.copy(alpha = 0.85f))
            .border(1.dp, NX.cardBorder, shape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            Icons.Filled.Schedule,
            contentDescription = null,
            tint = NX.accentLight,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = duration,
            fontSize = 13.5.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = NX.text
        )
    }
}

@Composable
private fun ServerCard(
    selected: Server?,
    isAuto: Boolean,
    isConnected: Boolean,
    onClick: () -> Unit
) {
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
        if (isAuto && !isConnected) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(NX.iconBtn), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Bolt, contentDescription = null, tint = NX.accent, modifier = Modifier.size(24.dp))
            }
        } else if (selected != null) {
            FlagBox(selected.name, size = 40.dp)
        } else {
            Box(Modifier.size(40.dp).clip(CircleShape).background(NX.iconBtn), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Bolt, contentDescription = null, tint = NX.accent, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        val titleText = when {
            isAuto && isConnected && selected != null -> stringResource(R.string.auto_server_connected, cleanServerName(selected.name))
            isAuto -> stringResource(R.string.auto_server_title)
            selected != null -> cleanServerName(selected.name)
            else -> stringResource(R.string.choose_server)
        }
        AnimatedContent(
            targetState = titleText,
            transitionSpec = { (slideInVertically(tween(220)) { it / 3 } + fadeIn(tween(200))).togetherWith(slideOutVertically(tween(180)) { -it / 3 } + fadeOut(tween(150))) },
            modifier = Modifier.weight(1f),
            label = "serverNameTransition"
        ) { text ->
            Column {
                Text(
                    text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                if (isAuto && !isConnected) {
                    Text(
                        stringResource(R.string.auto_server_desc),
                        fontSize = 12.sp,
                        color = NX.muted,
                        maxLines = 1
                    )
                }
            }
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
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = NX.accent, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f, fill = false)) {
            Text(label, color = NX.muted, fontSize = 12.sp, maxLines = 1)
            Box(Modifier.height(26.dp), contentAlignment = Alignment.CenterStart) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = NX.accent)
                } else {
                    AnimatedContent(
                        targetState = value,
                        transitionSpec = {
                            (slideInVertically(tween(200)) { it / 2 } + fadeIn(tween(180)))
                                .togetherWith(slideOutVertically(tween(160)) { -it / 2 } + fadeOut(tween(140)))
                        },
                        label = "infoTileValue"
                    ) { targetVal ->
                        Text(
                            targetVal,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun InAppSubscriptionNotification(
    daysLeft: Long,
    isTester: Boolean,
    onRenew: () -> Unit,
    onDismiss: () -> Unit
) {
    var offsetY by remember { mutableFloatStateOf(0f) }
    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "notificationSwipeY"
    )

    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, animatedOffsetY.roundToInt()) }
            .draggable(
                state = rememberDraggableState { delta ->
                    offsetY = (offsetY + delta).coerceAtMost(0f)
                },
                orientation = Orientation.Vertical,
                onDragStopped = { velocity ->
                    if (offsetY < -35f || velocity < -250f) {
                        onDismiss()
                    } else {
                        offsetY = 0f
                    }
                }
            )
            .shadow(
                elevation = 14.dp,
                shape = shape,
                spotColor = Color.Black.copy(alpha = 0.8f),
                ambientColor = NX.yellow.copy(alpha = 0.25f)
            )
            .clip(shape)
            .background(Color(0xFF0F172A))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        NX.yellow.copy(alpha = 0.7f),
                        NX.yellow.copy(alpha = 0.35f),
                        Color(0xFF38BDF8).copy(alpha = 0.25f)
                    )
                ),
                shape = shape
            )
            .clickable { onRenew() }
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(NX.yellow.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.HourglassTop,
                    contentDescription = null,
                    tint = NX.yellow,
                    modifier = Modifier.size(15.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.sub_expiry_warning_title),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    if (isTester) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.sub_expiry_test_badge),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NX.yellow
                        )
                    }
                }
                Text(
                    text = when {
                        daysLeft <= 0 -> stringResource(R.string.sub_expiry_warning_expired)
                        daysLeft == 1L -> stringResource(R.string.sub_expiry_warning_today)
                        else -> stringResource(R.string.sub_expiry_warning_desc, daysLeft)
                    },
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = NX.yellow,
                    maxLines = 1
                )
            }

            Spacer(Modifier.width(6.dp))

            Button(
                onClick = onRenew,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NX.yellow,
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text(
                    text = stringResource(R.string.sub_expiry_renew_short),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            Spacer(Modifier.width(4.dp))

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = NX.muted2,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
