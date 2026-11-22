package com.equalizer.app.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class Installer(private val context: Context) {
    suspend fun downloadAndInstall(r: ReleaseInfo): InstallResult {
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return InstallResult.Failed("Permite instalar apps de esta fuente y vuelve a intentar")
        }
        checkReleaseVerifiable(r)?.let { return it }
        if (!isTrustedDownloadUrl(r.apkUrl)) return InstallResult.Failed("La dirección de descarga no es de GitHub; no se instala")
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(dir, "update.apk")
        val result = withContext(Dispatchers.IO) { downloadVerified({ open(r.apkUrl) }, target, r.sha256) }
        return when (result) {
            is DownloadResult.Failed -> InstallResult.Failed(result.reason)
            is DownloadResult.Ok -> {
                if (!sameSigner(result.file)) {
                    result.file.delete()
                    return InstallResult.Failed("El APK descargado está firmado con otra llave; no se instala")
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", result.file)
                context.startActivity(
                    Intent(Intent.ACTION_VIEW)
                        .setDataAndType(uri, "application/vnd.android.package-archive")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                InstallResult.Started
            }
        }
    }

    private fun open(url: String) = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 30_000
    }.inputStream

    /** Compara los certificados del APK descargado con los de la app instalada. */
    @Suppress("DEPRECATION")
    private fun sameSigner(apk: File): Boolean {
        val pm = context.packageManager
        val flags = PackageManager.GET_SIGNING_CERTIFICATES
        val downloaded = pm.getPackageArchiveInfo(apk.path, flags)?.signingInfo?.apkContentsSigners ?: return false
        val installed = pm.getPackageInfo(context.packageName, flags).signingInfo?.apkContentsSigners ?: return false
        return downloaded.map { it.toCharsString() }.toSet() == installed.map { it.toCharsString() }.toSet()
    }
}
