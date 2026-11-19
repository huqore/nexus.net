package net.nexus.app.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.nexus.app.BuildConfig
import net.nexus.app.MainActivity
import net.nexus.app.R
import net.nexus.app.data.Store

/**
 * Background update check (system JobScheduler, no extra libraries): every 6 hours on any network.
 * Posts "Update X.Y.Z is out · 11 MB" once per version; a tap opens the app, which shows the update dialog.
 * Respects the "auto-check updates" switch.
 */
class UpdateCheckJob : JobService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var work: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        work = scope.launch {
            try {
                if (Store(applicationContext).loadSettings().autoCheckUpdates) checkAndNotify(applicationContext)
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        work?.cancel()
        return true
    }

    companion object {
        private const val JOB_ID = 4401
        private const val CHANNEL_ID = "nexus_updates"
        private const val NOTIF_ID = 4402
        private const val PREFS = "nexus_update_notify"
        const val EXTRA_OPEN_UPDATE = "nexus_open_update"

        fun schedule(context: Context) {
            val js = context.getSystemService(JobScheduler::class.java) ?: return
            if (js.getPendingJob(JOB_ID) != null) return
            val job = JobInfo.Builder(JOB_ID, ComponentName(context, UpdateCheckJob::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(6 * 60 * 60 * 1000L)
                .setPersisted(true)
                .build()
            runCatching { js.schedule(job) }
        }

        suspend fun checkAndNotify(context: Context) {
            val release = runCatching { UpdateChecker.check() }.getOrNull() ?: return
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (prefs.getString("notified", null) == release.versionName) return
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return

            val resolved = UpdatePatcher.resolve(context, release)
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.update_notif_channel), NotificationManager.IMPORTANCE_DEFAULT)
            )
            val open = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_UPDATE, true)
            }
            val pi = PendingIntent.getActivity(context, NOTIF_ID, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val mb = String.format(java.util.Locale.US, "%.1f", resolved.downloadSize / 1_000_000.0)
            val text = if (resolved.patch != null) context.getString(R.string.update_notif_text_patch, mb)
                else context.getString(R.string.update_notif_text, mb)
            val n = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(net.nexus.app.ui.AppIconManager.avatarRes(Store(context).loadSettings().appIcon))
                .setContentTitle(context.getString(R.string.update_notif_title, resolved.versionName))
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .addAction(0, context.getString(R.string.update_btn_update), pi)
                .build()
            runCatching { nm.notify(NOTIF_ID, n) }
            prefs.edit().putString("notified", release.versionName).apply()
            if (BuildConfig.DEBUG) android.util.Log.d("NexusUpdate", "notified ${release.versionName}")
        }
    }
}
