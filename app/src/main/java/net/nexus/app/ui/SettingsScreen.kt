package net.nexus.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.nexus.app.BuildConfig
import net.nexus.app.MainViewModel
import net.nexus.app.R
import net.nexus.app.auth.SubscriptionNotifier
import net.nexus.app.core.XrayCore
import net.nexus.app.data.AppIcon
import net.nexus.app.data.AppTheme
import net.nexus.app.data.HapticIntensity
import net.nexus.app.data.Settings
import net.nexus.app.data.SplitMode
import net.nexus.app.update.UpdateConfig
import net.nexus.app.update.UpdateState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SettingsSubscreen {
    MAIN,
    CONNECTION,
    APPEARANCE,
    APP_PICKER
}

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val s by vm.settings.collectAsState()
    var subscreen by remember { mutableStateOf(SettingsSubscreen.MAIN) }

    BackHandler(enabled = subscreen != SettingsSubscreen.MAIN) {
        subscreen = when (subscreen) {
            SettingsSubscreen.APP_PICKER -> SettingsSubscreen.CONNECTION
            else -> SettingsSubscreen.MAIN
        }
    }

    AnimatedContent(
        targetState = subscreen,
        transitionSpec = {
            if (targetState == SettingsSubscreen.MAIN) {
                slideInHorizontally(tween(250, easing = FastOutSlowInEasing)) { -it / 3 } togetherWith
                    slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { it }
            } else {
                slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { it } togetherWith
                    slideOutHorizontally(tween(250, easing = FastOutSlowInEasing)) { -it / 3 }
            }
        },
        label = "settingsSubscreens"
    ) { currentSubscreen ->
        when (currentSubscreen) {
            SettingsSubscreen.APP_PICKER -> {
                AppPicker(
                    selected = s.splitApps,
                    onToggle = vm::toggleSplitApp,
                    onBack = { subscreen = SettingsSubscreen.CONNECTION }
                )
            }
            SettingsSubscreen.CONNECTION -> {
                ConnectionSettingsScreen(
                    settings = s,
                    vm = vm,
                    onPickApps = { subscreen = SettingsSubscreen.APP_PICKER },
                    onBack = { subscreen = SettingsSubscreen.MAIN }
                )
            }
            SettingsSubscreen.APPEARANCE -> {
                AppearanceSettingsScreen(
                    settings = s,
                    vm = vm,
                    onBack = { subscreen = SettingsSubscreen.MAIN }
                )
            }
            SettingsSubscreen.MAIN -> {
                MainSettingsList(
                    vm = vm,
                    settings = s,
                    onOpenConnection = { subscreen = SettingsSubscreen.CONNECTION },
                    onOpenAppearance = { subscreen = SettingsSubscreen.APPEARANCE }
                )
            }
        }
    }
}

@Composable
private fun MainSettingsList(
    vm: MainViewModel,
    settings: Settings,
    onOpenConnection: () -> Unit,
    onOpenAppearance: () -> Unit
) {
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
        item {
            Text(stringResource(R.string.settings_title), fontSize = 34.sp, fontWeight = FontWeight.Bold)
        }

        // 1) ДАННЫЕ О ПОДПИСКЕ И ПОЛЬЗОВАТЕЛЕ
        item {
            val tgUser by vm.telegramUser.collectAsState()
            val isSyncing by vm.isSyncingTelegram.collectAsState()
            val providersList by vm.providers.collectAsState()
            val nexusProvider = providersList.firstOrNull { !it.isLocal } ?: providersList.firstOrNull()

            Section(stringResource(R.string.settings_section_user)) {
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
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.tg_auth_btn_login),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
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
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = user.formattedUsername,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NX.text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (user.telegramId > 0) {
                                    Text(
                                        "ID: ${user.telegramId}",
                                        fontSize = 12.sp,
                                        color = NX.muted,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (user.isSubscribed) NX.green.copy(alpha = 0.18f) else NX.red.copy(alpha = 0.18f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (user.isSubscribed) "Активна" else "Истекла",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (user.isSubscribed) NX.green else NX.red,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        val daysLeft: Long? = if (user.expireAt.isNotBlank()) {
                            SubscriptionNotifier.getDaysLeft(user.expireAt)
                        } else if (nexusProvider != null && nexusProvider.expireAt > 0) {
                            java.lang.Math.ceil((nexusProvider.expireAt * 1000 - System.currentTimeMillis()) / 86_400_000.0).toLong()
                        } else null

                        val formattedDate: String? = runCatching {
                            if (user.expireAt.isNotBlank()) {
                                try {
                                    val instant = java.time.Instant.parse(user.expireAt)
                                    val zdt = instant.atZone(java.time.ZoneId.systemDefault())
                                    java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy").format(zdt)
                                } catch (_: Exception) {
                                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                                    val date = sdf.parse(user.expireAt)
                                    SimpleDateFormat("dd.MM.yyyy", Locale.US).format(date!!)
                                }
                            } else if (nexusProvider != null && nexusProvider.expireAt > 0) {
                                SimpleDateFormat("dd.MM.yyyy", Locale.US).format(Date(nexusProvider.expireAt * 1000))
                            } else null
                        }.getOrNull()

                        val daysText = when {
                            formattedDate != null && daysLeft != null && daysLeft > 365 -> "до $formattedDate"
                            formattedDate != null && daysLeft != null && daysLeft in 1..365 -> stringResource(R.string.sub_until_date, formattedDate, daysLeft)
                            formattedDate != null && daysLeft == 0L -> stringResource(R.string.sub_until_today, formattedDate)
                            formattedDate != null && daysLeft != null && daysLeft < 0 -> stringResource(R.string.sub_expired_on, formattedDate)
                            formattedDate != null -> "до $formattedDate"
                            daysLeft != null && daysLeft in 1..365 -> "Осталось: $daysLeft дн."
                            else -> stringResource(R.string.sub_unlimited)
                        }

                        val usedBytes = if (user.usedTrafficBytes > 0) user.usedTrafficBytes else (nexusProvider?.used ?: 0L)
                        val totalBytes = if (user.trafficLimitBytes > 0) user.trafficLimitBytes else (nexusProvider?.total ?: 0L)

                        val trafficText = run {
                            val usedGb = usedBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                            val totalGb = totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                            val usedStr = when {
                                usedBytes == 0L -> "0.0 ГБ"
                                usedBytes < 1024L * 1024L -> "%.0f КБ".format(Locale.US, usedBytes.toDouble() / 1024.0)
                                usedBytes < 1024L * 1024L * 1024L -> "%.1f МБ".format(Locale.US, usedBytes.toDouble() / (1024.0 * 1024.0))
                                else -> "%.1f ГБ".format(Locale.US, usedGb)
                            }
                            if (totalBytes > 0) {
                                "$usedStr / %.1f ГБ".format(Locale.US, totalGb)
                            } else {
                                "$usedStr / ∞"
                            }
                        }

                        val devicesText = stringResource(R.string.settings_devices_max, user.devicesCount)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(NX.chip)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_sub_label),
                                    fontSize = 13.sp,
                                    color = NX.muted2,
                                    maxLines = 1,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Text(
                                    text = daysText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NX.text,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_traffic_label),
                                    fontSize = 13.sp,
                                    color = NX.muted2,
                                    maxLines = 1,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Text(
                                    text = trafficText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NX.text,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_devices_label),
                                    fontSize = 13.sp,
                                    color = NX.muted2,
                                    maxLines = 1,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Text(
                                    text = devicesText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NX.text,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { vm.syncTelegramSubscription() },
                                colors = ButtonDefaults.buttonColors(containerColor = NX.accent),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isSyncing
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                } else {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    text = stringResource(R.string.tg_auth_btn_sync),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            TextButton(
                                onClick = { vm.logoutTelegram() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.tg_auth_btn_logout),
                                    color = NX.red,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2) НАСТРОЙКИ ПОДКЛЮЧЕНИЯ (ПЕРЕХОД ВО ВНУТРЕННИЙ ЭКРАН)
        item {
            val splitSubtitle = when (settings.splitMode) {
                SplitMode.OFF -> stringResource(R.string.split_all_apps)
                SplitMode.BYPASS -> stringResource(R.string.split_bypass_selected) + ": ${settings.splitApps.size}"
                SplitMode.ONLY -> stringResource(R.string.split_only_selected) + ": ${settings.splitApps.size}"
            }
            Section(stringResource(R.string.settings_section_conn)) {
                LinkRow(
                    icon = Icons.Filled.Dns,
                    title = stringResource(R.string.settings_section_conn),
                    subtitle = splitSubtitle,
                    onClick = onOpenConnection
                )
            }
        }

        // 3) ЯЗЫК, ВНЕШНИЙ ВИД И ОТКЛИК (ПЕРЕХОД ВО ВНУТРЕННИЙ ЭКРАН)
        item {
            val langTitle = when (currentLang) {
                "ru" -> stringResource(R.string.lang_ru)
                "en" -> stringResource(R.string.lang_en)
                else -> stringResource(R.string.lang_system)
            }
            Section(stringResource(R.string.settings_section_appearance)) {
                LinkRow(
                    icon = Icons.Filled.Palette,
                    title = stringResource(R.string.settings_section_appearance),
                    subtitle = "${settings.appTheme.title} • $langTitle",
                    onClick = onOpenAppearance
                )
            }
        }

        // 4) ССЫЛКИ
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
                        subtitle = "@huqore",
                        onClick = { openUrl("https://t.me/huqore") }
                    )
                    LinkRow(
                        icon = Icons.Filled.Campaign,
                        title = stringResource(R.string.link_channel),
                        subtitle = "@nexus_net_channel",
                        onClick = { openUrl("https://t.me/nexus_net_channel") }
                    )
                }
            }
        }

        // 5) ОБНОВЛЕНИЯ
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
                                fontSize = 14.sp,
                                color = NX.text,
                                maxLines = 2,
                                softWrap = true
                            )
                        }
                        Switch(
                            checked = settings.autoCheckUpdates,
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (updateState is UpdateState.Available) NX.accent else NX.chip,
                            contentColor = if (updateState is UpdateState.Available) Color.White else NX.text,
                        )
                    ) {
                        when (val st = updateState) {
                            is UpdateState.Checking -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = NX.text
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.update_checking), maxLines = 1, softWrap = false)
                            }
                            is UpdateState.Available -> {
                                Icon(Icons.Filled.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.update_new_version, "v${st.release.versionName}"),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            else -> {
                                Icon(Icons.Filled.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.settings_check_updates), maxLines = 1, softWrap = false)
                            }
                        }
                    }

                    if (updateState is UpdateState.UpToDate) {
                        Text(
                            stringResource(R.string.update_already_latest, BuildConfig.VERSION_NAME),
                            fontSize = 12.sp,
                            color = NX.green,
                            modifier = Modifier.padding(start = 4.dp),
                            maxLines = 1
                        )
                    } else if (updateState is UpdateState.Error) {
                        Text(
                            stringResource(R.string.update_check_failed, (updateState as UpdateState.Error).message),
                            fontSize = 12.sp,
                            color = NX.red,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }

        // 6) О ПРИЛОЖЕНИИ
        item {
            Section(stringResource(R.string.settings_section_about)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.about_version), color = NX.muted2, fontSize = 14.sp, maxLines = 1)
                        Text("v${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.SemiBold, color = NX.text, fontSize = 14.sp, maxLines = 1)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.about_core), color = NX.muted2, fontSize = 14.sp, maxLines = 1)
                        val cleanCore = if (coreVersion.contains("Xray-core")) {
                            "Xray-core " + coreVersion.substringAfter("Xray-core").trim().removePrefix("v").let { "v$it" }
                        } else {
                            coreVersion
                        }
                        Text(cleanCore, fontFamily = FontFamily.Monospace, color = NX.text, fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
        }

        // 7) АВТОР: @HUQORE
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.about_author),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = NX.muted.copy(alpha = 0.40f),
                maxLines = 1,
                softWrap = false
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Внутренний экран: Настройки подключения
 */
@Composable
private fun ConnectionSettingsScreen(
    settings: Settings,
    vm: MainViewModel,
    onPickApps: () -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SubscreenHeader(title = stringResource(R.string.settings_section_conn), onBack = onBack)
        }

        // Раздельное туннелирование
        item {
            Section(stringResource(R.string.settings_split)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.split_note),
                        fontSize = 12.sp,
                        color = NX.muted,
                        lineHeight = 16.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    RadioRow(settings.splitMode == SplitMode.OFF, stringResource(R.string.split_all_apps)) {
                        vm.setSplitMode(SplitMode.OFF)
                    }
                    RadioRow(settings.splitMode == SplitMode.BYPASS, stringResource(R.string.split_bypass_selected)) {
                        vm.setSplitMode(SplitMode.BYPASS)
                    }
                    RadioRow(settings.splitMode == SplitMode.ONLY, stringResource(R.string.split_only_selected)) {
                        vm.setSplitMode(SplitMode.ONLY)
                    }

                    if (settings.splitMode != SplitMode.OFF) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(NX.cardBorder.copy(alpha = 0.35f))
                                .clickable { onPickApps() }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    stringResource(R.string.split_pick_apps),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NX.text,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(stringResource(R.string.split_selected_count, settings.splitApps.size), fontSize = 12.sp, color = NX.muted)
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = NX.muted
                            )
                        }
                    }
                }
            }
        }

        // Параметры соединения
        item {
            Section(stringResource(R.string.settings_conn_params)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Автоподключение при старте
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = stringResource(R.string.settings_auto_connect_boot),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = NX.text,
                                maxLines = 2,
                                softWrap = true,
                                lineHeight = 17.sp
                            )
                            Text(
                                text = stringResource(R.string.settings_auto_connect_boot_desc),
                                fontSize = 12.sp,
                                color = NX.muted
                            )
                        }
                        Switch(
                            checked = settings.autoConnectOnBoot,
                            onCheckedChange = { vm.setAutoConnectOnBoot(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NX.accent,
                                uncheckedThumbColor = NX.muted,
                                uncheckedTrackColor = NX.cardBorder
                            )
                        )
                    }

                    // Kill Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Kill Switch",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = NX.text,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = stringResource(R.string.kill_switch_desc_short),
                                fontSize = 12.sp,
                                color = NX.muted
                            )
                        }
                        Switch(
                            checked = settings.killSwitch,
                            onCheckedChange = { vm.setKillSwitch(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NX.accent,
                                uncheckedThumbColor = NX.muted,
                                uncheckedTrackColor = NX.cardBorder
                            )
                        )
                    }

                    // Блокировщик рекламы AdGuard DNS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = stringResource(R.string.settings_adblock),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = NX.text,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = stringResource(R.string.settings_adblock_desc),
                                fontSize = 12.sp,
                                color = NX.muted
                            )
                        }
                        Switch(
                            checked = settings.blockAds,
                            onCheckedChange = { vm.setBlockAds(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NX.accent,
                                uncheckedThumbColor = NX.muted,
                                uncheckedTrackColor = NX.cardBorder
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Внутренний экран: Язык, внешний вид и отклик
 */
@Composable
private fun AppearanceSettingsScreen(
    settings: Settings,
    vm: MainViewModel,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')
    var iconExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SubscreenHeader(title = stringResource(R.string.settings_section_appearance), onBack = onBack)
        }

        // 1: Язык
        item {
            Section(stringResource(R.string.settings_language)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RadioRow(currentLang.isEmpty(), stringResource(R.string.lang_system)) {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                        (ctx as? android.app.Activity)?.recreate()
                    }
                    RadioRow(currentLang == "ru", stringResource(R.string.lang_ru)) {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru"))
                        (ctx as? android.app.Activity)?.recreate()
                    }
                    RadioRow(currentLang == "en", stringResource(R.string.lang_en)) {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                        (ctx as? android.app.Activity)?.recreate()
                    }
                }
            }
        }

        // 2: Вибрация
        item {
            Section(stringResource(R.string.settings_haptics_label)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RadioRow(settings.hapticIntensity == HapticIntensity.OFF, stringResource(R.string.haptic_off)) {
                        vm.setHapticIntensity(HapticIntensity.OFF)
                    }
                    RadioRow(settings.hapticIntensity == HapticIntensity.LIGHT, stringResource(R.string.haptic_light)) {
                        HapticManager.trigger(ctx, HapticIntensity.LIGHT, HapticType.CLICK)
                        vm.setHapticIntensity(HapticIntensity.LIGHT)
                    }
                    RadioRow(settings.hapticIntensity == HapticIntensity.MEDIUM, stringResource(R.string.haptic_medium)) {
                        HapticManager.trigger(ctx, HapticIntensity.MEDIUM, HapticType.CLICK)
                        vm.setHapticIntensity(HapticIntensity.MEDIUM)
                    }
                    RadioRow(settings.hapticIntensity == HapticIntensity.STRONG, stringResource(R.string.haptic_strong)) {
                        HapticManager.trigger(ctx, HapticIntensity.STRONG, HapticType.CLICK)
                        vm.setHapticIntensity(HapticIntensity.STRONG)
                    }
                }
            }
        }

        // 3: Цветовая схема (HOME_SCREEN на самом верху)
        item {
            Section(stringResource(R.string.appearance_theme_title)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppTheme.entries.forEach { theme ->
                        ThemeOptionRow(
                            theme = theme,
                            palette = theme.palette(),
                            selected = settings.appTheme == theme,
                            onClick = { vm.setAppTheme(theme) }
                        )
                    }
                }
            }
        }

        // 4: Иконка приложения (выдвижной список)
        item {
            Section(stringResource(R.string.appearance_icon_title)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { iconExpanded = !iconExpanded }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val currentPreview = when (settings.appIcon) {
                                AppIcon.DEFAULT -> R.drawable.ic_avatar_base
                                AppIcon.MINIMAL_GREY -> R.drawable.ic_avatar_grey
                                AppIcon.TOTAL_BLACK -> R.drawable.ic_avatar_black
                                AppIcon.INVERSION_MINIMAL -> R.drawable.ic_avatar_inversion
                                AppIcon.WHITE_VIOLET -> R.drawable.ic_avatar_violet
                                AppIcon.NYA_VPN -> R.drawable.ic_avatar_nya
                            }
                            Image(
                                painter = painterResource(currentPreview),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(stringResource(R.string.appearance_icon_title), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NX.text, maxLines = 1)
                                Text(settings.appIcon.title, fontSize = 12.sp, color = NX.muted, maxLines = 1)
                            }
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = NX.muted,
                            modifier = Modifier.graphicsLayer { rotationZ = if (iconExpanded) 90f else 0f }
                        )
                    }

                    AnimatedVisibility(
                        visible = iconExpanded,
                        enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                        exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppIcon.entries.forEach { icon ->
                                val iconRes = when (icon) {
                                    AppIcon.DEFAULT -> R.drawable.ic_avatar_base
                                    AppIcon.MINIMAL_GREY -> R.drawable.ic_avatar_grey
                                    AppIcon.TOTAL_BLACK -> R.drawable.ic_avatar_black
                                    AppIcon.INVERSION_MINIMAL -> R.drawable.ic_avatar_inversion
                                    AppIcon.WHITE_VIOLET -> R.drawable.ic_avatar_violet
                                    AppIcon.NYA_VPN -> R.drawable.ic_avatar_nya
                                }
                                IconOptionRow(
                                    icon = icon,
                                    drawableRes = iconRes,
                                    selected = settings.appIcon == icon,
                                    onClick = { vm.setAppIcon(icon) }
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
private fun SubscreenHeader(title: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
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
                contentDescription = stringResource(R.string.back),
                tint = NX.text
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = NX.text,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ---------------------------------------------------------------- helpers & subcomponents

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(NX.card)
            .border(1.dp, NX.cardBorder, RoundedCornerShape(22.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = NX.text,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
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
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) NX.chip else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = androidx.compose.material3.RadioButtonDefaults.colors(
                selectedColor = NX.accent,
                unselectedColor = NX.muted,
            )
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            color = if (selected) NX.text else NX.muted2,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
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
        if (isPressed) 0.97f else 1f,
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
            .clip(RoundedCornerShape(14.dp))
            .background(NX.cardBorder.copy(alpha = 0.25f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(NX.iconBtn),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NX.accentLight,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = NX.text,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = NX.muted,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = NX.muted,
            modifier = Modifier.size(20.dp),
        )
    }
}

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
            Text(stringResource(R.string.settings_split), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
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
                            .clickable { onToggle(app.pkg) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (app.icon != null) Image(app.icon, null, Modifier.size(44.dp))
                        else Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(NX.iconBtn))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(app.label, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(app.pkg, fontSize = 12.sp, color = NX.muted, maxLines = 1, fontFamily = FontFamily.Monospace, overflow = TextOverflow.Ellipsis)
                        }
                        Checkbox(checked = app.pkg in selected, onCheckedChange = null)
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
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) NX.text else NX.muted2,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            softWrap = false
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
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) NX.chip else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
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
        Image(
            painter = painterResource(drawableRes),
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = icon.title,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) NX.text else NX.muted2,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            softWrap = false
        )
    }
}
