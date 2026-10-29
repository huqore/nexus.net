package net.nexus.app.ui

import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.nexus.app.MainViewModel
import net.nexus.app.R
import net.nexus.app.data.Provider
import net.nexus.app.data.Server
import kotlin.math.ceil

// ---------------------------------------------------------------- small pieces

@Composable
fun FlagBox(name: String, size: Dp = 48.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(12.dp)).background(NX.iconBtn),
        contentAlignment = Alignment.Center,
    ) { Text(flagFor(name), fontSize = (size.value * 0.5f).sp) }
}

private fun AnnotatedString.Builder.part(text: String, color: Color) =
    withStyle(SpanStyle(color = color)) { append(text) }

/** "VLESS · XHTTP · REALITY · JSON" with the same colour coding as the reference. */
@Composable
fun TagLine(s: Server) {
    val text = buildAnnotatedString {
        part(protoLabel(s.protocol), if (s.protocol == "shadowsocks") NX.green else NX.accent)
        part(" · ", NX.muted)
        part(s.network.uppercase(), NX.muted2)
        if (s.security != "none") {
            part(" · ", NX.muted); part(s.security.uppercase(), NX.accent)
        }
        part(" · ", NX.muted)
        part(s.source, if (s.source == "JSON") NX.yellow else NX.purple)
    }
    Text(text, fontFamily = FontFamily.Monospace, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
fun PingBadge(ms: Int, loading: Boolean) {
    if (loading) {
        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = NX.accent)
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        when {
            ms == -1 -> Text("—", color = NX.muted, fontFamily = FontFamily.Monospace)
            ms == -2 -> Text("n/a", color = NX.muted, fontFamily = FontFamily.Monospace)
            else -> {
                val c = when { ms < 250 -> NX.green; ms < 500 -> NX.yellow; else -> NX.red }
                val animatedColor by animateColorAsState(c, tween(300), label = "pingColor")
                Box(Modifier.size(8.dp).clip(CircleShape).background(animatedColor))
                Spacer(Modifier.width(8.dp))
                Text("${ms}ms", color = NX.muted2, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ServerRow(
    server: Server, selected: Boolean, pinging: Boolean,
    onClick: () -> Unit, onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val rowScale by animateFloatAsState(
        if (isPressed) 0.98f else 1f,
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "serverRowPress"
    )
    val bgColor by animateColorAsState(
        if (selected) NX.accentDim else Color.Transparent,
        tween(250),
        label = "rowBg"
    )
    val borderColor by animateColorAsState(
        if (selected) NX.accent.copy(alpha = 0.45f) else Color.Transparent,
        tween(250),
        label = "rowBorder"
    )

    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = rowScale
                    scaleY = rowScale
                }
                .clip(shape)
                .background(bgColor)
                .border(1.dp, borderColor, shape)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                    onLongClick = { menu = true }
                )
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlagBox(server.name)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(cleanServerName(server.name), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                TagLine(server)
            }
            Spacer(Modifier.width(8.dp))
            PingBadge(server.pingMs, pinging)
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete_server)) },
                onClick = { menu = false; onDelete() },
            )
        }
    }
}

@Composable
fun IconCircleButton(icon: ImageVector, onClick: () -> Unit, spinning: Boolean = false) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        if (isPressed) 0.88f else 1f,
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "iconBtnPress"
    )

    val angle = if (spinning) {
        val t = rememberInfiniteTransition(label = "spin")
        t.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "a").value
    } else 0f
    Box(
        Modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(CircleShape)
            .background(NX.iconBtn)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = NX.muted2, modifier = Modifier.size(22.dp).rotate(angle)) }
}

@Composable
private fun LinkChip(icon: ImageVector, label: String, color: Color, url: String) {
    val uri = LocalUriHandler.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        if (isPressed) 0.94f else 1f,
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "linkChipPress"
    )

    Row(
        Modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(NX.chip)
            .clickable(interactionSource = interactionSource, indication = null) {
                runCatching { uri.openUri(url) }
            }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = color, fontSize = 17.sp, fontWeight = FontWeight.Medium)
    }
}

// ---------------------------------------------------------------- provider card

@Composable
fun ProviderCard(
    p: Provider, selectedId: String?, refreshing: Boolean, pinging: Set<String>,
    onToggle: () -> Unit, onRefresh: () -> Unit, onPingAll: () -> Unit, onDeleteProvider: () -> Unit,
    onSelect: (Server) -> Unit, onDeleteServer: (Server) -> Unit,
) {
    val clip = LocalClipboardManager.current
    val locale = LocalConfiguration.current.locales[0]
    val ru = locale.language == "ru"
    var menu by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(28.dp)

    val arrowAngle by animateFloatAsState(
        targetValue = if (p.expanded) 0f else -90f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "providerArrowAngle"
    )

    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(NX.card)
            .border(1.dp, NX.cardBorder, shape)
            .padding(16.dp),
    ) {
        // header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).clickable(onClick = onToggle), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.KeyboardArrowDown, null, tint = NX.muted2,
                    modifier = Modifier.size(28.dp).rotate(arrowAngle),
                )
                Spacer(Modifier.width(8.dp))
                Text(p.name, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = NX.muted2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconCircleButton(Icons.Filled.FlashOn, onPingAll)
            Spacer(Modifier.width(8.dp))
            Box {
                IconCircleButton(Icons.Filled.MoreHoriz, { menu = true })
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (p.url != null) DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_copy_url)) },
                        onClick = { menu = false; clip.setText(AnnotatedString(p.url)) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_delete), color = NX.red) },
                        onClick = { menu = false; onDeleteProvider() },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = p.expanded,
            enter = expandVertically(
                animationSpec = tween(320, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(280)),
            exit = shrinkVertically(
                animationSpec = tween(260, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(200)),
        ) {
            Column {
                // traffic / expiry arranged in a column (столбиком, одно под другим)
                if (p.total > 0 || p.expireAt > 0 || p.used > 0) {
                    Spacer(Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(NX.chip)
                            .border(1.dp, NX.cardBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Дата окончания подписки
                        if (p.expireAt > 0) {
                            val d = ceil((p.expireAt * 1000 - System.currentTimeMillis()) / 86_400_000.0).toInt().coerceAtLeast(0)
                            val expireDateStr = SimpleDateFormat("dd.MM.yyyy", locale).format(Date(p.expireAt * 1000))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.days_left, d),
                                    color = NX.accentLight,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    expireDateStr,
                                    color = NX.muted2,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // Прогресс бар (если задан лимит общего трафика)
                        if (p.total > 0) {
                            val frac = (p.used.toFloat() / p.total).coerceIn(0f, 1f)
                            val animatedFrac by animateFloatAsState(
                                targetValue = frac,
                                animationSpec = tween(650, easing = FastOutSlowInEasing),
                                label = "trafficFrac"
                            )
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(NX.cardBorder)
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(animatedFrac.coerceAtLeast(0.03f))
                                        .fillMaxHeight()
                                        .background(NX.accent)
                                )
                            }
                        }

                        // 2. Использовано трафика
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (ru) "Использовано" else "Traffic used",
                                color = NX.muted,
                                fontSize = 14.sp
                            )
                            Text(
                                if (p.total > 0) stringResource(R.string.traffic_used, fmtBytes(p.used, ru, locale), fmtBytes(p.total, ru, locale))
                                else stringResource(R.string.traffic_used_unlimited, fmtBytes(p.used, ru, locale)),
                                color = NX.text,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    p.servers.forEach { s ->
                        ServerRow(s, selected = s.id == selectedId, pinging = s.id in pinging,
                            onClick = { onSelect(s) }, onDelete = { onDeleteServer(s) })
                    }
                }
            }
        }
    }
}

/** ProviderCard wired to the view model; used by both the Connect and Servers screens. */
@Composable
fun BoundProviderCard(vm: MainViewModel, p: Provider, selectedId: String?, refreshing: Set<String>, pinging: Set<String>) {
    ProviderCard(
        p = p, selectedId = selectedId, refreshing = p.id in refreshing, pinging = pinging,
        onToggle = { vm.toggleExpanded(p.id) },
        onRefresh = { vm.refresh(p.id) },
        onPingAll = { vm.pingProvider(p.id) },
        onDeleteProvider = { vm.deleteProvider(p.id) },
        onSelect = { vm.select(it.id) },
        onDeleteServer = { vm.deleteServer(it.id) },
    )
}

// ---------------------------------------------------------------- bottom bar

@Composable
fun FloatingNavBar(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    Row(
        modifier.fillMaxWidth().clip(shape).background(NX.sheetBg).border(1.dp, NX.cardBorder, shape)
            .navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        NavItem(Tab.CONNECT, Icons.Filled.Home, R.string.tab_connect, selected, onSelect)
        NavItem(Tab.SERVERS, Icons.Filled.Dns, R.string.tab_servers, selected, onSelect)
        NavItem(Tab.SETTINGS, Icons.Filled.Settings, R.string.tab_settings, selected, onSelect)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavItem(
    tab: Tab, icon: ImageVector, label: Int, selected: Tab, onSelect: (Tab) -> Unit,
) {
    val sel = tab == selected
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        if (isPressed) 0.92f else 1f,
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "navPressScale"
    )
    val iconScale by animateFloatAsState(
        if (sel) 1.15f else 1.0f,
        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "navIconScale"
    )
    val iconColor by animateColorAsState(
        if (sel) NX.accentLight else NX.muted,
        tween(220),
        label = "navIconColor"
    )
    val textColor by animateColorAsState(
        if (sel) NX.accentLight else NX.muted,
        tween(220),
        label = "navTextColor"
    )
    val pillAlpha by animateFloatAsState(
        if (sel) 0.14f else 0.0f,
        tween(220),
        label = "navPillAlpha"
    )

    Column(
        Modifier
            .weight(1f)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(NX.accent.copy(alpha = pillAlpha))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onSelect(tab) }
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon, null,
            tint = iconColor,
            modifier = Modifier
                .size(26.dp)
                .scale(iconScale)
        )
        Text(
            stringResource(label), fontSize = 12.sp,
            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor, maxLines = 1,
        )
    }
}

