package com.equalizer.app.ui

import com.equalizer.app.core.ContentType
import com.equalizer.app.core.CustomPreset
import com.equalizer.app.core.CustomPresets
import com.equalizer.app.core.EqCurve
import com.equalizer.app.core.DeviceType
import com.equalizer.app.core.Preset
import com.equalizer.app.core.baseProfile
import com.equalizer.app.core.effective
import com.equalizer.app.core.MAX_GAIN_DB
import com.equalizer.app.core.presetCurve
import com.equalizer.app.core.recommendedFor
import com.equalizer.app.core.snapBalance
import com.equalizer.app.device.ActiveOutput
import com.equalizer.app.device.DeviceRegistry
import com.equalizer.app.engine.EngineStatus
import com.equalizer.app.engine.EqMethod
import com.equalizer.app.engine.Settings
import kotlinx.coroutines.flow.MutableStateFlow
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
    val customPresets: List<CustomPreset> = emptyList(),
    val dumpAvailable: Boolean = true,
    /** Método elegido por el usuario. */
    val method: EqMethod = EqMethod.GLOBAL,
    /** Método que realmente se usa (puede diferir si el global falló). */
    val activeMethod: EqMethod = EqMethod.GLOBAL,
    /** El método global falló y la app pasó sola al modo por app. */
    val fellBack: Boolean = false,
    /** Hay una app sonando sin sesión propia (p. ej. un navegador). */
    val sessionlessApp: Boolean = false,
    val balance: Float = 0f,
)

class MainViewModel(
    private val scope: CoroutineScope,
    private val output: StateFlow<ActiveOutput>,
    sessions: StateFlow<Map<Int, String>>,
    failed: StateFlow<Set<Int>>,
    dumpAvailable: StateFlow<Boolean>,
    private val registry: DeviceRegistry,
    private val settings: Settings,
    private val custom: CustomPresets,
    engineStatus: StateFlow<EngineStatus> = MutableStateFlow(EngineStatus(EqMethod.GLOBAL, false)),
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
            sessionlessApp = sess.containsKey(0),
        )
    }.combine(failed) { s, f -> s.copy(failedCount = f.size) }
        .combine(custom.all) { s, c -> s.copy(customPresets = c) }
        .combine(dumpAvailable) { s, d -> s.copy(dumpAvailable = d) }
        .combine(settings.method) { s, m -> s.copy(method = m) }
        .combine(settings.balance) { s, b -> s.copy(balance = b) }
        .combine(engineStatus) { s, e -> s.copy(activeMethod = e.active, fellBack = e.fellBack) }
        .stateIn(
            scope, SharingStarted.Eagerly,
            MainUiState(output.value, true, Preset.FLAT, recommendedFor(output.value.deviceType), List(10) { 0f }, emptyList(), 0),
        )

    fun onSelectMethod(m: EqMethod) {
        scope.launch { settings.setMethod(m) }
    }

    fun onBalanceChanged(v: Float) {
        scope.launch { settings.setBalance(snapBalance(v)) }
    }

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

    /** Guarda preset actual + ajustes manuales (sin el perfil base del dispositivo). */
    fun onSaveCustom(name: String) {
        scope.launch {
            val preset = presetCurve(settings.preset.first()).gainsDb
            val offsets = settings.offsets.first()
            val gains = preset.mapIndexed { i, g -> (g + offsets[i]).coerceIn(-MAX_GAIN_DB, MAX_GAIN_DB) }
            custom.save(CustomPreset(name, EqCurve(gains)))
        }
    }

    fun onApplyCustom(p: CustomPreset) {
        scope.launch {
            settings.setPreset(Preset.FLAT)
            settings.setOffsets(p.curve.gainsDb)
        }
    }

    fun onDeleteCustom(name: String) {
        scope.launch { custom.delete(name) }
    }
}
