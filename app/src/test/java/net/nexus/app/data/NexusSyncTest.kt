package net.nexus.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NexusSyncTest {
    private fun s(id: String, name: String, ping: Int = -1, out: String = "[]") =
        Server(id = id, name = name, protocol = "vless", network = "tcp", security = "reality", source = "BFF", outbounds = out, pingMs = ping)

    @Test fun refreshKeepsIdsAndPings() {
        val list = listOf(Provider(id = NexusSync.PROVIDER_ID, name = "NEXUS", servers = listOf(s("node_de_1", "DE", 38), s("node_fi_1", "FI", 45))))
        // Fallback source: random ids, no pings
        val fresh = listOf(s("uuid-1", "DE"), s("uuid-2", "FI"), s("uuid-3", "NL"))
        val out = NexusSync.withNexus(list, fresh, 0, 0, 0, 0)
        val servers = out.single().servers
        assertEquals(listOf("node_de_1", "node_fi_1", "uuid-3"), servers.map { it.id })
        assertEquals(listOf(38, 45, -1), servers.map { it.pingMs })
    }

    @Test fun subscriptionUrlGroupIsFoldedIntoOne() {
        val list = listOf(
            Provider(id = NexusSync.PROVIDER_ID, name = "NEXUS", servers = listOf(s("a", "DE"))),
            Provider(id = "x", name = "NEXUS sub", url = "https://chad.nexus-net.xyz/abc", servers = listOf(s("b", "DE"))),
            Provider(id = "other", name = "Other", url = "https://example.com/sub", servers = listOf(s("c", "US"))),
        )
        val out = NexusSync.withNexus(list, listOf(s("new", "DE")), 0, 0, 0, 0)
        assertEquals(listOf(NexusSync.PROVIDER_ID, "other"), out.map { it.id })
        assertEquals("a", out.first().servers.single().id)
    }

    @Test fun withoutNexusDropsOnlyNexus() {
        val list = listOf(
            Provider(id = NexusSync.PROVIDER_ID, name = "NEXUS", servers = listOf(s("a", "DE"))),
            Provider(id = "other", name = "Other", url = "https://example.com/sub", servers = listOf(s("c", "US"))),
        )
        val out = NexusSync.withoutNexus(list)
        assertEquals(listOf("other"), out.map { it.id })
        assertTrue(out.none { NexusSync.isNexus(it) })
    }
}
