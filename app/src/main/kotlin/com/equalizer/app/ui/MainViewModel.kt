package com.equalizer.app.ui

import com.equalizer.app.core.ContentType
import com.equalizer.app.core.DeviceType
import com.equalizer.app.core.Preset
import com.equalizer.app.core.baseProfile
import com.equalizer.app.core.effective
import com.equalizer.app.core.MAX_GAIN_DB
import com.equalizer.app.core.presetCurve
import com.equalizer.app.core.recommendedFor
import com.equalizer.app.device.ActiveOutput
import com.equalizer.app.device.DeviceRegistry
import com.equalizer.app.engine.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val output: ActiveOutput,
    val enabled: Boolean,
    val preset: Preset,
    val recommended: Map<ContentType, Preset>,
    val bands: List<Float>,
    val apps: List<String>,
    val failedCount: Int,
)

class MainViewModel(
    private val scope: CoroutineScope,
    private val output: StateFlow<ActiveOutput>,
    sessions: StateFlow<Map<Int, String>>,
    failed: StateFlow<Set<Int>>,
    private val registry: DeviceRegistry,
    private val settings: Settings,
    private val refreshOutput: () -> Unit,
) {
    val uiState: StateFlow<MainUiState> = combine(
        output, settings.enabled, settings.preset, settings.offsets, sessions,
    ) { out, enabled, preset, offsets, sess ->
        MainUiState(
            output = out,
            enabled = enabled,
            preset = preset,
            recommended = recommendedFor(out.deviceType),
            bands = effective(out.deviceType, preset, offsets).bands,
            apps = sess.values.distinct(),
            failedCount = 0,
        )
    }.combine(failed) { s, f -> s.copy(failedCount = f.size) }
        .stateIn(
            scope, SharingStarted.Eagerly,
            MainUiState(output.value, true, Preset.FLAT, recommendedFor(output.value.deviceType), List(10) { 0f }, emptyList(), 0),
        )

    fun onTogglePower() {
        scope.launch { settings.setEnabled(!settings.enabled.first()) }
    }

    fun onSelectPreset(p: Preset) {
        scope.launch { settings.setPreset(p) }
    }

    fun onAssignType(t: DeviceType) {
        val out = output.value
        if (out.key == ActiveOutput.NONE.key) return
        scope.launch {
            registry.assign(out.key, out.displayName, t)
            refreshOutput()
        }
    }

    fun onBandChanged(index: Int, db: Float) {
        scope.launch {
            val target = db.coerceIn(-MAX_GAIN_DB, MAX_GAIN_DB)
            val device = output.value.deviceType
            val current = baseProfile(device).gainsDb[index] + presetCurve(settings.preset.first()).gainsDb[index]
            val offsets = settings.offsets.first().toMutableList()
            offsets[index] = target - current
            settings.setOffsets(offsets)
        }
    }
}
