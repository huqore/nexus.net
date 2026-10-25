package net.nexus.app.update

/**
 * Update configuration.
 *
 * To use your own repository:
 * Change [GITHUB_REPO] to "huqore/nexus.net"
 * (e.g. "sibas/nexus-client").
 */
object UpdateConfig {
    /**
     * GitHub repository in the format "owner/repo" used for releases.
     * Replace with your actual repository.
     */
    const val GITHUB_REPO = "huqore/nexus.net"

    /**
     * Optional custom JSON endpoint if you decide to host update metadata on your own server.
     * Leave empty to query GitHub Releases API directly.
     */
    const val CUSTOM_UPDATE_JSON_URL = ""
}
