package net.nexus.app.vpn

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.nexus.app.MainActivity
import net.nexus.app.R
import net.nexus.app.data.Store
import net.nexus.app.ui.cleanServerName

@RequiresApi(Build.VERSION_CODES.N)
class NexusTileService : TileService() {

    private var scope: CoroutineScope? = null
    private var job: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        scope?.cancel()
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        scope = s

        job = s.launch {
            VpnState.status.collect { status ->
                updateTile(status)
            }
        }
    }

    override fun onStopListening() {
        scope?.cancel()
        scope = null
        super.onStopListening()
    }

    private fun updateTile(status: VpnStatus) {
        val tile = qsTile ?: return
        when (status) {
            VpnStatus.CONNECTED -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = getString(R.string.qs_tile_name)
                val sName = VpnState.serverName.value
                val labelText = if (sName != null) {
                    cleanServerName(sName)
                } else {
                    getString(R.string.status_connected)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = labelText
                }
            }
            VpnStatus.CONNECTING -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = getString(R.string.qs_tile_name)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = getString(R.string.status_connecting)
                }
            }
            VpnStatus.DISCONNECTED, VpnStatus.ERROR -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = getString(R.string.qs_tile_name)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = getString(R.string.qs_tile_disconnected)
                }
            }
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val currentStatus = VpnState.status.value
        if (currentStatus == VpnStatus.CONNECTED || currentStatus == VpnStatus.CONNECTING) {
            NexusVpnService.stop(this)
        } else {
            val prepare = VpnService.prepare(this)
            if (prepare != null) {
                // VPN permission not granted, open MainActivity to prompt user
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val pi = PendingIntent.getActivity(
                        this, 0, intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    startActivityAndCollapse(pi)
                } else {
                    @Suppress("DEPRECATION")
                    startActivityAndCollapse(intent)
                }
            } else {
                val store = Store(this)
                val settings = store.loadSettings()
                val targetServerId = settings.selectedServerId ?: Store.AUTO_SERVER_ID
                NexusVpnService.start(this, targetServerId)
            }
        }
    }

    override fun onDestroy() {
        scope?.cancel()
        super.onDestroy()
    }
}
