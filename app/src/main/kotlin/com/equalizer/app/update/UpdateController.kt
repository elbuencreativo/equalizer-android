package com.equalizer.app.update

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class InstallResult {
    object Started : InstallResult()
    data class Failed(val reason: String) : InstallResult()
}

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    object UpToDate : UpdateState()
    data class Available(val release: ReleaseInfo) : UpdateState()
    object Working : UpdateState()
    data class Failed(val reason: String) : UpdateState()
}

class UpdateController(
    private val scope: CoroutineScope,
    val currentVersion: String,
    private val fetchLatest: suspend () -> ReleaseInfo?,
    private val installer: suspend (ReleaseInfo) -> InstallResult,
) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state

    fun check() {
        _state.value = UpdateState.Checking
        scope.launch {
            val r = try { fetchLatest() } catch (e: Exception) { null }
            _state.value = when {
                r == null -> UpdateState.Failed("No se pudo consultar las actualizaciones")
                isNewer(currentVersion, r.version) -> UpdateState.Available(r)
                else -> UpdateState.UpToDate
            }
        }
    }

    /** Solo se instala tras la confirmación explícita del usuario (botón). */
    fun install() {
        val available = _state.value as? UpdateState.Available ?: return
        _state.value = UpdateState.Working
        scope.launch {
            val result = try {
                installer(available.release)
            } catch (e: Exception) {
                InstallResult.Failed("No se pudo iniciar la instalación: ${e.message ?: e.javaClass.simpleName}")
            }
            _state.value = when (result) {
                InstallResult.Started -> UpdateState.Idle
                is InstallResult.Failed -> UpdateState.Failed(result.reason)
            }
        }
    }
}
