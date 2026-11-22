package com.equalizer.app.update

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

sealed class DownloadResult {
    data class Ok(val file: File) : DownloadResult()
    data class Failed(val reason: String) : DownloadResult()
}

fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

/** Descarga a [target]; si se corta o el hash no coincide, borra el archivo y falla. */
fun downloadVerified(open: () -> InputStream, target: File, expectedSha256: String?): DownloadResult {
    return try {
        val digest = MessageDigest.getInstance("SHA-256")
        open().use { input ->
            target.outputStream().use { out ->
                val buf = ByteArray(8192)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    digest.update(buf, 0, n)
                    out.write(buf, 0, n)
                }
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (expectedSha256 != null && !actual.equals(expectedSha256, ignoreCase = true)) {
            target.delete()
            DownloadResult.Failed("La descarga no coincide con la firma esperada (sha256 distinto)")
        } else DownloadResult.Ok(target)
    } catch (e: Exception) {
        target.delete()
        DownloadResult.Failed("Descarga interrumpida: ${e.message ?: e.javaClass.simpleName}")
    }
}

/** Una release sin sha256 no se puede verificar: no se instala. */
fun checkReleaseVerifiable(r: ReleaseInfo): InstallResult.Failed? =
    if (r.sha256.isNullOrBlank()) InstallResult.Failed("La release no incluye sha256; no se puede verificar el APK") else null
