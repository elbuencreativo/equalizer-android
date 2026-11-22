package com.equalizer.app.core

// Bandas: 31, 62, 125, 250, 500, 1k, 2k, 4k, 8k, 16k Hz.
// Valores de partida a validar de oído; clips y parlantes son hipótesis sin fuente.
private fun c(vararg g: Float) = EqCurve(g.toList())

private val IN_EAR_CURVE = c(8f, 9f, 6f, 2f, 0f, 0f, 1f, 1f, -1f, -2f) // Harman In-Ear 2019 aprox.

fun baseProfile(type: DeviceType?): EqCurve = when (type) {
    null -> EqCurve.FLAT
    DeviceType.OVER_EAR -> c(3f, 3f, 2f, 0f, 0f, 0f, 1f, 2f, -1f, -2f) // Harman Over-Ear 2019 aprox.
    DeviceType.BUDS, DeviceType.IN_EAR_WIRED -> IN_EAR_CURVE
    DeviceType.CLIPS -> c(8f, 8f, 6f, 3f, 0f, 0f, 1f, 1f, 0f, 0f)
    DeviceType.PHONE_SPEAKER -> c(-12f, -8f, -4f, 0f, 0f, 1f, 2f, 3f, 2f, 0f)
    DeviceType.TABLET_SPEAKER -> c(-10f, -6f, -2f, 0f, 0f, 1f, 2f, 2f, 1f, 0f)
    DeviceType.BT_SPEAKER -> c(0f, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 0f, 0f)
}

fun presetCurve(preset: Preset): EqCurve = when (preset) {
    Preset.FLAT -> EqCurve.FLAT
    Preset.BASS_BOOST -> c(4f, 6f, 5f, 3f, 0f, 0f, 0f, 0f, 0f, 0f)
    Preset.VOCAL -> c(-2f, -2f, -1f, 0f, 1f, 3f, 4f, 3f, 0f, 0f)
    Preset.PODCAST -> c(-12f, -8f, -3f, -2f, -2f, 2f, 2.5f, 2.5f, 0f, 0f)
    Preset.POP -> c(1f, 2f, 2f, 0f, 0f, 1f, 2f, 1f, 1f, 0f)
    Preset.ROCK -> c(1f, 3f, 3f, 1f, 0f, 0f, 1f, 3f, 2f, 0f)
    Preset.ELECTRONIC -> c(5f, 4f, 2f, 0f, -2f, -1f, 0f, 2f, 4f, 4f)
    Preset.JAZZ_CLASSICAL -> c(0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 2f)
    Preset.LOUDNESS -> c(5f, 4f, 2f, 0f, 0f, 0f, 0f, 1f, 3f, 4f)
    Preset.NIGHT -> c(-2f, -2f, -1f, 0f, 0f, 1f, 1f, 0f, -2f, -3f)
}
