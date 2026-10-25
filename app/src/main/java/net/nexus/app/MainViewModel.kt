package net.nexus.app

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import net.nexus.app.core.ConfigBuilder
import net.nexus.app.core.XrayCore
import net.nexus.app.data.FetchResult
import net.nexus.app.data.Provider
import net.nexus.app.data.Server
import net.nexus.app.data.Settings
import net.nexus.app.data.SplitMode
import net.nexus.app.data.Store
import net.nexus.app.data.SubscriptionFetcher
import kotlinx.coroutines.Job
import net.nexus.app.vpn.NexusVpnService
import net.nexus.app.vpn.VpnState
import net.nexus.app.vpn.VpnStatus
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL

import net.nexus.app.auth.TelegramAuthManager
import net.nexus.app.auth.TelegramAuthState
import net.nexus.app.auth.TelegramUser
import net.nexus.app.update.AppReleaseInfo
import net.nexus.app.update.UpdateChecker
import net.nexus.app.update.UpdateDownloader
import net.nexus.app.update.UpdateInstaller
import net.nexus.app.update.UpdateState
import java.io.File
import kotlinx.coroutines.CancellationException

/** A message for the UI, resolved to text in the activity so it follows the app language. */
data class UiText(@StringRes val id: Int, val args: List<Any> = emptyList())

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)

    val providers = MutableStateFlow(store.loadProviders())
    val settings = MutableStateFlow(store.loadSettings())
    val events = MutableSharedFlow<UiText>(extraBufferCapacity = 8)

    val refreshing = MutableStateFlow<Set<String>>(emptySet())
    val pinging = MutableStateFlow<Set<String>>(emptySet())
    val adding = MutableStateFlow(false)
    /** Last speed-test result in Mbit/s (null = not measured). Measured only when the user taps the tile. */
    val speedMbps = MutableStateFlow<Double?>(null)
    val speedTesting = MutableStateFlow(false)

    private val pingGate = Semaphore(4)

    val updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    private var downloadJob: Job? = null

    val telegramUser = MutableStateFlow<TelegramUser?>(store.loadTelegramUser())
    val telegramAuthState = MutableStateFlow<TelegramAuthState>(TelegramAuthState.Idle)
    val isSyncingTelegram = MutableStateFlow(false)
    private var telegramAuthJob: Job? = null

    init {
        ensureSelection()
        refreshAllSubscriptions(quiet = true)
        startPeriodicHourlyRefresh()
        if (telegramUser.value != null) {
            viewModelScope.launch {
                syncTelegramSubscription(quiet = true)
            }
        }
        viewModelScope.launch {
            VpnState.status.collect { st ->
                if (st == VpnStatus.CONNECTED) checkSelected() else speedMbps.value = null
            }
        }
        if (settings.value.autoCheckUpdates) {
            viewModelScope.launch {
                delay(2500)
                checkForUpdates(manual = false)
            }
        }
    }

    // ---------- state helpers ----------

    private fun mutate(f: (List<Provider>) -> List<Provider>) {
        providers.update(f)
        store.saveProviders(providers.value)
    }

    private fun updateSettings(f: (Settings) -> Settings) {
        settings.update(f)
        store.saveSettings(settings.value)
    }

    private fun emit(@StringRes id: Int, vararg args: Any) {
        events.tryEmit(UiText(id, args.toList()))
    }

    fun allServers(): List<Server> = providers.value.flatMap { it.servers }

    private fun ensureSelection() {
        val sel = settings.value.selectedServerId
        if (sel == null || allServers().none { it.id == sel }) {
            updateSettings { it.copy(selectedServerId = allServers().firstOrNull()?.id) }
        }
    }

    // ---------- adding / refreshing ----------

    fun addInput(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            adding.value = true
            try {
                var t = text.trim()
                if (t.startsWith("chad.nexus-net.xyz", ignoreCase = true)) {
                    t = "https://$t"
                }
                if (t.startsWith("http://", true) || t.startsWith("https://", true)) {
                    val url = t.lineSequence().first().trim()
                    val host = runCatching { URL(url).host }.getOrNull()?.lowercase() ?: ""
                    if (host != "chad.nexus-net.xyz") {
                        emit(R.string.error_only_nexus_sub)
                        return@launch
                    }
                    val r = SubscriptionFetcher.fetch(url)
                    upsertSubscription(url, r)
                    emit(R.string.add_ok, r.servers.size)
                } else {
                    val servers = SubscriptionFetcher.parseBody(t)
                    if (servers.isEmpty()) {
                        emit(R.string.add_fail)
                    } else {
                        addLocal(servers)
                        emit(R.string.add_ok, servers.size)
                    }
                }
            } catch (e: Exception) {
                emit(R.string.refresh_failed, e.message ?: e.javaClass.simpleName)
            } finally {
                adding.value = false
            }
        }
    }

    private fun addLocal(servers: List<Server>) {
        mutate { list ->
            val local = list.firstOrNull { it.isLocal }
                ?: Provider(id = "local", name = getApplication<Application>().getString(R.string.local_provider))
            val updated = local.copy(servers = local.servers + servers, updatedAt = System.currentTimeMillis())
            if (list.any { it.isLocal }) list.map { if (it.isLocal) updated else it } else list + updated
        }
        ensureSelection()
    }

    /** Keeps ids (and therefore the selected server and ping values) stable across refreshes. */
    private fun mergeServers(old: List<Server>, new: List<Server>): List<Server> {
        val byName = old.groupBy { it.name }.mapValues { it.value.toMutableList() }
        return new.map { n ->
            val match = byName[n.name]?.removeFirstOrNull()
            if (match != null) n.copy(id = match.id, pingMs = match.pingMs) else n
        }
    }

    private fun upsertSubscription(url: String, r: FetchResult) {
        mutate { list ->
            val existing = list.firstOrNull { it.url == url }
            val p = (existing ?: Provider(id = java.util.UUID.randomUUID().toString(), name = r.name, url = url)).copy(
                name = r.name,
                servers = mergeServers(existing?.servers ?: emptyList(), r.servers),
                upload = r.upload, download = r.download, total = r.total, expireAt = r.expireAt,
                announce = r.announce, channelUrl = r.channelUrl, supportUrl = r.supportUrl, siteUrl = r.siteUrl,
                updatedAt = System.currentTimeMillis(),
            )
            if (existing != null) list.map { if (it.id == existing.id) p else it } else list + p
        }
        ensureSelection()
    }

    fun refresh(providerId: String, quiet: Boolean = false) {
        val p = providers.value.firstOrNull { it.id == providerId } ?: return
        val url = p.url ?: return
        if (providerId in refreshing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            refreshing.update { it + providerId }
            try {
                val r = SubscriptionFetcher.fetch(url)
                upsertSubscription(url, r)
                if (!quiet) emit(R.string.refreshed, r.servers.size)
            } catch (e: Exception) {
                if (!quiet) emit(R.string.refresh_failed, e.message ?: e.javaClass.simpleName)
            } finally {
                refreshing.update { it - providerId }
            }
        }
    }

    fun refreshAllSubscriptions(quiet: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            providers.value.filter { !it.isLocal && !it.url.isNullOrBlank() }.forEach { p ->
                val url = p.url ?: return@forEach
                try {
                    val r = SubscriptionFetcher.fetch(url)
                    upsertSubscription(url, r)
                } catch (e: Exception) {
                    if (!quiet) emit(R.string.refresh_failed, e.message ?: e.javaClass.simpleName)
                }
            }
        }
    }

    private fun startPeriodicHourlyRefresh() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(3600_000L) // 1 hour
                refreshAllSubscriptions(quiet = true)
            }
        }
    }

    private fun refreshStale() {
        val limit = 3600_000L
        providers.value.filter { it.url != null && System.currentTimeMillis() - it.updatedAt > limit }
            .forEach { refresh(it.id, quiet = true) }
    }

    // ---------- editing ----------

    private var switchJob: Job? = null

    fun select(serverId: String) {
        val oldId = settings.value.selectedServerId
        if (oldId == serverId) return

        updateSettings { it.copy(selectedServerId = serverId) }

        val isVpnActive = VpnState.status.value == VpnStatus.CONNECTED ||
                          VpnState.status.value == VpnStatus.CONNECTING ||
                          NexusVpnService.instance?.isConnected == true

        if (isVpnActive) {
            switchJob?.cancel()
            switchJob = viewModelScope.launch(Dispatchers.Main) {
                // 1. Отключаем текущее соединение
                NexusVpnService.stop(getApplication())

                // 2. Ждем 2 секунды
                delay(2000L)

                // 3. Подключаемся к выбранному серверу
                NexusVpnService.start(getApplication(), serverId)
            }
        }
    }

    fun toggleExpanded(providerId: String) =
        mutate { l -> l.map { if (it.id == providerId) it.copy(expanded = !it.expanded) else it } }

    fun deleteProvider(providerId: String) {
        mutate { l -> l.filterNot { it.id == providerId } }
        ensureSelection()
    }

    fun deleteServer(serverId: String) {
        mutate { l -> l.map { p -> p.copy(servers = p.servers.filterNot { it.id == serverId }) }
            .filter { !it.isLocal || it.servers.isNotEmpty() } }
        ensureSelection()
    }

    // ---------- latency ----------

    fun pingServers(servers: List<Server>) {
        if (servers.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val ids = servers.map { it.id }.toSet()
            pinging.update { it + ids }
            runCatching { XrayCore.init(getApplication()) }
            val url = settings.value.testUrl
            servers.map { s ->
                async {
                    pingGate.withPermit {
                        val ms = runCatching { XrayCore.measure(ConfigBuilder.forPing(s), url).toInt() }
                            .getOrNull()?.takeIf { it > 0 } ?: -2
                        mutate { l -> l.map { p -> p.copy(servers = p.servers.map { if (it.id == s.id) it.copy(pingMs = ms) else it }) } }
                        pinging.update { it - s.id }
                        s.id to ms
                    }
                }
            }.awaitAll()
            pinging.update { it - ids }
        }
    }

    fun pingProvider(providerId: String) {
        providers.value.firstOrNull { it.id == providerId }?.let { pingServers(it.servers) }
    }

    /** Re-measures the selected server's latency (also runs automatically after connecting). */
    fun checkSelected() {
        val s = allServers().firstOrNull { it.id == settings.value.selectedServerId } ?: return
        if (s.id in pinging.value) return
        pingServers(listOf(s))
    }

    // ---------- settings ----------

    fun setSplitMode(m: SplitMode) = updateSettings { it.copy(splitMode = m) }
    fun toggleSplitApp(pkg: String) =
        updateSettings { it.copy(splitApps = if (pkg in it.splitApps) it.splitApps - pkg else it.splitApps + pkg) }
    fun setTestUrl(url: String) = updateSettings { it.copy(testUrl = url.trim().ifEmpty { Settings().testUrl }) }
    fun setFontScale(scale: Float) = updateSettings { it.copy(fontScale = scale) }
    fun setAutoCheckUpdates(enabled: Boolean) = updateSettings { it.copy(autoCheckUpdates = enabled) }

    // ---------- updates ----------

    fun checkForUpdates(manual: Boolean = false) {
        val current = updateState.value
        if (current is UpdateState.Checking || current is UpdateState.Downloading) return

        viewModelScope.launch(Dispatchers.IO) {
            updateState.value = UpdateState.Checking
            try {
                val release = UpdateChecker.check()
                if (release != null) {
                    updateState.value = UpdateState.Available(release, autoPrompt = true)
                } else {
                    updateState.value = UpdateState.UpToDate
                    if (manual) {
                        emit(R.string.update_already_latest, BuildConfig.VERSION_NAME)
                    }
                }
            } catch (e: Exception) {
                updateState.value = UpdateState.Error(e.message ?: "Update check failed", isManual = manual)
                if (manual) {
                    emit(R.string.update_check_failed, e.message ?: "")
                }
            }
        }
    }

    fun startDownloadUpdate(release: AppReleaseInfo) {
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch(Dispatchers.IO) {
            updateState.value = UpdateState.Downloading(release, 0f, 0L, release.apkSize)
            try {
                val file = UpdateDownloader.download(getApplication(), release) { prog, read, total ->
                    updateState.value = UpdateState.Downloading(release, prog, read, total)
                }
                updateState.value = UpdateState.ReadyToInstall(release, file)
            } catch (e: CancellationException) {
                updateState.value = UpdateState.Available(release, autoPrompt = false)
            } catch (e: Exception) {
                updateState.value = UpdateState.Error(e.message ?: "Download failed", isManual = true)
                emit(R.string.update_check_failed, e.message ?: "")
            }
        }
    }

    fun cancelUpdateDownload() {
        downloadJob?.cancel()
        downloadJob = null
        val cur = updateState.value
        if (cur is UpdateState.Downloading) {
            updateState.value = UpdateState.Available(cur.release, autoPrompt = false)
        }
    }

    fun installUpdate(apkFile: File) {
        try {
            UpdateInstaller.installApk(getApplication(), apkFile)
        } catch (e: Exception) {
            emit(R.string.update_check_failed, e.message ?: "")
        }
    }

    fun dismissUpdateDialog() {
        val cur = updateState.value
        if (cur is UpdateState.Available) {
            updateState.value = cur.copy(autoPrompt = false)
        } else {
            updateState.value = UpdateState.Idle
        }
    }

    fun showUpdateDialog() {
        val cur = updateState.value
        if (cur is UpdateState.Available) {
            updateState.value = cur.copy(autoPrompt = true)
        }
    }

    // ---------- speed (only on tap) ----------

    /**
     * Downloads a test file through the core's loopback SOCKS inbound, i.e. through the VPN outbound
     * (the app itself is excluded from the TUN, so a plain request would measure the direct connection).
     */
    fun runSpeedTest() {
        if (speedTesting.value) return
        val port = VpnState.socksPort
        if (VpnState.status.value != VpnStatus.CONNECTED || port <= 0) {
            emit(R.string.speed_need_connect)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            speedTesting.value = true
            try {
                val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", port))
                val conn = URL("https://speed.cloudflare.com/__down?bytes=30000000").openConnection(proxy) as HttpURLConnection
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                var total = 0L
                val start = System.nanoTime()
                val deadline = start + 8_000_000_000L
                try {
                    conn.inputStream.use { ins ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = ins.read(buf)
                            if (n < 0) break
                            total += n
                            if (System.nanoTime() > deadline) break
                        }
                    }
                } finally {
                    conn.disconnect()
                }
                val seconds = (System.nanoTime() - start) / 1e9
                if (total <= 0 || seconds <= 0) error("no data")
                speedMbps.value = total * 8 / 1e6 / seconds
            } catch (e: Exception) {
                speedMbps.value = null
                emit(R.string.speed_failed)
            } finally {
                speedTesting.value = false
            }
        }
    }

    // ---------- Telegram Auth & Sync ----------

    fun startTelegramAuth() {
        telegramAuthJob?.cancel()
        telegramAuthState.value = TelegramAuthState.CreatingSession
        telegramAuthJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val (sessionId, botUrl) = TelegramAuthManager.createLoginSession()
                telegramAuthState.value = TelegramAuthState.WaitingConfirmation(sessionId, botUrl)

                // Poll every 2 seconds for up to 3 minutes (90 iterations)
                var confirmedUser: TelegramUser? = null
                for (i in 0 until 90) {
                    delay(2000)
                    val u = TelegramAuthManager.checkLoginSession(sessionId)
                    if (u != null) {
                        confirmedUser = u
                        break
                    }
                }

                if (confirmedUser == null) {
                    telegramAuthState.value = TelegramAuthState.Error("Время ожидания подтверждения истекло")
                    return@launch
                }

                var serverCount = 0
                var finalUser = confirmedUser

                try {
                    finalUser = TelegramAuthManager.refreshFromRemnaWave(confirmedUser)
                } catch (_: Exception) {}

                telegramUser.value = finalUser
                store.saveTelegramUser(finalUser)

                if (finalUser.subscriptionUrl.isNotBlank()) {
                    try {
                        val r = SubscriptionFetcher.fetch(finalUser.subscriptionUrl)
                        upsertSubscription(finalUser.subscriptionUrl, r)
                        serverCount = r.servers.size
                    } catch (_: Exception) {}
                }

                telegramAuthState.value = TelegramAuthState.Success(finalUser, serverCount)
                emit(R.string.tg_auth_success, finalUser.tgUsername)
            } catch (e: CancellationException) {
                // User cancelled or dismissed
            } catch (e: Exception) {
                telegramAuthState.value = TelegramAuthState.Error(e.message ?: "Ошибка авторизации")
            }
        }
    }

    fun dismissTelegramAuthDialog() {
        telegramAuthJob?.cancel()
        telegramAuthJob = null
        telegramAuthState.value = TelegramAuthState.Idle
    }

    fun syncTelegramSubscription(quiet: Boolean = false) {
        val user = telegramUser.value ?: return
        if (isSyncingTelegram.value) return
        viewModelScope.launch(Dispatchers.IO) {
            isSyncingTelegram.value = true
            try {
                val updatedUser = TelegramAuthManager.refreshFromRemnaWave(user)
                telegramUser.value = updatedUser
                store.saveTelegramUser(updatedUser)

                if (updatedUser.subscriptionUrl.isNotBlank()) {
                    val r = SubscriptionFetcher.fetch(updatedUser.subscriptionUrl)
                    upsertSubscription(updatedUser.subscriptionUrl, r)
                    if (!quiet) {
                        emit(R.string.tg_sync_success, r.servers.size)
                    }
                } else if (!quiet) {
                    emit(R.string.tg_no_active_sub)
                }
            } catch (e: Exception) {
                if (!quiet) {
                    emit(R.string.tg_sync_failed, e.message ?: "Ошибка синхронизации")
                }
            } finally {
                isSyncingTelegram.value = false
            }
        }
    }

    fun logoutTelegram() {
        val user = telegramUser.value ?: return
        if (user.subscriptionUrl.isNotBlank()) {
            mutate { list -> list.filterNot { it.url == user.subscriptionUrl } }
            ensureSelection()
        }
        telegramUser.value = null
        store.saveTelegramUser(null)
        emit(R.string.tg_auth_logged_out)
    }
}

