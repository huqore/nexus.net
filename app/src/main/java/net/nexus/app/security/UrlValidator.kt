package net.nexus.app.security

import java.net.URI

object UrlValidator {

    /**
     * Strictly verifies whether a URL is secure.
     * Enforces HTTPS. Disallows cross-protocol downgrade and spoofed hosts.
     */
    fun isSecureUrl(url: String, allowLoopback: Boolean = false): Boolean {
        if (url.isBlank()) return false
        val uri = try {
            URI(url.trim())
        } catch (_: Exception) {
            return false
        }

        val scheme = uri.scheme?.lowercase() ?: return false
        val host = uri.host?.lowercase() ?: return false

        // Exact loopback check
        val isLoopback = host == "localhost" || host == "127.0.0.1" || host == "::1"
        if (allowLoopback && isLoopback) {
            return scheme == "https" || scheme == "http"
        }

        // Must strictly be HTTPS for all external internet hosts
        if (scheme != "https") {
            return false
        }

        // Host sanity check: reject empty host or host with userinfo injection
        if (host.isEmpty() || uri.rawUserInfo != null) {
            return false
        }

        return true
    }

    /**
     * Validates redirect safety: original URL and target redirect must both be HTTPS,
     * preventing SSL stripping / cross-protocol downgrade (e.g. https -> http).
     */
    fun isSafeRedirect(originalUrl: String, redirectLocation: String): Boolean {
        if (!isSecureUrl(originalUrl, allowLoopback = false)) return false

        val originalUri = try { URI(originalUrl.trim()) } catch (_: Exception) { return false }
        val targetUri = try { originalUri.resolve(redirectLocation.trim()) } catch (_: Exception) { return false }

        val targetUrl = targetUri.toString()
        if (!isSecureUrl(targetUrl, allowLoopback = false)) {
            return false
        }

        // Must remain HTTPS: no protocol downgrade
        if (!targetUri.scheme.equals("https", ignoreCase = true)) {
            return false
        }

        return true
    }
}

