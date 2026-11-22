package com.equalizer.app.core

val BAND_FREQS_HZ: List<Int> = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)

const val MAX_GAIN_DB = 12f

data class EqCurve(val gainsDb: List<Float>) {
    init {
        require(gainsDb.size == BAND_FREQS_HZ.size) { "EqCurve needs ${BAND_FREQS_HZ.size} bands" }
    }

    companion object {
        val FLAT = EqCurve(List(BAND_FREQS_HZ.size) { 0f })
    }
}

data class Combined(val bands: List<Float>, val preampDb: Float, val balance: Float = 0f)

fun combine(base: EqCurve, preset: EqCurve): Combined {
    val bands = base.gainsDb.zip(preset.gainsDb) { b, p -> (b + p).coerceIn(-MAX_GAIN_DB, MAX_GAIN_DB) }
    val preamp = -(bands.maxOrNull() ?: 0f).coerceAtLeast(0f)
    return Combined(bands, preamp)
}
