package net.nexus.app.auth

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import net.nexus.app.data.Server
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

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

    /**
     * Single BFF call to sync user status and obtain clean server outbounds.
     */
    suspend fun handshake(
        hwid: String,
        username: String? = null,
        telegramId: Long? = null,
        sessionToken: String? = null
    ): HandshakeResult = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("hwid", hwid)
                if (!username.isNullOrBlank()) put("username", username)
                if (telegramId != null && telegramId > 0) put("telegramId", telegramId)
                if (!sessionToken.isNullOrBlank()) put("sessionToken", sessionToken)
                put("clientVersion", net.nexus.app.BuildConfig.VERSION_NAME)
                put("platform", "android")
                put("deviceModel", "${Build.MANUFACTURER} ${Build.MODEL}")
            }
            val resp = httpPost("https://nexus-net.xyz/api/v1/client/handshake", payload.toString())
                ?: return@withContext HandshakeResult(success = false, authenticated = false, user = null, servers = emptyList())
            val json = JSONObject(resp)
            if (!json.optBoolean("success", false)) {
                return@withContext HandshakeResult(success = false, authenticated = false, user = null, servers = emptyList())
            }
            val auth = json.optBoolean("authenticated", false)
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
        val jsonStr = httpGet("$AUTH_BASE_URL?action=check&session=$sessionId")
        val json = JSONObject(jsonStr)

        if (!json.optBoolean("success", false) || !json.optBoolean("confirmed", false)) {
            return@withContext null
        }

        val uObj = json.optJSONObject("user") ?: return@withContext null
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
        )
    }

    /**
     * Refreshes the subscription and user info via secure backend API (zero secrets on device).
     */
    suspend fun refreshFromRemnaWave(user: TelegramUser): TelegramUser = withContext(Dispatchers.IO) {
        try {
            val clean = user.cleanUsername
            val url = "$AUTH_BASE_URL?action=refresh_user&username=$clean&telegramId=${user.telegramId}"
            val resp = httpGet(url)
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

            val resp = httpPost(AUTH_BASE_URL, payload.toString())
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
            val resp = httpPost(AUTH_BASE_URL, payload.toString())
            resp != null && JSONObject(resp).optBoolean("success", false)
        } catch (_: Exception) {
            false
        }
    }

    private fun httpGet(urlStr: String): String {
        if (!net.nexus.app.security.UrlValidator.isSecureUrl(urlStr, allowLoopback = false)) error("HTTPS is required for secure authentication")
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 12_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "nexus.net-app/${net.nexus.app.BuildConfig.VERSION_NAME} (Android)")
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) error("Server error HTTP $code")
            return BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun httpPost(urlStr: String, jsonBody: String): String? {
        if (!net.nexus.app.security.UrlValidator.isSecureUrl(urlStr, allowLoopback = false)) error("HTTPS is required for secure authentication")
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 12_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "nexus.net-app/${net.nexus.app.BuildConfig.VERSION_NAME} (Android)")
        }
        return try {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(jsonBody) }
            if (conn.responseCode in 200..299) {
                BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
            } else null
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }
}
