package net.nexus.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import net.nexus.app.MainViewModel
import net.nexus.app.R
import net.nexus.app.data.Server
import net.nexus.app.data.Store

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(vm: MainViewModel) {
    val providers by vm.providers.collectAsState()
    val settings by vm.settings.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val pinging by vm.pinging.collectAsState()
    val adding by vm.adding.collectAsState()

    var showAdd by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    LaunchedEffect(adding) {
        if (submitted && !adding) { showAdd = false; submitted = false }
    }

    val addInteraction = remember { MutableInteractionSource() }
    val addPressed by addInteraction.collectIsPressedAsState()
    val addScale by animateFloatAsState(
        if (addPressed) 0.88f else 1f,
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "addBtnScale"
    )

    val ctx = LocalContext.current
    val bestServer = remember(providers) { Store(ctx).findBestServer() }
    val allServers = remember(providers) { providers.flatMap { it.servers } }

    var showBottomSheet by remember { mutableStateOf(false) }
    var targetLocation by remember { mutableStateOf<Pair<Float, Float>?>(null) }

    val globeNodes = remember(allServers, settings.selectedServerId, bestServer) {
        buildGlobeNodes(allServers, settings.selectedServerId, bestServer)
    }

    // Determine current selected server instance
    val currentSelectedServer = remember(allServers, settings.selectedServerId, bestServer) {
        if (settings.selectedServerId == Store.AUTO_SERVER_ID || settings.selectedServerId == null) {
            bestServer
        } else {
            allServers.firstOrNull { it.id == settings.selectedServerId }
        }
    }

    if (!settings.use3dGlobe) {
        // ================= CLASSIC MINIMAL 1-COLUMN LIST MODE =================
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.servers_title), fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Box(
                        Modifier
                            .size(52.dp)
                            .graphicsLayer {
                                scaleX = addScale
                                scaleY = addScale
                            }
                            .clip(RoundedCornerShape(18.dp))
                            .background(NX.accent)
                            .clickable(interactionSource = addInteraction, indication = null) { showAdd = true },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
                }
            }
            if (providers.isNotEmpty()) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AutoServerButton(
                            modifier = Modifier.weight(1f),
                            selected = settings.selectedServerId == Store.AUTO_SERVER_ID,
                            bestServer = bestServer,
                            onClick = { vm.select(Store.AUTO_SERVER_ID) }
                        )
                        PingTestButton(
                            modifier = Modifier.weight(1f),
                            isPinging = pinging.isNotEmpty(),
                            onClick = {
                                HapticManager.trigger(ctx, settings.hapticIntensity, HapticType.CLICK)
                                vm.pingServers(allServers)
                            }
                        )
                    }
                }
            }
            if (providers.isEmpty()) {
                item {
                    Spacer(Modifier.height(30.dp))
                    Text(
                        stringResource(R.string.empty_servers), color = NX.muted, fontSize = 18.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                }
            }
            items(providers, key = { it.id }) { p ->
                Box(Modifier.animateItem()) {
                    BoundProviderCard(vm, p, settings.selectedServerId, refreshing, pinging)
                }
            }
        }
    } else {
        // ================= INTERACTIVE 3D GLOBE VIEW MODE =================
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.servers_title), fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Box(
                        Modifier
                            .size(52.dp)
                            .graphicsLayer {
                                scaleX = addScale
                                scaleY = addScale
                            }
                            .clip(RoundedCornerShape(18.dp))
                            .background(NX.accent)
                            .clickable(interactionSource = addInteraction, indication = null) { showAdd = true },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
                }
            }

            // 1. «Автовыбор» card (left 50%) and «Проверка пинга» (right 50%)
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AutoServerButton(
                        modifier = Modifier.weight(1f),
                        selected = settings.selectedServerId == Store.AUTO_SERVER_ID,
                        bestServer = bestServer,
                        onClick = {
                            vm.select(Store.AUTO_SERVER_ID)
                            bestServer?.let { bs ->
                                targetLocation = getLocationForServer(bs)
                            }
                        }
                    )

                    PingTestButton(
                        modifier = Modifier.weight(1f),
                        isPinging = pinging.isNotEmpty(),
                        onClick = {
                            HapticManager.trigger(ctx, settings.hapticIntensity, HapticType.CLICK)
                            vm.pingServers(allServers)
                        }
                    )
                }
            }

            // 2. Interactive 3D Globe with fluid touch, zoom and server location nodes
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(NX.card.copy(alpha = 0.35f))
                        .border(1.dp, NX.cardBorder.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    NexusGlobe3D(
                        modifier = Modifier.fillMaxSize(),
                        nodes = globeNodes,
                        targetLocation = targetLocation,
                        onNodeClick = { node ->
                            val s = if (node.isWlBypass) {
                                // Auto-select lowest ping WL Bypass server
                                node.servers.filter { it.pingMs > 0 }.minByOrNull { it.pingMs } ?: node.servers.firstOrNull()
                            } else {
                                node.bestServer ?: node.servers.firstOrNull()
                            }
                            if (s != null) {
                                vm.select(s.id)
                            }
                            targetLocation = Pair(node.lat, node.lon)
                        }
                    )
                }
            }

            // 3. Selected Server Card below globe
            item {
                SelectedServerCard(
                    selected = currentSelectedServer,
                    isAutoMode = settings.selectedServerId == Store.AUTO_SERVER_ID || settings.selectedServerId == null,
                    allServersCount = allServers.size,
                    onOpenAll = { showBottomSheet = true }
                )
            }
        }
    }

    // Modal Bottom Sheet showing full list of servers with search
    if (showBottomSheet) {
        AllServersBottomSheet(
            servers = allServers,
            selectedServerId = settings.selectedServerId,
            pinging = pinging,
            onSelect = { s ->
                vm.select(s.id)
                targetLocation = getLocationForServer(s)
                showBottomSheet = false
            },
            onDelete = { s -> vm.deleteServer(s.id) },
            onDismiss = { showBottomSheet = false }
        )
    }

    // Manual input dialog (+)
    if (showAdd) {
        AddDialog(
            adding = adding,
            onSubmit = { submitted = true; vm.addInput(it) },
            onDismiss = { if (!adding) showAdd = false },
        )
    }
}

/** Calculates target camera latitude and longitude for a server */
private fun getLocationForServer(server: Server): Pair<Float, Float> {
    val isWl = Store.isWlBypass(server)
    val n = server.name.lowercase()
    return when {
        isWl || n.contains("bypass") || n.contains("wl") || n.contains("\u0440\u043E\u0441\u0441\u0438\u044F") || n.contains("russia") ->
            Pair(GlobeCoordinates.RUSSIA_WL_LAT, GlobeCoordinates.RUSSIA_WL_LON)
        n.contains("\u0433\u0435\u0440\u043C\u0430\u043D") || n.contains("germany") || server.name.contains("\uD83C\uDDE9\uD83C\uDDEA") || Regex("""\\bde\\b""").containsMatchIn(n) ->
            Pair(GlobeCoordinates.GERMANY_LAT, GlobeCoordinates.GERMANY_LON)
        n.contains("\u0444\u0438\u043D\u043B\u044F\u043D\u0434") || n.contains("finland") || server.name.contains("\uD83C\uDDEB\uD83C\uDDEE") || Regex("""\\bfi\\b""").containsMatchIn(n) ->
            Pair(GlobeCoordinates.FINLAND_LAT, GlobeCoordinates.FINLAND_LON)
        n.contains("\u043D\u0438\u0434\u0435\u0440\u043B\u0430\u043D\u0434") || n.contains("netherlands") || n.contains("amsterdam") || server.name.contains("\uD83C\uDDF3\uD83C\uDDF1") || Regex("""\\bnl\\b""").containsMatchIn(n) ->
            Pair(GlobeCoordinates.NETHERLANDS_LAT, GlobeCoordinates.NETHERLANDS_LON)
        n.contains("\u0448\u0432\u0435\u0446") || n.contains("sweden") || n.contains("stockholm") || server.name.contains("\uD83C\uDDF8\uD83C\uDDEA") || Regex("""\\bse\\b""").containsMatchIn(n) ->
            Pair(GlobeCoordinates.SWEDEN_LAT, GlobeCoordinates.SWEDEN_LON)
        n.contains("\u043F\u043E\u043B\u044C\u0448") || n.contains("poland") || n.contains("warsaw") || server.name.contains("\uD83C\uDDF5\uD83C\uDDF1") || Regex("""\\bpl\\b""").containsMatchIn(n) ->
            Pair(GlobeCoordinates.POLAND_LAT, GlobeCoordinates.POLAND_LON)
        else ->
            Pair(GlobeCoordinates.GERMANY_LAT, GlobeCoordinates.GERMANY_LON)
    }
}

@Composable
private fun AutoServerButton(
    modifier: Modifier = Modifier,
    selected: Boolean,
    bestServer: Server?,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.98f else 1f,
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "autoBtnPress"
    )
    val bgColor by animateColorAsState(
        if (selected) NX.accentDim else NX.card,
        tween(250),
        label = "autoBtnBg"
    )
    val borderColor by animateColorAsState(
        if (selected) NX.accent.copy(alpha = 0.6f) else NX.cardBorder,
        tween(250),
        label = "autoBtnBorder"
    )

    Row(
        modifier
            .height(68.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) NX.accent.copy(alpha = 0.25f) else NX.iconBtn),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Bolt, null, tint = if (selected) Color.White else NX.accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.globe_auto_select),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = NX.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val sub = if (bestServer != null) cleanServerName(bestServer.name) else "AUTO"
            Text(
                sub,
                fontSize = 11.sp,
                color = NX.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PingTestButton(
    modifier: Modifier = Modifier,
    isPinging: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.98f else 1f,
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "pingBtnPress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pingRotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pingSpin"
    )

    Row(
        modifier
            .height(68.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(NX.card)
            .border(1.dp, NX.cardBorder, shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(NX.iconBtn),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Refresh,
                null,
                tint = NX.accent,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer { if (isPinging) rotationZ = rotation }
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.globe_ping_test),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = NX.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (isPinging) stringResource(R.string.globe_pinging) else stringResource(R.string.globe_all_servers_short),
                fontSize = 11.sp,
                color = NX.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SelectedServerCard(
    selected: Server?,
    isAutoMode: Boolean,
    allServersCount: Int,
    onOpenAll: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.985f else 1f,
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "selectedCardPress"
    )

    val isWl = selected != null && Store.isWlBypass(selected)
    val borderCol = if (isWl) NX.accentLight.copy(alpha = 0.85f) else NX.cardBorder
    val borderWidth = if (isWl) 1.5.dp else 1.dp

    Column(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(NX.card)
            .border(borderWidth, borderCol, shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onOpenAll)
            .padding(18.dp)
    ) {
        // Tag & Status Row
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.globe_selected_server),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isWl) NX.accentLight else NX.muted,
                letterSpacing = 1.sp
            )
            PingBadge(selected?.pingMs ?: -1, loading = false)
        }

        Spacer(Modifier.height(12.dp))

        // Main Server Info
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isWl) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(NX.accentDim.copy(alpha = 0.6f))
                        .border(1.dp, NX.accentLight.copy(alpha = 0.8f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Shield, contentDescription = null, tint = NX.accentLight, modifier = Modifier.size(28.dp))
                }
            } else if (selected != null) {
                FlagBox(selected.name, size = 52.dp)
            } else {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(NX.iconBtn),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Bolt, contentDescription = null, tint = NX.accent, modifier = Modifier.size(28.dp))
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                val title = if (selected != null) {
                    cleanServerName(selected.name)
                } else {
                    stringResource(R.string.auto_server_title)
                }
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                if (isWl) {
                    Text(
                        stringResource(R.string.globe_wl_badge),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NX.accentLight
                    )
                } else if (selected != null) {
                    TagLine(selected)
                } else {
                    Text(
                        stringResource(R.string.auto_server_desc),
                        fontSize = 13.sp,
                        color = NX.muted
                    )
                }
            }
        }

        // Whitelist Bypass special explanation note (Theme matched, no yellow)
        if (isWl) {
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NX.accentDim.copy(alpha = 0.35f))
                    .border(1.dp, NX.accent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    stringResource(R.string.globe_wl_desc),
                    fontSize = 11.sp,
                    color = NX.text,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Open All Servers Button
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(NX.iconBtn)
                .clickable { onOpenAll() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.globe_all_servers_btn),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NX.text
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "($allServersCount)",
                    fontSize = 13.sp,
                    color = NX.muted
                )
            }
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = NX.muted2,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllServersBottomSheet(
    servers: List<Server>,
    selectedServerId: String?,
    pinging: Set<String>,
    onSelect: (Server) -> Unit,
    onDelete: (Server) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }

    val filteredServers = remember(servers, searchQuery) {
        servers.filter { s ->
            if (searchQuery.isBlank()) true else {
                s.name.contains(searchQuery, ignoreCase = true) ||
                s.protocol.contains(searchQuery, ignoreCase = true) ||
                s.network.contains(searchQuery, ignoreCase = true) ||
                s.security.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NX.sheetBg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = NX.muted.copy(alpha = 0.4f)) }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 32.dp)
        ) {
            // Sheet Title
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.globe_all_servers_btn),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "${filteredServers.size} / ${servers.size}",
                    fontSize = 13.sp,
                    color = NX.muted
                )
            }

            Spacer(Modifier.height(12.dp))

            // Search Bar (Direct search without category tabs)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        stringResource(R.string.globe_search_hint),
                        fontSize = 14.sp,
                        color = NX.muted
                    )
                },
                leadingIcon = {
                    Icon(Icons.Filled.Search, null, tint = NX.muted, modifier = Modifier.size(20.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, null, tint = NX.muted, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NX.accent,
                    unfocusedBorderColor = NX.cardBorder,
                    focusedContainerColor = NX.card,
                    unfocusedContainerColor = NX.card,
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))

            // Server Rows List
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .height(390.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filteredServers.isEmpty()) {
                    item {
                        Spacer(Modifier.height(40.dp))
                        Text(
                            stringResource(R.string.empty_servers),
                            color = NX.muted,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                items(filteredServers, key = { it.id }) { s ->
                    ServerRow(
                        server = s,
                        selected = s.id == selectedServerId,
                        pinging = s.id in pinging,
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                onSelect(s)
                            }
                        },
                        onDelete = { onDelete(s) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddDialog(adding: Boolean, onSubmit: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val clip = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NX.card,
        title = { Text(stringResource(R.string.add_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.add_hint), color = NX.muted, fontSize = 14.sp) },
                    minLines = 4, maxLines = 8, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { clip.getText()?.text?.let { text = it } }) {
                        Icon(Icons.Filled.ContentPaste, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.add_paste))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank() && !adding, onClick = { onSubmit(text) }) {
                Text(stringResource(if (adding) R.string.add_loading else R.string.add_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
