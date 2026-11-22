package com.equalizer.app.engine

import com.equalizer.app.core.Combined
import com.equalizer.app.core.EqCurve
import com.equalizer.app.core.combine
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface EffectHandle {
    fun apply(c: Combined)
    fun release()
}

private const val TAG = "EqEngine"

class EqEngine(private val factory: (sessionId: Int) -> EffectHandle) {
    private val handles = HashMap<Int, EffectHandle>()
    private var curve: Combined = combine(EqCurve.FLAT, EqCurve.FLAT)
    private var enabled = true

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
        handles.remove(id)?.release()
        _sessions.value = _sessions.value - id
        _failed.value = _failed.value - id
    }

    @Synchronized fun setCurve(c: Combined) {
        curve = c
        handles.values.forEach { it.apply(c) }
    }

    @Synchronized fun setEnabled(on: Boolean) {
        if (enabled == on) return
        enabled = on
        if (on) _sessions.value.keys.forEach { ensureHandle(it) }
        else {
            handles.values.forEach { it.release() }
            handles.clear()
        }
    }

    private fun ensureHandle(id: Int) {
        if (handles.containsKey(id)) return
        try {
            val h = factory(id)
            h.apply(curve)
            handles[id] = h
            _failed.value = _failed.value - id
        } catch (e: Exception) {
            Log.w(TAG, "effect failed session=$id: $e")
            _failed.value = _failed.value + id
        }
    }
}
