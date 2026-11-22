package com.equalizer.app.engine

import android.content.Context
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "SessionDiscovery"

/**
 * Descubre sesiones de apps que no anuncian la suya, leyendo `dumpsys audio`
 * (requiere el permiso DUMP concedido por ADB). Cada lectura y su aplicación ocurren
 * dentro de un mismo bloqueo, así que un volcado viejo nunca pisa a uno nuevo.
 */
class SessionDiscovery(
    private val context: Context,
    private val engine: EqEngine,
    private val scope: CoroutineScope,
) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val reconciler = SessionReconciler()
    private val lock = Mutex()
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
            lock.withLock {
                val dump = readDump()
                if (dump == null) { _dumpAvailable.value = false; return@withLock }
                _dumpAvailable.value = true
                val uid = android.os.Process.myUid()
                val started = activeMediaSessions(dump, uid)
                val plan = reconciler.reconcile(
                    known = engine.sessions.value.keys,
                    started = started.map { it.sessionId }.toSet(),
                    present = presentSessionIds(dump, uid),
                )
                plan.open.forEach { id ->
                    val s = started.first { it.sessionId == id }
                    val pkg = context.packageManager.getPackagesForUid(s.uid)?.firstOrNull() ?: "uid ${s.uid}"
                    Log.i(TAG, "DISCOVERED session=$id")
                    engine.onSessionOpened(id, pkg)
                }
                plan.close.forEach {
                    Log.i(TAG, "GONE session=$it")
                    engine.onSessionClosed(it)
                }
            }
        }
    }

    private fun readDump(): String? {
        var p: java.lang.Process? = null
        return try {
            p = Runtime.getRuntime().exec(arrayOf("dumpsys", "audio"))
            val text = p.inputStream.bufferedReader().readText()
            p.waitFor()
            text.takeIf { it.contains("AudioPlaybackConfiguration") }
        } catch (e: Exception) {
            Log.w(TAG, "dumpsys failed: $e")
            null
        } finally {
            p?.destroy()
        }
    }
}
