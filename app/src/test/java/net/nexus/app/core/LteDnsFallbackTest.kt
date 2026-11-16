package net.nexus.app.core

import net.nexus.app.data.Server
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * LTE configs reach Yandex CDN by name (a.flarya.gay…). On whitelist-only mobile networks only the
 * operator's resolver works, so the address must turn into an IP before the core starts, and an LTE
 * config must still connect when that lookup fails.
 */
class LteDnsFallbackTest {
    private val realResolver = ConfigBuilder.systemResolver

    @Before fun noRealDns() { ConfigBuilder.systemResolver = { null } }
    @After fun restore() { ConfigBuilder.systemResolver = realResolver }

    private fun lteServer(address: String, pinned: Map<String, String> = emptyMap()) = Server(
        id = "lte", name = "LTE #1 | WL bypass", protocol = "json", network = "xhttp", security = "tls", source = "JSON",
        fullConfig = JSONObject()
            .put("dns", JSONObject().put("hosts", JSONObject().apply { pinned.forEach { (h, ip) -> put(h, JSONArray().put(ip)) } }))
            .put("outbounds", JSONArray().put(JSONObject()
                .put("tag", "proxy").put("protocol", "vless")
                .put("settings", JSONObject().put("vnext", JSONArray().put(JSONObject().put("address", address).put("port", 443))))))
            .toString(),
    )

    private fun wifiServer(address: String) = Server(
        id = "wifi", name = "Germany WI-FI", protocol = "vless", network = "tcp", security = "reality", source = "LINK",
        outbounds = JSONArray().put(JSONObject()
            .put("tag", "proxy").put("protocol", "vless")
            .put("settings", JSONObject().put("vnext", JSONArray().put(JSONObject().put("address", address).put("port", 8443)))))
            .toString(),
    )

    private fun proxyAddress(config: String): String =
        JSONObject(config).getJSONArray("outbounds").getJSONObject(0)
            .getJSONObject("settings").getJSONArray("vnext").getJSONObject(0).getString("address")

    @Test fun operatorDnsAnswerIsUsedFirst() {
        ConfigBuilder.systemResolver = { if (it == "b.flarya.gay") "188.72.112.7" else null }
        assertEquals("188.72.112.7", proxyAddress(ConfigBuilder.forTunnel(lteServer("b.flarya.gay"), 1080)))
    }

    @Test fun subscriptionPinnedAddressIsUsedWhenDnsFails() {
        val cfg = ConfigBuilder.forTunnel(lteServer("a.cdn.foggate.net", mapOf("a.cdn.foggate.net" to "188.72.111.36")), 1080)
        assertEquals("188.72.111.36", proxyAddress(cfg))
    }

    @Test fun lteFallsBackToYandexEdgeWhenNothingResolves() {
        assertEquals(ConfigBuilder.YANDEX_CDN_EDGES.first(), proxyAddress(ConfigBuilder.forTunnel(lteServer("b.flarya.gay"), 1080)))
        assertEquals(ConfigBuilder.YANDEX_CDN_EDGES.first(), proxyAddress(ConfigBuilder.forPing(lteServer("b.flarya.gay"))))
    }

    @Test fun wifiServerIsNeverSentToTheCdnEdge() {
        assertEquals("nl.example.net", proxyAddress(ConfigBuilder.forTunnel(wifiServer("nl.example.net"), 1080)))
        assertNull(ConfigBuilder.resolveServerAddress("nl.example.net", emptyMap(), isWl = false))
    }

    @Test fun ipLiteralsPassThrough() {
        assertEquals("191.217.204.169", ConfigBuilder.resolveServerAddress("191.217.204.169", emptyMap(), isWl = true))
    }

    @Test fun retiredDomainIsNoLongerHardCoded() {
        val cfg = ConfigBuilder.forTunnel(lteServer("b.flarya.gay"), 1080) + ConfigBuilder.forPing(lteServer("b.flarya.gay"))
        assertFalse(cfg.contains("wyverniumx"))
    }
}
