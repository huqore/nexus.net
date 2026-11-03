package net.nexus.app.update

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import net.nexus.app.vpn.VpnState
import net.nexus.app.vpn.VpnStatus
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import kotlin.coroutines.coroutineContext

object UpdateDownloader {

    private const val BUFFER_SIZE = 16 * 1024

    /**
     * Downloads [release] APK file into the app cache directory.
     * Invokes [onProgress] with (progress 0..1, bytesDownloaded, totalBytes).
     * Returns the downloaded [File].
     */
    suspend fun download(
        context: Context,
        release: AppReleaseInfo,
        onProgress: (Float, Long, Long) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val safeName = File(release.apkFileName.ifBlank { "nexus-update.apk" }).name.replace("..", "")
        val targetFile = File(updatesDir, if (safeName.endsWith(".apk", true)) safeName else "$safeName.apk")
        val tempFile = File(updatesDir, "${targetFile.name}.part")

        if (tempFile.exists()) tempFile.delete()

        // Build list of candidate URLs: direct download and mirror fallback (for Russian ISP DPI bypass)
        val candidateUrls = mutableListOf<String>()
        candidateUrls.add(release.downloadUrl)
        if (release.downloadUrl.contains("github.com", ignoreCase = true)) {
            candidateUrls.add("https://ghproxy.net/${release.downloadUrl}")
        }

        var lastError: Exception? = null
        for (url in candidateUrls) {
            try {
                downloadFromUrl(url, targetFile, tempFile, release, onProgress)
                return@withContext targetFile
            } catch (e: CancellationException) {
                if (tempFile.exists()) tempFile.delete()
                throw e
            } catch (e: Exception) {
                lastError = e
                if (tempFile.exists()) tempFile.delete()
            }
        }

        throw lastError ?: java.io.IOException("Failed to download update from all available sources")
    }

    private suspend fun downloadFromUrl(
        initialUrl: String,
        targetFile: File,
        tempFile: File,
        release: AppReleaseInfo,
        onProgress: (Float, Long, Long) -> Unit,
    ) {
        var currentUrl = initialUrl
        if (!net.nexus.app.security.UrlValidator.isSecureUrl(currentUrl, allowLoopback = false)) {
            error("Insecure download URL: HTTPS is required")
        }

        val port = VpnState.socksPort
        val proxy = if (VpnState.status.value == VpnStatus.CONNECTED && port > 0) {
            Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", port))
        } else {
            null
        }

        var conn: HttpURLConnection? = null
        var redirects = 0

        try {
            while (redirects < 6) {
                val connection = (if (proxy != null) {
                    URL(currentUrl).openConnection(proxy) as HttpURLConnection
                } else {
                    URL(currentUrl).openConnection() as HttpURLConnection
                }).apply {
                    connectTimeout = 10_000
                    readTimeout = 30_000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "nexus.net-updater/1.0 (Android)")
                    if (UpdateConfig.GITHUB_TOKEN.isNotBlank() && currentUrl.contains("api.github.com")) {
                        setRequestProperty("Authorization", "Bearer ${UpdateConfig.GITHUB_TOKEN.trim()}")
                        setRequestProperty("Accept", "application/octet-stream")
                    } else {
                        setRequestProperty("Accept", "*/*")
                    }
                }

                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location") ?: break
                    if (!net.nexus.app.security.UrlValidator.isSafeRedirect(currentUrl, location)) {
                        error("Insecure redirect rejected: HTTPS required")
                    }
                    val redirectedUrl = java.net.URI(currentUrl).resolve(location).toString()
                    currentUrl = redirectedUrl
                    redirects++
                    connection.disconnect()
                    continue
                }
                conn = connection
                break
            }

            val finalConn = conn ?: error("Failed to establish download connection")
            val code = finalConn.responseCode
            if (code !in 200..299) {
                error("Download failed with HTTP $code")
            }

            val totalBytes = finalConn.contentLengthLong.let { if (it > 0) it else release.apkSize }

            finalConn.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesDownloaded = 0L
                    var lastReported = 0L

                    while (coroutineContext.isActive) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        bytesDownloaded += read

                        val now = System.currentTimeMillis()
                        if (now - lastReported > 80 || bytesDownloaded == totalBytes) {
                            lastReported = now
                            val progress = if (totalBytes > 0) {
                                (bytesDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f)
                            } else {
                                -1f
                            }
                            onProgress(progress, bytesDownloaded, totalBytes)
                        }
                    }
                    output.flush()
                }
            }

            if (!coroutineContext.isActive) {
                if (tempFile.exists()) tempFile.delete()
                throw CancellationException("Download cancelled")
            }

            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
        } finally {
            conn?.disconnect()
        }
    }
}
