package net.nexus.app.vpn

import net.nexus.app.core.XrayCore
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy

/**
 * The app itself always stays outside the VPN (its core sockets must not loop into the TUN), so its
 * API and subscription requests normally go straight to our servers. On whitelist-only mobile networks
 * those servers are unreachable directly, while the running tunnel (LTE through Yandex CDN) still
 * works. Requests therefore go direct first and, only if that fails at the network level, retry
 * through the core's loopback SOCKS inbound (credentials answered by LocalSocksAuth).
 *
 * Direct stays first by default: through the tunnel every user of a node shares one exit IP, which
 * would trip per-IP limits on the site. Once direct has failed and the tunnel worked, the tunnel goes
 * first for a while, so a whitelist network does not pay the direct timeouts on every request.
 */
object TunnelRoute {
    private const val TUNNEL_FIRST_MS = 10 * 60_000L

    @Volatile private var tunnelFirstUntil = 0L

    /** Loopback SOCKS of the running core, or null when the VPN is not connected. */
    fun proxy(): Proxy? {
        val port = VpnState.socksPort
        if (port <= 0 || VpnState.status.value != VpnStatus.CONNECTED || !XrayCore.isRunning) return null
        return Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", port))
    }

    /**
     * Runs [block] with a null proxy (direct) or the tunnel proxy, falling back to the other route on a
     * network error. HTTP-level errors are not IOExceptions in our callers and are never retried.
     */
    fun <T> directThenTunnel(block: (proxy: Proxy?) -> T): T =
        directThenTunnel(proxy(), System::currentTimeMillis, block)

    /** Pure form of [directThenTunnel] for tests. */
    fun <T> directThenTunnel(tunnel: Proxy?, now: () -> Long, block: (proxy: Proxy?) -> T): T {
        if (tunnel != null && now() < tunnelFirstUntil) {
            try {
                return block(tunnel)
            } catch (_: IOException) {
                tunnelFirstUntil = 0L // the network changed: back to direct-first
            }
            return block(null)
        }
        try {
            return block(null)
        } catch (direct: IOException) {
            if (tunnel == null) throw direct
            val result = try {
                block(tunnel)
            } catch (viaTunnel: IOException) {
                viaTunnel.addSuppressed(direct)
                throw viaTunnel
            }
            tunnelFirstUntil = now() + TUNNEL_FIRST_MS
            return result
        }
    }

    /** Test hook: forget the learned preference. */
    internal fun reset() { tunnelFirstUntil = 0L }
}
