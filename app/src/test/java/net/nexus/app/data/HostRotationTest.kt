package net.nexus.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class HostRotationTest {
    private val defaults = listOf("nexus-net.xyz", "nexusnet.click")

    @Test fun defaultsInOrder() {
        assertEquals(defaults, HostRotation(defaults).candidates())
    }

    @Test fun blockedDomainFallsBackAndIsRemembered() {
        var saved: String? = null
        val r = HostRotation(defaults, onChange = { p, _ -> saved = p })
        val tried = mutableListOf<String>()
        val result = r.withFallback { host ->
            tried += host
            if (host == "nexus-net.xyz") throw IOException("reset by DPI")
            "ok from $host"
        }
        assertEquals("ok from nexusnet.click", result)
        assertEquals(defaults, tried)
        assertEquals("nexusnet.click", saved)
        // Next time the working domain goes first: no slow timeout on the blocked one
        assertEquals("nexusnet.click", r.candidates().first())
    }

    @Test fun httpErrorFromReachableDomainIsFinal() {
        val r = HostRotation(defaults)
        val tried = mutableListOf<String>()
        try {
            r.withFallback<String> { host -> tried += host; error("HTTP 403") }
        } catch (_: IllegalStateException) {}
        assertEquals(listOf("nexus-net.xyz"), tried)
    }

    @Test fun allDownThrowsIOException() {
        val r = HostRotation(defaults)
        var thrown = false
        try {
            r.withFallback<String> { throw IOException("down") }
        } catch (_: IOException) {
            thrown = true
        }
        assertTrue(thrown)
    }

    @Test fun serverListIsValidatedAndUsed() {
        val r = HostRotation(defaults)
        r.applyRemote(listOf("fresh-mirror.online", "bad host!", "javascript:alert(1)", "NEXUSNET.CLICK"))
        assertEquals(listOf("fresh-mirror.online", "nexusnet.click", "nexus-net.xyz"), r.candidates())
    }

    @Test fun storedPreferredIsRestored() {
        val r = HostRotation(defaults, preferred = "nexusnet.click", remote = listOf("fresh-mirror.online"))
        assertEquals(listOf("nexusnet.click", "fresh-mirror.online", "nexus-net.xyz"), r.candidates())
    }

    @Test fun subscriptionVariantsCoverEveryDomain() {
        val r = HostRotation(defaults, preferred = "nexusnet.click")
        assertEquals(
            listOf("https://chad.nexusnet.click/AbC123", "https://chad.nexus-net.xyz/AbC123"),
            r.subscriptionUrlVariants("https://chad.nexus-net.xyz/AbC123"),
        )
    }

    @Test fun foreignSubscriptionIsUntouched() {
        val r = HostRotation(defaults)
        assertEquals(listOf("https://sub.example.com/x"), r.subscriptionUrlVariants("https://sub.example.com/x"))
    }

    @Test fun nexusSubDetectionOnAllDomains() {
        val r = HostRotation(defaults)
        assertTrue(r.isNexusSubUrl("https://chad.nexus-net.xyz/AbC"))
        assertTrue(r.isNexusSubUrl("https://chad.nexusnet.click/AbC"))
        assertTrue(r.isNexusSubUrl("chad.nexusnet.click/AbC"))
        assertFalse(r.isNexusSubUrl("https://chad.nexusnet.click.evil.com/AbC"))
        assertFalse(r.isNexusSubUrl("https://example.com/chad.nexus-net.xyz"))
        assertFalse(r.isNexusSubUrl(null))
    }

    @Test fun raceSkipsSilentlyBlockedDomainQuickly() {
        val r = HostRotation(defaults)
        val started = System.currentTimeMillis()
        r.ensureFastest(probe = { host ->
            if (host == "nexus-net.xyz") { Thread.sleep(5_000); false } // DPI drops packets: hangs
            else { Thread.sleep(150); true }
        }, headStartMs = 1_000, timeoutMs = 6_000)
        val took = System.currentTimeMillis() - started
        assertEquals("nexusnet.click", r.candidates().first())
        assertTrue("race took $took ms", took < 2_000)
    }

    @Test fun rememberedFastDomainWinsWithoutRace() {
        val r = HostRotation(defaults, preferred = "nexus-net.xyz")
        val probed = java.util.Collections.synchronizedList(mutableListOf<String>())
        r.ensureFastest(probe = { host -> probed += host; true })
        assertEquals(listOf("nexus-net.xyz"), probed.toList())
        assertEquals("nexus-net.xyz", r.candidates().first())
    }

    @Test fun refusedDomainStartsRaceImmediately() {
        val r = HostRotation(defaults)
        val started = System.currentTimeMillis()
        r.ensureFastest(probe = { host -> host == "nexusnet.click" }, headStartMs = 3_000)
        assertEquals("nexusnet.click", r.candidates().first())
        assertTrue(System.currentTimeMillis() - started < 1_000)
    }

    @Test fun probeIsCachedBetweenCalls() {
        val r = HostRotation(defaults)
        var calls = 0
        r.ensureFastest(probe = { calls++; true })
        r.ensureFastest(probe = { calls++; true })
        assertEquals(1, calls)
    }

    @Test fun isKnownIgnoresWww() {
        val r = HostRotation(defaults)
        assertTrue(r.isKnown("www.nexusnet.click"))
        assertFalse(r.isKnown("nexusnet.click.evil.com"))
    }
}
