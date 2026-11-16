package net.nexus.app.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy

class TunnelRouteTest {
    private val tunnel = Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", 10808))
    private var clock = 0L
    private val now = { clock }

    @Before fun fresh() { TunnelRoute.reset(); clock = 0L }

    @Test fun directWorksSoTunnelIsNeverUsed() {
        val routes = mutableListOf<Proxy?>()
        val r = TunnelRoute.directThenTunnel(tunnel, now) { p -> routes += p; "ok" }
        assertEquals("ok", r)
        assertEquals(listOf<Proxy?>(null), routes)
    }

    @Test fun whitelistNetworkFallsBackToTheTunnel() {
        val routes = mutableListOf<Proxy?>()
        val r = TunnelRoute.directThenTunnel(tunnel, now) { p -> routes += p; if (p == null) throw IOException("blocked") else "via tunnel" }
        assertEquals("via tunnel", r)
        assertEquals(listOf(null, tunnel), routes)
    }

    @Test fun afterAFallbackTheTunnelGoesFirstForAWhile() {
        TunnelRoute.directThenTunnel(tunnel, now) { p -> if (p == null) throw IOException("blocked") else 1 }
        clock += 60_000
        val routes = mutableListOf<Proxy?>()
        TunnelRoute.directThenTunnel(tunnel, now) { p -> routes += p; 2 }
        assertEquals(listOf<Proxy?>(tunnel), routes)

        clock += 11 * 60_000 // preference expires: direct first again
        routes.clear()
        TunnelRoute.directThenTunnel(tunnel, now) { p -> routes += p; 3 }
        assertEquals(listOf<Proxy?>(null), routes)
    }

    @Test fun noVpnMeansTheDirectErrorIsReported() {
        val e = runCatching { TunnelRoute.directThenTunnel<String>(null, now) { throw IOException("direct down") } }.exceptionOrNull()
        assertEquals("direct down", e?.message)
    }

    @Test fun httpErrorsAreNotRetriedThroughTheTunnel() {
        val routes = mutableListOf<Proxy?>()
        val e = runCatching {
            TunnelRoute.directThenTunnel<String>(tunnel, now) { p -> routes += p; error("Server error HTTP 401") }
        }.exceptionOrNull()
        assertTrue(e is IllegalStateException)
        assertEquals(listOf<Proxy?>(null), routes)
    }
}
