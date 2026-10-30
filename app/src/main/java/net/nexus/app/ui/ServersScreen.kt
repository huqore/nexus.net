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
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import net.nexus.app.MainViewModel
import net.nexus.app.R
import net.nexus.app.data.Server
import net.nexus.app.data.Store

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

        // Top Controls: «Автовыбор» (50%) and «Тест пинга» (50%)
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

        // Clean vertical list of provider cards with servers
        if (providers.isEmpty()) {
            item {
                Spacer(Modifier.height(30.dp))
                Text(
                    stringResource(R.string.empty_servers), color = NX.muted, fontSize = 18.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }
        } else {
            items(providers, key = { it.id }) { p ->
                Box(Modifier.animateItem()) {
                    BoundProviderCard(vm, p, settings.selectedServerId, refreshing, pinging)
                }
            }
        }
    }

    if (showAdd) {
        AddDialog(
            adding = adding,
            onSubmit = { submitted = true; vm.addInput(it) },
            onDismiss = { if (!adding) showAdd = false },
        )
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
            .height(66.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) NX.accent.copy(alpha = 0.25f) else NX.iconBtn),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Bolt, null, tint = if (selected) Color.White else NX.accent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.globe_auto_select),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = NX.text,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            val sub = if (bestServer != null) cleanServerName(bestServer.name) else "AUTO"
            Text(
                sub,
                fontSize = 10.sp,
                color = NX.muted,
                maxLines = 1,
                softWrap = false,
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
            .height(66.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(NX.card)
            .border(1.dp, NX.cardBorder, shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(NX.iconBtn),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Refresh,
                null,
                tint = NX.accent,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer { if (isPinging) rotationZ = rotation }
            )
        }
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.globe_ping_test),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = NX.text,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (isPinging) stringResource(R.string.globe_pinging) else stringResource(R.string.globe_all_servers_short),
                fontSize = 10.sp,
                color = NX.muted,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
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
