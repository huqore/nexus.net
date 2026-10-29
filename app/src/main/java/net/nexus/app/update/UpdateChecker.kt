package net.nexus.app.update

import net.nexus.app.BuildConfig
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {

    private const val CONNECT_TIMEOUT = 12_000
    private const val READ_TIMEOUT = 15_000

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
            checkGitHubReleases(repo)
        } ?: return null

        val currentVersion = BuildConfig.VERSION_NAME
        val isNewer = isVersionNewer(release.versionName, currentVersion)
        return if (isNewer) release else null
    }

    private fun checkGitHubReleases(repo: String): AppReleaseInfo? {
        val endpoint = "https://api.github.com/repos/$repo/releases?per_page=10"
        val jsonText = httpGet(endpoint, accept = "application/vnd.github.v3+json")
            ?: httpGet("https://api.github.com/repos/$repo/releases/latest", accept = "application/vnd.github.v3+json")
            ?: return null

        val candidates = if (jsonText.trim().startsWith("[")) {
            val arr = org.json.JSONArray(jsonText)
            (0 until arr.length())
                .map { arr.getJSONObject(it) }
                .filter { !it.optBoolean("draft", false) }
        } else {
            listOf(JSONObject(jsonText))
        }

        for (json in candidates) {
            val tagName = json.optString("tag_name", "").trim()
            val name = json.optString("name", "").trim()
            val body = json.optString("body", "").trim()
            val publishedAt = json.optString("published_at", "")

            val versionName = cleanVersion(if (name.isNotBlank()) name else tagName)
            val assets = json.optJSONArray("assets") ?: continue

            var apkUrl: String? = null
            var apkName: String? = null
            var apkSize: Long = 0

            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val aName = asset.optString("name", "")
                if (aName.endsWith(".apk", ignoreCase = true)) {
                    val browserUrl = asset.optString("browser_download_url", "")
                    val apiUrl = asset.optString("url", "")
                    val size = asset.optLong("size", 0)
                    // Prefer universal or release APK if multiple are present
                    if (apkUrl == null || aName.contains("universal", ignoreCase = true) || aName.contains("release", ignoreCase = true)) {
                        apkUrl = if (UpdateConfig.GITHUB_TOKEN.isNotBlank() && apiUrl.isNotBlank()) apiUrl else browserUrl
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
        val jsonText = httpGet(url, accept = "application/json") ?: return null
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

    private fun httpGet(urlStr: String, accept: String): String? {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
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
            if (code !in 200..299) error("Server returned HTTP $code")

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
