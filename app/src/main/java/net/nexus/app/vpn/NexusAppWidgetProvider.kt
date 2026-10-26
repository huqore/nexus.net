package net.nexus.app.vpn

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.VpnService
import android.os.Build
import android.widget.RemoteViews
import net.nexus.app.MainActivity
import net.nexus.app.R
import net.nexus.app.data.Store
import net.nexus.app.ui.HapticManager
import net.nexus.app.ui.HapticType
import net.nexus.app.ui.cleanServerName
import net.nexus.app.ui.flagFor

class NexusAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_VPN) {
            handleToggle(context)
        }
    }

    private fun handleToggle(context: Context) {
        val store = Store(context)
        val settings = store.loadSettings()
        HapticManager.trigger(context, settings.hapticIntensity, HapticType.TOGGLE)

        val currentStatus = VpnState.status.value
        if (currentStatus == VpnStatus.CONNECTED || currentStatus == VpnStatus.CONNECTING) {
            NexusVpnService.stop(context)
        } else {
            val prepare = VpnService.prepare(context)
            if (prepare != null) {
                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(openIntent)
            } else {
                val targetServerId = settings.selectedServerId ?: Store.AUTO_SERVER_ID
                NexusVpnService.start(context, targetServerId)
            }
        }
        updateAllWidgets(context)
    }

    companion object {
        const val ACTION_TOGGLE_VPN = "net.nexus.app.widget.ACTION_TOGGLE_VPN"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, NexusAppWidgetProvider::class.java))
            if (ids != null && ids.isNotEmpty()) {
                for (id in ids) {
                    updateWidget(context, appWidgetManager, id)
                }
            }
        }

        private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_nexus)
            val status = VpnState.status.value
            val store = Store(context)
            val settings = store.loadSettings()
            val isAuto = settings.selectedServerId == Store.AUTO_SERVER_ID || settings.selectedServerId == null

            val serverName = if (isAuto) {
                val activeName = VpnState.serverName.value
                if (status == VpnStatus.CONNECTED && activeName != null) {
                    "${cleanServerName(activeName)}"
                } else {
                    context.getString(R.string.auto_server_title)
                }
            } else {
                val server = store.findServer(settings.selectedServerId)
                if (server != null) cleanServerName(server.name) else context.getString(R.string.choose_server)
            }

            views.setTextViewText(R.id.widget_server_name, serverName)

            when (status) {
                VpnStatus.CONNECTED -> {
                    views.setTextViewText(R.id.widget_status_text, context.getString(R.string.status_connected))
                    views.setTextColor(R.id.widget_status_text, Color.parseColor("#3DDC97"))
                    views.setImageViewResource(R.id.widget_status_dot, R.drawable.widget_dot_green)
                    views.setImageViewResource(R.id.widget_toggle_btn, R.drawable.ic_widget_power)
                    views.setInt(R.id.widget_toggle_btn, "setBackgroundResource", R.drawable.widget_power_btn_connected)
                    views.setInt(R.id.widget_toggle_btn, "setColorFilter", Color.WHITE)
                    views.setTextViewText(R.id.widget_server_sub, if (isAuto) context.getString(R.string.auto_server_tag) else "VPN Online")
                }
                VpnStatus.CONNECTING -> {
                    views.setTextViewText(R.id.widget_status_text, context.getString(R.string.status_connecting))
                    views.setTextColor(R.id.widget_status_text, Color.parseColor("#E5C14A"))
                    views.setImageViewResource(R.id.widget_status_dot, R.drawable.widget_dot_yellow)
                    views.setImageViewResource(R.id.widget_toggle_btn, R.drawable.ic_widget_power)
                    views.setInt(R.id.widget_toggle_btn, "setBackgroundResource", R.drawable.widget_power_btn_disconnected)
                    views.setInt(R.id.widget_toggle_btn, "setColorFilter", Color.parseColor("#E5C14A"))
                    views.setTextViewText(R.id.widget_server_sub, context.getString(R.string.status_connecting))
                }
                VpnStatus.DISCONNECTED, VpnStatus.ERROR -> {
                    views.setTextViewText(R.id.widget_status_text, context.getString(R.string.status_disconnected))
                    views.setTextColor(R.id.widget_status_text, Color.parseColor("#8C88A8"))
                    views.setImageViewResource(R.id.widget_status_dot, R.drawable.widget_dot_gray)
                    views.setImageViewResource(R.id.widget_toggle_btn, R.drawable.ic_widget_power)
                    views.setInt(R.id.widget_toggle_btn, "setBackgroundResource", R.drawable.widget_power_btn_disconnected)
                    views.setInt(R.id.widget_toggle_btn, "setColorFilter", Color.parseColor("#B79CFF"))
                    views.setTextViewText(R.id.widget_server_sub, if (isAuto) context.getString(R.string.auto_server_desc) else context.getString(R.string.status_disconnected))
                }
            }

            // Click on widget body -> open MainActivity
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPendingIntent = PendingIntent.getActivity(
                context, 0, openIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_root, openPendingIntent)

            // Click on toggle button -> toggle VPN
            val toggleIntent = Intent(context, NexusAppWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_VPN
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context, 1, toggleIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_toggle_btn, togglePendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
