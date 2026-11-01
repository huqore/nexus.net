package net.nexus.app

import android.Manifest
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import net.nexus.app.ui.*
import net.nexus.app.vpn.NexusVpnService
import net.nexus.app.vpn.VpnState
import net.nexus.app.vpn.VpnStatus

class MainActivity : AppCompatActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)
        enableEdgeToEdge()
        setContent {
            val settings by vm.settings.collectAsState()
            NexusTheme(
                fontScale = settings.fontScale,
                appTheme = settings.appTheme,
                appFont = settings.appFont
            ) {
                val ctx = LocalContext.current
                var tab by rememberSaveable { mutableStateOf(Tab.CONNECT) }
                val status by VpnState.status.collectAsState()
                val vpnError by VpnState.error.collectAsState()
                val updateState by vm.updateState.collectAsState()
                val tgAuthState by vm.telegramAuthState.collectAsState()
                val tgUser by vm.telegramUser.collectAsState()

                fun startVpn() {
                    val id = settings.selectedServerId ?: net.nexus.app.data.Store.AUTO_SERVER_ID
                    NexusVpnService.start(ctx, id)
                }

                val vpnPermission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    if (it.resultCode == RESULT_OK) startVpn()
                    else Toast.makeText(ctx, ctx.getString(R.string.vpn_permission_denied), Toast.LENGTH_SHORT).show()
                }
                val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    if (NexusVpnService.instance?.isConnected == true) {
                        VpnState.connected()
                    } else if (NexusVpnService.instance == null && status == VpnStatus.CONNECTED) {
                        VpnState.disconnected()
                    }
                }
                LaunchedEffect(Unit) {
                    vm.events.collect { Toast.makeText(ctx, ctx.getString(it.id, *it.args.toTypedArray()), Toast.LENGTH_SHORT).show() }
                }
                LaunchedEffect(vpnError) {
                    vpnError?.let { Toast.makeText(ctx, "${ctx.getString(R.string.status_error)}: $it", Toast.LENGTH_LONG).show() }
                }

                fun toggleVpn() {
                    HapticManager.trigger(ctx, settings.hapticIntensity, HapticType.TOGGLE)
                    when (status) {
                        VpnStatus.CONNECTED, VpnStatus.CONNECTING -> NexusVpnService.stop(ctx)
                        else -> {
                            val prepare = VpnService.prepare(ctx)
                            if (prepare != null) vpnPermission.launch(prepare) else startVpn()
                        }
                    }
                }

                LaunchedEffect(status) {
                    when (status) {
                        VpnStatus.CONNECTED -> HapticManager.trigger(ctx, settings.hapticIntensity, HapticType.SUCCESS)
                        VpnStatus.ERROR -> HapticManager.trigger(ctx, settings.hapticIntensity, HapticType.ERROR)
                        else -> {}
                    }
                }

                // System swipe back gesture handlers:
                // 1. If on SERVERS or SETTINGS, back swipe returns to CONNECT
                BackHandler(enabled = tab != Tab.CONNECT) {
                    tab = Tab.CONNECT
                }

                // 2. If on CONNECT, minimize to background smoothly with moveTaskToBack(true) instead of crashing/killing the app
                BackHandler(enabled = tab == Tab.CONNECT) {
                    (ctx as? android.app.Activity)?.moveTaskToBack(true)
                }

                Box(Modifier.fillMaxSize()) {
                    if (tgUser == null) {
                        MandatoryTelegramAuthScreen(vm = vm)
                    } else {
                        AnimatedContent(
                            targetState = tab,
                            transitionSpec = {
                                val forward = targetState.ordinal > initialState.ordinal
                                val springSpec = spring<androidx.compose.ui.unit.IntOffset>(
                                    stiffness = Spring.StiffnessMediumLow,
                                    dampingRatio = Spring.DampingRatioNoBouncy
                                )
                                if (forward) {
                                    (slideInHorizontally(
                                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                                        initialOffsetX = { width -> (width * 0.3f).toInt() }
                                    ) + fadeIn(animationSpec = tween(260, easing = FastOutSlowInEasing)))
                                        .togetherWith(
                                            slideOutHorizontally(
                                                animationSpec = tween(250, easing = FastOutSlowInEasing),
                                                targetOffsetX = { width -> -(width * 0.3f).toInt() }
                                            ) + fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing))
                                        )
                                } else {
                                    (slideInHorizontally(
                                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                                        initialOffsetX = { width -> -(width * 0.3f).toInt() }
                                    ) + fadeIn(animationSpec = tween(260, easing = FastOutSlowInEasing)))
                                        .togetherWith(
                                            slideOutHorizontally(
                                                animationSpec = tween(250, easing = FastOutSlowInEasing),
                                                targetOffsetX = { width -> (width * 0.3f).toInt() }
                                            ) + fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing))
                                        )
                                }.using(SizeTransform(clip = false))
                            },
                            label = "tabTransition",
                            modifier = Modifier.fillMaxSize()
                        ) { currentTab ->
                            when (currentTab) {
                                Tab.CONNECT -> ConnectScreen(vm, onToggle = ::toggleVpn, onAddServers = { tab = Tab.SERVERS })
                                Tab.SERVERS -> ServersScreen(vm)
                                Tab.SETTINGS -> SettingsScreen(vm)
                            }
                        }
                        FloatingNavBar(
                            selected = tab, onSelect = { tab = it },
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                    UpdateDialog(state = updateState, vm = vm)
                    TelegramAuthDialog(state = tgAuthState, vm = vm)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.silentUpdateSubscriptions()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme.equals("nexus", ignoreCase = true)) {
            // Direct auth payload from Telegram bot or web login
            val userBase64 = uri.getQueryParameter("user")
                ?: uri.getQueryParameter("tg_auth")
            val sessionParam = uri.getQueryParameter("session")

            if (!userBase64.isNullOrBlank()) {
                vm.applyTelegramAuthBase64(userBase64)
            } else if (!sessionParam.isNullOrBlank()) {
                vm.checkAndApplyTelegramSession(sessionParam)
            }

            // Direct subscription import URL
            val subUrl = uri.getQueryParameter("url")
                ?: uri.getQueryParameter("sub")
                ?: uri.getQueryParameter("link")
            if (!subUrl.isNullOrBlank()) {
                vm.addInput(subUrl.trim())
            }
        }
    }
}
