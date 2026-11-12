package net.nexus.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Tiny persistence layer: everything lives as JSON in SharedPreferences. */
class Store(context: Context) {
    private val prefsContext = context.applicationContext
    private val prefs = prefsContext.getSharedPreferences("nexus", Context.MODE_PRIVATE)

    fun loadProviders(): List<Provider> = runCatching {
        val arr = JSONArray(prefs.getString("providers", "[]"))
        (0 until arr.length()).map { Provider.fromJson(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    fun saveProviders(list: List<Provider>) {
        val arr = JSONArray().also { a -> list.forEach { a.put(it.toJson()) } }
        prefs.edit().putString("providers", arr.toString()).apply()
    }

    fun loadSettings(): Settings = runCatching {
        val o = JSONObject(prefs.getString("settings", "{}") ?: "{}")
        val apps = o.optJSONArray("splitApps") ?: JSONArray()
        Settings(
            splitMode = runCatching { SplitMode.valueOf(o.optString("splitMode", "OFF")) }.getOrDefault(SplitMode.OFF),
            splitApps = (0 until apps.length()).map { apps.getString(it) }.toSet(),
            testUrl = o.optString("testUrl").ifBlank { Settings().testUrl },
            selectedServerId = o.optString("selectedServerId").ifEmpty { null },
            fontScale = o.optDouble("fontScale", 1.0).toFloat(),
            autoCheckUpdates = o.optBoolean("autoCheckUpdates", true),
            autoConnectOnBoot = o.optBoolean("autoConnectOnBoot", false),
            hapticIntensity = runCatching { HapticIntensity.valueOf(o.optString("hapticIntensity", "MEDIUM")) }.getOrDefault(HapticIntensity.MEDIUM),
            killSwitch = o.optBoolean("killSwitch", false),
            blockAds = o.optBoolean("blockAds", false),
            // The old "amoled black" theme became a toggle: migrate it to Base + AMOLED on.
            appTheme = AppTheme.fromId(o.optString("appTheme", "base")).let { if (it == AppTheme.AMOLED_BLACK) AppTheme.BASE else it },
            amoledMode = o.optBoolean("amoledMode", o.optString("appTheme") == "amoled_black"),
            lightMode = o.optBoolean("lightMode", false),
            appIcon = AppIcon.fromId(o.optString("appIcon", "default")),
            appFont = AppFont.fromId(o.optString("appFont", "russo_one")),
            use3dGlobe = o.optBoolean("use3dGlobe", true),
            themeGridMode = o.optBoolean("themeGridMode", true),
            appSounds = o.optBoolean("appSounds", true),
            designStyle = DesignStyle.fromId(o.optString("designStyle", "fluent")),
            customAccent = o.optInt("customAccent", Settings().customAccent),
            customBg = o.optInt("customBg", Settings().customBg),
            customParticles = CustomParticles.fromId(o.optString("customParticles", "none")),
        )
    }.getOrDefault(Settings())

    fun saveSettings(s: Settings) {
        val o = JSONObject()
            .put("splitMode", s.splitMode.name)
            .put("splitApps", JSONArray(s.splitApps.toList()))
            .put("testUrl", s.testUrl)
            .put("selectedServerId", s.selectedServerId)
            .put("fontScale", s.fontScale.toDouble())
            .put("autoCheckUpdates", s.autoCheckUpdates)
            .put("autoConnectOnBoot", s.autoConnectOnBoot)
            .put("hapticIntensity", s.hapticIntensity.name)
            .put("killSwitch", s.killSwitch)
            .put("blockAds", s.blockAds)
            .put("appTheme", s.appTheme.id)
            .put("appIcon", s.appIcon.id)
            .put("appFont", s.appFont.id)
            .put("use3dGlobe", s.use3dGlobe)
            .put("themeGridMode", s.themeGridMode)
            .put("appSounds", s.appSounds)
            .put("designStyle", s.designStyle.id)
            .put("customAccent", s.customAccent)
            .put("customBg", s.customBg)
            .put("customParticles", s.customParticles.id)
            .put("amoledMode", s.amoledMode)
            .put("lightMode", s.lightMode)
        prefs.edit().putString("settings", o.toString()).apply()
    }

    fun isWlBypass(server: Server): Boolean = Companion.isWlBypass(server)

    /** Finds the best server by lowest positive ping, or falls back to the first available server.
     * Excludes WL bypass servers if other servers have a valid ping. */
    fun findBestServer(): Server? = findNextBestServer(null)

    /** Finds the best server excluding a specific server ID (e.g. for failover). */
    fun findNextBestServer(excludeId: String?): Server? {
        val servers = loadProviders().flatMap { it.servers }
            .filter { it.outbounds != null || it.fullConfig != null }
            .filter { excludeId == null || it.id != excludeId }
        if (servers.isEmpty()) return null
        val withPing = servers.filter { it.pingMs > 0 }.sortedBy { it.pingMs }

        // If other (non-WL-bypass) servers have positive ping, select among them
        val nonWlWithPing = withPing.filterNot { isWlBypass(it) }
        if (nonWlWithPing.isNotEmpty()) {
            return nonWlWithPing.first()
        }

        // If only WL-bypass servers have positive ping, use them
        if (withPing.isNotEmpty()) {
            return withPing.first()
        }

        // Fallback when no ping is measured yet: prefer non-WL bypass
        return servers.firstOrNull { !isWlBypass(it) } ?: servers.firstOrNull()
    }

    /** Server that "Auto" locked onto the first time it connected (null = not chosen yet). */
    var autoLockedServerId: String?
        get() = prefs.getString("auto_locked_server", null)
        set(value) = prefs.edit().putString("auto_locked_server", value).apply()

    private fun connectableServers(): List<Server> =
        loadProviders().flatMap { it.servers }.filter { it.outbounds != null || it.fullConfig != null }

    /**
     * Auto mode picks a server ONCE and then sticks to it (no periodic re-pinging/switching).
     * It re-picks only when the locked server disappears from the subscription, or when the
     * user taps "Auto-select" again (MainViewModel clears the lock).
     */
    fun resolveAutoServer(): Server? {
        val all = connectableServers()
        autoLockedServerId?.let { id -> all.firstOrNull { it.id == id }?.let { return it } }
        val best = findBestServer()
        autoLockedServerId = best?.id
        return best
    }

    /** What Auto would connect to, without locking anything (for UI labels / ping target). */
    fun peekAutoServer(): Server? {
        val all = connectableServers()
        autoLockedServerId?.let { id -> all.firstOrNull { it.id == id }?.let { return it } }
        return findBestServer()
    }

    /** Looks a server up by id across all providers (used by the VPN service). Supports AUTO_SERVER_ID. */
    fun findServer(id: String?): Server? = when (id) {
        null -> null
        AUTO_SERVER_ID -> resolveAutoServer()
        else -> loadProviders().asSequence().flatMap { it.servers.asSequence() }.firstOrNull { it.id == id }
    }

    fun loadTelegramUser(): net.nexus.app.auth.TelegramUser? = runCatching {
        val s = prefs.getString("telegram_user", null) ?: return null
        net.nexus.app.auth.TelegramUser.fromJson(JSONObject(s))
    }.getOrNull()

    fun saveTelegramUser(user: net.nexus.app.auth.TelegramUser?) {
        if (user == null) {
            prefs.edit().remove("telegram_user").apply()
        } else {
            prefs.edit().putString("telegram_user", user.toJson().toString()).apply()
        }
    }

    fun getOrCreateHwid(): String {
        val existing = prefs.getString("device_hwid", null)
        if (!existing.isNullOrBlank()) return existing

        val androidId = runCatching {
            android.provider.Settings.Secure.getString(prefsContext.contentResolver, android.provider.Settings.Secure.ANDROID_ID)
        }.getOrNull().orEmpty()

        val rawFingerprint = "${android.os.Build.MANUFACTURER}:${android.os.Build.MODEL}:${android.os.Build.HARDWARE}:$androidId"
        val newHwid = if (androidId.isNotBlank() && androidId != "9774d56d682e549c") {
            try {
                val md = java.security.MessageDigest.getInstance("SHA-256")
                md.digest(rawFingerprint.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
            } catch (_: Exception) {
                androidId
            }
        } else {
            java.util.UUID.randomUUID().toString()
        }
        prefs.edit().putString("device_hwid", newHwid).apply()
        return newHwid
    }

    var lastExpiryNotifiedDay: String?
        get() = prefs.getString("last_expiry_notified_day", null)
        set(value) = prefs.edit().putString("last_expiry_notified_day", value).apply()

    companion object {
        const val AUTO_SERVER_ID = "__auto__"

        private val WL_WORD = Regex("""(^|[^\p{L}\p{N}])wl([^\p{L}\p{N}]|$)""", RegexOption.IGNORE_CASE)

        /**
         * A server belongs to the "Whitelist bypass" section only when its NAME says so
         * ("Whitelist bypass", "WL bypass", "WL-…", a standalone "WL"). Everything else —
         * e.g. "🇵🇱 Польша Wi-Fi" — is a regular Wi-Fi server, even if it runs on the same
         * CDN/IP range as the WL nodes. The config contents must not decide the section.
         */
        fun isWlBypass(server: Server): Boolean {
            val n = server.name.lowercase()
            return n.contains("whitelist") ||
                n.contains("white list") ||
                n.contains("wl bypass") ||
                n.contains("wl-bypass") ||
                n.contains("wl_bypass") ||
                n.contains("обход белых") ||
                WL_WORD.containsMatchIn(server.name)
        }
    }
}
