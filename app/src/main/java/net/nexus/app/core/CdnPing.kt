package net.nexus.app.core

import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket

/**
 * Latency of an LTE (Yandex CDN) server when the full ping can't answer in time.
 *
 * The normal ping starts a fresh core and makes a real request through the proxy. Through the CDN in
 * packet-up mode the first exchange of a cold connection is slow (the CDN holds the start of the
 * response), so that ping times out although the running tunnel works fine (Happ shows the same
 * empty first ping). Here we time a TLS handshake with the CDN edge under the server's own name:
 * the real delay to the entrance, and proof the CDN serves this resource (wrong/blocked = no ping).
 */
object CdnPing {
    /** Handshake time in ms, or -2 if the edge is unreachable or doesn't serve this name. */
    fun measure(pingConfig: String, timeoutMs: Int = 5_000): Int = runCatching {
        val proxy = JSONObject(pingConfig).getJSONArray("outbounds").let { arr ->
            (0 until arr.length()).map { arr.getJSONObject(it) }.first { it.optString("tag") == "proxy" }
        }
        val srv = proxy.getJSONObject("settings").getJSONArray("vnext").getJSONObject(0)
        val host = srv.getString("address")
        val port = srv.optInt("port", 443)
        val sni = proxy.optJSONObject("streamSettings")?.optJSONObject("tlsSettings")?.optString("serverName")
            ?.takeIf { it.isNotBlank() } ?: host
        handshake(host, port, sni, timeoutMs)
    }.getOrDefault(-2)

    private fun handshake(host: String, port: Int, sni: String, timeoutMs: Int): Int {
        val start = System.nanoTime()
        Socket().use { raw ->
            raw.connect(InetSocketAddress(host, port), timeoutMs)
            raw.soTimeout = timeoutMs
            val ssl = SSLContext.getDefault().socketFactory.createSocket(raw, sni, port, false) as SSLSocket
            ssl.use {
                it.sslParameters = it.sslParameters.apply { serverNames = listOf(SNIHostName(sni)) }
                it.startHandshake() // certificate must be valid for the name: the CDN really serves it
            }
        }
        return ((System.nanoTime() - start) / 1_000_000).toInt().coerceAtLeast(1)
    }
}
