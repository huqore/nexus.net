package net.nexus.app.core

import net.nexus.app.data.Server
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigBuilderTest {

    private fun sampleVlessServer(): Server {
        val stream = JSONObject()
            .put("network", "tcp")
            .put("security", "reality")
            .put("realitySettings", JSONObject()
                .put("serverName", "yahoo.com")
                .put("publicKey", "test_pub_key")
                .put("shortId", "abcd")
                .put("fingerprint", "chrome"))

        val settings = JSONObject()
            .put("vnext", JSONArray().put(JSONObject()
                .put("address", "194.87.111.222")
                .put("port", 443)
                .put("users", JSONArray().put(JSONObject()
                    .put("id", "00000000-0000-0000-0000-000000000000")
                    .put("encryption", "none")
                    .put("flow", "xtls-rprx-vision")))))
            .put("packetEncoding", "xudp")

        val proxyOutbound = JSONObject()
            .put("tag", "proxy")
            .put("protocol", "vless")
            .put("settings", settings)
            .put("streamSettings", stream)

        return Server(
            id = "server-1",
            name = "Germany WI-FI",
            protocol = "vless",
            network = "tcp",
            security = "reality",
            source = "LINK",
            outbounds = JSONArray().put(proxyOutbound).toString()
        )
    }

    @Test
    fun `tun inbound has routeOnly true and no quic in destOverride`() {
        val server = sampleVlessServer()
        val configStr = ConfigBuilder.forTunnel(server, socksPort = 10808)
        val cfg = JSONObject(configStr)

        val inbounds = cfg.getJSONArray("inbounds")
        val tun = (0 until inbounds.length())
            .map { inbounds.getJSONObject(it) }
            .first { it.getString("protocol") == "tun" }

        val sniffing = tun.getJSONObject("sniffing")
        assertTrue("sniffing must be enabled", sniffing.getBoolean("enabled"))
        assertTrue("routeOnly must be true to avoid rewriting destination IPs", sniffing.getBoolean("routeOnly"))

        val destOverride = sniffing.getJSONArray("destOverride")
        val overrides = (0 until destOverride.length()).map { destOverride.getString(it) }
        assertTrue("destOverride should include http", overrides.contains("http"))
        assertTrue("destOverride should include tls", overrides.contains("tls"))
        assertFalse("destOverride MUST NOT include quic to avoid gVisor UDP DomainAddress panic", overrides.contains("quic"))
    }

    @Test
    fun `vless outbound has packetEncoding xudp`() {
        val server = sampleVlessServer()
        val configStr = ConfigBuilder.forTunnel(server, socksPort = 10808)
        val cfg = JSONObject(configStr)

        val outbounds = cfg.getJSONArray("outbounds")
        val proxy = (0 until outbounds.length())
            .map { outbounds.getJSONObject(it) }
            .first { it.getString("tag") == "proxy" }

        val settings = proxy.getJSONObject("settings")
        assertEquals("packetEncoding must be xudp", "xudp", settings.getString("packetEncoding"))
    }

    @Test
    fun `routing rules do not loop port 53 to dns-out`() {
        val server = sampleVlessServer()
        val configStr = ConfigBuilder.forTunnel(server, socksPort = 10808)
        val cfg = JSONObject(configStr)

        val routing = cfg.getJSONObject("routing")
        val rules = routing.getJSONArray("rules")
        for (i in 0 until rules.length()) {
            val rule = rules.getJSONObject(i)
            val port = rule.optString("port")
            val outboundTag = rule.optString("outboundTag")
            assertFalse("port 53 must NOT be routed to dns-out (causes recursive deadlock)",
                port == "53" && outboundTag == "dns-out")
        }
    }

    @Test
    fun `dns configuration uses valid standard upstream IP resolvers`() {
        val server = sampleVlessServer()
        val configStr = ConfigBuilder.forTunnel(server, socksPort = 10808)
        val cfg = JSONObject(configStr)

        val dns = cfg.getJSONObject("dns")
        val servers = dns.getJSONArray("servers")
        val serverList = (0 until servers.length()).map { servers.getString(it) }
        assertTrue("contains 1.1.1.1", serverList.contains("1.1.1.1"))
        assertTrue("contains 8.8.8.8", serverList.contains("8.8.8.8"))
        assertEquals("UseIPv4", dns.getString("queryStrategy"))
    }

    @Test
    fun `server IP has direct freedom route to prevent loops`() {
        val server = sampleVlessServer()
        val configStr = ConfigBuilder.forTunnel(server, socksPort = 10808)
        val cfg = JSONObject(configStr)

        val routing = cfg.getJSONObject("routing")
        val rules = routing.getJSONArray("rules")
        val serverIpRule = (0 until rules.length())
            .map { rules.getJSONObject(it) }
            .firstOrNull { rule ->
                val ipArr = rule.optJSONArray("ip")
                ipArr != null && (0 until ipArr.length()).any { ipArr.getString(it) == "194.87.111.222" }
            }
        assertNotNull("Direct rule for server IP must exist", serverIpRule)
        assertEquals("direct", serverIpRule!!.getString("outboundTag"))
    }
}
