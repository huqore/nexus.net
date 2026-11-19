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
    /** URL of nexus-net-vX.Y.Z.patches.json in the release, if the release ships delta patches. */
    val patchManifestUrl: String = "",
    /** Download URLs of the release's .nxpatch assets by file name. */
    val patchAssetUrls: Map<String, String> = emptyMap(),
    /** Delta patch that fits the APK installed on this phone (resolved after the check), or null. */
    val patch: UpdatePatch? = null,
    /** SHA-256 of the official new APK, from the patch manifest. */
    val targetSha256: String = "",
) {
    /** What the user will actually download. */
    val downloadSize: Long get() = patch?.size ?: apkSize
}

/** One entry of the patch manifest that matches the installed APK. */
data class UpdatePatch(
    val fromVersion: String,
    val fileName: String,
    val url: String,
    val size: Long,
    val sha256: String,
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
        val isPatch: Boolean = false,
        /** true while the downloaded patch is being applied to the installed APK. */
        val assembling: Boolean = false,
    ) : UpdateState
    data class ReadyToInstall(val release: AppReleaseInfo, val apkFile: File) : UpdateState
    data class Error(val message: String, val isManual: Boolean = false) : UpdateState
}
