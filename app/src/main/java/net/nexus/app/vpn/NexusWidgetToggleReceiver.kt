package net.nexus.app.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Non-exported BroadcastReceiver specifically dedicated to handling widget toggle clicks.
 * Because android:exported="false" is set in AndroidManifest.xml, 3rd-party apps cannot
 * send broadcasts to this receiver (enforced by the Android OS).
 */
class NexusWidgetToggleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NexusAppWidgetProvider.handleToggle(context)
    }
}

