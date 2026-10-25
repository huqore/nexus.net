package net.nexus.app.update

import java.io.File

data class AppReleaseInfo(
    val tagName: String,
    val versionName: String,
    val changelog: String,
    val downloadUrl: String,
    val apkFileName: String,
    val apkSize: Long,
    val publishedAt: String = "",
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class Available(val release: AppReleaseInfo, val autoPrompt: Boolean = true) : UpdateState
    data object UpToDate : UpdateState
    data class Downloading(
        val release: AppReleaseInfo,
        val progress: Float, // 0.0f .. 1.0f (or -1f if indeterminate)
        val bytesDownloaded: Long,
        val totalBytes: Long,
    ) : UpdateState
    data class ReadyToInstall(val release: AppReleaseInfo, val apkFile: File) : UpdateState
    data class Error(val message: String, val isManual: Boolean = false) : UpdateState
}
