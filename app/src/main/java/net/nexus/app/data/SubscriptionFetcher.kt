package net.nexus.app.data

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class FetchResult(
    val name: String,
    val servers: List<Server>,
    val upload: Long = 0,
    val download: Long = 0,
    val total: Long = 0,
    val expireAt: Long = 0,
    val announce: String = "",
    val channelUrl: String = "",
    val supportUrl: String = "",
    val siteUrl: String = "",
)

/** Downloads a subscription URL and turns it into servers + account info (traffic, expiry, links). */
object SubscriptionFetcher {

    private const val MAX_BYTES = 8 * 1024 * 1024

    fun fetch(url: String): FetchResult {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            // RemnaWave and standard panels detect v2ray / Xray User-Agent to deliver full configuration
            setRequestProperty("User-Agent", "nexus.net/1.0 (v2ray; Xray; Android)")
            setRequestProperty("Accept", "*/*")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) error("HTTP $code")

            val body = conn.inputStream.use { it.readBytes() }
                .also { if (it.size > MAX_BYTES) error("response too large") }
                .toString(Charsets.UTF_8)
            fun h(name: String): String = decodeHeader(conn.getHeaderField(name))

            val info = parseUserInfo(conn.getHeaderField("subscription-userinfo"))
            val title = h("profile-title").ifBlank {
                URL(url).let { it.ref?.takeIf { r -> r.isNotBlank() } ?: it.host }
            }

            val servers = parseBody(body)
            if (servers.isEmpty()) error("no servers found")

            return FetchResult(
                name = title,
                servers = servers,
                upload = info["upload"] ?: 0, download = info["download"] ?: 0,
                total = info["total"] ?: 0, expireAt = info["expire"] ?: 0,
                announce = h("announce"),
                channelUrl = h("announce-url"),
                supportUrl = h("support-url"),
                siteUrl = h("profile-web-page-url"),
            )
        } finally {
            conn.disconnect()
        }
    }

    /** Header values may be sent as "base64:<data>" or standard percent/utf-8 encoded string. */
    fun decodeHeader(v: String?): String {
        if (v.isNullOrBlank()) return ""
        val trimmed = v.trim()
        if (trimmed.startsWith("base64:", ignoreCase = true)) {
            val payload = trimmed.substring(7).trim()
            decodeBase64Safe(payload)?.let { return it.trim() }
        }
        return trimmed
    }

    private fun parseUserInfo(v: String?): Map<String, Long> {
        if (v.isNullOrBlank()) return emptyMap()
        return v.split(";").mapNotNull {
            val kv = it.trim().split("=", limit = 2)
            if (kv.size == 2) kv[0].trim().lowercase() to (kv[1].trim().toDoubleOrNull()?.toLong() ?: return@mapNotNull null)
            else null
        }.toMap()
    }

    /** Safe multi-format Base64 decoder supporting both Standard and URL-Safe alphabets without throwing. */
    fun decodeBase64Safe(text: String): String? {
        val clean = text.trim().replace("\r", "").replace("\n", "").replace(" ", "")
        if (clean.isEmpty()) return null

        // 1. Try standard base64 (Base64.DEFAULT)
        runCatching {
            String(Base64.decode(clean, Base64.DEFAULT), Charsets.UTF_8)
        }.getOrNull()?.takeIf { it.isNotBlank() }?.let { return it }

        // 2. Try URL-safe base64 (Base64.URL_SAFE)
        runCatching {
            String(Base64.decode(clean, Base64.URL_SAFE), Charsets.UTF_8)
        }.getOrNull()?.takeIf { it.isNotBlank() }?.let { return it }

        // 3. Normalize url-safe characters (- to + and _ to /) and ensure proper padding
        val normalized = clean.replace('-', '+').replace('_', '/')
        val pad = when (normalized.length % 4) {
            2 -> normalized + "=="
            3 -> normalized + "="
            else -> normalized
        }
        return runCatching {
            String(Base64.decode(pad, Base64.DEFAULT), Charsets.UTF_8)
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private val proxyProtocols = setOf("vless", "vmess", "trojan", "shadowsocks", "hysteria", "wireguard", "socks", "http")

    /**
     * Accepts:
     * - RemnaWave / v2ray JSON configs (array or single config)
     * - Direct share link lines (vless://, vmess://, etc.)
     * - Base64 encoded lines (whole file or line-by-line)
     */
    fun parseBody(raw: String): List<Server> {
        val text = raw.trim().removePrefix("\uFEFF")
        if (text.isEmpty()) return emptyList()

        // 1. JSON configs
        if (text.startsWith("[") || text.startsWith("{")) {
            val fromJson = parseJsonConfigs(text)
            if (fromJson.isNotEmpty()) return fromJson
        }

        // 2. Direct share links lines
        val directServers = LinkParser.parseLines(text)
        if (directServers.isNotEmpty()) return directServers

        // 3. Whole-body base64 decode
        val decoded = decodeBase64Safe(text)
        if (decoded != null && decoded != text) {
            val fromDecoded = parseBody(decoded)
            if (fromDecoded.isNotEmpty()) return fromDecoded
        }

        // 4. Per-line decode (in case each line is individually base64 encoded or mixed)
        val perLineServers = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .flatMap { line ->
                LinkParser.parse(line)?.let { listOf(it) }
                    ?: decodeBase64Safe(line)?.let { parseBody(it) }
                    ?: emptyList()
            }.toList()
        if (perLineServers.isNotEmpty()) return perLineServers

        return emptyList()
    }

    fun parseJsonConfigs(text: String): List<Server> {
        val root = runCatching {
            if (text.startsWith("[")) JSONArray(text)
            else JSONObject(text)
        }.getOrNull() ?: return emptyList()

        if (root is JSONArray) {
            return (0 until root.length()).mapNotNull { i ->
                val cfg = root.optJSONObject(i) ?: return@mapNotNull null
                parseSingleXrayConfig(cfg, i + 1)
            }
        } else if (root is JSONObject) {
            val outs = root.optJSONArray("outbounds")
            if (outs != null) {
                val proxyOuts = (0 until outs.length()).map { outs.getJSONObject(it) }
                    .filter { it.optString("tag") == "proxy" || it.optString("protocol") in proxyProtocols }
                if (proxyOuts.size > 1) {
                    return proxyOuts.mapIndexed { idx, pOut ->
                        val stream = pOut.optJSONObject("streamSettings")
                        val tagName = pOut.optString("tag").ifBlank { "server ${idx + 1}" }
                        Server(
                            id = UUID.randomUUID().toString(),
                            name = tagName,
                            protocol = pOut.optString("protocol"),
                            network = stream?.optString("network")?.ifEmpty { "tcp" } ?: "tcp",
                            security = stream?.optString("security")?.ifEmpty { "none" } ?: "none",
                            source = "LINK",
                            outbounds = JSONArray().put(pOut).toString(),
                        )
                    }
                }
            }
            return listOfNotNull(parseSingleXrayConfig(root, 1))
        }
        return emptyList()
    }

    private fun parseSingleXrayConfig(cfg: JSONObject, index: Int): Server? {
        val outs = cfg.optJSONArray("outbounds") ?: return null
        val first = (0 until outs.length()).map { outs.getJSONObject(it) }
            .firstOrNull { it.optString("tag") == "proxy" || it.optString("protocol") in proxyProtocols }
            ?: return null
        val stream = first.optJSONObject("streamSettings")
        val name = cfg.optString("remarks").ifBlank {
            cfg.optString("ps").ifBlank {
                first.optString("tag").takeIf { it != "proxy" && it.isNotBlank() } ?: "config $index"
            }
        }
        return Server(
            id = UUID.randomUUID().toString(),
            name = name,
            protocol = first.optString("protocol"),
            network = stream?.optString("network")?.ifEmpty { "tcp" } ?: "tcp",
            security = stream?.optString("security")?.ifEmpty { "none" } ?: "none",
            source = "JSON",
            fullConfig = cfg.toString(),
        )
    }
}
