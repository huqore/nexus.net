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
     * Also used by the latency/speed test. Password-protected: see [LocalSocksAuth].
     */
    private fun socksInbound(port: Int, user: String, pass: String) = JSONObject()
        .put("tag", "socks")
        .put("listen", "127.0.0.1")
        .put("port", port)
        .put("protocol", "socks")
        .put("settings", JSONObject()
            .put("auth", "password")
            .put("accounts", JSONArray().put(JSONObject().put("user", user).put("pass", pass)))
            .put("udp", true)
            .put("ip", "127.0.0.1"))
        .put("sniffing", JSONObject()
            .put("enabled", true)
            .put("destOverride", JSONArray().put("http").put("tls"))
            .put("routeOnly", false))

    private fun inbounds(socksPort: Int, user: String, pass: String) =
        JSONArray().put(socksInbound(socksPort, user, pass))

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
        if (!hasTag(outs, "dns-out"))
            outs.put(JSONObject().put("tag", "dns-out").put("protocol", "dns"))
    }

    private fun resolveHostToIpv4(host: String): String? {
        if (host.isBlank()) return null
        if (host.matches(Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}"""))) return host
        if (host.equals("cdn1.wyverniumx.ru", ignoreCase = true) || host.equals("cdn.wyverniumx.ru", ignoreCase = true)) {
            val resolved = runCatching {
                InetAddress.getAllByName(host)
                    .firstOrNull { it is Inet4Address }
                    ?.hostAddress
            }.getOrNull()
            return resolved ?: "188.72.112.1"
        }
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

    private fun defaultDns(hostsMap: Map<String, String>, blockAds: Boolean = false, isWl: Boolean = false) = JSONObject()
        .put("hosts", JSONObject().apply {
            put("cdn1.wyverniumx.ru", JSONArray().put("188.72.112.1").put("188.72.111.36"))
            put("cdn.wyverniumx.ru", JSONArray().put("188.72.112.1").put("188.72.111.36"))
            for ((domain, ip) in hostsMap) {
                put(domain, JSONArray().put(ip))
            }
        })
        .put("servers", if (blockAds) {
            JSONArray().put("94.140.14.14").put("94.140.15.15")
        } else {
            JSONArray().put("1.1.1.1").put("8.8.8.8").put("9.9.9.9")
        })
        .put("queryStrategy", "UseIPv4")

    private fun defaultRoutingRules(serverIps: List<String>, isWl: Boolean = false): JSONArray {
        val rules = JSONArray()

        // 1. Direct route for the proxy server's own IP address(es) to completely prevent routing loops
        val distinctIps = (serverIps + if (isWl) listOf("188.72.112.1", "188.72.111.36") else emptyList()).distinct().filter { it.isNotBlank() }
        if (distinctIps.isNotEmpty()) {
            val ipsArray = JSONArray()
            distinctIps.forEach { ipsArray.put(it) }
            rules.put(JSONObject()
                .put("type", "field")
                .put("ip", ipsArray)
                .put("outboundTag", "direct"))
        }

        if (isWl) {
            rules.put(JSONObject()
                .put("type", "field")
                .put("ip", JSONArray().put("188.72.112.0/24").put("188.72.111.0/24"))
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


        // 5. Bypass private LAN IPs
        rules.put(JSONObject()
            .put("type", "field")
            .put("ip", JSONArray()
                .put("geoip:private")
                .put("10.0.0.0/8")
                .put("172.16.0.0/12")
                .put("192.168.0.0/16")
                .put("127.0.0.0/8"))
            .put("outboundTag", "direct"))

        // 6. Direct routing for Russian traffic ONLY when NOT in WL bypass mode.
        // During WL bypass mode, direct Russian routing fails due to operator whitelist blocks.
        if (!isWl) {
            rules.put(JSONObject()
                .put("type", "field")
                .put("domain", JSONArray()
                    .put("geosite:category-ru")
                    .put("domain:ru")
                    .put("domain:su")
                    .put("domain:xn--p1ai"))
                .put("outboundTag", "direct"))

            rules.put(JSONObject()
                .put("type", "field")
                .put("ip", JSONArray().put("geoip:ru"))
                .put("outboundTag", "direct"))
        }

        return rules
    }

    /** Full config used to run the VPN (TUN inbound + local SOCKS inbound for the speed test). */
    fun forTunnel(
        server: Server,
        socksPort: Int,
        blockAds: Boolean = false,
        socksUser: String = LocalSocksAuth.user,
        socksPass: String = LocalSocksAuth.pass,
    ): String {
        val isWl = net.nexus.app.data.Store.isWlBypass(server)
        server.fullConfig?.let { raw ->
            // Server-supplied config: keep its outbounds / routing / dns, but we own the inbounds.
            val cfg = JSONObject(raw)
            cfg.put("inbounds", inbounds(socksPort, socksUser, socksPass))
            cfg.put("log", JSONObject().put("loglevel", "warning"))
            val outs = cfg.optJSONArray("outbounds") ?: JSONArray().also { cfg.put("outbounds", it) }
            ensureBuiltins(outs)
            val (hostsMap, serverIps) = prepareOutbounds(outs)

            if (!cfg.has("dns")) {
                cfg.put("dns", defaultDns(hostsMap, blockAds, isWl))
            } else {
                val dns = cfg.getJSONObject("dns")
                if (blockAds) {
                    dns.put("servers", JSONArray().put("94.140.14.14").put("94.140.15.15"))
                } else if (!dns.has("servers") || dns.getJSONArray("servers").length() == 0) {
                    dns.put("servers", JSONArray().put("1.1.1.1").put("8.8.8.8").put("9.9.9.9"))
                }
                val hosts = dns.optJSONObject("hosts") ?: JSONObject().also { dns.put("hosts", it) }
                hosts.put("cdn1.wyverniumx.ru", JSONArray().put("188.72.112.1").put("188.72.111.36"))
                hosts.put("cdn.wyverniumx.ru", JSONArray().put("188.72.112.1").put("188.72.111.36"))
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

            val distinctIps = (serverIps + if (isWl) listOf("188.72.112.1", "188.72.111.36") else emptyList()).distinct().filter { it.isNotBlank() }
            if (distinctIps.isNotEmpty()) {
                val ipsArray = JSONArray()
                distinctIps.forEach { ipsArray.put(it) }
                newRules.put(JSONObject()
                    .put("type", "field")
                    .put("ip", ipsArray)
                    .put("outboundTag", "direct"))
            }

            if (isWl) {
                newRules.put(JSONObject()
                    .put("type", "field")
                    .put("ip", JSONArray().put("188.72.112.0/24").put("188.72.111.0/24"))
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

            for (i in 0 until existingRules.length()) {
                val r = existingRules.getJSONObject(i)
                if (isWl && r.optString("outboundTag") == "direct") {
                    val doms = r.optJSONArray("domain")
                    val ips = r.optJSONArray("ip")
                    val isRuRule = (doms != null && (0 until doms.length()).any { doms.getString(it).contains("ru") }) ||
                                   (ips != null && (0 until ips.length()).any { ips.getString(it).contains(":ru") })
                    if (isRuRule) continue
                }
                newRules.put(r)
            }
            routing.put("rules", newRules)
            return cfg.toString()
        }

        val outs = JSONArray(server.outbounds ?: error("server has no outbounds"))
        ensureBuiltins(outs)
        val (hostsMap, serverIps) = prepareOutbounds(outs)

        val cfg = JSONObject()
            .put("log", JSONObject().put("loglevel", "warning"))
            .put("dns", defaultDns(hostsMap, blockAds, isWl))
            .put("inbounds", inbounds(socksPort, socksUser, socksPass))
            .put("outbounds", outs)
            .put("routing", JSONObject()
                .put("domainMatcher", "hybrid")
                .put("domainStrategy", "IPIfNonMatch")
                .put("rules", defaultRoutingRules(serverIps, isWl)))
        return cfg.toString()
    }

    /** Minimal config (no inbound) used only for the latency test. */
    fun forPing(server: Server): String {
        val isWl = net.nexus.app.data.Store.isWlBypass(server)
        server.fullConfig?.let { raw ->
            val cfg = JSONObject(raw)
            cfg.remove("inbounds")
            val outs = cfg.optJSONArray("outbounds") ?: JSONArray().also { cfg.put("outbounds", it) }
            ensureBuiltins(outs)
            val (hostsMap, _) = prepareOutbounds(outs)
            val dns = cfg.optJSONObject("dns") ?: defaultDns(hostsMap, isWl = isWl).also { cfg.put("dns", it) }
            val hosts = dns.optJSONObject("hosts") ?: JSONObject().also { dns.put("hosts", it) }
            hosts.put("cdn1.wyverniumx.ru", JSONArray().put("188.72.112.1").put("188.72.111.36"))
            hosts.put("cdn.wyverniumx.ru", JSONArray().put("188.72.112.1").put("188.72.111.36"))
            for ((domain, ip) in hostsMap) {
                hosts.put(domain, JSONArray().put(ip))
            }
            return cfg.toString()
        }
        val outs = JSONArray(server.outbounds ?: error("server has no outbounds"))
        ensureBuiltins(outs)
        val (hostsMap, _) = prepareOutbounds(outs)
        val cfg = JSONObject()
            .put("dns", defaultDns(hostsMap, isWl = isWl))
            .put("outbounds", outs)
        return cfg.toString()
    }
}
