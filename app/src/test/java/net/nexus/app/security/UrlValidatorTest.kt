package net.nexus.app.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlValidatorTest {

    @Test
    fun testValidHttpsUrlAccepted() {
        assertTrue(UrlValidator.isSecureUrl("https://nexus-net.xyz/api/auth/telegram"))
        assertTrue(UrlValidator.isSecureUrl("https://api.github.com/repos/huqore/nexus.net/releases"))
        assertTrue(UrlValidator.isSecureUrl("https://nexus.net/sub/123"))
    }

    @Test
    fun testPlaintextHttpRejectedByDefault() {
        assertFalse(UrlValidator.isSecureUrl("http://nexus-net.xyz"))
        assertFalse(UrlValidator.isSecureUrl("http://api.github.com"))
    }

    @Test
    fun testSpoofedLocalhostHostRejected() {
        // Crucial security test: http://localhost.evil.com or http://127.0.0.1.attacker.com MUST be rejected!
        assertFalse(UrlValidator.isSecureUrl("http://localhost.evil.com", allowLoopback = true))
        assertFalse(UrlValidator.isSecureUrl("http://localhost.evil.com/sub", allowLoopback = true))
        assertFalse(UrlValidator.isSecureUrl("http://127.0.0.1.attacker.com", allowLoopback = true))
        assertFalse(UrlValidator.isSecureUrl("http://evil-localhost", allowLoopback = true))
    }

    @Test
    fun testExactLoopbackAllowedOnlyWhenExplicitlyRequested() {
        // Disallowed by default
        assertFalse(UrlValidator.isSecureUrl("http://localhost:8080", allowLoopback = false))
        assertFalse(UrlValidator.isSecureUrl("http://127.0.0.1:8080", allowLoopback = false))

        // Allowed only when allowLoopback is explicitly true
        assertTrue(UrlValidator.isSecureUrl("http://localhost:8080", allowLoopback = true))
        assertTrue(UrlValidator.isSecureUrl("http://127.0.0.1:8080", allowLoopback = true))
        assertTrue(UrlValidator.isSecureUrl("https://127.0.0.1:8080", allowLoopback = true))
    }

    @Test
    fun testUserInfoInjectionRejected() {
        // Attackers sometimes use https://legit.com@evil.com
        assertFalse(UrlValidator.isSecureUrl("https://nexus-net.xyz@evil.com/api"))
    }

    @Test
    fun testCrossProtocolRedirectBlocked() {
        val original = "https://github.com/huqore/nexus.net/releases/download/v2.4.5/app.apk"

        // HTTPS to HTTP downgrade MUST be blocked
        assertFalse(UrlValidator.isSafeRedirect(original, "http://attacker.com/evil.apk"))

        // HTTPS to relative or HTTPS redirect must be allowed
        assertTrue(UrlValidator.isSafeRedirect(original, "https://objects.githubusercontent.com/nexus.apk"))
        assertTrue(UrlValidator.isSafeRedirect(original, "/relative/path/nexus.apk"))
    }
}
