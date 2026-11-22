package com.equalizer.app.device

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Build
import java.util.concurrent.atomic.AtomicInteger
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

    private val seq = AtomicInteger()

    private val playbackCallback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>?) = refresh()
    }

    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>) = refresh()
        override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>) = refresh()
    }

    fun start() {
        audio.registerAudioDeviceCallback(callback, null)
        audio.registerAudioPlaybackCallback(playbackCallback, null)
        refresh()
    }

    fun stop() {
        audio.unregisterAudioDeviceCallback(callback)
        audio.unregisterAudioPlaybackCallback(playbackCallback)
    }

    /** Reevalúa la salida activa; útil tras asignar un tipo. */
    fun refresh() {
        val my = seq.incrementAndGet()
        val candidates = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).mapNotNull { toCandidate(it) }
        val picked = pickActive(candidates, routedKeys())
        if (picked == null) {
            _active.value = ActiveOutput.NONE
            return
        }
        scope.launch {
            val assigned = registry.typeFor(picked.key, picked.displayName)
            // Solo la última evaluación gana: una más antigua no pisa a una nueva.
            if (seq.get() == my) _active.value = buildActive(picked, assigned, isTablet)
        }
    }

    /** Salidas por donde Android enruta realmente el audio multimedia (API 33+). */
    private fun routedKeys(): Set<String> {
        if (Build.VERSION.SDK_INT < 33) return emptySet()
        val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()
        return audio.getAudioDevicesForAttributes(attrs).mapNotNull { toCandidate(it)?.key }.toSet()
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
