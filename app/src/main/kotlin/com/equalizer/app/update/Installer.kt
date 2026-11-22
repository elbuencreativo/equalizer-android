package com.equalizer.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
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
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(dir, "update.apk")
        val result = withContext(Dispatchers.IO) { downloadVerified({ URL(r.apkUrl).openStream() }, target, r.sha256) }
        return when (result) {
            is DownloadResult.Failed -> InstallResult.Failed(result.reason)
            is DownloadResult.Ok -> {
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
}
