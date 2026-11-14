package net.nexus.app.data

/**
 * The account's NEXUS servers (from the handshake, or the subscription URL as a fallback) live in
 * ONE provider with a fixed id. Every sync goes through [withNexus], so:
 *  - server ids and ping results survive refreshes (matched by name), whichever source they came from;
 *  - there is never a second NEXUS group next to the first one.
 */
object NexusSync {
    const val PROVIDER_ID = "nexus_provider"

    /** The account's own provider, or a subscription-URL provider that belongs to NEXUS. */
    fun isNexus(p: Provider): Boolean =
        p.id == PROVIDER_ID || p.url?.contains("chad.nexus-net.xyz", ignoreCase = true) == true ||
            (!p.isLocal && p.name.contains("NEXUS", ignoreCase = true))

    /** Keeps ids (and therefore the selected server) and ping values of servers that are still there. */
    fun mergeServers(old: List<Server>, new: List<Server>): List<Server> {
        val byName = old.groupBy { it.name }.mapValues { it.value.toMutableList() }
        return new.map { n ->
            val match = byName[n.name]?.removeFirstOrNull()
            if (match != null) n.copy(id = match.id, pingMs = match.pingMs) else n
        }
    }

    /** [list] with the NEXUS group replaced by [servers] (merged with the previous ones). */
    fun withNexus(
        list: List<Provider>,
        servers: List<Server>,
        upload: Long,
        download: Long,
        total: Long,
        expireAt: Long,
        now: Long = System.currentTimeMillis(),
    ): List<Provider> {
        val old = list.filter(::isNexus)
        val previous = old.firstOrNull { it.id == PROVIDER_ID } ?: old.firstOrNull()
        val provider = Provider(
            id = PROVIDER_ID,
            name = "NEXUS",
            servers = mergeServers(old.flatMap { it.servers }, servers),
            upload = upload,
            download = download,
            total = total,
            expireAt = expireAt,
            updatedAt = now,
            expanded = previous?.expanded ?: true,
        )
        return listOf(provider) + list.filterNot(::isNexus)
    }

    /** [list] without any NEXUS servers (subscription expired, or logged out). */
    fun withoutNexus(list: List<Provider>): List<Provider> = list.filterNot(::isNexus)
}
