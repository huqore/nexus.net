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
import hev.htproxy.TProxyService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.nexus.app.MainActivity
import net.nexus.app.R
import net.nexus.app.core.ConfigBuilder
import net.nexus.app.core.XrayCore
import net.nexus.app.data.SplitMode
import net.nexus.app.data.AppIcon
import net.nexus.app.data.Store
import java.io.File

class NexusVpnService : VpnService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tun: ParcelFileDescriptor? = null
    private var vpnJob: kotlinx.coroutines.Job? = null
    private var watchdogJob: kotlinx.coroutines.Job? = null
    private var sessionWatchdogJob: kotlinx.coroutines.Job? = null
    private var activeServerId: String? = null
    private var isAutoMode: Boolean = false
    private var speedTickerJob: kotlinx.coroutines.Job? = null

    val isConnected: Boolean get() = tun != null && XrayCore.isRunning

    private var isScreenOn: Boolean = true
    private var screenReceiverRegistered = false
    private val screenReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                }
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        runCatching {
            val filter = android.content.IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            registerReceiver(screenReceiver, filter)
            screenReceiverRegistered = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopVpn()
            else -> {
                val serverId = intent?.getStringExtra(EXTRA_SERVER_ID)
                showNotification(getString(R.string.notif_connecting))
                vpnJob?.cancel()
                vpnJob = scope.launch { startVpn(serverId) }
            }
        }
        return START_NOT_STICKY
    }

    private fun startVpn(serverId: String?) {
        runCatching { TProxyService.TProxyStopService() }
        XrayCore.stopQuietly()
        runCatching { tun?.close() }; tun = null

        val store = Store(this)
        val isAuto = (serverId == Store.AUTO_SERVER_ID || serverId == null)
        isAutoMode = isAuto
        val server = if (isAuto) store.resolveAutoServer() else store.findServer(serverId)
        if (server == null) {
            fail("server not found"); return
        }
        activeServerId = server.id
        VpnState.connecting(server.id, isAuto = isAuto, serverName = server.name)
        updateTileState()
        NexusAppWidgetProvider.updateAllWidgets(this)

        try {
            XrayCore.init(this)
            val socksPort = java.net.ServerSocket(0).use { it.localPort }
            VpnState.socksPort = socksPort
            val settings = store.loadSettings()
            val (socksUser, socksPass) = net.nexus.app.core.LocalSocksAuth.rotate(socksPort)
            val config = ConfigBuilder.forTunnel(server, socksPort, blockAds = settings.blockAds, socksUser = socksUser, socksPass = socksPass)

            val pfd = buildTun(store) ?: error("VPN permission was revoked")
            tun = pfd

            val yamlFile = File(filesDir, "hev-tunnel.yaml")
            val yamlContent = """
                tunnel:
                  name: tun0
                  mtu: 1360
                  ipv4: $TUN_IPV4
                  ipv6: '$TUN_IPV6'

                socks5:
                  port: $socksPort
                  address: 127.0.0.1
                  username: '$socksUser'
                  password: '$socksPass'
                  udp: 'udp'
                  udp-address: 127.0.0.1

                misc:
                  task-stack-size: 86016
                  connect-timeout: 5000
                  tcp-read-write-timeout: 300000
                  udp-read-write-timeout: 60000
                  log-file: /dev/null
                  log-level: error
                  limit-nofile: 65535
            """.trimIndent()
            yamlFile.writeText(yamlContent)

            // Start Xray in SOCKS mode (tunFd = 0)
            XrayCore.start(config, 0) { line -> android.util.Log.i("NexusVPN", "Xray: $line") }

            // Bridge Android TUN to Xray SOCKS5 via high-performance lwIP native stack (same engine as INCY and Happ)
            val started = TProxyService.TProxyStartService(yamlFile.absolutePath, pfd.fd)
            if (!started) {
                error("hev-socks5-tunnel failed to start")
            }

            VpnState.connected(server.id, isAuto = isAuto, serverName = server.name)
            val notifText = if (isAuto) {
                getString(R.string.notif_connected_auto, server.name)
            } else {
                getString(R.string.notif_connected, server.name)
            }
            showNotification(notifText)
            startSpeedTicker(server.name)
            updateTileState()
            NexusAppWidgetProvider.updateAllWidgets(this)
            startSessionWatchdog()

            if (isAuto) {
                startWatchdog(socksPort)
            }
        } catch (t: Throwable) {
            runCatching { TProxyService.TProxyStopService() }
            XrayCore.stopQuietly()
            runCatching { tun?.close() }; tun = null
            fail(t.message ?: t.javaClass.simpleName)
            updateTileState()
            NexusAppWidgetProvider.updateAllWidgets(this)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun startWatchdog(socksPort: Int) {
        // Disabled on purpose: the periodic health probe (every 20-60 s) made Auto mode hop between
        // servers whenever the probe host was unreachable (e.g. on whitelist-only mobile networks).
        // Auto now locks onto one server (Store.resolveAutoServer); switching is the user's choice.
        watchdogJob?.cancel()
        watchdogJob = null
    }

    private fun buildTun(store: Store): ParcelFileDescriptor? {
        val settings = store.loadSettings()
        val b = Builder()
            .setSession("nexus.net")
            .setMtu(1360)
            .addAddress(TUN_IPV4, 16)
            .addRoute("0.0.0.0", 0)

        if (settings.blockAds) {
            b.addDnsServer("94.140.14.14")
            b.addDnsServer("94.140.15.15")
        } else {
            b.addDnsServer("1.1.1.1")
            b.addDnsServer("8.8.8.8")
        }

        // Add IPv6 route to prevent leaks on LTE / dual-stack networks
        runCatching {
            b.addAddress(TUN_IPV6, 128)
            b.addRoute("::", 0)
        }

        if (Build.VERSION.SDK_INT >= 29) b.setMetered(false)
        if (settings.killSwitch) {
            b.setBlocking(true)
        }

        // The app itself always stays outside the tunnel: the core's own sockets to the proxy server
        // must not loop back into the TUN, and subscription updates must work while connected.
        when (settings.splitMode) {
            SplitMode.ONLY -> {
                val apps = settings.splitApps - packageName
                if (apps.isEmpty()) {
                    android.util.Log.w("NexusVPN", "SplitMode.ONLY selected with 0 apps, routing all traffic")
                } else {
                    apps.forEach { runCatching { b.addAllowedApplication(it) } }
                }
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
        speedTickerJob?.cancel()
        speedTickerJob = null
        watchdogJob?.cancel()
        watchdogJob = null
        runCatching { TProxyService.TProxyStopService() }
        XrayCore.stopQuietly()
        runCatching { tun?.close() }; tun = null
        VpnState.failed(message)
        updateTileState()
        NexusAppWidgetProvider.updateAllWidgets(this)
        stopForegroundCompat()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIF_ID)
        stopSelf()
    }

    private fun startSessionWatchdog() {
        sessionWatchdogJob?.cancel()
        sessionWatchdogJob = scope.launch(Dispatchers.IO) {
            val store = Store(this@NexusVpnService)
            val user = store.loadTelegramUser()
            val hwid = store.getOrCreateHwid()
            if (user != null) {
                runCatching {
                    net.nexus.app.auth.TelegramAuthManager.handshake(hwid, user.cleanUsername, user.telegramId, appToken = user.appToken)
                }
            }

            while (isActive) {
                delay(15_000L)
                if (!XrayCore.isRunning || tun == null) break
                val currentUser = store.loadTelegramUser()
                if (currentUser != null) {
                    val valid = net.nexus.app.auth.TelegramAuthManager.validateDeviceSession(
                        hwid,
                        currentUser.cleanUsername,
                        currentUser.telegramId,
                        appToken = currentUser.appToken
                    )
                    if (!valid) {
                        android.util.Log.w("NexusVPN", "Remote session revoked: stopping VPN")
                        store.saveTelegramUser(null)
                        store.saveProviders(emptyList())
                        withContext(Dispatchers.Main) {
                            stopVpn()
                            showNotification("Устройство отвязано от аккаунта")
                            val intent = Intent("net.nexus.app.ACTION_SESSION_REVOKED").apply {
                                setPackage(packageName)
                            }
                            sendBroadcast(intent)
                        }
                        break
                    }
                }
            }
        }
    }

    fun stopVpn() {
        sessionWatchdogJob?.cancel()
        sessionWatchdogJob = null
        vpnJob?.cancel()
        vpnJob = null
        speedTickerJob?.cancel()
        speedTickerJob = null
        watchdogJob?.cancel()
        watchdogJob = null
        runCatching { TProxyService.TProxyStopService() }
        XrayCore.stopQuietly()
        runCatching { tun?.close() }; tun = null
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
        if (screenReceiverRegistered) {
            runCatching { unregisterReceiver(screenReceiver) }
            screenReceiverRegistered = false
        }
        sessionWatchdogJob?.cancel()
        sessionWatchdogJob = null
        vpnJob?.cancel()
        vpnJob = null
        speedTickerJob?.cancel()
        speedTickerJob = null
        if (instance === this) instance = null
        runCatching { TProxyService.TProxyStopService() }
        XrayCore.stopQuietly()
        runCatching { tun?.close() }; tun = null
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
        val store = Store(this)
        val avatarRes = when (store.loadSettings().appIcon) {
            AppIcon.MINIMAL_GREY -> R.drawable.ic_avatar_grey
            AppIcon.TOTAL_BLACK -> R.drawable.ic_avatar_black
            AppIcon.INVERSION_MINIMAL -> R.drawable.ic_avatar_inversion
            AppIcon.WHITE_VIOLET -> R.drawable.ic_avatar_violet
            AppIcon.NYA_VPN -> R.drawable.ic_avatar_nya
            else -> R.drawable.ic_avatar_base
        }

        val activeAlias = store.loadSettings().appIcon.aliasName
        val openIntent = Intent().apply {
            component = android.content.ComponentName(packageName, activeAlias)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val open = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getBroadcast(
            this, 1, Intent(this, VpnActionReceiver::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(avatarRes)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentIntent(open)
            .addAction(0, getString(R.string.notif_disconnect), stop)

        val n = builder.build()
        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(this, NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, n)
        }
        nm.notify(NOTIF_ID, n)
    }

    private fun startSpeedTicker(serverName: String) {
        speedTickerJob?.cancel()
        speedTickerJob = scope.launch {
            val (baseRx, baseTx) = getTrafficBytes()
            var lastRx = baseRx
            var lastTx = baseTx
            var lastTime = System.currentTimeMillis()

            while (isActive && isConnected) {
                val interval = if (isScreenOn) 3000L else 30_000L
                delay(interval)
                val now = System.currentTimeMillis()
                val (curRx, curTx) = getTrafficBytes()
                val dtSec = ((now - lastTime) / 1000f).coerceAtLeast(0.5f)

                val rxDiff = (curRx - lastRx).coerceAtLeast(0)
                val txDiff = (curTx - lastTx).coerceAtLeast(0)
                val deltaBytes = rxDiff + txDiff
                if (deltaBytes > 0) {
                    net.nexus.app.data.TrafficStatsManager.recordBytes(this@NexusVpnService, deltaBytes)
                }

                val rxSpeedBps = (rxDiff / dtSec).toLong()
                val txSpeedBps = (txDiff / dtSec).toLong()

                val sessionRx = (curRx - baseRx).coerceAtLeast(0)
                val sessionTx = (curTx - baseTx).coerceAtLeast(0)

                lastRx = curRx
                lastTx = curTx
                lastTime = now

                VpnState.updateTraffic(sessionRx, sessionTx, rxSpeedBps, txSpeedBps)

                if (isScreenOn) {
                    val speedText = "↓ ${formatSpeed(rxSpeedBps)}   ↑ ${formatSpeed(txSpeedBps)}"
                    updateLiveNotification(serverName, speedText)
                }
            }
        }
    }

    private fun getTrafficBytes(): Pair<Long, Long> {
        val stats = runCatching { TProxyService.TProxyGetStats() }.getOrNull()
        if (stats != null && stats.size >= 4 && (stats[1] > 0 || stats[3] > 0)) {
            // Native hev-socks5-tunnel stats: [txPackets, txBytes, rxPackets, rxBytes]
            return Pair(stats[3], stats[1])
        }
        val rx = android.net.TrafficStats.getTotalRxBytes()
        val tx = android.net.TrafficStats.getTotalTxBytes()
        val r = if (rx != android.net.TrafficStats.UNSUPPORTED.toLong()) rx
                else android.net.TrafficStats.getUidRxBytes(android.os.Process.myUid()).coerceAtLeast(0)
        val t = if (tx != android.net.TrafficStats.UNSUPPORTED.toLong()) tx
                else android.net.TrafficStats.getUidTxBytes(android.os.Process.myUid()).coerceAtLeast(0)
        return Pair(r, t)
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec < 1024 -> "$bytesPerSec Б/с"
            bytesPerSec < 1024 * 1024 -> String.format(java.util.Locale.US, "%.0f КБ/с", bytesPerSec / 1024f)
            else -> String.format(java.util.Locale.US, "%.1f МБ/с", bytesPerSec / (1024f * 1024f))
        }
    }

    private fun updateLiveNotification(serverName: String, speedText: String) {
        if (!isConnected) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val store = Store(this)
        val avatarRes = when (store.loadSettings().appIcon) {
            AppIcon.MINIMAL_GREY -> R.drawable.ic_avatar_grey
            AppIcon.TOTAL_BLACK -> R.drawable.ic_avatar_black
            AppIcon.INVERSION_MINIMAL -> R.drawable.ic_avatar_inversion
            AppIcon.WHITE_VIOLET -> R.drawable.ic_avatar_violet
            AppIcon.NYA_VPN -> R.drawable.ic_avatar_nya
            else -> R.drawable.ic_avatar_base
        }

        val activeAlias = store.loadSettings().appIcon.aliasName
        val openIntent = Intent().apply {
            component = android.content.ComponentName(packageName, activeAlias)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val open = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getBroadcast(
            this, 1, Intent(this, VpnActionReceiver::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cleanName = net.nexus.app.ui.cleanServerName(serverName)
        val title = "${getString(R.string.app_name)} • $cleanName"

        val builder = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(avatarRes)
            .setContentTitle(title)
            .setContentText(speedText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$cleanName\n$speedText"))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentIntent(open)
            .addAction(0, getString(R.string.notif_disconnect), stop)

        nm.notify(NOTIF_ID, builder.build())
    }


    companion object {
        const val ACTION_START = "net.nexus.app.START"
        const val ACTION_STOP = "net.nexus.app.STOP"
        const val EXTRA_SERVER_ID = "server_id"
        private const val CHANNEL = "vpn_status"
        private const val NOTIF_ID = 1

        private const val TUN_IPV4 = "198.18.0.1"
        private const val TUN_IPV6 = "fdfe:dcba:9876::1"

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
