package net.nexus.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.nexus.app.BuildConfig
import net.nexus.app.MainViewModel
import net.nexus.app.R
import net.nexus.app.update.AppReleaseInfo
import net.nexus.app.update.UpdateInstaller
import net.nexus.app.update.UpdateState
import java.io.File
import java.util.Locale

@Composable
fun UpdateDialog(
    state: UpdateState,
    vm: MainViewModel,
    onConnect: () -> Unit = {},
) {
    when (state) {
        is UpdateState.Available -> {
            if (state.autoPrompt && net.nexus.app.update.UpdateChecker.isEmergency(state.release.versionName, BuildConfig.VERSION_NAME)) {
                EmergencyUpdateDialog(release = state.release, onConnect = onConnect, onLater = { vm.dismissUpdateDialog() })
            } else if (state.autoPrompt) {
                AvailableDialog(
                    release = state.release,
                    onUpdate = { vm.startDownloadUpdate(state.release) },
                    onDismiss = { vm.dismissUpdateDialog() }
                )
            }
        }
        is UpdateState.Downloading -> {
            DownloadingDialog(
                release = state.release,
                progress = state.progress,
                bytesDownloaded = state.bytesDownloaded,
                totalBytes = state.totalBytes,
                isPatch = state.isPatch,
                assembling = state.assembling,
                onCancel = { vm.cancelUpdateDownload() }
            )
        }
        is UpdateState.ReadyToInstall -> {
            ReadyDialog(
                release = state.release,
                apkFile = state.apkFile,
                onInstall = { vm.installUpdate(state.apkFile) },
                onDismiss = { vm.dismissUpdateDialog() }
            )
        }
        else -> Unit
    }
}

@Composable
private fun AvailableDialog(
    release: AppReleaseInfo,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
    var hasInstallPermission by remember { mutableStateOf(UpdateInstaller.canInstallApk(ctx)) }
    var askedPermission by remember { mutableStateOf(false) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val granted = UpdateInstaller.canInstallApk(ctx)
                hasInstallPermission = granted
                if (granted && askedPermission) {
                    askedPermission = false
                    onUpdate()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val shape = RoundedCornerShape(28.dp)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .nxDialogSurface(shape)
                .padding(22.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(NX.accentDim),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SystemUpdate,
                            contentDescription = null,
                            tint = NX.accentLight,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.update_dialog_title),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = NX.text
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                fontSize = 13.sp,
                                color = NX.muted,
                                fontFamily = FontFamily.Monospace
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = NX.accentLight,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "v${release.versionName}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NX.accentLight,
                                fontFamily = FontFamily.Monospace
                            )
                            if (release.downloadSize > 0) {
                                Text(
                                    text = if (release.patch != null)
                                        "• " + stringResource(R.string.update_patch_size, formatMb(release.downloadSize), formatMb(release.apkSize))
                                    else "• ${formatMb(release.downloadSize)}",
                                    fontSize = 12.sp,
                                    color = NX.muted
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text = stringResource(R.string.update_changelog_title),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = NX.muted2
                )

                Spacer(Modifier.height(8.dp))

                val changelogText = release.changelog.ifBlank {
                    stringResource(R.string.update_no_changelog)
                }
                val scrollState = rememberScrollState()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp, max = 160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(NX.bg)
                        .nxBorder(RoundedCornerShape(16.dp))
                        .padding(14.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = changelogText,
                        fontSize = 13.sp,
                        color = NX.text,
                        lineHeight = 18.sp
                    )
                }

                if (!hasInstallPermission) {
                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(NX.yellow.copy(alpha = 0.12f))
                            .border(1.dp, NX.yellow.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = NX.yellow,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.update_permission_required),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NX.text
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.update_permission_pre_hint),
                                    fontSize = 12.sp,
                                    color = NX.muted2,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = NX.muted2)
                    ) {
                        Text(stringResource(R.string.update_btn_later))
                    }

                    Spacer(Modifier.width(8.dp))

                    if (!hasInstallPermission) {
                        Button(
                            onClick = {
                                askedPermission = true
                                UpdateInstaller.openInstallPermissionSettings(ctx)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NX.yellow,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.update_btn_grant_permission), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onUpdate,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NX.accent,
                                contentColor = NX.onAccent
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.update_btn_update), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadingDialog(
    release: AppReleaseInfo,
    progress: Float,
    bytesDownloaded: Long,
    totalBytes: Long,
    isPatch: Boolean = false,
    assembling: Boolean = false,
    onCancel: () -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .nxDialogSurface(shape)
                .padding(22.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(NX.accentDim),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            tint = NX.accentLight,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(
                                when {
                                    assembling -> R.string.update_assembling
                                    isPatch -> R.string.update_downloading_patch
                                    else -> R.string.update_downloading
                                }
                            ),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = NX.text
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "v${release.versionName}",
                            fontSize = 13.sp,
                            color = NX.muted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                if (progress >= 0f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = NX.accent,
                        trackColor = NX.cardBorder,
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = NX.accent,
                        trackColor = NX.cardBorder,
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val percent = if (progress >= 0f) "${(progress * 100).toInt()}%" else "…"
                    Text(percent, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NX.accentLight)

                    if (!assembling) {
                        val downloadedStr = formatMb(bytesDownloaded)
                        val totalStr = if (totalBytes > 0) formatMb(totalBytes) else "…"
                        Text("$downloadedStr / $totalStr", fontSize = 12.sp, color = NX.muted)
                    }
                }

                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NX.muted2)
                    ) {
                        Text(stringResource(R.string.update_btn_cancel))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadyDialog(
    release: AppReleaseInfo,
    apkFile: File,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
    var hasInstallPermission by remember { mutableStateOf(UpdateInstaller.canInstallApk(ctx)) }

    // Re-check permission and auto-launch installer if permission is already granted
    LaunchedEffect(Unit) {
        val granted = UpdateInstaller.canInstallApk(ctx)
        hasInstallPermission = granted
        if (granted) {
            onInstall()
        }
    }

    val shape = RoundedCornerShape(28.dp)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .nxDialogSurface(shape)
                .padding(22.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(NX.green.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = NX.green,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.update_ready_title),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = NX.text
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "v${release.versionName}",
                            fontSize = 13.sp,
                            color = NX.muted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.update_ready_desc),
                    fontSize = 14.sp,
                    color = NX.muted2,
                    lineHeight = 20.sp
                )

                if (!hasInstallPermission) {
                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(NX.yellow.copy(alpha = 0.12f))
                            .border(1.dp, NX.yellow.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = NX.yellow,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.update_permission_required),
                                fontSize = 12.sp,
                                color = NX.text,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = NX.muted2)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }

                    Spacer(Modifier.width(8.dp))

                    if (!hasInstallPermission) {
                        Button(
                            onClick = {
                                UpdateInstaller.openInstallPermissionSettings(ctx)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NX.yellow,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                stringResource(R.string.update_btn_grant_permission),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = onInstall,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NX.green,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                stringResource(R.string.update_btn_install),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatMb(bytes: Long): String {
    return AppLang.bytes(bytes, gbDecimals = 1)
}

/** Channel where every release APK is posted. */
private const val APK_CHANNEL_URL = "https://t.me/NEXUSnet_news"

/**
 * Emergency update (X of X.Y.Z went up). Telegram is often unreachable without the VPN, so the
 * dialog lets the user connect first and then opens the channel with the new APK by itself.
 * It can only be put off once the VPN is up (the user has a working way to the channel).
 */
@Composable
private fun EmergencyUpdateDialog(release: AppReleaseInfo, onConnect: () -> Unit, onLater: () -> Unit) {
    val ctx = LocalContext.current
    val status by net.nexus.app.vpn.VpnState.status.collectAsState()
    val connected = status == net.nexus.app.vpn.VpnStatus.CONNECTED
    val connecting = status == net.nexus.app.vpn.VpnStatus.CONNECTING
    var opened by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    fun openChannel() {
        runCatching {
            ctx.startActivity(
                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(APK_CHANNEL_URL))
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        opened = true
    }
    LaunchedEffect(connected) { if (connected && !opened) openChannel() }

    val shape = RoundedCornerShape(28.dp)
    Dialog(
        onDismissRequest = { if (connected) onLater() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = connected, dismissOnClickOutside = connected),
    ) {
        Box(Modifier.padding(24.dp).fillMaxWidth().nxDialogSurface(shape).padding(22.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(NX.red.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.SystemUpdate, null, tint = NX.red, modifier = Modifier.size(26.dp))
                    }
                    Column {
                        Text(stringResource(R.string.update_emergency_title), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NX.text)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "v${BuildConfig.VERSION_NAME} → v${release.versionName}",
                            fontSize = 13.sp, color = NX.muted, fontFamily = FontFamily.Monospace,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.update_emergency_text), fontSize = 14.sp, color = NX.text, lineHeight = 20.sp)
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { if (connected) openChannel() else if (!connecting) onConnect() },
                    enabled = !connecting,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NX.accent, contentColor = Color.White),
                ) {
                    Text(
                        stringResource(
                            when {
                                connected -> R.string.update_emergency_open
                                connecting -> R.string.update_emergency_connecting
                                else -> R.string.update_emergency_connect
                            }
                        ),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (connected) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = onLater, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.update_emergency_later), color = NX.muted)
                    }
                }
            }
        }
    }
}
