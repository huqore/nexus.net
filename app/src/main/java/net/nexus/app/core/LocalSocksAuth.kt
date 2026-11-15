package net.nexus.app.core

import java.net.Authenticator
import java.net.PasswordAuthentication
import java.security.SecureRandom

/**
 * Per-session credentials for the core's loopback SOCKS inbound.
 *
 * Every app on the device can reach 127.0.0.1, so a no-auth inbound lets any of them push traffic
 * through the VPN (bypassing split tunneling) and learn the exit IP. The inbound therefore requires
 * a random username/password that only this process knows: hev-socks5-tunnel gets it via its YAML
 * config, and the app's own proxied requests (speed test, update check/download) get it through
 * the default [Authenticator], which answers only for the current loopback SOCKS port.
 */
object LocalSocksAuth {
    private val random = SecureRandom()

    @Volatile var user: String = newSecret(); private set
    @Volatile var pass: String = newSecret(); private set
    @Volatile private var port: Int = 0

    /** Fresh credentials for a new core session; returns them for config building. */
    @Synchronized
    fun rotate(socksPort: Int): Pair<String, String> {
        user = newSecret()
        pass = newSecret()
        port = socksPort
        install()
        return user to pass
    }

    private var installed = false

    @Synchronized
    private fun install() {
        if (installed) return
        installed = true
        Authenticator.setDefault(object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication? {
                val isOurSocks = requestingProtocol?.startsWith("SOCKS", ignoreCase = true) == true &&
                    port > 0 && requestingPort == port &&
                    (requestingHost == "127.0.0.1" || requestingHost == "localhost" ||
                        requestingSite?.isLoopbackAddress == true)
                return if (isOurSocks) PasswordAuthentication(user, pass.toCharArray()) else null
            }
        })
    }

    private fun newSecret(): String {
        val bytes = ByteArray(16).also { random.nextBytes(it) }
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
