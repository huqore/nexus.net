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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Campaign
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
import net.nexus.app.data.SplitMode
import net.nexus.app.update.UpdateConfig
import net.nexus.app.update.UpdateState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SettingsSubscreen { MAIN, APP_PICKER }

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val s by vm.settings.collectAsState()
    var subscreen by remember { mutableStateOf(SettingsSubscreen.MAIN) }

    AnimatedContent(
        targetState = subscreen,
        transitionSpec = {
            if (targetState == SettingsSubscreen.APP_PICKER) {
                slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { it } togetherWith slideOutHorizontally(tween(250, easing = FastOutSlowInEasing)) { -it / 3 }
            } else {
                slideInHorizontally(tween(250, easing = FastOutSlowInEasing)) { -it / 3 } togetherWith slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { it }
            }
        },
        label = "settingsSubscreens"
    ) { currentSubscreen ->
        when (currentSubscreen) {
            SettingsSubscreen.APP_PICKER -> {
                AppPicker(
                    selected = s.splitApps,
                    onToggle = vm::toggleSplitApp,
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

                var themeExpanded by remember { mutableStateOf(false) }
                var iconExpanded by remember { mutableStateOf(false) }

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
                                                .background(if (user.isSubscribed) NX.green.copy(alpha = 0.18f) else NX.red.copy(alpha = 0.18f))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (user.isSubscribed) "Активна" else "Истекла",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (user.isSubscribed) NX.green else NX.red
                                            )
                                        }
                                    }

                                    val daysLeft: Long? = if (user.expireAt.isNotBlank()) {
                                        SubscriptionNotifier.getDaysLeft(user.expireAt)
                                    } else if (nexusProvider != null && nexusProvider.expireAt > 0) {
                                        java.lang.Math.ceil((nexusProvider.expireAt * 1000 - System.currentTimeMillis()) / 86_400_000.0).toLong()
                                    } else null

                                    val dateStr = if (user.expireAt.isNotBlank()) {
                                        user.expireAt.take(10)
                                    } else if (nexusProvider != null && nexusProvider.expireAt > 0) {
                                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(nexusProvider.expireAt * 1000))
                                    } else null

                                    val daysText = when {
                                        daysLeft == null -> null
                                        daysLeft > 3650 -> "Срок: Безлимит"
                                        daysLeft > 0 -> "Осталось дней: $daysLeft" + (if (dateStr != null) " (до $dateStr)" else "")
                                        daysLeft == 0L -> "Истекает сегодня" + (if (dateStr != null) " (до $dateStr)" else "")
                                        else -> "Подписка истекла"
                                    }

                                    val usedBytes = nexusProvider?.used ?: 0L
                                    val usedGb = usedBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                                    val totalBytes = nexusProvider?.total ?: 0L
                                    val totalGb = totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                                    val trafficText = if (totalBytes > 0) {
                                        "%.1f ГБ / %.1f ГБ".format(Locale.US, usedGb, totalGb)
                                    } else {
                                        "%.1f ГБ / ∞".format(Locale.US, usedGb)
                                    }

                                    val devicesText = "${user.devicesCount} макс."

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(NX.cardBorder.copy(alpha = 0.25f))
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (daysText != null) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Подписка", fontSize = 13.sp, color = NX.muted2)
                                                Text(daysText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NX.text)
                                            }
                                        }
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Трафик", fontSize = 13.sp, color = NX.muted2)
                                            Text(trafficText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NX.text)
                                        }
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Устройств", fontSize = 13.sp, color = NX.muted2)
                                            Text(devicesText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NX.text)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = { vm.syncTelegramSubscription() },
                                            colors = ButtonDefaults.buttonColors(containerColor = NX.accent),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f),
                                            enabled = !isSyncing
                                        ) {
                                            if (isSyncing) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NX.text, strokeWidth = 2.dp)
                                                Spacer(Modifier.width(8.dp))
                                            } else {
                                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(8.dp))
                                            }
                                            Text(stringResource(R.string.tg_auth_btn_sync), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        }

                                        TextButton(
                                            onClick = { vm.logoutTelegram() },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(stringResource(R.string.tg_auth_btn_logout), color = NX.red, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2) НАСТРОЙКИ ПОДКЛЮЧЕНИЯ
                    item {
                        Section("Настройки подключения") {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        "Настройки раздельного туннелирования",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NX.text
                                    )
                                    Text(
                                        stringResource(R.string.split_note),
                                        fontSize = 12.sp,
                                        color = NX.muted,
                                        lineHeight = 16.sp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    RadioRow(s.splitMode == SplitMode.OFF, stringResource(R.string.split_off)) {
                                        vm.setSplitMode(SplitMode.OFF)
                                    }
                                    RadioRow(s.splitMode == SplitMode.BYPASS, stringResource(R.string.split_bypass)) {
                                        vm.setSplitMode(SplitMode.BYPASS)
                                    }
                                    RadioRow(s.splitMode == SplitMode.ONLY, stringResource(R.string.split_only)) {
                                        vm.setSplitMode(SplitMode.ONLY)
                                    }

                                    if (s.splitMode != SplitMode.OFF) {
                                        Spacer(Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(NX.cardBorder.copy(alpha = 0.35f))
                                                .clickable { subscreen = SettingsSubscreen.APP_PICKER }
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(stringResource(R.string.split_pick_apps), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NX.text)
                                                Text("Выбрано приложений: ${s.splitApps.size}", fontSize = 12.sp, color = NX.muted)
                                            }
                                            Icon(
                                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = NX.muted
                                            )
                                        }
                                    }
                                }

                                Box(Modifier.fillMaxWidth().height(1.dp).background(NX.cardBorder.copy(alpha = 0.4f)))

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        "Подключение",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NX.text
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text(
                                                stringResource(R.string.settings_auto_connect_boot),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
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
                                                uncheckedTrackColor = NX.cardBorder
                                            )
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text(
                                                stringResource(R.string.settings_kill_switch),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
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
                                                uncheckedTrackColor = NX.cardBorder
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3) ЯЗЫК, ВНЕШНИЙ ВИД И ОТКЛИК
                    item {
                        Section("Язык, внешний вид и отклик") {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                // Пункт 1: Язык
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Язык", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NX.text)
                                    RadioRow(currentLang == "ru" || currentLang.isEmpty(), "Русский") {
                                        AppCompatDelegate.setApplicationLocales(
                                            LocaleListCompat.forLanguageTags("ru")
                                        )
                                    }
                                    RadioRow(currentLang == "en", "English") {
                                        AppCompatDelegate.setApplicationLocales(
                                            LocaleListCompat.forLanguageTags("en")
                                        )
                                    }
                                }

                                Box(Modifier.fillMaxWidth().height(1.dp).background(NX.cardBorder.copy(alpha = 0.4f)))

                                // Пункт 2: Вибрация
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(stringResource(R.string.settings_haptics_title), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NX.text)
                                    RadioRow(s.hapticIntensity == HapticIntensity.OFF, stringResource(R.string.haptic_off)) {
                                        vm.setHapticIntensity(HapticIntensity.OFF)
                                    }
                                    RadioRow(s.hapticIntensity == HapticIntensity.LIGHT, stringResource(R.string.haptic_light)) {
                                        HapticManager.trigger(ctx, HapticIntensity.LIGHT, HapticType.CLICK)
                                        vm.setHapticIntensity(HapticIntensity.LIGHT)
                                    }
                                    RadioRow(s.hapticIntensity == HapticIntensity.MEDIUM, stringResource(R.string.haptic_medium)) {
                                        HapticManager.trigger(ctx, HapticIntensity.MEDIUM, HapticType.CLICK)
                                        vm.setHapticIntensity(HapticIntensity.MEDIUM)
                                    }
                                    RadioRow(s.hapticIntensity == HapticIntensity.STRONG, stringResource(R.string.haptic_strong)) {
                                        HapticManager.trigger(ctx, HapticIntensity.STRONG, HapticType.CLICK)
                                        vm.setHapticIntensity(HapticIntensity.STRONG)
                                    }
                                }

                                Box(Modifier.fillMaxWidth().height(1.dp).background(NX.cardBorder.copy(alpha = 0.4f)))

                                // Пункт 3: Цветовая схема
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { themeExpanded = !themeExpanded }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Palette, null, tint = NX.accent, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(10.dp))
                                            Column {
                                                Text("Цветовая схема", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NX.text)
                                                Text(s.appTheme.title, fontSize = 12.sp, color = NX.muted)
                                            }
                                        }
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = NX.muted,
                                            modifier = Modifier.graphicsLayer { rotationZ = if (themeExpanded) 90f else 0f }
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = themeExpanded,
                                        enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                                        exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(top = 6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            AppTheme.entries.forEach { theme ->
                                                ThemeOptionRow(
                                                    theme = theme,
                                                    palette = theme.palette(),
                                                    selected = s.appTheme == theme,
                                                    onClick = { vm.setAppTheme(theme) }
                                                )
                                            }
                                        }
                                    }
                                }

                                Box(Modifier.fillMaxWidth().height(1.dp).background(NX.cardBorder.copy(alpha = 0.4f)))

                                // Пункт 4: Иконка приложения (выдвижной список)
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
                                            val currentPreview = when (s.appIcon) {
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
                                                Text("Иконка приложения", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NX.text)
                                                Text(s.appIcon.title, fontSize = 12.sp, color = NX.muted)
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
                                                    selected = s.appIcon == icon,
                                                    onClick = { vm.setAppIcon(icon) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
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
                                            Text(stringResource(R.string.settings_check_updates))
                                        }
                                    }
                                }

                                if (updateState is UpdateState.UpToDate) {
                                    Text(
                                        stringResource(R.string.update_already_latest, BuildConfig.VERSION_NAME),
                                        fontSize = 12.sp,
                                        color = NX.green,
                                        modifier = Modifier.padding(start = 4.dp)
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
                        Section(stringResource(R.string.settings_about)) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Версия", color = NX.muted2, fontSize = 14.sp)
                                    Text("v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", fontWeight = FontWeight.SemiBold, color = NX.text, fontSize = 14.sp)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Ядро Xray", color = NX.muted2, fontSize = 14.sp)
                                    Text(coreVersion, fontFamily = FontFamily.Monospace, color = NX.text, fontSize = 13.sp)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Архитектура", color = NX.muted2, fontSize = 14.sp)
                                    Text("RemnaWave BFF / Xray-core", color = NX.text, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // 7) АВТОР: @HUQORE
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "автор: @huqore",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = NX.muted.copy(alpha = 0.40f)
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
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
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = NX.text)
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
        Text(label, fontSize = 15.sp, color = if (selected) NX.text else NX.muted2)
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

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = NX.text,
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = NX.muted,
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
                fontSize = 17.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) NX.text else NX.muted2
            )
            Text(
                text = if (icon == AppIcon.DEFAULT) "Оригинальная иконка NEXUS" else "Кастомный стиль иконки",
                fontSize = 12.sp,
                color = NX.muted
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
