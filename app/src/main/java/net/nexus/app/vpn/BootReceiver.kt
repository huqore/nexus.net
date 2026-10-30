package net.nexus.app.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import net.nexus.app.data.Store

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Enforce system-only BOOT_COMPLETED broadcast; reject any vendor quickboot or custom spoofed actions
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val store = Store(context)
        val settings = store.loadSettings()
        if (!settings.autoConnectOnBoot) return

        // Auto-connect only if the system VPN permission is already granted
        if (VpnService.prepare(context) == null) {
            val targetServerId = settings.selectedServerId ?: Store.AUTO_SERVER_ID
            NexusVpnService.start(context, targetServerId)
        }
    }
}
