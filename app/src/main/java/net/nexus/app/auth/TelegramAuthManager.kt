package net.nexus.app.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object TelegramAuthManager {

    private const val AUTH_BASE_URL = "https://nexus-net.xyz/api/auth/telegram"
    private const val PANEL_URL = "https://panel.nexus.net"
    private const val PANEL_TOKEN =
        "REDACTED_API_TOKEN"

    /**
     * Creates a login session with @NEXUSnet_robot.
     * Returns Pair(sessionId, botUrl).
     */
    suspend fun createLoginSession(): Pair<String, String> = withContext(Dispatchers.IO) {
        val jsonStr = httpGet("$AUTH_BASE_URL?action=create")
        val json = JSONObject(jsonStr)
        if (!json.optBoolean("success", false)) {
            error(json.optString("message", "Failed to create session"))
        }

        val sessionId = json.getString("sessionId")
        val botUrl = json.getString("botUrl")
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

        TelegramUser(
            tgUsername = uObj.optString("tgUsername", "").ifBlank { uObj.optString("siteName", "") },
            siteName = uObj.optString("siteName", ""),
            telegramId = uObj.optLong("telegramChatId", 0L),
            subscriptionUrl = subUrl,
            expireAt = expireAt,
            status = status,
            devicesCount = devicesCount,
        )
    }

    /**
     * Refreshes the subscription directly from RemnaWave panel for an existing user.
     */
    suspend fun refreshFromRemnaWave(user: TelegramUser): TelegramUser = withContext(Dispatchers.IO) {
        val clean = user.tgUsername.replace("^@", "").trim()
        val candidates = listOf(clean, "nexus_${clean.lowercase()}", clean.lowercase())

        for (candidate in candidates) {
            try {
                val url = "$PANEL_URL/api/users/by-username/$candidate"
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8_000
                    readTimeout = 10_000
                    setRequestProperty("Authorization", "Bearer $PANEL_TOKEN")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "nexus.net-app/1.0")
                }

                if (conn.responseCode in 200..299) {
                    val raw = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
                    conn.disconnect()
                    val data = JSONObject(raw).optJSONObject("response")
                    if (data != null) {
                        val subUrl = data.optString("subscriptionUrl", user.subscriptionUrl)
                        val status = data.optString("status", "ACTIVE")
                        val expireAt = data.optString("expireAt", user.expireAt)
                        val devLimit = data.optInt("hwidDeviceLimit", user.devicesCount)
                        return@withContext user.copy(
                            subscriptionUrl = subUrl,
                            status = if (status.equals("ACTIVE", true)) "active" else "disabled",
                            expireAt = expireAt,
                            devicesCount = devLimit,
                        )
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                // Ignore and try next candidate
            }
        }
        user
    }

    private fun httpGet(urlStr: String): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 12_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "nexus.net-app/1.0 (Android)")
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
}
