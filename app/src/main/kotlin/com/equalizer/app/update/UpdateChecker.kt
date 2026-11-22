package com.equalizer.app.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class HttpResult(val code: Int, val body: String)

fun interface HttpGet {
    fun get(url: String): HttpResult
}

data class ReleaseInfo(val version: String, val apkUrl: String, val sha256: String?)

/** Consulta la última release de un repositorio público de GitHub (sin token). */
class UpdateChecker(private val http: HttpGet, private val owner: String, private val repo: String) {
    suspend fun latest(): ReleaseInfo? {
        if (owner.isBlank() || repo.isBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val res = http.get("https://api.github.com/repos/$owner/$repo/releases/latest")
                if (res.code != 200) return@withContext null
                val info = parse(res.body) ?: return@withContext null
                if (info.sha256 != null) info else info.copy(sha256 = fetchSiblingSha(res.body, info.apkUrl))
            } catch (e: Exception) {
                null
            }
        }
    }

    /** Busca un asset `<apk>.sha256` junto al APK y lee su primer token. */
    private fun fetchSiblingSha(body: String, apkUrl: String): String? {
        val assets = JSONObject(body).optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            if (a.optString("name").endsWith(".apk.sha256")) {
                val res = http.get(a.getString("browser_download_url"))
                if (res.code != 200) return null
                return res.body.trim().split(Regex("\\s+")).firstOrNull()?.lowercase()?.takeIf { it.isNotEmpty() }
            }
        }
        return null
    }

    private fun parse(body: String): ReleaseInfo? {
        val json = JSONObject(body)
        val tag = json.optString("tag_name").removePrefix("v").ifEmpty { return null }
        val assets = json.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            if (a.optString("name").endsWith(".apk")) {
                val digest = a.optString("digest").takeIf { it.startsWith("sha256:") }?.removePrefix("sha256:")?.lowercase()
                return ReleaseInfo(tag, a.getString("browser_download_url"), digest)
            }
        }
        return null
    }
}

object RealHttp : HttpGet {
    override fun get(url: String): HttpResult {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 15_000
        c.setRequestProperty("Accept", "application/vnd.github+json")
        c.setRequestProperty("User-Agent", "Equalizer-App")
        return try {
            val stream = if (c.responseCode < 400) c.inputStream else c.errorStream
            HttpResult(c.responseCode, stream?.bufferedReader()?.readText().orEmpty())
        } finally {
            c.disconnect()
        }
    }
}
