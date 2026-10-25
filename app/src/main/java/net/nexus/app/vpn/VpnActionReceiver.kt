package net.nexus.app.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class VpnActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NexusVpnService.ACTION_STOP) {
            NexusVpnService.stop(context)
        }
    }
}
