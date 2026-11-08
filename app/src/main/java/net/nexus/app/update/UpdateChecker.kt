package net.nexus.app.update

import net.nexus.app.BuildConfig
import net.nexus.app.vpn.VpnState
import net.nexus.app.vpn.VpnStatus
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL

object UpdateChecker {

    private const val CONNECT_TIMEOUT = 5_000
    private const val READ_TIMEOUT = 10_000

    /**
     * Checks if a newer version is available.
     * Returns [AppReleaseInfo] if a newer version exists, or null if up-to-date.
     */
    fun check(): AppReleaseInfo? {
        val customUrl = UpdateConfig.CUSTOM_UPDATE_JSON_URL.trim()
        val release = if (customUrl.isNotBlank()) {
            checkCustomJson(customUrl)
        } else {
            val repo = UpdateConfig.GITHUB_REPO.trim()
            if (repo.isBlank() || repo.contains("YOUR_GITHUB_USERNAME", ignoreCase = true)) {
                error("GitHub repository not configured in UpdateConfig.kt")
            }
            fetchReleasesWithFallback(repo)
        } ?: return null

        val currentVersion = BuildConfig.VERSION_NAME
        val isNewer = isVersionNewer(release.versionName, currentVersion)
        return if (isNewer) release else null
    }

    private fun fetchReleasesWithFallback(repo: String): AppReleaseInfo? {
        val proxy = getProxyIfVpnActive()

        // 1. If VPN is active, route through SOCKS proxy to official GitHub Releases API
        if (proxy != null) {
            val vpnResult = queryEndpoint("https://api.github.com/repos/$repo/releases?per_page=10", accept = "application/vnd.github.v3+json", proxy = proxy)
            if (vpnResult != null) return vpnResult
        }

        // 2. Direct or Fallback mirrors (Cloudflare/CDN backed - bypasses Russian ISP DPI blocks)
        val endpoints = listOf(
            // Nexus BFF proxy route (real-time, hosted on our fast Cloudflare-backed infrastructure)
            "https://nexus-net.xyz/api/v1/client/releases",
            // jsDelivr global CDN mirrors for static releases manifest
            "https://cdn.jsdelivr.net/gh/$repo@v3-pure/releases.json",
            "https://cdn.jsdelivr.net/gh/$repo@main/releases.json",
            // ungh.cc is a high-speed Cloudflare-backed GitHub releases CDN, globally accessible without DPI blocks
            "https://ungh.cc/repos/$repo/releases",
            // official GitHub Releases API (direct connection, short timeout)
            "https://api.github.com/repos/$repo/releases?per_page=10",
            // GitHub latest release endpoint
            "https://api.github.com/repos/$repo/releases/latest"
        )

        for (endpoint in endpoints) {
            val release = queryEndpoint(endpoint, accept = "application/json, application/vnd.github.v3+json", proxy = null)
            if (release != null) return release
        }

        return null
    }

    private fun queryEndpoint(endpoint: String, accept: String, proxy: Proxy?): AppReleaseInfo? {
        val jsonText = runCatching { httpGet(endpoint, accept = accept, proxy = proxy) }.getOrNull() ?: return null
        return parseReleaseCandidates(jsonText)
    }

    private fun getProxyIfVpnActive(): Proxy? {
        val port = VpnState.socksPort
        return if (VpnState.status.value == VpnStatus.CONNECTED && port > 0) {
            Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", port))
        } else {
            null
        }
    }

    fun parseReleaseCandidates(jsonText: String): AppReleaseInfo? {
        val trimmed = jsonText.trim()
        val candidates: List<JSONObject> = when {
            trimmed.startsWith("[") -> {
                val arr = JSONArray(trimmed)
                (0 until arr.length())
                    .map { arr.getJSONObject(it) }
                    .filter { !it.optBoolean("draft", false) }
            }
            trimmed.startsWith("{") -> {
                val root = JSONObject(trimmed)
                if (root.has("releases")) {
                    val arr = root.optJSONArray("releases") ?: JSONArray()
                    (0 until arr.length())
                        .map { arr.getJSONObject(it) }
                        .filter { !it.optBoolean("draft", false) }
                } else if (!root.optBoolean("draft", false)) {
                    listOf(root)
                } else {
                    emptyList()
                }
            }
            else -> emptyList()
        }

        for (json in candidates) {
            val tagName = json.optString("tag_name", "").ifBlank { json.optString("tag", "") }.trim()
            val name = json.optString("name", "").trim()
            val body = json.optString("body", "").ifBlank { json.optString("markdown", "") }.trim()
            val publishedAt = json.optString("published_at", "").ifBlank { json.optString("publishedAt", "") }

            val versionName = cleanVersion(if (name.isNotBlank()) name else tagName)
            val assets = json.optJSONArray("assets") ?: continue

            var apkUrl: String? = null
            var apkName: String? = null
            var apkSize: Long = 0

            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val browserUrl = asset.optString("browser_download_url", "")
                val apiUrl = asset.optString("url", "")
                val unghUrl = asset.optString("downloadUrl", "")

                val directUrl = when {
                    browserUrl.isNotBlank() -> browserUrl
                    unghUrl.isNotBlank() -> unghUrl
                    apiUrl.isNotBlank() -> apiUrl
                    else -> ""
                }

                var aName = asset.optString("name", "")
                if (aName.isBlank() && directUrl.isNotBlank()) {
                    aName = directUrl.substringAfterLast('/')
                }

                if (aName.endsWith(".apk", ignoreCase = true) || directUrl.endsWith(".apk", ignoreCase = true)) {
                    val size = asset.optLong("size", 0)
                    // Prefer universal or release APK if multiple are present
                    if (apkUrl == null || aName.contains("universal", ignoreCase = true) || aName.contains("release", ignoreCase = true)) {
                        apkUrl = if (UpdateConfig.GITHUB_TOKEN.isNotBlank() && apiUrl.isNotBlank()) apiUrl else directUrl
                        apkName = aName
                        apkSize = size
                    }
                }
            }

            if (!apkUrl.isNullOrBlank()) {
                return AppReleaseInfo(
                    tagName = tagName,
                    versionName = versionName,
                    changelog = body,
                    downloadUrl = apkUrl,
                    apkFileName = apkName ?: "nexus-vpn-$versionName.apk",
                    apkSize = apkSize,
                    publishedAt = publishedAt,
                )
            }
        }

        return null
    }

    private fun checkCustomJson(url: String): AppReleaseInfo? {
        val jsonText = httpGet(url, accept = "application/json", proxy = getProxyIfVpnActive()) ?: return null
        val json = JSONObject(jsonText)
        val versionName = json.optString("versionName", "").ifBlank { json.optString("version", "") }
        val apkUrl = json.optString("apkUrl", "").ifBlank { json.optString("url", "") }
        val changelog = json.optString("changelog", "").ifBlank { json.optString("description", "") }
        val fileName = json.optString("fileName", "nexus-vpn-$versionName.apk")
        val fileSize = json.optLong("fileSize", 0)

        if (versionName.isBlank() || apkUrl.isBlank()) {
            error("Invalid update JSON schema")
        }

        return AppReleaseInfo(
            tagName = "v$versionName",
            versionName = cleanVersion(versionName),
            changelog = changelog,
            downloadUrl = apkUrl,
            apkFileName = fileName,
            apkSize = fileSize,
        )
    }

    private fun httpGet(urlStr: String, accept: String, proxy: Proxy? = null): String? {
        val conn = (if (proxy != null) {
            URL(urlStr).openConnection(proxy) as HttpURLConnection
        } else {
            URL(urlStr).openConnection() as HttpURLConnection
        }).apply {
            connectTimeout = CONNECT_TIMEOUT
            readTimeout = READ_TIMEOUT
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "nexus.net-updater/1.0 (Android)")
            setRequestProperty("Accept", accept)
            if (UpdateConfig.GITHUB_TOKEN.isNotBlank() && urlStr.contains("api.github.com")) {
                setRequestProperty("Authorization", "Bearer ${UpdateConfig.GITHUB_TOKEN.trim()}")
            }
        }
        try {
            val code = conn.responseCode
            // 404 means no releases are published yet for this repository
            if (code == 404) return null
            if (code !in 200..299) return null

            BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { reader ->
                return reader.readText()
            }
        } finally {
            conn.disconnect()
        }
    }

    fun isVersionNewer(remoteVersion: String, currentVersion: String): Boolean {
        val remoteDigits = extractVersionDigits(remoteVersion)
        val currentDigits = extractVersionDigits(currentVersion)
        if (remoteDigits.isEmpty() || currentDigits.isEmpty()) return false

        val currentIsPreRelease = currentVersion.contains("-") || currentVersion.contains("test", ignoreCase = true)
        val remoteIsPreRelease = remoteVersion.contains("-") || remoteVersion.contains("test", ignoreCase = true)

        // If the current app is any test/pre-release build (e.g. 2.3.1-test, 3.2.2-test)
        // and remote is an official release, ALWAYS offer the update!
        if (currentIsPreRelease && !remoteIsPreRelease) {
            return true
        }

        val maxLen = maxOf(remoteDigits.size, currentDigits.size)
        for (i in 0 until maxLen) {
            val r = remoteDigits.getOrElse(i) { 0 }
            val c = currentDigits.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) {
                // If current is a test version and remote is a different stable version, update
                if (currentIsPreRelease && cleanVersion(remoteVersion) != cleanVersion(currentVersion)) {
                    return true
                }
                return false
            }
        }

        // If numeric components are identical:
        // A stable release is newer than a test version (e.g., 2.4.0 > 2.4.0-test)
        if (currentIsPreRelease && !remoteIsPreRelease) return true

        // If current is a test build and remote has a different clean version
        if (currentIsPreRelease && cleanVersion(remoteVersion) != cleanVersion(currentVersion)) {
            return true
        }

        return false
    }

    private fun extractVersionDigits(v: String): List<Int> =
        Regex("""\d+""").findAll(v).mapNotNull { it.value.toIntOrNull() }.toList()

    fun cleanVersion(raw: String): String {
        var s = raw.trim()
        if (s.startsWith("nexus", ignoreCase = true)) {
            s = s.substringAfter(" ").trim()
        }
        return s.removePrefix("v").removePrefix("V").trim()
    }
}
