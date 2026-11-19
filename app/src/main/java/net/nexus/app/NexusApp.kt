package net.nexus.app

import android.app.Application
import net.nexus.app.data.ApiHosts

class NexusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Remembered working domain and the server-provided domain list (see ApiHosts)
        ApiHosts.init(this)
        // Background update check every 6 h, posts a notification when a new version is out
        net.nexus.app.update.UpdateCheckJob.schedule(this)
    }
}
