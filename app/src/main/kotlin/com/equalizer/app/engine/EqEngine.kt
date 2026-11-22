package com.equalizer.app.engine

import android.util.Log
import com.equalizer.app.core.Combined
import com.equalizer.app.core.EqCurve
import com.equalizer.app.core.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val TAG = "EqEngine"

interface EffectHandle {
    fun apply(c: Combined)
    fun release()
}

/**
 * Mantiene un efecto por sesión de audio. Empieza APAGADO: nada se crea hasta que
 * el servicio lee el ajuste guardado y llama a [setEnabled]. Ningún fallo de un efecto
 * nativo se propaga: la sesión se marca como fallida y su efecto se libera.
 */
class EqEngine(private val factory: (sessionId: Int) -> EffectHandle) {
    private val handles = HashMap<Int, EffectHandle>()
    private var curve: Combined = combine(EqCurve.FLAT, EqCurve.FLAT)
    private var enabled = false

    private val _sessions = MutableStateFlow<Map<Int, String>>(emptyMap())
    val sessions: StateFlow<Map<Int, String>> = _sessions

    private val _failed = MutableStateFlow<Set<Int>>(emptySet())
    val failed: StateFlow<Set<Int>> = _failed

    @Synchronized fun onSessionOpened(id: Int, pkg: String) {
        Log.i(TAG, "OPEN session=$id pkg=$pkg enabled=$enabled")
        _sessions.value = _sessions.value + (id to pkg)
        if (enabled) ensureHandle(id)
    }

    @Synchronized fun onSessionClosed(id: Int) {
        Log.i(TAG, "CLOSE session=$id")
        handles.remove(id)?.let { safeRelease(it) }
        _sessions.value = _sessions.value - id
        _failed.value = _failed.value - id
    }

    /** Olvida todas las sesiones (p. ej. al detener el servicio). */
    @Synchronized fun clearSessions() {
        handles.values.forEach { safeRelease(it) }
        handles.clear()
        _sessions.value = emptyMap()
        _failed.value = emptySet()
    }

    @Synchronized fun setCurve(c: Combined) {
        curve = c
        handles.entries.toList().forEach { (id, h) ->
            try {
                h.apply(c)
            } catch (e: Exception) {
                Log.w(TAG, "apply failed session=$id: $e")
                handles.remove(id)
                safeRelease(h)
                _failed.value = _failed.value + id
            }
        }
    }

    @Synchronized fun setEnabled(on: Boolean) {
        if (enabled == on) return
        enabled = on
        if (on) {
            _sessions.value.keys.forEach { ensureHandle(it) }
        } else {
            handles.values.forEach { safeRelease(it) }
            handles.clear()
        }
    }

    private fun ensureHandle(id: Int) {
        if (handles.containsKey(id)) return
        var h: EffectHandle? = null
        try {
            h = factory(id)
            h.apply(curve)
            handles[id] = h
            _failed.value = _failed.value - id
        } catch (e: Exception) {
            Log.w(TAG, "effect failed session=$id: $e")
            h?.let { safeRelease(it) }
            _failed.value = _failed.value + id
        }
    }

    private fun safeRelease(h: EffectHandle) {
        try { h.release() } catch (e: Exception) { Log.w(TAG, "release failed: $e") }
    }
}
