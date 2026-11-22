package com.equalizer.app.core

/** Curva final: perfil base del dispositivo + preset + ajustes manuales por banda. */
fun effective(device: DeviceType?, preset: Preset, offsets: List<Float>): Combined {
    val presetGains = presetCurve(preset).gainsDb.mapIndexed { i, g -> g + offsets.getOrElse(i) { 0f } }
    return combine(baseProfile(device), EqCurve(presetGains))
}
