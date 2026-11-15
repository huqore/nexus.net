package net.nexus.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RussianAppsAndNetworkTest {

    @Test fun banksGovYandexVkMarketplacesAreRussian() {
        listOf(
            "ru.sberbankmobile", "com.idamob.tinkoff.android", "ru.vtb24.mobilebanking.android",
            "ru.alfabank.mobile.android", "ru.rostel", "com.yandex.browser", "ru.yandex.taxi",
            "com.vkontakte.android", "ru.ok.android", "ru.ozon.app.android", "com.wildberries.ru",
            "com.avito.android", "ru.mts.mymts", "ru.rzd.pass",
        ).forEach { assertTrue(it, RussianApps.isRussian(it)) }
    }

    @Test fun unknownAppsFromKnownDevelopersAreCaughtByPrefix() {
        assertTrue(RussianApps.isRussian("ru.yandex.some.new.app"))
        assertTrue(RussianApps.isRussian("com.vk.newthing"))
        assertTrue(RussianApps.isRussian("ru.tinkoff.business.next"))
    }

    @Test fun foreignAppsAndTheVpnItselfStayInTheTunnel() {
        listOf(
            "org.telegram.messenger", "com.google.android.youtube", "com.instagram.android",
            "com.discord", "com.zhiliaoapp.musically", "net.nexus.app", "com.android.chrome",
            "ru.example.unrelated", "com.whatsapp",
        ).forEach { assertFalse(it, RussianApps.isRussian(it)) }
    }

    private fun s(id: String, name: String, ping: Int = -1) =
        Server(id = id, name = name, protocol = "vless", network = "tcp", security = "reality", source = "BFF", outbounds = "[]", pingMs = ping)

    private val servers = listOf(
        s("de", "🇩🇪 Германия Wi-Fi", 110), s("fi", "🇫🇮 Финляндия Wi-Fi", 84),
        s("lte1", "LTE #1 | WL bypass", 150), s("lte2", "LTE #2 | WL bypass", 120),
    )

    @Test fun wifiPicksFastestRegularServer() {
        assertEquals("fi", Store.pickForNetwork(servers, onWifi = true)?.id)
    }

    @Test fun mobilePicksFastestWhitelistBypassServer() {
        assertEquals("lte2", Store.pickForNetwork(servers, onWifi = false)?.id)
    }

    @Test fun noPingYetStillPicksFromTheRightGroup() {
        val noPing = servers.map { it.copy(pingMs = -1) }
        assertEquals("lte1", Store.pickForNetwork(noPing, onWifi = false)?.id)
        assertEquals("de", Store.pickForNetwork(noPing, onWifi = true)?.id)
    }

    @Test fun emptyGroupReturnsNull() {
        assertNull(Store.pickForNetwork(servers.filterNot { Store.isWlBypass(it) }, onWifi = false))
    }
}
