package net.nexus.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WlBypassClassificationTest {

    private fun server(name: String, raw: String? = null) = Server(
        id = name, name = name, protocol = "vless", network = "xhttp", security = "reality",
        source = "LINK", outbounds = raw,
    )

    private val wlInfraOutbound =
        """[{"protocol":"vless","settings":{"vnext":[{"address":"cdn1.wyverniumx.ru","port":443}]}},""" +
            """{"protocol":"vless","settings":{"vnext":[{"address":"188.72.112.1","port":443}]}}]"""

    @Test fun `country plus wifi on WL infrastructure stays in wifi`() {
        assertFalse(Store.isWlBypass(server("🇵🇱 Польша Wi-Fi", wlInfraOutbound)))
        assertFalse(Store.isWlBypass(server("Poland WI-FI", wlInfraOutbound)))
        assertFalse(Store.isWlBypass(server("🇩🇪 Германия LTE #1")))
    }

    @Test fun `names marked as whitelist bypass go to WL`() {
        assertTrue(Store.isWlBypass(server("LTE #1 | WL bypass")))
        assertTrue(Store.isWlBypass(server("🇷🇺 Whitelist Bypass #2")))
        assertTrue(Store.isWlBypass(server("WL-Moscow")))
        assertTrue(Store.isWlBypass(server("🇵🇱 Польша WL")))
    }

    @Test fun `wl inside other words is not a marker`() {
        assertFalse(Store.isWlBypass(server("Bowl Server")))
        assertFalse(Store.isWlBypass(server("Owl Finland")))
    }
}
