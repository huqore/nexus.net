package net.nexus.app

import android.Manifest
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
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
        enableEdgeToEdge()
        setContent {
            val settings by vm.settings.collectAsState()
            NexusTheme {
                val ctx = LocalContext.current
                var tab by rememberSaveable { mutableStateOf(Tab.CONNECT) }
                val status by VpnState.status.collectAsState()
                val vpnError by VpnState.error.collectAsState()
                val updateState by vm.updateState.collectAsState()

                fun startVpn() {
                    val id = settings.selectedServerId
                    if (id == null) {
                        Toast.makeText(ctx, ctx.getString(R.string.no_server_selected), Toast.LENGTH_SHORT).show()
                        return
                    }
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
                    when (status) {
                        VpnStatus.CONNECTED, VpnStatus.CONNECTING -> NexusVpnService.stop(ctx)
                        else -> {
                            val prepare = VpnService.prepare(ctx)
                            if (prepare != null) vpnPermission.launch(prepare) else startVpn()
                        }
                    }
                }

                Box(Modifier.fillMaxSize()) {
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
                    UpdateDialog(state = updateState, vm = vm)
                }
            }
        }
    }
}
