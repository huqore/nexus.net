package net.nexus.app.ui

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import net.nexus.app.update.UpdateConfig
import net.nexus.app.update.UpdateState
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.nexus.app.BuildConfig
import net.nexus.app.MainViewModel
import net.nexus.app.R
import net.nexus.app.core.XrayCore
import net.nexus.app.data.SplitMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.ui.res.painterResource
import android.widget.Toast
import net.nexus.app.data.AppIcon
import net.nexus.app.data.AppTheme
import net.nexus.app.data.Settings



enum class SettingsSubscreen { MAIN, APP_PICKER, APPEARANCE }

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val s by vm.settings.collectAsState()
    var subscreen by remember { mutableStateOf(SettingsSubscreen.MAIN) }

    AnimatedContent(
        targetState = subscreen,
        transitionSpec = {
            if (targetState != SettingsSubscreen.MAIN) {
                (slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(260)))
                    .togetherWith(slideOutHorizontally(tween(250, easing = FastOutSlowInEasing)) { -it / 3 } + fadeOut(tween(200)))
            } else {
                (slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it / 3 } + fadeIn(tween(260)))
                    .togetherWith(slideOutHorizontally(tween(250, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(200)))
            }
        },
        label = "settingsSubscreenTransition"
    ) { currentSubscreen ->
        when (currentSubscreen) {
            SettingsSubscreen.APP_PICKER -> {
                AppPicker(selected = s.splitApps, onToggle = vm::toggleSplitApp, onBack = { subscreen = SettingsSubscreen.MAIN })
            }
            SettingsSubscreen.APPEARANCE -> {
                AppearanceScreen(
                    settings = s,
                    onSetTheme = vm::setAppTheme,
                    onSetIcon = vm::setAppIcon,
                    onSetUse3dGlobe = vm::setUse3dGlobe,
                    onBack = { subscreen = SettingsSubscreen.MAIN }
                )
            }
            SettingsSubscreen.MAIN -> {
            val currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')
            val coreVersion = remember { XrayCore.version() }
            val ctx = LocalContext.current
            val openUrl: (String) -> Unit = { link ->
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    ctx.startActivity(intent)
                }
            }

            LazyColumn(
                Modifier.fillMaxSize().statusBarsPadding(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 130.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { Text(stringResource(R.string.settings_title), fontSize = 34.sp, fontWeight = FontWeight.Bold) }

                item {
                    val tgUser by vm.telegramUser.collectAsState()
                    val isSyncing by vm.isSyncingTelegram.collectAsState()

                    Section(stringResource(R.string.tg_auth_account_title)) {
                        if (tgUser == null) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    stringResource(R.string.tg_auth_banner_subtitle),
                                    fontSize = 13.sp,
                                    color = NX.muted2,
                                    lineHeight = 18.sp,
                                )
                                Button(
                                    onClick = { vm.startTelegramAuth() },
                                    colors = ButtonDefaults.buttonColors(containerColor = NX.accent),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.tg_auth_btn_login), fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            val user = tgUser!!
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column {
                                        Text(
                                            user.formattedUsername,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NX.text
                                        )
                                        if (user.telegramId > 0) {
                                            Text(
                                                "ID: ${user.telegramId}",
                                                fontSize = 12.sp,
                                                color = NX.muted,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (user.isSubscribed) NX.green.copy(alpha = 0.18f) else NX.yellow.copy(alpha = 0.18f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (user.isSubscribed) "Активна" else "Истекла",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (user.isSubscribed) NX.green else NX.yellow
                                        )
                                    }
                                }

                                if (user.expireAt.isNotBlank()) {
                                    Text(
                                        text = "Истекает: ${user.expireAt.take(10)}",
                                        fontSize = 12.sp,
                                        color = NX.muted2
                                    )
                                }

                                Button(
                                    onClick = { openUrl(ctx.getString(R.string.tg_auth_bot_url)) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NX.accent,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.sub_renew_btn), fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { vm.syncTelegramSubscription() },
                                    enabled = !isSyncing,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NX.iconBtn,
                                        contentColor = NX.accentLight
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isSyncing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = NX.accentLight,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.tg_auth_syncing))
                                    } else {
                                        Icon(
                                            Icons.Filled.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.tg_auth_btn_sync), fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                TextButton(
                                    onClick = { vm.logoutTelegram() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = NX.red),
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text(stringResource(R.string.tg_auth_btn_logout), fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                item {
                    Section(stringResource(R.string.settings_language)) {
                        RadioRow(currentLang.isEmpty(), stringResource(R.string.lang_system)) {
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                        }
                        RadioRow(currentLang == "ru", stringResource(R.string.lang_ru)) {
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru"))
                        }
                        RadioRow(currentLang == "en", stringResource(R.string.lang_en)) {
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                        }
                    }
                }

                item {
                    Section(stringResource(R.string.settings_split)) {
                        RadioRow(s.splitMode == SplitMode.OFF, stringResource(R.string.split_off)) { vm.setSplitMode(SplitMode.OFF) }
                        RadioRow(s.splitMode == SplitMode.BYPASS, stringResource(R.string.split_bypass)) { vm.setSplitMode(SplitMode.BYPASS) }
                        RadioRow(s.splitMode == SplitMode.ONLY, stringResource(R.string.split_only)) { vm.setSplitMode(SplitMode.ONLY) }
                        AnimatedVisibility(
                            visible = s.splitMode != SplitMode.OFF,
                            enter = expandVertically(tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(250)),
                            exit = shrinkVertically(tween(250, easing = FastOutSlowInEasing)) + fadeOut(tween(200)),
                        ) {
                            Column {
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { subscreen = SettingsSubscreen.APP_PICKER },
                                    colors = ButtonDefaults.buttonColors(containerColor = NX.accent),
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text(stringResource(R.string.split_pick_apps, s.splitApps.size)) }
                                Spacer(Modifier.height(6.dp))
                                Text(stringResource(R.string.split_note), color = NX.muted, fontSize = 13.sp)
                            }
                        }
                    }
                }

                item {
                    Section(stringResource(R.string.settings_connection_title)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    stringResource(R.string.settings_auto_connect_boot),
                                    fontSize = 15.sp,
                                    color = NX.text
                                )
                                Text(
                                    stringResource(R.string.settings_auto_connect_boot_desc),
                                    fontSize = 12.sp,
                                    color = NX.muted
                                )
                            }
                            Switch(
                                checked = s.autoConnectOnBoot,
                                onCheckedChange = { vm.setAutoConnectOnBoot(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = NX.accent,
                                    uncheckedThumbColor = NX.muted,
                                    uncheckedTrackColor = NX.iconBtn,
                                )
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    stringResource(R.string.settings_kill_switch),
                                    fontSize = 15.sp,
                                    color = NX.text
                                )
                                Text(
                                    stringResource(R.string.settings_kill_switch_desc),
                                    fontSize = 12.sp,
                                    color = NX.muted
                                )
                            }
                            Switch(
                                checked = s.killSwitch,
                                onCheckedChange = { vm.setKillSwitch(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = NX.accent,
                                    uncheckedThumbColor = NX.muted,
                                    uncheckedTrackColor = NX.iconBtn,
                                )
                            )
                        }
                    }
                }

                item {
                    Section(stringResource(R.string.settings_appearance_title)) {
                        LinkRow(
                            icon = Icons.Filled.Palette,
                            title = stringResource(R.string.settings_appearance_title),
                            subtitle = "${s.appTheme.title} • ${s.appIcon.title}",
                            onClick = { subscreen = SettingsSubscreen.APPEARANCE }
                        )
                    }
                }

                item {
                    Section(stringResource(R.string.settings_haptics_title)) {
                        RadioRow(s.hapticIntensity == net.nexus.app.data.HapticIntensity.OFF, stringResource(R.string.haptic_off)) {
                            vm.setHapticIntensity(net.nexus.app.data.HapticIntensity.OFF)
                        }
                        RadioRow(s.hapticIntensity == net.nexus.app.data.HapticIntensity.LIGHT, stringResource(R.string.haptic_light)) {
                            HapticManager.trigger(ctx, net.nexus.app.data.HapticIntensity.LIGHT, HapticType.CLICK)
                            vm.setHapticIntensity(net.nexus.app.data.HapticIntensity.LIGHT)
                        }
                        RadioRow(s.hapticIntensity == net.nexus.app.data.HapticIntensity.MEDIUM, stringResource(R.string.haptic_medium)) {
                            HapticManager.trigger(ctx, net.nexus.app.data.HapticIntensity.MEDIUM, HapticType.CLICK)
                            vm.setHapticIntensity(net.nexus.app.data.HapticIntensity.MEDIUM)
                        }
                        RadioRow(s.hapticIntensity == net.nexus.app.data.HapticIntensity.STRONG, stringResource(R.string.haptic_strong)) {
                            HapticManager.trigger(ctx, net.nexus.app.data.HapticIntensity.STRONG, HapticType.CLICK)
                            vm.setHapticIntensity(net.nexus.app.data.HapticIntensity.STRONG)
                        }
                    }
                }

                item {
                    Section(stringResource(R.string.settings_about)) {
                        Text(
                            stringResource(R.string.about_text, BuildConfig.VERSION_NAME, coreVersion),
                            color = NX.muted2, fontFamily = FontFamily.Monospace, fontSize = 14.sp,
                        )
                    }
                }

                item {
                    val updateState by vm.updateState.collectAsState()
                    Section(stringResource(R.string.settings_updates_title)) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        stringResource(R.string.settings_auto_check),
                                        fontSize = 15.sp,
                                        color = NX.text
                                    )
                                    Text(
                                        stringResource(R.string.update_repo_label, UpdateConfig.GITHUB_REPO),
                                        fontSize = 12.sp,
                                        color = NX.muted,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Switch(
                                    checked = s.autoCheckUpdates,
                                    onCheckedChange = { vm.setAutoCheckUpdates(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = NX.accent,
                                        uncheckedThumbColor = NX.muted,
                                        uncheckedTrackColor = NX.iconBtn,
                                    )
                                )
                            }

                            Button(
                                onClick = {
                                    if (updateState is UpdateState.Available) {
                                        vm.showUpdateDialog()
                                    } else {
                                        vm.checkForUpdates(manual = true)
                                    }
                                },
                                enabled = updateState !is UpdateState.Checking && updateState !is UpdateState.Downloading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (updateState is UpdateState.Available) NX.accentLight else NX.accent,
                                    contentColor = if (updateState is UpdateState.Available) Color.Black else Color.White
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                when (val st = updateState) {
                                    is UpdateState.Checking -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.update_checking))
                                    }
                                    is UpdateState.Available -> {
                                        Icon(Icons.Filled.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            stringResource(R.string.update_new_version, "v${st.release.versionName}"),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    else -> {
                                        Icon(Icons.Filled.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.settings_check_updates), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            if (updateState is UpdateState.UpToDate) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = NX.green,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        stringResource(R.string.update_already_latest, BuildConfig.VERSION_NAME),
                                        fontSize = 12.sp,
                                        color = NX.green
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Section(stringResource(R.string.links_title)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LinkRow(
                                icon = Icons.AutoMirrored.Filled.Send,
                                title = stringResource(R.string.link_bot),
                                subtitle = "t.me/NEXUSnet_robot",
                                onClick = { openUrl("https://t.me/NEXUSnet_robot") }
                            )
                            LinkRow(
                                icon = Icons.Filled.Language,
                                title = stringResource(R.string.link_site),
                                subtitle = "nexus-net.xyz",
                                onClick = { openUrl("https://nexus-net.xyz") }
                            )
                            LinkRow(
                                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                                title = stringResource(R.string.link_support),
                                subtitle = "t.me/NEXUS_net_support",
                                onClick = { openUrl("https://t.me/NEXUS_net_support") }
                            )
                            LinkRow(
                                icon = Icons.Filled.Campaign,
                                title = stringResource(R.string.link_channel),
                                subtitle = "t.me/NEXUSnet_news",
                                onClick = { openUrl("https://t.me/NEXUSnet_news") }
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(NX.card).border(1.dp, NX.cardBorder, shape).padding(16.dp)) {
        Text(title, color = NX.muted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun RadioRow(selected: Boolean, label: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        if (isPressed) 0.98f else 1f,
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "radioRowPress"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 17.sp)
    }
}

@Composable
private fun LinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        if (isPressed) 0.98f else 1f,
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "linkRowPress"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(NX.cardBorder.copy(alpha = 0.35f))
            .border(1.dp, NX.cardBorder.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(NX.accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = NX.accentLight,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NX.text)
            Text(subtitle, fontSize = 12.sp, color = NX.muted)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = NX.muted,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ---------------------------------------------------------------- split tunneling app picker

private data class AppItem(val pkg: String, val label: String, val icon: ImageBitmap?)

private fun loadApps(ctx: android.content.Context, preselected: Set<String>): List<AppItem> {
    val pm = ctx.packageManager
    val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(launcher, 0)
        .map { it.activityInfo.packageName }.distinct().filter { it != ctx.packageName }
        .mapNotNull { pkg ->
            runCatching {
                AppItem(
                    pkg, pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString(),
                    runCatching { pm.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap() }.getOrNull(),
                )
            }.getOrNull()
        }
        .sortedWith(compareByDescending<AppItem> { it.pkg in preselected }.thenBy { it.label.lowercase() })
}

@Composable
private fun AppPicker(selected: Set<String>, onToggle: (String) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf<List<AppItem>?>(null) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { apps = withContext(Dispatchers.IO) { loadApps(ctx, selected) } }

    val backInteraction = remember { MutableInteractionSource() }
    val backPressed by backInteraction.collectIsPressedAsState()
    val backScale by animateFloatAsState(
        if (backPressed) 0.88f else 1f,
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "backScale"
    )

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .graphicsLayer {
                        scaleX = backScale
                        scaleY = backScale
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = backInteraction,
                        indication = null,
                        onClick = onBack
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
            }
            Text(stringResource(R.string.settings_split), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        }
        OutlinedTextField(
            value = query, onValueChange = { query = it }, singleLine = true,
            placeholder = { Text(stringResource(R.string.split_search), color = NX.muted) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        val list = apps
        if (list == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = NX.accent) }
        } else {
            val filtered = list.filter { query.isBlank() || it.label.contains(query, true) || it.pkg.contains(query, true) }
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp)) {
                items(filtered, key = { it.pkg }) { app ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .clickable { onToggle(app.pkg) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (app.icon != null) Image(app.icon, null, Modifier.size(44.dp))
                        else Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(NX.iconBtn))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(app.label, fontSize = 17.sp, maxLines = 1)
                            Text(app.pkg, fontSize = 12.sp, color = NX.muted, maxLines = 1, fontFamily = FontFamily.Monospace)
                        }
                        Checkbox(checked = app.pkg in selected, onCheckedChange = null)
                    }
                }
            }
        }
    }
}


// ---------------------------------------------------------------- appearance & theme & icon picker

@Composable
private fun AppearanceScreen(
    settings: Settings,
    onSetTheme: (AppTheme) -> Unit,
    onSetIcon: (AppIcon) -> Unit,
    onSetUse3dGlobe: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NX.iconBtn)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NX.text
                    )
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    stringResource(R.string.settings_appearance_title),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = NX.text
                )
            }
        }

        item {
            Section(stringResource(R.string.settings_server_display_title)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(stringResource(R.string.settings_use_3d_globe), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            stringResource(R.string.settings_use_3d_globe_desc),
                            fontSize = 12.sp,
                            color = NX.muted
                        )
                    }
                    Switch(
                        checked = settings.use3dGlobe,
                        onCheckedChange = { onSetUse3dGlobe(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NX.accent,
                            uncheckedThumbColor = NX.muted,
                            uncheckedTrackColor = NX.iconBtn,
                        )
                    )
                }
            }
        }

        item {
            Section(stringResource(R.string.appearance_theme_title)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppTheme.entries.forEach { theme ->
                        val selected = settings.appTheme == theme
                        val palette = theme.palette()
                        ThemeOptionRow(
                            theme = theme,
                            palette = palette,
                            selected = selected,
                            onClick = { onSetTheme(theme) }
                        )
                    }
                }
            }
        }

        item {
            Section(stringResource(R.string.appearance_icon_title)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppIcon.entries.forEach { icon ->
                        val selected = settings.appIcon == icon
                        val iconRes = when (icon) {
                            AppIcon.DEFAULT -> R.drawable.ic_avatar_base
                            AppIcon.MINIMAL_GREY -> R.drawable.ic_avatar_grey
                            AppIcon.TOTAL_BLACK -> R.drawable.ic_avatar_black
                        }
                        IconOptionRow(
                            icon = icon,
                            drawableRes = iconRes,
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    onSetIcon(icon)
                                    Toast.makeText(
                                        ctx,
                                        ctx.getString(R.string.appearance_icon_applied),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    theme: AppTheme,
    palette: NexusColorPalette,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        if (isPressed) 0.98f else 1f,
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "themeRowPress"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) NX.chip else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = androidx.compose.material3.RadioButtonDefaults.colors(
                selectedColor = NX.accent,
                unselectedColor = NX.muted
            )
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = theme.title,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) NX.text else NX.muted2,
            modifier = Modifier.weight(1f)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ColorCircle(palette.bg, borderColor = palette.cardBorder)
            ColorCircle(palette.card, borderColor = palette.cardBorder)
            ColorCircle(palette.accent)
            ColorCircle(palette.accentLight)
        }
    }
}

@Composable
private fun ColorCircle(color: Color, borderColor: Color? = null) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(RoundedCornerShape(50))
            .background(color)
            .then(
                if (borderColor != null) Modifier.border(1.dp, borderColor, RoundedCornerShape(50))
                else Modifier
            )
    )
}

@Composable
private fun IconOptionRow(
    icon: AppIcon,
    drawableRes: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        if (isPressed) 0.98f else 1f,
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "iconRowPress"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) NX.chip else NX.cardBorder.copy(alpha = 0.25f))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) NX.accent else NX.cardBorder.copy(alpha = 0.6f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(drawableRes),
            contentDescription = icon.title,
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, NX.cardBorder, RoundedCornerShape(14.dp))
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = icon.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = NX.text
            )
        }
        RadioButton(
            selected = selected,
            onClick = null,
            colors = androidx.compose.material3.RadioButtonDefaults.colors(
                selectedColor = NX.accent,
                unselectedColor = NX.muted
            )
        )
    }
}
