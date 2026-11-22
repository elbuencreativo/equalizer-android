package com.equalizer.app.engine

import android.util.Log
import com.equalizer.app.core.Combined
import com.equalizer.app.core.EqCurve
import com.equalizer.app.core.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val TAG = "EqEngine"

/** Cómo se aplica el ecualizador: a toda la mezcla del sistema o a cada app por separado. */
enum class EqMethod { GLOBAL, LEGACY, PER_APP }

/** [active] es el método que realmente se está usando; [fellBack] indica que se cambió solo porque el global falló. */
data class EngineStatus(val active: EqMethod, val fellBack: Boolean)

private const val GLOBAL_ID = 0

/** Tope de sesiones recordadas: el aviso de sesión lo puede enviar cualquier app. */
private const val MAX_SESSIONS = 64

interface EffectHandle {
    fun apply(c: Combined)
    fun release()
}

/**
 * Mantiene los efectos de audio. Empieza APAGADO: nada se crea hasta que el servicio lee el
 * ajuste guardado y llama a [setEnabled]. Ningún fallo de un efecto nativo se propaga.
 *
 * Método GLOBAL: un único efecto sobre la mezcla de salida (sesión 0), que alcanza también a
 * apps sin sesión propia, como los navegadores. Si falla, el motor pasa solo a PER_APP
 * ([EngineStatus.fellBack]). Método PER_APP: un efecto por cada sesión de audio de una app.
 * Nunca están activos los dos a la vez, para no aplicar el ecualizador dos veces.
 */
class EqEngine(
    method: EqMethod = EqMethod.PER_APP,
    private val legacyFactory: ((sessionId: Int) -> EffectHandle)? = null,
    private val factory: (sessionId: Int) -> EffectHandle,
) {
    private val handles = HashMap<Int, EffectHandle>()
    private var curve: Combined = combine(EqCurve.FLAT, EqCurve.FLAT)
    private var enabled = false
    private var preferred = method
    private var globalFailed = false
    private var legacyFailed = false
    private var builtFor: EqMethod? = null

    private val _sessions = MutableStateFlow<Map<Int, String>>(emptyMap())
    val sessions: StateFlow<Map<Int, String>> = _sessions

    private val _failed = MutableStateFlow<Set<Int>>(emptySet())
    val failed: StateFlow<Set<Int>> = _failed

    private val _status = MutableStateFlow(EngineStatus(method, false))
    val status: StateFlow<EngineStatus> = _status

    private fun legacyOk() = legacyFactory != null && !legacyFailed

    /** Método que se usa de verdad: el elegido y, si falla, el siguiente de la cadena GLOBAL → LEGACY → PER_APP. */
    private fun active(): EqMethod = when (preferred) {
        EqMethod.GLOBAL -> when {
            !globalFailed -> EqMethod.GLOBAL
            legacyOk() -> EqMethod.LEGACY
            else -> EqMethod.PER_APP
        }
        EqMethod.LEGACY -> if (legacyOk()) EqMethod.LEGACY else EqMethod.PER_APP
        EqMethod.PER_APP -> EqMethod.PER_APP
    }

    private fun markGlobalSlotFailed() {
        if (active() == EqMethod.LEGACY) legacyFailed = true else globalFailed = true
    }

    @Synchronized fun setMethod(m: EqMethod) {
        Log.i(TAG, "METHOD $m")
        preferred = m
        globalFailed = false // cada elección reintenta los métodos globales
        legacyFailed = false
        rebuild()
    }

    @Synchronized fun onSessionOpened(id: Int, pkg: String) {
        if (id !in _sessions.value && _sessions.value.size >= MAX_SESSIONS) return
        Log.i(TAG, "OPEN session=$id pkg=$pkg enabled=$enabled")
        _sessions.value = _sessions.value + (id to pkg)
        rebuild()
    }

    @Synchronized fun onSessionClosed(id: Int) {
        Log.i(TAG, "CLOSE session=$id")
        _sessions.value = _sessions.value - id
        _failed.value = _failed.value - id
        rebuild()
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
        var slotFailed = false
        handles.entries.toList().forEach { (id, h) ->
            try {
                h.apply(c)
            } catch (e: Exception) {
                Log.w(TAG, "apply failed session=$id: $e")
                handles.remove(id)
                safeRelease(h)
                if (id == GLOBAL_ID) slotFailed = true else _failed.value = _failed.value + id
            }
        }
        if (slotFailed) { markGlobalSlotFailed(); rebuild() }
    }

    @Synchronized fun setEnabled(on: Boolean) {
        if (enabled == on) return
        enabled = on
        rebuild()
    }

    /** Deja vivos exactamente los efectos que el método activo necesita. */
    private fun rebuild() {
        val act = active()
        if (builtFor != act) {
            handles.values.forEach { safeRelease(it) }
            handles.clear()
            builtFor = act
        }
        val wanted: Set<Int> = when {
            !enabled -> emptySet()
            act != EqMethod.PER_APP -> setOf(GLOBAL_ID)
            else -> _sessions.value.keys.filter { it != GLOBAL_ID }.toSet()
        }
        handles.keys.filter { it !in wanted }.forEach { id -> handles.remove(id)?.let { safeRelease(it) } }
        wanted.forEach { ensureHandle(it) }
        // active() otra vez: un fallo dentro de ensureHandle pudo cambiar el método durante este rebuild.
        val now = active()
        _status.value = EngineStatus(now, now != preferred)
    }

    private fun ensureHandle(id: Int) {
        if (handles.containsKey(id)) return
        var h: EffectHandle? = null
        try {
            h = (if (active() == EqMethod.LEGACY) legacyFactory!! else factory)(id)
            h.apply(curve)
            handles[id] = h
            _failed.value = _failed.value - id
        } catch (e: Exception) {
            Log.w(TAG, "effect failed session=$id", e)
            h?.let { safeRelease(it) }
            if (id == GLOBAL_ID) {
                markGlobalSlotFailed()
                rebuild() // pasa al siguiente método de la cadena
            } else {
                _failed.value = _failed.value + id
            }
        }
    }

    private fun safeRelease(h: EffectHandle) {
        try { h.release() } catch (e: Exception) { Log.w(TAG, "release failed: $e") }
    }
}
