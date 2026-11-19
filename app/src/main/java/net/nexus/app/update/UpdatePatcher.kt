package net.nexus.app.update

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * Delta updates. A release may ship nexus-net-vX.Y.Z.patches.json plus .nxpatch files (tools/make_patches.py).
 * If one of them was made from exactly the APK installed here (matched by SHA-256), the app downloads the
 * small patch, rebuilds the new APK locally and checks it byte for byte against the official one.
 * Anything unexpected -> the caller falls back to the full APK, so the worst case is today's behaviour.
 */
object UpdatePatcher {

    private const val PREFS = "nexus_update_patch"

    /** Adds the matching patch to [release], or returns it unchanged. Never throws. */
    suspend fun resolve(context: Context, release: AppReleaseInfo): AppReleaseInfo = withContext(Dispatchers.IO) {
        runCatching {
            val manifestName = "nexus-net-v${release.versionName}.patches.json"
            val hosts = net.nexus.app.data.ApiHosts.candidates()
            val urls = (listOf(release.patchManifestUrl) + hosts.map { "https://$it/downloads/$manifestName" })
                .filter { it.isNotBlank() }.distinct()
            val json = urls.firstNotNullOfOrNull { UpdateChecker.fetchText(it) } ?: return@runCatching release
            val m = JSONObject(json)
            if (m.optString("format") != "NXP1") return@runCatching release
            if (UpdateChecker.cleanVersion(m.optString("version")) != release.versionName) return@runCatching release
            val target = m.optJSONObject("target") ?: return@runCatching release
            val targetSha = target.optString("sha256").lowercase()
            if (targetSha.length != 64) return@runCatching release

            val own = installedApkSha(context) ?: return@runCatching release
            val arr = m.optJSONArray("patches") ?: return@runCatching release
            for (i in 0 until arr.length()) {
                val p = arr.getJSONObject(i)
                if (p.optString("from_sha256").lowercase() != own) continue
                val name = File(p.optString("name")).name
                if (!name.endsWith(".nxpatch") || p.optString("sha256").length != 64) continue
                return@runCatching release.copy(
                    patch = UpdatePatch(
                        fromVersion = p.optString("from_version"),
                        fileName = name,
                        url = release.patchAssetUrls[name] ?: "",
                        size = p.optLong("size"),
                        sha256 = p.optString("sha256").lowercase(),
                    ),
                    targetSha256 = targetSha,
                    apkSize = if (release.apkSize > 0) release.apkSize else target.optLong("size"),
                )
            }
            release
        }.getOrDefault(release)
    }

    /**
     * Downloads the patch, applies it to the installed APK and verifies the result.
     * [onDownload] (progress, bytes, total) while downloading, [onAssemble] (progress) while rebuilding.
     */
    suspend fun downloadAndAssemble(
        context: Context,
        release: AppReleaseInfo,
        onDownload: (Float, Long, Long) -> Unit,
        onAssemble: (Float) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val patch = release.patch ?: error("no patch")
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val patchFile = File(dir, patch.fileName)
        val patchTemp = File(dir, "${patch.fileName}.part")
        val apkName = File(release.apkFileName.ifBlank { "nexus-net-v${release.versionName}.apk" }).name
        val outFile = File(dir, if (apkName.endsWith(".apk", true)) apkName else "$apkName.apk")
        val outTemp = File(dir, "${outFile.name}.build")

        try {
            val urls = (listOf(patch.url) + net.nexus.app.data.ApiHosts.candidates().map { "https://$it/downloads/${patch.fileName}" })
                .filter { it.isNotBlank() }.distinct()
            var ok = false
            var last: Exception? = null
            for (u in urls) {
                try {
                    UpdateDownloader.downloadFromUrl(u, patchFile, patchTemp, patch.size, onDownload)
                    if (patchFile.length() == patch.size && PatchApplier.sha256(patchFile) == patch.sha256) { ok = true; break }
                    last = IllegalStateException("patch checksum mismatch")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    last = e
                }
                patchFile.delete(); patchTemp.delete()
            }
            if (!ok) throw last ?: IllegalStateException("patch download failed")

            onAssemble(0f)
            val installed = File(context.applicationInfo.sourceDir)
            PatchApplier.apply(installed, patchFile, outTemp, onAssemble)
            if (PatchApplier.sha256(outTemp) != release.targetSha256) error("rebuilt APK does not match the release")
            if (outFile.exists()) outFile.delete()
            if (!outTemp.renameTo(outFile)) { outTemp.copyTo(outFile, overwrite = true); outTemp.delete() }
            outFile
        } finally {
            patchFile.delete(); patchTemp.delete(); outTemp.delete()
        }
    }

    /** SHA-256 of the installed base APK, cached per install so the 70 MB file is hashed once. */
    fun installedApkSha(context: Context): String? = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val key = "sha_${info.lastUpdateTime}_${info.longVersionCodeCompat()}"
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(key, null) ?: PatchApplier.sha256(File(context.applicationInfo.sourceDir)).also {
            prefs.edit().clear().putString(key, it).apply()
        }
    }.getOrNull()

    @Suppress("DEPRECATION")
    private fun android.content.pm.PackageInfo.longVersionCodeCompat(): Long =
        if (android.os.Build.VERSION.SDK_INT >= 28) longVersionCode else versionCode.toLong()
}
