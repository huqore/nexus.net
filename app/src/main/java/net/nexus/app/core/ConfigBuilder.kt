package net.nexus.app.core

import net.nexus.app.data.Server
import org.json.JSONArray
import org.json.JSONObject

/** Turns a [Server] into the JSON config the Xray core runs. */
object ConfigBuilder {

    private fun tunInbound() = JSONObject()
        .put("tag", "tun-in")
        .put("protocol", "tun")
        .put("settings", JSONObject()
            .put("name", "xray0")
            .put("mtu", 1500)
            .put("MTU", 1500)
            .put("userLevel", 0))
        .put("sniffing", JSONObject()
            .put("enabled", true)
            .put("destOverride", JSONArray().put("http").put("tls").put("quic"))
            .put("routeOnly", false))

    /**
     * Loopback-only SOCKS inbound on a random port. The app itself is excluded from the TUN, so this is how
     * the speed test sends traffic *through* the VPN outbound.
     */
    private fun socksInbound(port: Int) = JSONObject()
        .put("tag", "socks-in")
        .put("listen", "127.0.0.1")
        .put("port", port)
        .put("protocol", "socks")
        .put("settings", JSONObject().put("auth", "noauth").put("udp", true))

    private fun inbounds(socksPort: Int) = JSONArray().put(tunInbound()).put(socksInbound(socksPort))

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

    private fun defaultDns() = JSONObject()
        .put("servers", JSONArray()
            .put("https://1.1.1.1/dns-query")
            .put("https://8.8.8.8/dns-query")
            .put("1.1.1.1")
            .put("8.8.8.8"))
        .put("queryStrategy", "UseIPv4")

    private fun defaultRoutingRules(): JSONArray {
        val rules = JSONArray()

        // 1. Block QUIC (UDP 443) so browsers/apps immediately fall back to TCP TLS Vision without hanging
        rules.put(JSONObject()
            .put("type", "field")
            .put("port", "443")
            .put("network", "udp")
            .put("outboundTag", "block"))

        // 2. Intercept DNS traffic (port 53) from TUN inbound and route to internal DNS module
        rules.put(JSONObject()
            .put("type", "field")
            .put("inboundTag", JSONArray().put("tun-in"))
            .put("port", "53")
            .put("outboundTag", "dns-out"))

        // 3. Bypass private LAN IPs
        rules.put(JSONObject()
            .put("type", "field")
            .put("ip", JSONArray().put("geoip:private"))
            .put("outboundTag", "direct"))

        return rules
    }

    /** Full config used to run the VPN (TUN inbound + local SOCKS inbound for the speed test). */
    fun forTunnel(server: Server, socksPort: Int): String {
        server.fullConfig?.let { raw ->
            // Server-supplied config: keep its outbounds / routing / dns, but we own the inbounds.
            val cfg = JSONObject(raw)
            cfg.put("inbounds", inbounds(socksPort))
            cfg.put("log", JSONObject().put("loglevel", "warning"))
            val outs = cfg.optJSONArray("outbounds") ?: JSONArray().also { cfg.put("outbounds", it) }
            ensureBuiltins(outs)
            if (!cfg.has("dns")) {
                cfg.put("dns", defaultDns())
            }
            val routing = cfg.optJSONObject("routing") ?: JSONObject().also { cfg.put("routing", it) }
            if (!routing.has("domainStrategy")) {
                routing.put("domainStrategy", "IPIfNonMatch")
            }
            val existingRules = routing.optJSONArray("rules") ?: JSONArray().also { routing.put("rules", it) }
            val newRules = JSONArray()
            newRules.put(JSONObject()
                .put("type", "field")
                .put("port", "443")
                .put("network", "udp")
                .put("outboundTag", "block"))
            newRules.put(JSONObject()
                .put("type", "field")
                .put("inboundTag", JSONArray().put("tun-in"))
                .put("port", "53")
                .put("outboundTag", "dns-out"))
            for (i in 0 until existingRules.length()) {
                newRules.put(existingRules.getJSONObject(i))
            }
            routing.put("rules", newRules)
            return cfg.toString()
        }

        val outs = JSONArray(server.outbounds ?: error("server has no outbounds"))
        ensureBuiltins(outs)
        val cfg = JSONObject()
            .put("log", JSONObject().put("loglevel", "warning"))
            .put("dns", defaultDns())
            .put("inbounds", inbounds(socksPort))
            .put("outbounds", outs)
            .put("routing", JSONObject()
                .put("domainStrategy", "IPIfNonMatch")
                .put("rules", defaultRoutingRules()))
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
