package com.equalizer.app.engine

import android.media.audiofx.DynamicsProcessing
import com.equalizer.app.core.BAND_FREQS_HZ
import com.equalizer.app.core.Combined

class DynamicsEffectHandle(sessionId: Int) : EffectHandle {
    private val dp: DynamicsProcessing

    init {
        val n = BAND_FREQS_HZ.size
        val config = DynamicsProcessing.Config.Builder(
            DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION, 2,
            true, n, false, 0, false, 0, true,
        ).build()
        dp = DynamicsProcessing(0, sessionId, config)
        val eq = DynamicsProcessing.Eq(true, true, n)
        BAND_FREQS_HZ.forEachIndexed { i, f -> eq.setBand(i, DynamicsProcessing.EqBand(true, f.toFloat(), 0f)) }
        dp.setPreEqAllChannelsTo(eq)
        dp.setLimiterAllChannelsTo(DynamicsProcessing.Limiter(true, true, 0, 1f, 60f, 10f, -2f, 0f))
        dp.enabled = true
    }

    override fun apply(c: Combined) {
        c.bands.forEachIndexed { i, g ->
            dp.setPreEqBandAllChannelsTo(i, DynamicsProcessing.EqBand(true, BAND_FREQS_HZ[i].toFloat(), g))
        }
        dp.setInputGainAllChannelsTo(c.preampDb)
    }

    override fun release() {
        try { dp.enabled = false } finally { dp.release() }
    }
}
