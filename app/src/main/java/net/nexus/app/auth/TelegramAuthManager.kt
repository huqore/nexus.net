package net.nexus.app.auth

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.dnsoverhttps.DnsOverHttps
import org.json.JSONArray
import org.json.JSONObject
import net.nexus.app.data.Server
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

data class HandshakeResult(
    val success: Boolean,
    val authenticated: Boolean,
    val user: TelegramUser?,
    val servers: List<Server>,
    val motd: String? = null,
)

data class DeviceRegisterResult(
    val success: Boolean,
    val trialStatus: String? = null,
    val message: String? = null,
    val subscriptionUrl: String? = null,
)

object TelegramAuthManager {

    private const val AUTH_BASE_URL = "https://nexus-net.xyz/api/auth/telegram"
    private val authHttpClient: OkHttpClient by lazy {
        val cloudflareDoh = createDohResolver(
            "https://cloudflare-dns.com/dns-query",
            "cloudflare-dns.com",
            listOf("1.1.1.1", "1.0.0.1")
        )
        val googleDoh = createDohResolver(
            "https://dns.google/dns-query",
            "dns.google",
            listOf("8.8.8.8", "8.8.4.4")
        )
        val fallbackDns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                try {
                    return Dns.SYSTEM.lookup(hostname).ifEmpty { throw UnknownHostException("No addresses for $hostname") }
                } catch (systemDnsError: UnknownHostException) {
                    if (!hostname.equals("nexus-net.xyz", ignoreCase = true)) throw systemDnsError
                    return runCatching { cloudflareDoh.lookup(hostname) }
                        .getOrElse { runCatching { googleDoh.lookup(hostname) }.getOrElse { throw systemDnsError } }
                }
            }
        }
        OkHttpClient.Builder()
            .dns(fallbackDns)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private fun createDohResolver(url: String, resolverHost: String, bootstrapIps: List<String>): DnsOverHttps {
        val bootstrapAddresses = bootstrapIps.map { InetAddress.getByName(it) }
        val bootstrapClient = OkHttpClient.Builder()
            .dns(object : Dns {
                override fun lookup(hostname: String): List<InetAddress> =
                    if (hostname.equals(resolverHost, ignoreCase = true)) bootstrapAddresses
                    else Dns.SYSTEM.lookup(hostname)
            })
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
        return DnsOverHttps.Builder()
            .client(bootstrapClient)
            .url(url.toHttpUrl())
            .build()
    }

    /**
     * Single BFF call to sync user status and obtain clean server outbounds.
     */
    suspend fun handshake(
        hwid: String,
        username: String? = null,
        telegramId: Long? = null,
        sessionToken: String? = null,
        appToken: String = ""
    ): HandshakeResult = withContext(Dispatchers.IO) {
        try {
            val cleanName = username?.removePrefix("@")?.removePrefix("nexus_")?.trim()
            val deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
            val payload = JSONObject().apply {
                put("hwid", hwid)
                if (!cleanName.isNullOrBlank()) put("username", cleanName)
                if (telegramId != null && telegramId > 0) put("telegramId", telegramId)
                if (!sessionToken.isNullOrBlank()) put("sessionToken", sessionToken)
                put("clientVersion", net.nexus.app.BuildConfig.VERSION_NAME)
                put("platform", "android")
                put("deviceModel", deviceModel)
                put("osVersion", "Android ${Build.VERSION.RELEASE ?: ""}".trim())
                put("userAgent", "NEXUS.net App/${net.nexus.app.BuildConfig.VERSION_NAME} (Android; $deviceModel)")
            }
            val resp = httpPost("https://nexus-net.xyz/api/v1/client/handshake", payload.toString(), appToken = appToken)
                ?: return@withContext HandshakeResult(success = false, authenticated = false, user = null, servers = emptyList())
            val json = JSONObject(resp)
            if (!json.optBoolean("success", false)) {
                return@withContext HandshakeResult(success = false, authenticated = false, user = null, servers = emptyList())
            }
            val auth = json.optBoolean("authenticated", false)
            val isKicked = json.optBoolean("kicked", false) || (!auth && json.optBoolean("requiresAuth", false))
            if (isKicked) {
                return@withContext HandshakeResult(success = true, authenticated = false, user = null, servers = emptyList())
            }
            val uObj = json.optJSONObject("user")
            val user = if (uObj != null) {
                TelegramUser(
                    tgUsername = uObj.optString("username", "").removePrefix("@").removePrefix("nexus_").trim(),
                    siteName = uObj.optString("username", ""),
                    telegramId = uObj.optLong("telegramId", 0L),
                    subscriptionUrl = uObj.optString("subscriptionUrl", "").ifBlank { "https://nexus-net.xyz" },
                    expireAt = uObj.optString("expireAt", ""),
                    status = uObj.optString("status", "none"),
                    devicesCount = uObj.optInt("deviceLimit", 3),
                    activeDevicesCount = uObj.optInt("activeDevices", uObj.optInt("activeDevicesCount", 1)).coerceAtLeast(1),
                    usedTrafficBytes = uObj.optLong("usedTrafficBytes", 0L),
                    trafficLimitBytes = uObj.optLong("trafficLimitBytes", 0L),
                )
            } else null

            val serversArr = json.optJSONArray("servers") ?: JSONArray()
            val servers = mutableListOf<Server>()
            for (i in 0 until serversArr.length()) {
                val s = serversArr.getJSONObject(i)
                val outboundObj = s.optJSONObject("outboundConfig")
                val streamSettings = outboundObj?.optJSONObject("streamSettings")
                val net = streamSettings?.optString("network", "tcp") ?: "tcp"
                val sec = streamSettings?.optString("security", "reality") ?: "reality"
                servers.add(
                    Server(
                        id = s.getString("id"),
                        name = s.getString("name"),
                        protocol = s.optString("protocol", "vless"),
                        network = net,
                        security = sec,
                        source = "BFF",
                        outbounds = if (outboundObj != null) JSONArray().put(outboundObj).toString() else null,
                        fullConfig = null,
                        pingMs = -1
                    )
                )
            }
            val appConfig = json.optJSONObject("appConfig")
            val motd = if (appConfig?.isNull("motd") == false) appConfig.optString("motd") else null

            HandshakeResult(
                success = true,
                authenticated = auth,
                user = user,
                servers = servers,
                motd = motd
            )
        } catch (e: Exception) {
            HandshakeResult(success = false, authenticated = false, user = null, servers = emptyList())
        }
    }

    /**
     * Fast check to see if the device session is still active in RemnaWave.
     * Returns false if the device was kicked / unlinked by user from bot or website.
     */
    suspend fun validateDeviceSession(
        hwid: String,
        username: String? = null,
        telegramId: Long? = null,
        appToken: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanName = username?.removePrefix("@")?.removePrefix("nexus_")?.trim() ?: ""
            val tgIdStr = if (telegramId != null && telegramId > 0) telegramId.toString() else ""
            val encHwid = java.net.URLEncoder.encode(hwid, "UTF-8")
            val encUser = java.net.URLEncoder.encode(cleanName, "UTF-8")
            val url = "https://nexus-net.xyz/api/v1/client/session-check?hwid=$encHwid&username=$encUser&telegramId=$tgIdStr"
            val resp = httpGet(url, appToken = appToken)
            val json = JSONObject(resp)
            if (json.optBoolean("kicked", false)) {
                return@withContext false
            }
            json.optBoolean("valid", false)
        } catch (_: Exception) {
            // Keep active on network timeouts to prevent flickering
            true
        }
    }

    /**
     * Authenticates with website credentials (login and password).
     */
    suspend fun loginWithWebsite(
        identifier: String,
        pass: String
    ): WebLoginResult = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("identifier", identifier.trim())
                put("password", pass)
            }
            val resp = httpPost("https://nexus-net.xyz/api/auth/login", payload.toString())
                ?: return@withContext WebLoginResult(success = false, error = "Нет ответа от сервера")
            val json = JSONObject(resp)
            if (!json.optBoolean("success", false)) {
                return@withContext WebLoginResult(
                    success = false,
                    error = json.optString("message", "Неверный логин или пароль")
                )
            }
            val token = json.optString("token", "")
            val uObj = json.optJSONObject("user")
                ?: return@withContext WebLoginResult(success = false, error = "Некорректный ответ сервера")

            val subObj = uObj.optJSONObject("subscription")
            val rawName = uObj.optString("siteName", "").ifBlank { uObj.optString("tgUsername", identifier) }
            val cleanName = rawName.removePrefix("@").trim()
            val user = TelegramUser(
                tgUsername = cleanName,
                siteName = uObj.optString("siteName", cleanName),
                telegramId = uObj.optLong("telegramChatId", 0L).let { if (it > 0) it else uObj.optLong("telegramId", 0L) },
                subscriptionUrl = subObj?.optString("subscriptionUrl", "") ?: "",
                expireAt = subObj?.optString("expireAt", "") ?: "",
                status = subObj?.optString("status", "active") ?: "active",
                devicesCount = subObj?.optInt("devicesCount", 3) ?: 3,
                activeDevicesCount = 1,
                usedTrafficBytes = subObj?.optLong("usedTrafficBytes", 0L) ?: 0L,
                trafficLimitBytes = subObj?.optLong("trafficLimitBytes", 0L) ?: 0L,
            )
            WebLoginResult(success = true, token = token, user = user.copy(appToken = json.optString("appToken", "")))
        } catch (e: Exception) {
            WebLoginResult(success = false, error = e.message ?: "Ошибка подключения")
        }
    }


    /**
     * Creates a login session with @NEXUSnet_robot.
     * Returns Pair(sessionId, botUrl).
     */
    suspend fun createLoginSession(): Pair<String, String> = withContext(Dispatchers.IO) {
        val jsonStr = httpGet("$AUTH_BASE_URL?action=create&app=true")
        val json = JSONObject(jsonStr)
        if (!json.optBoolean("success", false)) {
            error(json.optString("message", "Failed to create session"))
        }

        val sessionId = json.getString("sessionId")
        val botUrl = json.optString("botUrl", "").ifBlank {
            "https://t.me/NEXUSnet_robot?start=login_$sessionId"
        }
        Pair(sessionId, botUrl)
    }

    /**
     * Checks if the session was confirmed by the Telegram bot.
     * Returns [TelegramUser] if confirmed, or null if still pending.
     */
    suspend fun checkLoginSession(sessionId: String): TelegramUser? = withContext(Dispatchers.IO) {
        runCatching {
            val jsonStr = httpGet("$AUTH_BASE_URL?action=check&session=$sessionId", timeoutMs = 5000)
            val json = JSONObject(jsonStr)

            if (!json.optBoolean("success", false) || !json.optBoolean("confirmed", false)) {
                return@runCatching null
            }

            val uObj = json.optJSONObject("user") ?: return@runCatching null
            val subObj = uObj.optJSONObject("subscription")

            val subUrl = subObj?.optString("subscriptionUrl", "") ?: ""
            val status = subObj?.optString("status", "none") ?: "none"
            val expireAt = subObj?.optString("expireAt", "") ?: ""
            val devicesCount = subObj?.optInt("devicesCount", 3) ?: 3

            val rawUsername = uObj.optString("tgUsername", "").ifBlank { uObj.optString("siteName", "") }
            TelegramUser(
                tgUsername = rawUsername.removePrefix("@").trim(),
                siteName = uObj.optString("siteName", ""),
                telegramId = uObj.optLong("telegramChatId", 0L).let { if (it > 0) it else uObj.optLong("telegramId", 0L) },
                subscriptionUrl = subUrl,
                expireAt = expireAt,
                status = status,
                devicesCount = devicesCount,
                activeDevicesCount = subObj?.optInt("activeDevices", subObj.optInt("activeDevicesCount", 1))?.coerceAtLeast(1) ?: 1,
                usedTrafficBytes = subObj?.optLong("usedTrafficBytes", 0L) ?: 0L,
                trafficLimitBytes = subObj?.optLong("trafficLimitBytes", 0L) ?: 0L,
                appToken = json.optString("appToken", ""),
            )
        }.getOrNull()
    }

    /**
     * Refreshes the subscription and user info via secure backend API (zero secrets on device).
     */
    suspend fun refreshFromRemnaWave(user: TelegramUser): TelegramUser = withContext(Dispatchers.IO) {
        try {
            val clean = user.cleanUsername
            val url = "$AUTH_BASE_URL?action=refresh_user&username=$clean&telegramId=${user.telegramId}"
            val resp = httpGet(url, appToken = user.appToken)
            val json = JSONObject(resp)
            if (json.optBoolean("success", false)) {
                val uObj = json.optJSONObject("user") ?: return@withContext user
                val subObj = uObj.optJSONObject("subscription")
                val subUrl = subObj?.optString("subscriptionUrl", user.subscriptionUrl) ?: user.subscriptionUrl
                val status = subObj?.optString("status", user.status) ?: user.status
                val expireAt = subObj?.optString("expireAt", user.expireAt) ?: user.expireAt
                val devicesCount = subObj?.optInt("devicesCount", user.devicesCount) ?: user.devicesCount
                return@withContext user.copy(
                    subscriptionUrl = subUrl,
                    status = if (status.equals("active", true)) "active" else "disabled",
                    expireAt = expireAt,
                    devicesCount = devicesCount,
            activeDevicesCount = subObj?.optInt("activeDevices", subObj.optInt("activeDevicesCount", 1))?.coerceAtLeast(1) ?: 1,
                    usedTrafficBytes = subObj?.optLong("usedTrafficBytes", user.usedTrafficBytes) ?: user.usedTrafficBytes,
                    trafficLimitBytes = subObj?.optLong("trafficLimitBytes", user.trafficLimitBytes) ?: user.trafficLimitBytes,
                )
            }
        } catch (_: Exception) {}
        user
    }

    /**
     * Registers the current device session via secure backend API (zero secrets on device).
     */
    suspend fun registerDevice(user: TelegramUser, hwid: String): DeviceRegisterResult = withContext(Dispatchers.IO) {
        try {
            val deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
            val payload = JSONObject().apply {
                put("action", "register_device")
                put("username", user.cleanUsername)
                put("telegramId", user.telegramId)
                put("hwid", hwid)
                put("platform", "Android")
                put("deviceModel", deviceModel)
                put("osVersion", Build.VERSION.RELEASE ?: "Android")
                put("userAgent", "NEXUS.net App/${net.nexus.app.BuildConfig.VERSION_NAME} (Android)")
            }

            val resp = httpPost(AUTH_BASE_URL, payload.toString(), appToken = user.appToken)
            if (resp != null) {
                val json = JSONObject(resp)
                val success = json.optBoolean("success", false)
                val trialStatus = json.optString("trialStatus", "none")
                val message = json.optString("message", "")
                val subObj = json.optJSONObject("subscription")
                val subUrl = subObj?.optString("subscriptionUrl", "")

                DeviceRegisterResult(
                    success = success,
                    trialStatus = trialStatus,
                    message = message,
                    subscriptionUrl = subUrl,
                )
            } else {
                DeviceRegisterResult(success = false)
            }
        } catch (_: Exception) {
            DeviceRegisterResult(success = false)
        }
    }

    /**
     * Unregisters the current device from active sessions on logout.
     */
    suspend fun unregisterDevice(user: TelegramUser, hwid: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("action", "unregister_device")
                put("username", user.cleanUsername)
                put("telegramId", user.telegramId)
                put("hwid", hwid)
            }
            val resp = httpPost(AUTH_BASE_URL, payload.toString(), appToken = user.appToken)
            resp != null && JSONObject(resp).optBoolean("success", false)
        } catch (_: Exception) {
            false
        }
    }

    private fun httpGet(urlStr: String, timeoutMs: Int = 8_000, appToken: String = ""): String {
        if (!net.nexus.app.security.UrlValidator.isSecureUrl(urlStr, allowLoopback = false)) error("HTTPS is required for secure authentication")
        val request = Request.Builder()
            .url(urlStr)
            .header("User-Agent", "nexus.net-app/${net.nexus.app.BuildConfig.VERSION_NAME} (Android)")
            .header("Accept", "application/json")
            .apply { if (appToken.isNotBlank()) header("X-App-Token", appToken) }
            .get()
            .build()
        authHttpClient.newBuilder().readTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS).build().newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Server error HTTP ${response.code}")
            return response.body?.string() ?: ""
        }
    }

    private fun httpPost(urlStr: String, jsonBody: String, appToken: String = ""): String? {
        if (!net.nexus.app.security.UrlValidator.isSecureUrl(urlStr, allowLoopback = false)) error("HTTPS is required for secure authentication")
        return try {
            val request = Request.Builder()
                .url(urlStr)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "nexus.net-app/${net.nexus.app.BuildConfig.VERSION_NAME} (Android)")
                .apply { if (appToken.isNotBlank()) header("X-App-Token", appToken) }
                .post(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            authHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
