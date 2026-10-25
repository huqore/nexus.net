package net.nexus.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.util.UUID

/** Parses vless:// vmess:// trojan:// ss:// share links into Xray outbound objects. */
object LinkParser {

    fun parseLines(text: String): List<Server> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { runCatching { parse(it) }.getOrNull() }
            .toList()

    fun parse(link: String): Server? {
        val trimmed = link.trim()
        return when {
            trimmed.startsWith("vless://", ignoreCase = true) -> parseVless(trimmed)
            trimmed.startsWith("trojan://", ignoreCase = true) -> parseTrojan(trimmed)
            trimmed.startsWith("vmess://", ignoreCase = true) -> parseVmess(trimmed)
            trimmed.startsWith("ss://", ignoreCase = true) || trimmed.startsWith("shadowsocks://", ignoreCase = true) -> parseShadowsocks(trimmed)
            else -> null
        }
    }

    // ---------- helpers ----------

    private fun dec(s: String?): String = if (s == null) "" else runCatching { URLDecoder.decode(s, "UTF-8") }.getOrDefault(s)

    private fun queryMap(q: String?): Map<String, String> {
        if (q.isNullOrEmpty()) return emptyMap()
        return q.split("&").filter { it.isNotEmpty() }.associate {
            val i = it.indexOf('=')
            if (i < 0) dec(it) to "" else dec(it.substring(0, i)) to dec(it.substring(i + 1))
        }
    }

    private fun newId() = UUID.randomUUID().toString()

    data class LinkParts(
        val scheme: String,
        val userInfo: String,
        val host: String,
        val port: Int,
        val query: Map<String, String>,
        val name: String,
    )

    /**
     * Splits a link without relying strictly on java.net.URI, which throws URISyntaxException
     * on special characters in query parameters (such as extra JSON) or unescaped fragments.
     */
    private fun parseLinkParts(link: String): LinkParts? {
        val hashIdx = link.indexOf('#')
        val linkNoFragment = if (hashIdx >= 0) link.substring(0, hashIdx) else link
        val rawName = if (hashIdx >= 0) link.substring(hashIdx + 1) else ""
        val name = runCatching { URLDecoder.decode(rawName.replace("+", "%2B"), "UTF-8") }.getOrDefault(rawName).trim()

        val scheme = linkNoFragment.substringBefore("://", "").lowercase()
        if (scheme.isEmpty()) return null
        val rest = linkNoFragment.substringAfter("://")

        val queryIdx = rest.indexOf('?')
        val authority = if (queryIdx >= 0) rest.substring(0, queryIdx) else rest
        val rawQuery = if (queryIdx >= 0) rest.substring(queryIdx + 1) else ""
        val q = queryMap(rawQuery)

        val userInfo: String
        val hostPort: String
        if (authority.contains('@')) {
            userInfo = authority.substringBeforeLast('@')
            hostPort = authority.substringAfterLast('@')
        } else {
            userInfo = ""
            hostPort = authority
        }

        val host: String
        val port: Int
        if (hostPort.startsWith("[")) { // IPv6 literal
            val closeBracket = hostPort.indexOf(']')
            if (closeBracket > 0) {
                host = hostPort.substring(1, closeBracket)
                val after = hostPort.substring(closeBracket + 1)
                port = if (after.startsWith(":")) after.substring(1).toIntOrNull() ?: -1 else -1
            } else {
                host = hostPort
                port = -1
            }
        } else if (hostPort.contains(':')) {
            host = hostPort.substringBefore(':')
            port = hostPort.substringAfter(':').toIntOrNull() ?: -1
        } else {
            host = hostPort
            port = -1
        }

        return LinkParts(scheme, userInfo, host, port, q, name)
    }

    /** Builds the Xray streamSettings object shared by vless / vmess / trojan. */
    private fun streamSettings(
        type: String, security: String, q: Map<String, String>, host: String,
    ): JSONObject {
        val net = when (type.lowercase()) {
            "", "tcp", "raw" -> "tcp"
            "splithttp", "xhttp" -> "xhttp"
            "h2", "http" -> "h2"
            else -> type.lowercase()
        }
        val ss = JSONObject().put("network", net).put("security", security)

        when (net) {
            "ws" -> ss.put("wsSettings", JSONObject()
                .put("path", q["path"] ?: "/")
                .put("headers", JSONObject().apply { q["host"]?.takeIf { it.isNotEmpty() }?.let { put("Host", it) } }))
            "grpc" -> ss.put("grpcSettings", JSONObject()
                .put("serviceName", q["serviceName"] ?: q["path"] ?: "")
                .put("multiMode", q["mode"] == "multi"))
            "httpupgrade" -> ss.put("httpupgradeSettings", JSONObject()
                .put("path", q["path"] ?: "/").put("host", q["host"] ?: ""))
            "h2" -> ss.put("httpSettings", JSONObject()
                .put("path", q["path"] ?: "/")
                .put("host", JSONArray().apply { (q["host"] ?: "").split(",").filter { it.isNotEmpty() }.forEach { put(it) } }))
            "xhttp" -> ss.put("xhttpSettings", JSONObject().apply {
                put("path", q["path"] ?: "/")
                q["host"]?.takeIf { it.isNotEmpty() }?.let { put("host", it) }
                put("mode", q["mode"]?.takeIf { it.isNotEmpty() } ?: "auto")
                // RemnaWave WL bypass sends extra JSON with sequence keys, padding, tokens
                q["extra"]?.takeIf { it.isNotEmpty() }?.let { runCatching { put("extra", JSONObject(it)) } }
            })
            "tcp" -> if (q["headerType"] == "http") {
                ss.put("tcpSettings", JSONObject().put("header", JSONObject()
                    .put("type", "http")
                    .put("request", JSONObject()
                        .put("path", JSONArray().put(q["path"] ?: "/"))
                        .put("headers", JSONObject().put("Host", JSONArray().put(q["host"] ?: host))))))
            }
        }

        when (security) {
            "tls" -> ss.put("tlsSettings", JSONObject().apply {
                put("serverName", q["sni"]?.takeIf { it.isNotEmpty() } ?: q["host"]?.takeIf { it.isNotEmpty() } ?: host)
                q["fp"]?.takeIf { it.isNotEmpty() }?.let { put("fingerprint", it) }
                q["alpn"]?.takeIf { it.isNotEmpty() }?.let { a ->
                    put("alpn", JSONArray().apply { a.split(",").forEach { put(it) } })
                }
                if (q["allowInsecure"] == "1" || q["insecure"] == "1") put("allowInsecure", true)
            })
            "reality" -> ss.put("realitySettings", JSONObject().apply {
                put("serverName", q["sni"] ?: host)
                put("fingerprint", q["fp"]?.takeIf { it.isNotEmpty() } ?: "chrome")
                put("publicKey", q["pbk"] ?: "")
                put("shortId", q["sid"] ?: "")
                put("spiderX", q["spx"] ?: "")
                q["pqv"]?.takeIf { it.isNotEmpty() }?.let { put("mldsa65Verify", it) }
            })
        }
        return ss
    }

    private fun secOf(q: Map<String, String>) = when (val s = (q["security"] ?: "none").lowercase()) {
        "", "none" -> "none"
        else -> s
    }

    private fun outbound(protocol: String, settings: JSONObject, stream: JSONObject?) =
        JSONObject().put("tag", "proxy").put("protocol", protocol).put("settings", settings).apply {
            if (stream != null) put("streamSettings", stream)
        }

    private fun wrap(name: String, proto: String, net: String, sec: String, out: JSONObject) = Server(
        id = newId(), name = name.ifBlank { "server" }, protocol = proto, network = net, security = sec,
        source = "LINK", outbounds = JSONArray().put(out).toString(),
    )

    // ---------- protocol parsers ----------

    private fun parseVless(link: String): Server? {
        val p = parseLinkParts(link) ?: return null
        val q = p.query
        val security = secOf(q)
        val host = p.host
        val port = if (p.port > 0) p.port else if (security == "tls" || security == "reality") 443 else 80
        val stream = streamSettings(q["type"] ?: "tcp", security, q, host)
        val user = JSONObject().put("id", p.userInfo).put("encryption", q["encryption"] ?: "none")
            .apply { q["flow"]?.takeIf { it.isNotEmpty() }?.let { put("flow", it) } }
        val settings = JSONObject().put("vnext", JSONArray().put(JSONObject()
            .put("address", host).put("port", port).put("users", JSONArray().put(user))))
        val finalName = p.name.ifBlank { "$host:$port" }
        return wrap(finalName, "vless", stream.getString("network"), security,
            outbound("vless", settings, stream))
    }

    private fun parseTrojan(link: String): Server? {
        val p = parseLinkParts(link) ?: return null
        val q = p.query
        val security = if (q["security"].isNullOrEmpty()) "tls" else secOf(q)
        val host = p.host
        val port = if (p.port > 0) p.port else 443
        val stream = streamSettings(q["type"] ?: "tcp", security, q, host)
        val settings = JSONObject().put("servers", JSONArray().put(JSONObject()
            .put("address", host).put("port", port).put("password", dec(p.userInfo))))
        val finalName = p.name.ifBlank { "$host:$port" }
        return wrap(finalName, "trojan", stream.getString("network"), security,
            outbound("trojan", settings, stream))
    }

    private fun parseVmess(link: String): Server? {
        val b64Content = link.substring("vmess://".length)
        val decoded = SubscriptionFetcher.decodeBase64Safe(b64Content) ?: return null
        val j = runCatching { JSONObject(decoded) }.getOrNull() ?: return null
        val host = j.optString("add")
        val port = j.opt("port")?.toString()?.toIntOrNull() ?: 443
        val tls = j.optString("tls")
        val security = if (tls == "tls") "tls" else "none"
        val q = mutableMapOf<String, String>()
        j.optString("path").takeIf { it.isNotEmpty() }?.let { q["path"] = it }
        j.optString("host").takeIf { it.isNotEmpty() }?.let { q["host"] = it }
        j.optString("sni").takeIf { it.isNotEmpty() }?.let { q["sni"] = it }
        j.optString("fp").takeIf { it.isNotEmpty() }?.let { q["fp"] = it }
        j.optString("alpn").takeIf { it.isNotEmpty() }?.let { q["alpn"] = it }
        if (j.optString("type") == "http") q["headerType"] = "http"
        val stream = streamSettings(j.optString("net", "tcp"), security, q, host)
        val user = JSONObject().put("id", j.optString("id"))
            .put("alterId", j.optString("aid", "0").toIntOrNull() ?: 0)
            .put("security", j.optString("scy").ifEmpty { "auto" })
        val settings = JSONObject().put("vnext", JSONArray().put(JSONObject()
            .put("address", host).put("port", port)
            .put("users", JSONArray().put(user))))
        val finalName = j.optString("ps").ifBlank { "$host:$port" }
        return wrap(finalName, "vmess", stream.getString("network"), security,
            outbound("vmess", settings, stream))
    }

    private fun parseShadowsocks(link: String): Server? {
        val p = parseLinkParts(link) ?: return null
        val name = p.name
        val host: String
        val port: Int
        val method: String
        val password: String

        if (p.userInfo.isNotEmpty()) {
            val cred = if (p.userInfo.contains(':')) dec(p.userInfo) else (SubscriptionFetcher.decodeBase64Safe(p.userInfo) ?: "")
            if (!cred.contains(':')) return null
            method = cred.substringBefore(':')
            password = cred.substringAfter(':')
            host = p.host
            port = if (p.port > 0) p.port else 8388
        } else {
            val decoded = SubscriptionFetcher.decodeBase64Safe(p.host) ?: return null
            if (!decoded.contains('@')) return null
            val ui = decoded.substringBeforeLast('@')
            val hp = decoded.substringAfterLast('@')
            method = ui.substringBefore(':')
            password = ui.substringAfter(':')
            host = hp.substringBeforeLast(':').trim('[', ']')
            port = hp.substringAfterLast(':').toIntOrNull() ?: 8388
        }
        val q = p.query
        val stream = if (q["type"] != null || q["security"] != null)
            streamSettings(q["type"] ?: "tcp", secOf(q), q, host) else null
        val settings = JSONObject().put("servers", JSONArray().put(JSONObject()
            .put("address", host).put("port", port).put("method", method).put("password", password)))
        val finalName = name.ifBlank { "$host:$port" }
        return wrap(finalName, "shadowsocks", stream?.getString("network") ?: "tcp", stream?.let { secOf(q) } ?: "none",
            outbound("shadowsocks", settings, stream))
    }
}
