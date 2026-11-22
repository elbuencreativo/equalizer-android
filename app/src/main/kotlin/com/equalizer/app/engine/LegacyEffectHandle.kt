package com.equalizer.app.engine

import android.media.audiofx.Equalizer
import com.equalizer.app.core.Combined
import com.equalizer.app.core.legacyBandLevelsMb

/**
 * Ecualizador clásico de Android sobre la mezcla global, para teléfonos donde el moderno
 * ([DynamicsEffectHandle]) no se deja usar. Tiene pocas bandas (según el fabricante) y no permite balance.
 */
class LegacyEffectHandle(sessionId: Int) : EffectHandle {
    private val eq = Equalizer(0, sessionId)
    private val centers: List<Int>
    private val minMb: Int
    private val maxMb: Int

    init {
        try {
            centers = (0 until eq.numberOfBands.toInt()).map { eq.getCenterFreq(it.toShort()) }
            val range = eq.bandLevelRange
            minMb = range[0].toInt()
            maxMb = range[1].toInt()
            eq.enabled = true
        } catch (e: Exception) {
            eq.release()
            throw e
        }
    }

    override fun apply(c: Combined) {
        legacyBandLevelsMb(centers, c.bands, c.preampDb, minMb, maxMb)
            .forEachIndexed { i, level -> eq.setBandLevel(i.toShort(), level) }
    }

    override fun release() {
        try { eq.enabled = false } finally { eq.release() }
    }
}
