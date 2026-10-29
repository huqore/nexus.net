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

data class DeviceRegisterResult(
    val success: Boolean,
    val trialStatus: String? = null,
    val message: String? = null,
    val subscriptionUrl: String? = null,
)

object TelegramAuthManager {

    private const val AUTH_BASE_URL = "https://nexus-net.xyz/api/auth/telegram"

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
