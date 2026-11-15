package net.nexus.app.data

import android.content.Context
import java.io.IOException

/**
 * NEXUS backend domains, tried in order. nexus-net.xyz is blocked in Russia, nexusnet.click is the
 * direct mirror (same server). The last domain that answered is tried first next time, and the
 * server can hand out a fresh list in the handshake (appConfig.apiHosts), so a future block only
 * needs a new domain on the server, not a new app build.
 */
object ApiHosts {
    val DEFAULT_HOSTS = listOf("nexus-net.xyz", "nexusnet.click")

    private const val PREFS = "nexus_api_hosts"
    private const val KEY_PREFERRED = "preferred"
    private const val KEY_REMOTE = "remote"

    @Volatile
    private var rotation = HostRotation(DEFAULT_HOSTS)

    fun init(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        rotation = HostRotation(
            defaults = DEFAULT_HOSTS,
            preferred = prefs.getString(KEY_PREFERRED, null),
            remote = prefs.getString(KEY_REMOTE, null)?.split(',')?.filter { it.isNotBlank() } ?: emptyList(),
            onChange = { preferred, remote ->
                prefs.edit().putString(KEY_PREFERRED, preferred).putString(KEY_REMOTE, remote.joinToString(",")).apply()
            },
        )
    }

    /** Domains in the order to try them. */
    fun candidates(): List<String> = rotation.candidates()

    /** Domain to show / open in the browser (the one that worked last). */
    fun primary(): String = rotation.candidates().first()

    fun siteUrl(): String = "https://${primary()}"

    fun isOurHost(host: String?): Boolean = rotation.isKnown(host)

    fun markWorking(host: String) = rotation.markWorking(host)

    fun applyRemote(hosts: List<String>) = rotation.applyRemote(hosts)

    /**
     * Picks the fastest reachable domain once per ~10 minutes: the remembered one gets 1.5 s alone,
     * then all are probed in parallel and the first answer wins. A blocked domain that silently drops
     * packets therefore costs ~1.5 s instead of a full connect timeout.
     */
    fun ensureFastest(probe: (host: String) -> Boolean) = rotation.ensureFastest(probe)

    /**
     * Runs [block] against each domain until one answers. A network failure (DNS block, reset,
     * forged certificate, timeout) moves on to the next domain; any HTTP answer counts as reachable.
     */
    fun <T> withFallback(block: (host: String) -> T): T = rotation.withFallback(block)

    /** The same subscription link on every NEXUS domain (chad.<domain>), working one first. */
    fun subscriptionUrlVariants(url: String): List<String> = rotation.subscriptionUrlVariants(url)

    /** True for a NEXUS subscription link on any of our domains (chad.nexus-net.xyz, chad.nexusnet.click, …). */
    fun isNexusSubUrl(url: String?): Boolean = rotation.isNexusSubUrl(url)
}

/** Pure logic behind [ApiHosts] (no Android), unit-tested. */
class HostRotation(
    private val defaults: List<String>,
    preferred: String? = null,
    remote: List<String> = emptyList(),
    private val onChange: (preferred: String?, remote: List<String>) -> Unit = { _, _ -> },
) {
    @Volatile private var preferred: String? = preferred?.lowercase()?.takeIf { isValidHost(it) }
    @Volatile private var remote: List<String> = remote.map { it.lowercase() }.filter { isValidHost(it) }

    fun candidates(): List<String> =
        (listOfNotNull(preferred) + remote + defaults).distinct().ifEmpty { defaults }

    fun isKnown(host: String?): Boolean {
        val h = host?.lowercase()?.removePrefix("www.") ?: return false
        return candidates().contains(h)
    }

    fun markWorking(host: String) {
        val h = host.lowercase()
        if (h != preferred && isKnown(h)) {
            preferred = h
            onChange(preferred, remote)
        }
    }

    fun applyRemote(hosts: List<String>) {
        val clean = hosts.map { it.trim().lowercase() }.filter { isValidHost(it) }.distinct().take(MAX_REMOTE)
        if (clean.isNotEmpty() && clean != remote) {
            remote = clean
            onChange(preferred, remote)
        }
    }

    fun <T> withFallback(block: (host: String) -> T): T {
        var last: IOException? = null
        for ((i, host) in candidates().withIndex()) {
            try {
                val result = block(host)
                markWorking(host)
                return result
            } catch (e: IOException) {
                last = e
                // The chosen domain stopped answering (network changed, new block): re-probe next time
                if (i == 0) verifiedAt = 0L
            }
        }
        throw last ?: IOException("No NEXUS domain reachable")
    }

    @Volatile private var verifiedAt = 0L
    private val probeLock = Any()

    fun ensureFastest(
        probe: (host: String) -> Boolean,
        headStartMs: Long = 1_500,
        timeoutMs: Long = 6_000,
        now: () -> Long = System::currentTimeMillis,
    ) {
        if (now() - verifiedAt < VERIFIED_FOR_MS) return
        synchronized(probeLock) {
            if (now() - verifiedAt < VERIFIED_FOR_MS) return
            val hosts = candidates()
            val answers = java.util.concurrent.LinkedBlockingQueue<Pair<String, Boolean>>()
            fun launch(h: String) = Thread {
                answers.offer(h to runCatching { probe(h) }.getOrDefault(false))
            }.apply { isDaemon = true; start() }

            val start = now()
            var started = 1
            var finished = 0
            launch(hosts.first())
            while (finished < hosts.size) {
                // The remembered domain runs alone for the head start, then everyone races
                val othersPending = started < hosts.size
                val wait = if (othersPending) headStartMs - (now() - start) else timeoutMs - (now() - start)
                val got = if (wait > 0) answers.poll(wait, java.util.concurrent.TimeUnit.MILLISECONDS) else null
                if (got == null) {
                    if (othersPending) {
                        hosts.drop(started).forEach { launch(it) }
                        started = hosts.size
                        continue
                    }
                    break // nobody answered in time
                }
                finished++
                if (got.second) {
                    markWorking(got.first)
                    verifiedAt = now()
                    return
                }
                if (othersPending && finished >= started) {
                    hosts.drop(started).forEach { launch(it) }
                    started = hosts.size
                }
            }
            // Nothing answered (offline?): keep the order, try probing again in 15 s
            verifiedAt = now() - VERIFIED_FOR_MS + 15_000
        }
    }

    fun subscriptionUrlVariants(url: String): List<String> {
        val m = SUB_URL.matchEntire(url.trim()) ?: return listOf(url)
        val (sub, host, rest) = m.destructured
        if (!isKnown(host)) return listOf(url)
        return (candidates().map { "https://$sub.$it$rest" } + url).distinct()
    }

    fun isNexusSubUrl(url: String?): Boolean {
        val u = url?.trim()?.lowercase() ?: return false
        return candidates().any { u.startsWith("https://chad.$it/") || u.startsWith("chad.$it") || u == "https://chad.$it" }
    }

    companion object {
        private const val VERIFIED_FOR_MS = 10 * 60 * 1000L
        private const val MAX_REMOTE = 8
        private val HOST_RE = Regex("^(?=.{4,253}$)([a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,63}$")
        private val SUB_URL = Regex("^https://([a-z0-9-]+)\\.([a-z0-9.-]+\\.[a-z]{2,63})(/.*)?$", RegexOption.IGNORE_CASE)

        fun isValidHost(h: String): Boolean = HOST_RE.matches(h)
    }
}
