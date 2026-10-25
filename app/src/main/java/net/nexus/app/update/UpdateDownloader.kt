package net.nexus.app.update

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
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
        val targetFile = File(updatesDir, release.apkFileName.ifBlank { "nexus-update.apk" })
        val tempFile = File(updatesDir, "${targetFile.name}.part")

        if (tempFile.exists()) tempFile.delete()

        var currentUrl = release.downloadUrl
        var conn: HttpURLConnection? = null
        var redirects = 0

        try {
            while (redirects < 6) {
                val connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "nexus.net-updater/1.0 (Android)")
                    setRequestProperty("Accept", "*/*")
                }

                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location") ?: break
                    currentUrl = URL(URL(currentUrl), location).toString()
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

                        // Throttle progress updates slightly to keep UI smooth
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

                    if (!coroutineContext.isActive) {
                        tempFile.delete()
                        throw CancellationException("Download cancelled")
                    }
                    output.flush()
                }
            }

            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            onProgress(1.0f, targetFile.length(), targetFile.length())
            targetFile
        } finally {
            conn?.disconnect()
        }
    }
}
