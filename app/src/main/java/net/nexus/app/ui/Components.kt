package net.nexus.app.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.unit.TextUnit
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.core.animateDpAsState
import net.nexus.app.data.DesignStyle
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
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.runtime.CompositionLocalProvider
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
import net.nexus.app.data.Store
import kotlin.math.ceil

// ---------------------------------------------------------------- small pieces

fun countryCodeFor(name: String): String {
    val n = name.lowercase()
    if (Regex("""lte\s*#?1\b""", RegexOption.IGNORE_CASE).containsMatchIn(n)) return "DE"
    if (Regex("""lte\s*#?2\b""", RegexOption.IGNORE_CASE).containsMatchIn(n)) return "PL"
    return when {
        n.contains("wl") || n.contains("bypass") -> "WL"
        n.contains("\u0433\u0435\u0440\u043C\u0430\u043D") || n.contains("germany") || name.contains("\uD83C\uDDE9\uD83C\uDDEA") || Regex("""\\bde\\b""").containsMatchIn(n) -> "DE"
        n.contains("\u0444\u0438\u043D\u043B\u044F\u043D\u0434") || n.contains("finland") || name.contains("\uD83C\uDDEB\uD83C\uDDEE") || Regex("""\\bfi\\b""").containsMatchIn(n) -> "FI"
        n.contains("\u043D\u0438\u0434\u0435\u0440\u043B\u0430\u043D\u0434") || n.contains("netherlands") || n.contains("amsterdam") || name.contains("\uD83C\uDDF3\uD83C\uDDF1") || Regex("""\\bnl\\b""").containsMatchIn(n) -> "NL"
        n.contains("\u0448\u0432\u0435\u0446") || n.contains("sweden") || n.contains("stockholm") || name.contains("\uD83C\uDDF8\uD83C\uDDEA") || Regex("""\\bse\\b""").containsMatchIn(n) -> "SE"
        n.contains("\u043F\u043E\u043B\u044C\u0448") || n.contains("poland") || n.contains("warsaw") || name.contains("\uD83C\uDDF5\uD83C\uDDF1") || Regex("""\\bpl\\b""").containsMatchIn(n) -> "PL"
        n.contains("\u0440\u043E\u0441\u0441\u0438\u044F") || n.contains("russia") || name.contains("\uD83C\uDDF7\uD83C\uDDFA") || Regex("""\\bru\\b""").containsMatchIn(n) -> "RU"
        n.contains("usa") || n.contains("united") || name.contains("\uD83C\uDDFA\uD83C\uDDF8") || Regex("""\\bus\\b""").containsMatchIn(n) -> "US"
        else -> ""
    }
}

@Composable
fun CountryFlag(code: String, size: Dp, modifier: Modifier = Modifier) {
    when (code) {
        "DE" -> {
            // Germany: 3 horizontal stripes Black, Red, Gold
            Canvas(modifier = modifier.fillMaxSize()) {
                val h = this.size.height / 3f
                drawRect(Color(0xFF1E1E1E), Offset(0f, 0f), Size(this.size.width, h))
                drawRect(Color(0xFFDD0000), Offset(0f, h), Size(this.size.width, h))
                drawRect(Color(0xFFFFCC00), Offset(0f, h * 2f), Size(this.size.width, h))
            }
        }
        "FI" -> {
            // Finland: White background with Blue Nordic cross
            Canvas(modifier = modifier.fillMaxSize()) {
                drawRect(Color(0xFFFFFFFF), Offset.Zero, this.size)
                val barH = this.size.height * 0.26f
                val barW = this.size.width * 0.22f
                val crossX = this.size.width * 0.36f
                drawRect(Color(0xFF003580), Offset(0f, (this.size.height - barH) / 2f), Size(this.size.width, barH))
                drawRect(Color(0xFF003580), Offset(crossX - barW / 2f, 0f), Size(barW, this.size.height))
            }
        }
        "NL" -> {
            // Netherlands: 3 horizontal stripes Bright Red, White, Cobalt Blue
            Canvas(modifier = modifier.fillMaxSize()) {
                val h = this.size.height / 3f
                drawRect(Color(0xFFC8102E), Offset(0f, 0f), Size(this.size.width, h))
                drawRect(Color(0xFFFFFFFF), Offset(0f, h), Size(this.size.width, h))
                drawRect(Color(0xFF003DA5), Offset(0f, h * 2f), Size(this.size.width, h))
            }
        }
        "SE" -> {
            // Sweden: Blue background with Yellow Nordic cross
            Canvas(modifier = modifier.fillMaxSize()) {
                drawRect(Color(0xFF006AA7), Offset.Zero, this.size)
                val barH = this.size.height * 0.26f
                val barW = this.size.width * 0.22f
                val crossX = this.size.width * 0.36f
                drawRect(Color(0xFFFECC00), Offset(0f, (this.size.height - barH) / 2f), Size(this.size.width, barH))
                drawRect(Color(0xFFFECC00), Offset(crossX - barW / 2f, 0f), Size(barW, this.size.height))
            }
        }
        "PL" -> {
            // Poland: 2 horizontal stripes White, Crimson Red
            Canvas(modifier = modifier.fillMaxSize()) {
                val h = this.size.height / 2f
                drawRect(Color(0xFFFFFFFF), Offset(0f, 0f), Size(this.size.width, h))
                drawRect(Color(0xFFDC143C), Offset(0f, h), Size(this.size.width, h))
            }
        }
        "RU" -> {
            // Russia: 3 horizontal stripes White, Blue, Red
            Canvas(modifier = modifier.fillMaxSize()) {
                val h = this.size.height / 3f
                drawRect(Color(0xFFFFFFFF), Offset(0f, 0f), Size(this.size.width, h))
                drawRect(Color(0xFF0039A6), Offset(0f, h), Size(this.size.width, h))
                drawRect(Color(0xFFD52B1E), Offset(0f, h * 2f), Size(this.size.width, h))
            }
        }
        "US" -> {
            // USA
            Canvas(modifier = modifier.fillMaxSize()) {
                val stripeH = this.size.height / 7f
                for (i in 0 until 7) {
                    drawRect(if (i % 2 == 0) Color(0xFFB22234) else Color.White, Offset(0f, i * stripeH), Size(this.size.width, stripeH))
                }
                drawRect(Color(0xFF3C3B6E), Offset.Zero, Size(this.size.width * 0.45f, stripeH * 4f))
            }
        }
        "WL" -> {
            // Whitelist Bypass (Shield vector icon styled to theme)
            Box(
                modifier = modifier.fillMaxSize().background(NX.accentDim.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Shield,
                    contentDescription = null,
                    tint = NX.accentLight,
                    modifier = Modifier.size(size * 0.56f)
                )
            }
        }
        else -> {
            // Generic server icon
            Box(
                modifier = modifier.fillMaxSize().background(NX.iconBtn),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Dns,
                    contentDescription = null,
                    tint = NX.muted2,
                    modifier = Modifier.size(size * 0.52f)
                )
            }
        }
    }
}

@Composable
fun FlagBox(name: String, size: Dp = 42.dp) {
    val code = countryCodeFor(name)
    val isWl = code == "WL"
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .size(size)
            .clip(shape)
            .border(1.dp, if (isWl) NX.accentLight.copy(alpha = 0.7f) else NX.cardBorder.copy(alpha = 0.65f), shape),
        contentAlignment = Alignment.Center,
    ) {
        CountryFlag(code = code, size = size)
    }
}

private fun AnnotatedString.Builder.part(text: String, color: Color) =
    withStyle(SpanStyle(color = color)) { append(text) }

/** "VLESS · XHTTP · REALITY · JSON" with the same colour coding as the reference. */
@Composable
fun TagLine(s: Server) {
    val text = buildAnnotatedString {
        part(protoLabel(s.protocol), if (s.protocol == "shadowsocks") NX.green else NX.accent)
        part(" - ", NX.muted)
        part(s.network.uppercase(), NX.muted2)
        if (s.security != "none") {
            part(" - ", NX.muted); part(s.security.uppercase(), NX.accent)
        }
        part(" - ", NX.muted)
        part(s.source, if (s.source == "JSON") NX.yellow else NX.purple)
    }
    Text(text, fontFamily = FontFamily.Monospace, fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
}

@Composable
fun PingBadge(ms: Int, loading: Boolean) {
    if (loading) {
        CircularProgressIndicator(Modifier.size(13.dp), strokeWidth = 2.dp, color = NX.accent)
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        when {
            ms == -1 -> Text("—", color = NX.muted, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            ms == -2 -> Text("n/a", color = NX.muted, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            else -> {
                val c = when { ms < 250 -> NX.green; ms < 500 -> NX.yellow; else -> NX.red }
                val animatedColor by animateColorAsState(c, tween(300), label = "pingColor")
                Box(Modifier.size(6.dp).clip(CircleShape).background(animatedColor))
                Spacer(Modifier.width(5.dp))
                Text("${ms}ms", color = NX.muted2, fontFamily = FontFamily.Monospace, fontSize = 13.sp, maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
fun ServerRow(
    server: Server, selected: Boolean, pinging: Boolean,
    onClick: () -> Unit, onDelete: () -> Unit,
) {
    val shape = nxCardShape(18.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val rowScale by animateFloatAsState(
        if (isPressed) 0.98f else 1f,
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "serverRowPress"
    )
    val bgColor by animateColorAsState(
        if (selected) NX.selectedFill else Color.Transparent,
        tween(250),
        label = "rowBg"
    )
    val borderColor by animateColorAsState(
        if (selected) NX.selectedBorder else Color.Transparent,
        tween(250),
        label = "rowBorder"
    )

    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = rowScale
                scaleY = rowScale
            }
            .then(
                if (LocalSegmentedList.current && NX.style == DesignStyle.MATERIAL3) Modifier.m3Segment(selected)
                else Modifier.clip(shape).background(bgColor).border(1.dp, borderColor, shape)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = if (NX.style == DesignStyle.MATERIAL3) androidx.compose.material3.ripple() else null,
                onClick = onClick,
            )
            // M3 one-line list item with a leading avatar: 56dp minimum.
            .heightIn(min = 0.dp)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FlagBox(server.name, size = 36.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            cleanServerName(server.name),
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(6.dp))
        PingBadge(server.pingMs, pinging)
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
        Text(label, color = color, fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
    }
}

// ---------------------------------------------------------------- provider card

@Composable
fun ProviderCard(
    p: Provider, selectedId: String?, refreshing: Boolean, pinging: Set<String>,
    onToggle: () -> Unit, onRefresh: () -> Unit, onPingAll: () -> Unit, onDeleteProvider: () -> Unit,
    onSelect: (Server) -> Unit, onDeleteServer: (Server) -> Unit,
) {
    val shape = nxCardShape(28.dp)

    val (wlServers, regularServers) = remember(p.servers) {
        p.servers.partition { Store.isWlBypass(it) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (regularServers.isNotEmpty()) {
            ServerGroup(Icons.Filled.Wifi, R.string.wifi_category, regularServers, selectedId, pinging, onSelect, shape)
        }
        if (wlServers.isNotEmpty()) {
            ServerGroup(Icons.Filled.Shield, R.string.wl_bypass_category, wlServers, selectedId, pinging, onSelect, shape)
        }
    }
}

/**
 * One server section (Wi-Fi / WL bypass).
 * M3 Expressive: header above, rows as a segmented list (selected row morphs, primaryContainer).
 * Fluent / Liquid Glass: the classic card with header and divider.
 */
@Composable
private fun ServerGroup(
    icon: ImageVector, titleRes: Int, servers: List<Server>, selectedId: String?, pinging: Set<String>,
    onSelect: (Server) -> Unit, shape: androidx.compose.ui.graphics.Shape,
) {    if (NX.style == DesignStyle.MATERIAL3) {
        // Material 3 Expressive: header above, servers as a segmented list (selected row morphs).
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(icon, null, tint = NX.md3.primary, modifier = Modifier.size(18.dp))
                Text(stringResource(titleRes), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NX.md3.primary)
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                CompositionLocalProvider(LocalSegmentedList provides true) {
                    servers.forEach { s ->
                        ServerRow(server = s, selected = s.id == selectedId, pinging = s.id in pinging, onClick = { onSelect(s) }, onDelete = { })
                    }
                }
            }
        }
        return
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(NX.card)
            .nxBorder(shape)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = NX.accent, modifier = Modifier.size(16.dp))
            Text(text = stringResource(titleRes), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NX.muted2, letterSpacing = 0.5.sp)
        }
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).height(1.dp)
                .background(NX.cardBorder.copy(alpha = 0.6f))
        )
        servers.forEach { s ->
            ServerRow(server = s, selected = s.id == selectedId, pinging = s.id in pinging, onClick = { onSelect(s) }, onDelete = { })
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

private val NavTabs = listOf(Tab.CONNECT, Tab.SERVERS, Tab.SETTINGS)

/**
 * Bottom navigation with a single sliding selection indicator.
 * Tap selects a tab; press-and-hold then drag makes the indicator follow the finger,
 * switching tabs as it passes over them ([onSlideTick] fires on each change) and
 * snapping to the nearest tab on release. Works in all three design styles.
 */
@Composable
fun FloatingNavBar(
    selected: Tab,
    onSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier,
    onSlideTick: () -> Unit = {},
) {
    val style = NX.style
    val m3 = style == DesignStyle.MATERIAL3
    val glass = style == DesignStyle.LIQUID_GLASS

    var dragging by remember { mutableStateOf(false) }
    var dragPos by remember { mutableFloatStateOf(0f) }
    val selectedIndex = NavTabs.indexOf(selected).coerceAtLeast(0)
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentTick by rememberUpdatedState(onSlideTick)
    val indicatorPos by animateFloatAsState(
        targetValue = if (dragging) dragPos else selectedIndex.toFloat(),
        animationSpec = if (dragging) spring(dampingRatio = 1f, stiffness = Spring.StiffnessHigh)
        else spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "navIndicatorPos",
    )
    val liftScale by animateFloatAsState(if (dragging) 1.06f else 1f, spring(dampingRatio = 0.6f), label = "navLift")

    val padH = if (glass) 8.dp else 12.dp
    val padV = when { glass -> 7.dp; else -> 10.dp }
    val light = NX.isLightTheme
    val pillColor = when {
        m3 -> NX.md3.secondaryContainer
        glass -> Color.White.copy(alpha = if (light) 0.49f else if (dragging) 0.22f else 0.154f)
        else -> NX.accent.copy(alpha = if (dragging) 0.20f else 0.14f)
    }
    val glassRim = Color.White.copy(alpha = if (light) 0.9f else 0.28f)

    val rowModifier = Modifier
        .fillMaxWidth()
        .pointerInput(Unit) {
            val padPx = padH.toPx()
            fun follow(x: Float) {
                val itemW = (size.width - 2 * padPx) / NavTabs.size
                dragPos = ((x - padPx) / itemW - 0.5f).coerceIn(0f, (NavTabs.size - 1).toFloat())
                val tab = NavTabs[kotlin.math.round(dragPos).toInt()]
                if (tab != currentSelected) {
                    currentOnSelect(tab)
                    currentTick()
                }
            }
            detectDragGesturesAfterLongPress(
                onDragStart = { o -> dragging = true; currentTick(); follow(o.x) },
                onDrag = { change, _ -> change.consume(); follow(change.position.x) },
                onDragEnd = { dragging = false },
                onDragCancel = { dragging = false },
            )
        }
        .drawBehind {
            val padPx = padH.toPx()
            val itemW = (size.width - 2 * padPx) / NavTabs.size
            val grow = (liftScale - 1f) * itemW / 2
            val tl = Offset(padPx + indicatorPos * itemW - grow, padV.toPx() - grow / 3)
            val sz = Size(itemW + 2 * grow, size.height - 2 * padV.toPx() + 2 * grow / 3)
            val r = when { glass -> 24.dp.toPx(); m3 -> sz.height / 2; else -> 16.dp.toPx() }
            drawRoundRect(pillColor, topLeft = tl, size = sz, cornerRadius = CornerRadius(r, r))
            if (glass) {
                drawRoundRect(
                    glassRim, topLeft = tl, size = sz, cornerRadius = CornerRadius(r, r),
                    style = Stroke(width = 0.8.dp.toPx()),
                )
            }
        }
        .padding(horizontal = padH, vertical = padV)

    // Highlight follows the indicator while sliding, the real selection otherwise.
    val highlighted = if (dragging) NavTabs[kotlin.math.round(dragPos).toInt().coerceIn(0, NavTabs.size - 1)] else selected

    val items: @Composable RowScope.() -> Unit = {
        NavItem(Tab.CONNECT, Icons.Filled.Home, R.string.tab_connect, highlighted, onSelect)
        NavItem(Tab.SERVERS, Icons.Filled.Dns, R.string.tab_servers, highlighted, onSelect)
        NavItem(Tab.SETTINGS, Icons.Filled.Settings, R.string.tab_settings, highlighted, onSelect)
    }

    if (glass) {
        // iOS-style floating glass capsule.
        val capsule = RoundedCornerShape(32.dp)
        Box(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .shadow(18.dp, capsule, ambientColor = Color.Black.copy(alpha = 0.5f), spotColor = Color.Black.copy(alpha = 0.5f))
                    .clip(capsule)
                    .glassBlur()
                    .background(NX.card)
                    .nxBorder(capsule)
            )
            Row(modifier = rowModifier, horizontalArrangement = Arrangement.SpaceAround, content = items)
        }
        return
    }
    if (m3) {
        // Material You Expressive dock: floating tonal pill with an outline and soft elevation;
        // the active indicator is a fully rounded pill that slides with the finger.
        val dock = RoundedCornerShape(28.dp)
        Box(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .shadow(10.dp, dock, ambientColor = Color.Black.copy(alpha = 0.45f), spotColor = Color.Black.copy(alpha = 0.45f))
                    .clip(dock)
                    .background(NX.md3.surfaceContainerHigh)
                    .border(1.dp, NX.md3.outlineVariant, dock)
            )
            Row(modifier = rowModifier, horizontalArrangement = Arrangement.SpaceAround, content = items)
        }
        return
    }
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(if (m3) NX.md3.surfaceContainer else NX.bg)
                .then(if (m3) Modifier else Modifier.background(NX.sheetBg))
                // M3 navigation bar has no outline (tonal container only).
                .then(
                    if (m3) Modifier.border(1.dp, NX.md3.outlineVariant, shape)
                    else Modifier.nxBorder(shape, color = NX.cardBorder.copy(alpha = 0.85f))
                )
        )
        Box(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Row(
                // M3 navigation bar: 80dp container.
                modifier = rowModifier,
                horizontalArrangement = Arrangement.SpaceAround,
                content = items,
            )
        }
    }
}

@Composable
private fun RowScope.NavItem(
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
    val m3 = NX.style == DesignStyle.MATERIAL3
    val iconColor by animateColorAsState(
        when {
            m3 && sel -> NX.md3.onSecondaryContainer
            m3 -> NX.md3.onSurfaceVariant
            sel -> NX.accentLight
            else -> NX.muted
        },
        tween(220),
        label = "navIconColor"
    )
    val textColor by animateColorAsState(
        when {
            m3 && sel -> NX.md3.onSurface
            m3 -> NX.md3.onSurfaceVariant
            sel -> NX.accentLight
            else -> NX.muted
        },
        tween(220),
        label = "navTextColor"
    )

    Column(
        Modifier
            .weight(1f)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onSelect(tab) }
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The selection pill is drawn by the bar (one sliding indicator), not per item.
        Box(Modifier.height(30.dp), contentAlignment = Alignment.Center) {
            Icon(
                icon, null,
                tint = iconColor,
                modifier = Modifier
                    .size(26.dp)
                    .scale(iconScale)
            )
        }
        Text(
            stringResource(label),
            // M3: labelMedium (12sp, medium weight, 0.5sp tracking).
            fontSize = 12.sp,
            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor, maxLines = 1, softWrap = false,
        )
    }
}

