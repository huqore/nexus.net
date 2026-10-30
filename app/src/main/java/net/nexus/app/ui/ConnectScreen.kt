package net.nexus.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import net.nexus.app.auth.SubscriptionNotifier
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import net.nexus.app.auth.TelegramUser
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
import net.nexus.app.data.Server
import net.nexus.app.vpn.VpnState
import net.nexus.app.vpn.VpnStatus

/** Home screen: logo, power button, status, server card, speed + ping tiles. */
@Composable
fun ConnectScreen(vm: MainViewModel, onToggle: () -> Unit, onAddServers: () -> Unit) {
    val providers by vm.providers.collectAsState()
    val settings by vm.settings.collectAsState()
    val tgUser by vm.telegramUser.collectAsState()
    val isSyncingTg by vm.isSyncingTelegram.collectAsState()
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
            PowerButton(status) {
                HapticManager.trigger(ctx, settings.hapticIntensity, HapticType.TOGGLE)
                onToggle()
            }
            Spacer(Modifier.height(20.dp))
            StatusLabel(status)
            Spacer(Modifier.weight(1f))
            val openUrl: (String) -> Unit = { link ->
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    ctx.startActivity(intent)
                }
            }
            SubscriptionExpiringBanner(
                user = tgUser,
                onRenew = {
                    HapticManager.trigger(ctx, settings.hapticIntensity, HapticType.CLICK)
                    openUrl(ctx.getString(R.string.tg_auth_bot_url))
                }
            )
            TelegramSyncBar(
                user = tgUser,
                isSyncing = isSyncingTg,
                onLogin = { vm.startTelegramAuth() },
                onSync = { vm.syncTelegramSubscription() },
                onRenew = { openUrl(ctx.getString(R.string.tg_auth_bot_url)) }
            )
            Spacer(Modifier.height(10.dp))
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
private fun TelegramSyncBar(
    user: TelegramUser?,
    isSyncing: Boolean,
    onLogin: () -> Unit,
    onSync: () -> Unit,
    onRenew: () -> Unit,
) {
    val daysLeft = user?.let { SubscriptionNotifier.getDaysLeft(it.expireAt) }
    val isExpiringSoon = user != null && user.isSubscribed && daysLeft != null && daysLeft in 0..3
    val isExpired = user != null && (!user.isSubscribed || (daysLeft != null && daysLeft <= 0))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NX.card)
            .border(
                1.dp,
                when {
                    isExpired -> NX.red.copy(alpha = 0.5f)
                    isExpiringSoon -> NX.yellow.copy(alpha = 0.5f)
                    user != null -> NX.accent.copy(alpha = 0.35f)
                    else -> NX.cardBorder
                },
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = if (user == null) onLogin else onSync)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (user != null) NX.accentDim else NX.iconBtn),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                tint = if (user != null) NX.accentLight else NX.muted,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            if (user == null) {
                Text(
                    text = stringResource(R.string.tg_auth_banner_title),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NX.text
                )
                Text(
                    text = stringResource(R.string.tg_auth_banner_subtitle),
                    fontSize = 11.sp,
                    color = NX.muted
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.formattedUsername,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NX.text
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isExpired -> NX.red
                                    isExpiringSoon -> NX.yellow
                                    else -> NX.green
                                }
                            )
                    )
                }
                val statusText = when {
                    isExpired -> stringResource(R.string.sub_expired)
                    isExpiringSoon && daysLeft != null -> stringResource(R.string.sub_expiring_soon, daysLeft)
                    user.isSubscribed -> stringResource(R.string.tg_auth_status_active)
                    else -> stringResource(R.string.tg_auth_status_inactive)
                }
                val statusColor = when {
                    isExpired -> NX.red
                    isExpiringSoon -> NX.yellow
                    user.isSubscribed -> NX.green
                    else -> NX.yellow
                }
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    color = statusColor
                )
            }
        }

        if (user != null && (isExpired || isExpiringSoon)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isExpired) NX.accent else NX.yellow.copy(alpha = 0.2f))
                    .clickable(onClick = onRenew)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.sub_renew_btn),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isExpired) Color.White else NX.yellow
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        if (isSyncing) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = NX.accentLight,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = if (user == null) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.Filled.Refresh,
                contentDescription = null,
                tint = NX.muted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}





@Composable
private fun SubscriptionExpiringBanner(
    user: TelegramUser?,
    onRenew: () -> Unit,
) {
    val daysLeft = user?.let { SubscriptionNotifier.getDaysLeft(it.expireAt) }
    val isVisible = user != null && ((daysLeft != null && daysLeft in 0..3) || !user.isSubscribed || (daysLeft != null && daysLeft < 0))

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(250)) + expandVertically(tween(250)),
        exit = fadeOut(tween(200)) + shrinkVertically(tween(200)),
    ) {
        val isExpired = user != null && (!user.isSubscribed || (daysLeft != null && daysLeft <= 0))
        val isCritical = isExpired || daysLeft == 0L || daysLeft == 1L
        val borderColor = if (isCritical) NX.red.copy(alpha = 0.6f) else NX.yellow.copy(alpha = 0.6f)
        val iconBg = if (isCritical) NX.red.copy(alpha = 0.15f) else NX.yellow.copy(alpha = 0.15f)
        val iconTint = if (isCritical) NX.red else NX.yellow

        val title = when {
            isExpired -> stringResource(R.string.sub_expired)
            daysLeft == 0L -> stringResource(R.string.sub_banner_today)
            daysLeft == 1L -> stringResource(R.string.sub_banner_1_day)
            daysLeft != null -> stringResource(R.string.sub_banner_days_left, daysLeft)
            else -> stringResource(R.string.sub_expired)
        }

        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(NX.card)
                    .border(1.dp, borderColor, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NX.text,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.sub_banner_subtitle),
                        fontSize = 11.sp,
                        color = NX.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(NX.accent, NX.accentLight)
                            )
                        )
                        .clickable(onClick = onRenew)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.sub_banner_renew_1click),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}
