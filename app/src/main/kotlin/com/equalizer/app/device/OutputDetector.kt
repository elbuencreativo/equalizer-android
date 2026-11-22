package com.equalizer.app.device

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OutputDetector(
    private val context: Context,
    private val registry: DeviceRegistry,
    private val scope: CoroutineScope,
) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val isTablet = context.resources.configuration.smallestScreenWidthDp >= 600
    private val _active = MutableStateFlow(ActiveOutput.NONE)
    val active: StateFlow<ActiveOutput> = _active

    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>) = refresh()
        override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>) = refresh()
    }

    fun start() {
        audio.registerAudioDeviceCallback(callback, null)
        refresh()
    }

    fun stop() = audio.unregisterAudioDeviceCallback(callback)

    /** Reevalúa la salida activa; útil tras asignar un tipo. */
    fun refresh() {
        val candidates = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).mapNotNull { toCandidate(it) }
        val picked = pickActive(candidates)
        if (picked == null) {
            _active.value = ActiveOutput.NONE
            return
        }
        scope.launch {
            val assigned = registry.typeFor(picked.key, picked.displayName)
            _active.value = buildActive(picked, assigned, isTablet)
        }
    }

    private fun toCandidate(d: AudioDeviceInfo): OutputCandidate? {
        val kind = mapKind(d.type)
        if (kind == OutputKind.UNKNOWN) return null
        val name = d.productName?.toString().orEmpty()
        return when (kind) {
            OutputKind.BUILT_IN_SPEAKER ->
                OutputCandidate("builtin", if (isTablet) "Parlante de la tableta" else "Parlante del celular", kind)
            else -> {
                val key = d.address.takeIf { it.isNotEmpty() } ?: "${d.type}:$name"
                OutputCandidate(key, name.ifEmpty { "Dispositivo de audio" }, kind)
            }
        }
    }
}
