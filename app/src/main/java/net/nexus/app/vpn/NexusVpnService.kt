package net.nexus.app.vpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.nexus.app.MainActivity
import net.nexus.app.R
import net.nexus.app.core.ConfigBuilder
import net.nexus.app.core.XrayCore
import net.nexus.app.data.SplitMode
import net.nexus.app.data.AppIcon
import net.nexus.app.data.Store

class NexusVpnService : VpnService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tun: ParcelFileDescriptor? = null
    private var currentGoFd: Int = -1

    private var watchdogJob: kotlinx.coroutines.Job? = null
    private var activeServerId: String? = null
    private var isAutoMode: Boolean = false

    val isConnected: Boolean get() = tun != null && XrayCore.isRunning

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopVpn()
            else -> {
                val serverId = intent?.getStringExtra(EXTRA_SERVER_ID)
                showNotification(getString(R.string.notif_connecting))
                scope.launch { startVpn(serverId) }
            }
        }
        return START_NOT_STICKY
    }

    private fun startVpn(serverId: String?) {
        val store = Store(this)
        val isAuto = (serverId == Store.AUTO_SERVER_ID || serverId == null)
        isAutoMode = isAuto
        val server = if (isAuto) store.findBestServer() else store.findServer(serverId)
        if (server == null) {
            fail("server not found"); return
        }
        activeServerId = server.id
        VpnState.connecting(server.id, isAuto = isAuto, serverName = server.name)
        updateTileState()
        NexusAppWidgetProvider.updateAllWidgets(this)

        var goFd = -1
        try {
            XrayCore.init(this)
            val socksPort = java.net.ServerSocket(0).use { it.localPort }
            VpnState.socksPort = socksPort
            val config = ConfigBuilder.forTunnel(server, socksPort)

            val pfd = buildTun(store) ?: error("VPN permission was revoked")
            tun = pfd
            // The core gets its own dup'd descriptor; we retain currentGoFd so we can close both descriptors on teardown.
            goFd = pfd.dup().detachFd()
            currentGoFd = goFd

            XrayCore.start(config, goFd) { /* core log line; ignored in release */ }

            VpnState.connected(server.id, isAuto = isAuto, serverName = server.name)
            val notifText = if (isAuto) {
                getString(R.string.notif_connected_auto, server.name)
            } else {
                getString(R.string.notif_connected, server.name)
            }
            showNotification(notifText)
            updateTileState()
            NexusAppWidgetProvider.updateAllWidgets(this)

            if (isAuto) {
                startWatchdog(socksPort)
            }
        } catch (t: Throwable) {
            val fd = currentGoFd
            currentGoFd = -1
            if (fd >= 0) runCatching { ParcelFileDescriptor.adoptFd(fd).close() }
            if (goFd >= 0 && goFd != fd) runCatching { ParcelFileDescriptor.adoptFd(goFd).close() }
            XrayCore.stopQuietly()
            runCatching { tun?.close() }; tun = null
            fail(t.message ?: t.javaClass.simpleName)
            updateTileState()
            NexusAppWidgetProvider.updateAllWidgets(this)
        }
    }

    private fun startWatchdog(socksPort: Int) {
        watchdogJob?.cancel()
        watchdogJob = scope.launch {
            var consecutiveFailures = 0
            while (isActive) {
                delay(20_000)
                if (!XrayCore.isRunning || tun == null) break

                val ok = checkTunnelHealth(socksPort)
                if (!ok) {
                    consecutiveFailures++
                    if (consecutiveFailures >= 2) {
                        consecutiveFailures = 0
                        performAutoFailover(socksPort)
                    }
                } else {
                    consecutiveFailures = 0
                }
            }
        }
    }

    private fun checkTunnelHealth(socksPort: Int): Boolean = runCatching {
        val proxy = java.net.Proxy(java.net.Proxy.Type.SOCKS, java.net.InetSocketAddress("127.0.0.1", socksPort))
        val conn = java.net.URL("http://www.google.com/generate_204").openConnection(proxy) as java.net.HttpURLConnection
        conn.connectTimeout = 4000
        conn.readTimeout = 4000
        conn.instanceFollowRedirects = false
        try {
            conn.responseCode in 200..399
        } finally {
            conn.disconnect()
        }
    }.getOrDefault(false)

    private fun performAutoFailover(socksPort: Int) {
        if (!isAutoMode || tun == null) return
        val store = Store(this)
        val nextServer = store.findNextBestServer(activeServerId) ?: return
        if (nextServer.id == activeServerId) return

        try {
            val newConfig = ConfigBuilder.forTunnel(nextServer, socksPort)
            val currentFd = currentGoFd
            if (currentFd >= 0 && tun != null) {
                // Dup fd for the restarted core
                val newGoFd = tun!!.dup().detachFd()
                XrayCore.stopQuietly()
                currentGoFd = newGoFd
                XrayCore.start(newConfig, newGoFd) { }

                activeServerId = nextServer.id
                VpnState.connected(nextServer.id, isAuto = true, serverName = nextServer.name)
                showNotification(getString(R.string.notif_connected_auto, nextServer.name))
                updateTileState()
                NexusAppWidgetProvider.updateAllWidgets(this)
            }
        } catch (_: Throwable) {
            // If failover failed, let watchdog retry on next cycle
        }
    }

    private fun buildTun(store: Store): ParcelFileDescriptor? {
        val settings = store.loadSettings()
        val b = Builder()
            .setSession("nexus.net")
            .setMtu(1500)
            .addAddress("172.19.0.1", 30)
            .addRoute("0.0.0.0", 0)
            .addAddress("fdfe:dcba:9876::1", 126)
            .addRoute("::", 0)
            .addDnsServer("1.1.1.1")
            .addDnsServer("8.8.8.8")
        if (Build.VERSION.SDK_INT >= 29) b.setMetered(false)
        if (settings.killSwitch) {
            b.setBlocking(true)
        }

        // The app itself always stays outside the tunnel: the core's own sockets to the proxy server
        // must not loop back into the TUN, and subscription updates must work while connected.
        when (settings.splitMode) {
            SplitMode.ONLY -> {
                val apps = settings.splitApps - packageName
                if (apps.isEmpty()) error("Split tunneling: no apps selected")
                apps.forEach { runCatching { b.addAllowedApplication(it) } }
            }
            SplitMode.BYPASS -> {
                settings.splitApps.forEach { runCatching { b.addDisallowedApplication(it) } }
                b.addDisallowedApplication(packageName)
            }
            SplitMode.OFF -> b.addDisallowedApplication(packageName)
        }
        return b.establish()
    }

    private fun fail(message: String) {
        watchdogJob?.cancel()
        watchdogJob = null
        runCatching { tun?.close() }; tun = null
        val fd = currentGoFd
        currentGoFd = -1
        if (fd >= 0) runCatching { ParcelFileDescriptor.adoptFd(fd).close() }
        XrayCore.stopQuietly()
        VpnState.failed(message)
        updateTileState()
        NexusAppWidgetProvider.updateAllWidgets(this)
        stopForegroundCompat()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIF_ID)
        stopSelf()
    }

    fun stopVpn() {
        watchdogJob?.cancel()
        watchdogJob = null
        runCatching { tun?.close() }; tun = null
        val fd = currentGoFd
        currentGoFd = -1
        if (fd >= 0) runCatching { ParcelFileDescriptor.adoptFd(fd).close() }
        XrayCore.stopQuietly()
        VpnState.disconnected()
        updateTileState()
        NexusAppWidgetProvider.updateAllWidgets(this)
        stopForegroundCompat()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIF_ID)
        stopSelf()
    }

    override fun onRevoke() {
        // user turned the VPN off from system settings / another VPN took over
        stopVpn()
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        runCatching { tun?.close() }; tun = null
        val fd = currentGoFd
        currentGoFd = -1
        if (fd >= 0) runCatching { ParcelFileDescriptor.adoptFd(fd).close() }
        XrayCore.stopQuietly()
        if (VpnState.status.value != VpnStatus.ERROR) VpnState.disconnected()
        updateTileState()
        stopForegroundCompat()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIF_ID)
        scope.cancel()
        super.onDestroy()
    }

    private fun updateTileState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching {
                android.service.quicksettings.TileService.requestListeningState(
                    this,
                    android.content.ComponentName(this, NexusTileService::class.java)
                )
            }
        }
    }

    // ---------- notification ----------

    private fun stopForegroundCompat() =
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)

    private var lastNotifText: String = ""

    fun refreshNotification() {
        if (lastNotifText.isNotBlank()) {
            showNotification(lastNotifText)
        }
    }

    private fun showNotification(text: String) {
        lastNotifText = text
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, getString(R.string.notif_channel), NotificationManager.IMPORTANCE_LOW)
            )
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getBroadcast(
            this, 1, Intent(this, VpnActionReceiver::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Resolve current active avatar/icon for notification large icon
        val store = Store(this)
        val avatarRes = when (store.loadSettings().appIcon) {
            AppIcon.MINIMAL_GREY -> R.drawable.ic_avatar_grey
            AppIcon.TOTAL_BLACK -> R.drawable.ic_avatar_black
            else -> R.drawable.ic_avatar_base
        }
        val largeIcon = runCatching {
            BitmapFactory.decodeResource(resources, avatarRes)
        }.getOrNull()

        val builder = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_vpn)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(0, getString(R.string.notif_disconnect), stop)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        val n = builder.build()
        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(this, NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    companion object {
        const val ACTION_START = "net.nexus.app.START"
        const val ACTION_STOP = "net.nexus.app.STOP"
        const val EXTRA_SERVER_ID = "server_id"
        private const val CHANNEL = "vpn_status"
        private const val NOTIF_ID = 1

        @Volatile
        var instance: NexusVpnService? = null

        fun start(context: Context, serverId: String) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, NexusVpnService::class.java)
                    .setAction(ACTION_START)
                    .putExtra(EXTRA_SERVER_ID, serverId)
            )
        }

        fun stop(context: Context) {
            val s = instance
            if (s != null) {
                s.stopVpn()
            } else {
                runCatching {
                    context.startService(Intent(context, NexusVpnService::class.java).setAction(ACTION_STOP))
                }
            }
        }
    }
}
