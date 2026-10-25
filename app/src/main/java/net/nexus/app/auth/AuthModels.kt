package net.nexus.app.auth

import org.json.JSONObject

data class TelegramUser(
    val tgUsername: String,
    val siteName: String = "",
    val telegramId: Long = 0,
    val subscriptionUrl: String = "",
    val expireAt: String = "",
    val status: String = "active",
    val devicesCount: Int = 3,
    val remnaUserId: Long = 0,
) {
    val cleanUsername: String
        get() = tgUsername.removePrefix("@").trim()

    val formattedUsername: String
        get() = if (cleanUsername.isNotBlank()) "@$cleanUsername" else siteName.ifBlank { "User" }

    val isSubscribed: Boolean
        get() = status.equals("active", ignoreCase = true) && subscriptionUrl.isNotBlank()

    fun toJson(): JSONObject = JSONObject()
        .put("tgUsername", cleanUsername)
        .put("siteName", siteName)
        .put("telegramId", telegramId)
        .put("subscriptionUrl", subscriptionUrl)
        .put("expireAt", expireAt)
        .put("status", status)
        .put("devicesCount", devicesCount)
        .put("remnaUserId", remnaUserId)

    companion object {
        fun fromJson(o: JSONObject): TelegramUser = TelegramUser(
            tgUsername = o.optString("tgUsername", "").removePrefix("@").trim(),
            siteName = o.optString("siteName", ""),
            telegramId = o.optLong("telegramId", 0L),
            subscriptionUrl = o.optString("subscriptionUrl", ""),
            expireAt = o.optString("expireAt", ""),
            status = o.optString("status", "none"),
            devicesCount = o.optInt("devicesCount", 3),
            remnaUserId = o.optLong("remnaUserId", 0L),
        )
    }
}

sealed interface TelegramAuthState {
    data object Idle : TelegramAuthState
    data object CreatingSession : TelegramAuthState
    data class WaitingConfirmation(val sessionId: String, val botUrl: String) : TelegramAuthState
    data class Success(val user: TelegramUser, val serverCount: Int) : TelegramAuthState
    data class Error(val message: String) : TelegramAuthState
}
