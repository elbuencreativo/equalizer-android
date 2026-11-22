package com.equalizer.app.engine

import android.content.Context
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "SessionDiscovery"

/**
 * Descubre sesiones de apps que no anuncian la suya, leyendo `dumpsys audio`
 * (requiere el permiso DUMP concedido por ADB).
 */
class SessionDiscovery(
    private val context: Context,
    private val engine: EqEngine,
    private val scope: CoroutineScope,
) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val opened = HashSet<Int>()
    private val _dumpAvailable = MutableStateFlow(true)
    val dumpAvailable: StateFlow<Boolean> = _dumpAvailable

    private val callback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>?) = refresh()
    }

    fun start() {
        audio.registerAudioPlaybackCallback(callback, Handler(Looper.getMainLooper()))
        refresh()
    }

    fun stop() = audio.unregisterAudioPlaybackCallback(callback)

    fun refresh() {
        scope.launch(Dispatchers.IO) {
            val dump = readDump()
            if (dump == null) { _dumpAvailable.value = false; return@launch }
            _dumpAvailable.value = true
            apply(activeMediaSessions(dump, Process.myUid()))
        }
    }

    @Synchronized private fun apply(active: List<PlaybackSession>) {
        val ids = active.map { it.sessionId }.toSet()
        active.forEach {
            if (opened.add(it.sessionId)) {
                val pkg = context.packageManager.getPackagesForUid(it.uid)?.firstOrNull() ?: "uid ${it.uid}"
                Log.i(TAG, "DISCOVERED session=${it.sessionId} pkg=$pkg")
                engine.onSessionOpened(it.sessionId, pkg)
            }
        }
        (opened - ids).forEach {
            Log.i(TAG, "GONE session=$it")
            engine.onSessionClosed(it)
            opened.remove(it)
        }
    }

    private fun readDump(): String? = try {
        val p = Runtime.getRuntime().exec(arrayOf("dumpsys", "audio"))
        val text = p.inputStream.bufferedReader().readText()
        p.waitFor()
        text.takeIf { it.contains("AudioPlaybackConfiguration") }
    } catch (e: Exception) {
        Log.w(TAG, "dumpsys failed: $e")
        null
    }
}
