package net.nexus.app.auth

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object TelegramAuthManager {

    private const val AUTH_BASE_URL = "https://nexus-net.xyz/api/auth/telegram"
    private const val PANEL_URL = "https://panel.nexus-net.xyz"
    private const val PANEL_TOKEN =
        "REDACTED_API_TOKEN"

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
            telegramId = uObj.optLong("telegramChatId", 0L),
            subscriptionUrl = subUrl,
            expireAt = expireAt,
            status = status,
            devicesCount = devicesCount,
        )
    }

    /**
     * Refreshes the subscription and user ID directly from RemnaWave panel for an existing user.
     */
    suspend fun refreshFromRemnaWave(user: TelegramUser): TelegramUser = withContext(Dispatchers.IO) {
        val clean = user.cleanUsername
        val candidates = listOf(clean, "nexus_${clean.lowercase()}", clean.lowercase(), "nexus_$clean")

        // 1. Try finding user directly by username
        for (candidate in candidates) {
            try {
                val url = "$PANEL_URL/api/users/by-username/$candidate"
                val raw = httpPanelGet(url) ?: continue
                val data = JSONObject(raw).optJSONObject("response")
                if (data != null) {
                    val remnaId = data.optLong("id", user.remnaUserId)
                    val subUrl = data.optString("subscriptionUrl", user.subscriptionUrl)
                    val status = data.optString("status", "ACTIVE")
                    val expireAt = data.optString("expireAt", user.expireAt)
                    val devLimit = data.optInt("hwidDeviceLimit", user.devicesCount)
                    return@withContext user.copy(
                        remnaUserId = remnaId,
                        subscriptionUrl = subUrl,
                        status = if (status.equals("ACTIVE", true)) "active" else "disabled",
                        expireAt = expireAt,
                        devicesCount = devLimit,
                    )
                }
            } catch (_: Exception) {}
        }

        // 2. Fallback: Search all users by telegramId if available
        if (user.telegramId > 0) {
            try {
                val listRaw = httpPanelGet("$PANEL_URL/api/users?size=100")
                if (listRaw != null) {
                    val arr = JSONObject(listRaw).optJSONObject("response")?.optJSONArray("users")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            if (obj.optLong("telegramId", 0L) == user.telegramId) {
                                val remnaId = obj.optLong("id", user.remnaUserId)
                                val subUrl = obj.optString("subscriptionUrl", user.subscriptionUrl)
                                val status = obj.optString("status", "ACTIVE")
                                val expireAt = obj.optString("expireAt", user.expireAt)
                                val devLimit = obj.optInt("hwidDeviceLimit", user.devicesCount)
                                return@withContext user.copy(
                                    remnaUserId = remnaId,
                                    subscriptionUrl = subUrl,
                                    status = if (status.equals("ACTIVE", true)) "active" else "disabled",
                                    expireAt = expireAt,
                                    devicesCount = devLimit,
                                )
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        user
    }

    /**
     * Registers the current device in RemnaWave so active sessions appear in the bot and WebApp.
     */
    suspend fun registerDevice(user: TelegramUser, hwid: String): Boolean = withContext(Dispatchers.IO) {
        val userId = user.remnaUserId
        if (userId <= 0) return@withContext false

        try {
            val deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
            val payload = JSONObject().apply {
                put("hwid", hwid)
                put("userId", userId)
                put("platform", "Android")
                put("deviceModel", deviceModel)
                put("osVersion", Build.VERSION.RELEASE ?: "Android")
                put("userAgent", "NEXUS.net App/2.0 (Android)")
            }

            val resp = httpPanelPost("$PANEL_URL/api/hwid/devices", payload.toString())
            resp != null
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Unregisters the current device from RemnaWave on logout.
     */
    suspend fun unregisterDevice(user: TelegramUser, hwid: String): Boolean = withContext(Dispatchers.IO) {
        val userId = user.remnaUserId
        if (userId <= 0) return@withContext false

        try {
            val payload = JSONObject().apply {
                put("hwid", hwid)
                put("userId", userId)
            }
            val resp = httpPanelPost("$PANEL_URL/api/hwid/devices/delete", payload.toString())
            resp != null
        } catch (_: Exception) {
            false
        }
    }

    private fun httpGet(urlStr: String): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 12_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "nexus.net-app/2.0 (Android)")
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

    private fun httpPanelGet(urlStr: String): String? {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 10_000
            setRequestProperty("Authorization", "Bearer $PANEL_TOKEN")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "nexus.net-app/2.0 (Android)")
        }
        return try {
            if (conn.responseCode in 200..299) {
                BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
            } else null
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun httpPanelPost(urlStr: String, jsonBody: String): String? {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 8_000
            readTimeout = 10_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $PANEL_TOKEN")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "nexus.net-app/2.0 (Android)")
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
