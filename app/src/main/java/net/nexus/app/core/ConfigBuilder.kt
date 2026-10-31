package net.nexus.app.core

import net.nexus.app.data.Server
import org.json.JSONArray
import org.json.JSONObject
import java.net.Inet4Address
import java.net.InetAddress

/** Turns a [Server] into the JSON config the Xray core runs. */
object ConfigBuilder {

    /**
     * Loopback-only SOCKS inbound on a random port.
     * Bridged to Android's VpnService Layer-3 TUN via native libhev-socks5-tunnel (lwIP in C).
     * Also used by the latency/speed test.
     */
    private fun socksInbound(port: Int) = JSONObject()
        .put("tag", "socks-in")
        .put("listen", "127.0.0.1")
        .put("port", port)
        .put("protocol", "socks")
        .put("settings", JSONObject().put("auth", "noauth").put("udp", true))
        .put("sniffing", JSONObject()
            .put("enabled", true)
            .put("destOverride", JSONArray().put("http").put("tls"))
            .put("routeOnly", true))

    private fun inbounds(socksPort: Int) = JSONArray().put(socksInbound(socksPort))

    private fun hasTag(outs: JSONArray, tag: String) =
        (0 until outs.length()).any { outs.getJSONObject(it).optString("tag") == tag }

    private fun ensureBuiltins(outs: JSONArray) {
        if (!hasTag(outs, "direct"))
            outs.put(JSONObject()
                .put("tag", "direct")
                .put("protocol", "freedom")
                .put("settings", JSONObject().put("domainStrategy", "UseIP")))
        if (!hasTag(outs, "block"))
            outs.put(JSONObject().put("tag", "block").put("protocol", "blackhole"))
    }

    private fun resolveHostToIpv4(host: String): String? {
        if (host.isBlank()) return null
        if (host.matches(Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}"""))) return host
        return runCatching {
            InetAddress.getAllByName(host)
                .firstOrNull { it is Inet4Address }
                ?.hostAddress
        }.getOrNull()
    }

    /**
     * Inspects outbounds to resolve domain names to IPv4 addresses before core startup.
     * Prevents circular deadlocks where the core cannot dial the proxy server because
     * it cannot resolve the proxy server's domain name.
     * Also guarantees packetEncoding = "xudp" on VLESS/VMess outbounds so UDP packets (DNS, etc.) succeed.
     */
    private fun prepareOutbounds(outs: JSONArray): Pair<Map<String, String>, List<String>> {
        val hostsMap = mutableMapOf<String, String>()
        val serverIps = mutableListOf<String>()

        for (i in 0 until outs.length()) {
            val out = outs.optJSONObject(i) ?: continue
            val protocol = out.optString("protocol").lowercase()
            val settings = out.optJSONObject("settings") ?: continue

            if (protocol == "vless" || protocol == "vmess") {
                if (!settings.has("packetEncoding")) {
                    settings.put("packetEncoding", "xudp")
                }
            }

            // vnext format (vless, vmess)
            val vnext = settings.optJSONArray("vnext")
            if (vnext != null) {
                for (j in 0 until vnext.length()) {
                    val srv = vnext.optJSONObject(j) ?: continue
                    val users = srv.optJSONArray("users")
                    if (users != null) {
                        for (k in 0 until users.length()) {
                            val u = users.optJSONObject(k) ?: continue
                            if (!u.has("packetEncoding")) {
                                u.put("packetEncoding", "xudp")
                            }
                        }
                    }
                    val rawAddress = srv.optString("address")
                    if (rawAddress.isNotBlank()) {
                        val ip = resolveHostToIpv4(rawAddress)
                        if (ip != null) {
                            srv.put("address", ip)
                            hostsMap[rawAddress] = ip
                            serverIps.add(ip)
                        } else if (rawAddress.matches(Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}"""))) {
                            serverIps.add(rawAddress)
                        }
                    }
                }
            }

            // servers format (trojan, shadowsocks)
            val servers = settings.optJSONArray("servers")
            if (servers != null) {
                for (j in 0 until servers.length()) {
                    val srv = servers.optJSONObject(j) ?: continue
                    val rawAddress = srv.optString("address")
                    if (rawAddress.isNotBlank()) {
                        val ip = resolveHostToIpv4(rawAddress)
                        if (ip != null) {
                            srv.put("address", ip)
                            hostsMap[rawAddress] = ip
                            serverIps.add(ip)
                        } else if (rawAddress.matches(Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}"""))) {
                            serverIps.add(rawAddress)
                        }
                    }
                }
            }
        }
        return Pair(hostsMap, serverIps)
    }

    private fun defaultDns(hostsMap: Map<String, String>) = JSONObject()
        .put("hosts", JSONObject().apply {
            for ((domain, ip) in hostsMap) {
                put(domain, JSONArray().put(ip))
            }
        })
        .put("servers", JSONArray()
            .put("1.1.1.1")
            .put("8.8.8.8")
            .put("9.9.9.9"))
        .put("queryStrategy", "UseIPv4")

    private fun defaultRoutingRules(serverIps: List<String>, bypassRu: Boolean = true): JSONArray {
        val rules = JSONArray()

        // 1. Direct route for the proxy server's own IP address(es) to completely prevent routing loops
        val distinctIps = serverIps.distinct().filter { it.isNotBlank() }
        if (distinctIps.isNotEmpty()) {
            val ipsArray = JSONArray()
            distinctIps.forEach { ipsArray.put(it) }
            rules.put(JSONObject()
                .put("type", "field")
                .put("ip", ipsArray)
                .put("outboundTag", "direct"))
        }

        // 2. Block DoT (port 853) to eliminate Android Private DNS leaks outside tunnel
        rules.put(JSONObject()
            .put("type", "field")
            .put("port", "853")
            .put("network", "tcp,udp")
            .put("outboundTag", "block"))

        // 3. Block QUIC (UDP 443) so browsers/apps immediately fall back to TCP TLS Vision without hanging
        rules.put(JSONObject()
            .put("type", "field")
            .put("port", "443")
            .put("network", "udp")
            .put("outboundTag", "block"))

        // 4. Bypass private LAN IPs
        rules.put(JSONObject()
            .put("type", "field")
            .put("ip", JSONArray()
                .put("geoip:private")
                .put("10.0.0.0/8")
                .put("172.16.0.0/12")
                .put("192.168.0.0/16")
                .put("127.0.0.0/8"))
            .put("outboundTag", "direct"))

        // 5. Smart Direct RU Routing (when enabled by user)
        if (bypassRu) {
            rules.put(JSONObject()
                .put("type", "field")
                .put("domain", JSONArray()
                    .put("geosite:category-gov-ru")
                    .put("geosite:ru")
                    .put("domain:ru")
                    .put("domain:su")
                    .put("domain:xn--p1ai")
                    .put("domain:gosuslugi.ru")
                    .put("domain:sberbank.ru")
                    .put("domain:tinkoff.ru")
                    .put("domain:t-bank.ru")
                    .put("domain:vtb.ru")
                    .put("domain:alfabank.ru")
                    .put("domain:ozon.ru")
                    .put("domain:wildberries.ru")
                    .put("domain:yandex.ru")
                    .put("domain:ya.ru")
                    .put("domain:vk.com")
                    .put("domain:vkvideo.ru")
                    .put("domain:kinopoisk.ru")
                    .put("domain:dzen.ru")
                    .put("domain:mail.ru")
                    .put("domain:avito.ru")
                    .put("domain:2gis.ru")
                    .put("domain:hh.ru")
                    .put("domain:rutube.ru"))
                .put("outboundTag", "direct"))

            rules.put(JSONObject()
                .put("type", "field")
                .put("ip", JSONArray().put("geoip:ru"))
                .put("outboundTag", "direct"))
        }

        return rules
    }

    /** Full config used to run the VPN (TUN inbound + local SOCKS inbound for the speed test). */
    fun forTunnel(server: Server, socksPort: Int, bypassRu: Boolean = true): String {
        server.fullConfig?.let { raw ->
            // Server-supplied config: keep its outbounds / routing / dns, but we own the inbounds.
            val cfg = JSONObject(raw)
            cfg.put("inbounds", inbounds(socksPort))
            cfg.put("log", JSONObject().put("loglevel", "warning"))
            val outs = cfg.optJSONArray("outbounds") ?: JSONArray().also { cfg.put("outbounds", it) }
            ensureBuiltins(outs)
            val (hostsMap, serverIps) = prepareOutbounds(outs)

            if (!cfg.has("dns")) {
                cfg.put("dns", defaultDns(hostsMap))
            } else {
                val dns = cfg.getJSONObject("dns")
                val hosts = dns.optJSONObject("hosts") ?: JSONObject().also { dns.put("hosts", it) }
                for ((domain, ip) in hostsMap) {
                    hosts.put(domain, JSONArray().put(ip))
                }
                if (!dns.has("queryStrategy")) {
                    dns.put("queryStrategy", "UseIPv4")
                }
            }

            val routing = cfg.optJSONObject("routing") ?: JSONObject().also { cfg.put("routing", it) }
            if (!routing.has("domainStrategy")) {
                routing.put("domainStrategy", "IPIfNonMatch")
            }
            val existingRules = routing.optJSONArray("rules") ?: JSONArray().also { routing.put("rules", it) }
            val newRules = JSONArray()

            val distinctIps = serverIps.distinct().filter { it.isNotBlank() }
            if (distinctIps.isNotEmpty()) {
                val ipsArray = JSONArray()
                distinctIps.forEach { ipsArray.put(it) }
                newRules.put(JSONObject()
                    .put("type", "field")
                    .put("ip", ipsArray)
                    .put("outboundTag", "direct"))
            }

            newRules.put(JSONObject()
                .put("type", "field")
                .put("port", "853")
                .put("network", "tcp,udp")
                .put("outboundTag", "block"))

            newRules.put(JSONObject()
                .put("type", "field")
                .put("port", "443")
                .put("network", "udp")
                .put("outboundTag", "block"))

            if (bypassRu) {
                newRules.put(JSONObject()
                    .put("type", "field")
                    .put("domain", JSONArray()
                        .put("geosite:category-gov-ru")
                        .put("geosite:ru")
                        .put("domain:ru")
                        .put("domain:su")
                        .put("domain:xn--p1ai")
                        .put("domain:gosuslugi.ru")
                        .put("domain:sberbank.ru")
                        .put("domain:tinkoff.ru")
                        .put("domain:t-bank.ru")
                        .put("domain:vtb.ru")
                        .put("domain:alfabank.ru")
                        .put("domain:ozon.ru")
                        .put("domain:wildberries.ru")
                        .put("domain:yandex.ru")
                        .put("domain:ya.ru")
                        .put("domain:vk.com")
                        .put("domain:vkvideo.ru")
                        .put("domain:kinopoisk.ru")
                        .put("domain:dzen.ru")
                        .put("domain:mail.ru")
                        .put("domain:avito.ru")
                        .put("domain:2gis.ru")
                        .put("domain:hh.ru")
                        .put("domain:rutube.ru"))
                    .put("outboundTag", "direct"))

                newRules.put(JSONObject()
                    .put("type", "field")
                    .put("ip", JSONArray().put("geoip:ru"))
                    .put("outboundTag", "direct"))
            }

            for (i in 0 until existingRules.length()) {
                newRules.put(existingRules.getJSONObject(i))
            }
            routing.put("rules", newRules)
            return cfg.toString()
        }

        val outs = JSONArray(server.outbounds ?: error("server has no outbounds"))
        ensureBuiltins(outs)
        val (hostsMap, serverIps) = prepareOutbounds(outs)

        val cfg = JSONObject()
            .put("log", JSONObject().put("loglevel", "warning"))
            .put("dns", defaultDns(hostsMap))
            .put("inbounds", inbounds(socksPort))
            .put("outbounds", outs)
            .put("routing", JSONObject()
                .put("domainStrategy", "IPIfNonMatch")
                .put("rules", defaultRoutingRules(serverIps, bypassRu)))
        return cfg.toString()
    }

    /** Minimal config (no inbound) used only for the latency test. */
    fun forPing(server: Server): String {
        server.fullConfig?.let { raw ->
            val cfg = JSONObject(raw)
            cfg.remove("inbounds")
            val outs = cfg.optJSONArray("outbounds") ?: JSONArray().also { cfg.put("outbounds", it) }
            ensureBuiltins(outs)
            return cfg.toString()
        }
        val outs = JSONArray(server.outbounds ?: error("server has no outbounds"))
        ensureBuiltins(outs)
        return JSONObject().put("outbounds", outs).toString()
    }
}
