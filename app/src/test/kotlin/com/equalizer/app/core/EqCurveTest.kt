package com.equalizer.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EqCurveTest {
    private fun curve(vararg first: Float): EqCurve =
        EqCurve(List(10) { i -> first.getOrElse(i) { 0f } })

    @Test fun combine_sumsPerBand() {
        val c = combine(curve(2f), curve(3f))
        assertEquals(5f, c.bands[0], 0.001f)
    }

    @Test fun combine_clampsEachBandTo12() {
        val c = combine(curve(10f), curve(10f))
        assertEquals(12f, c.bands[0], 0.001f)
    }

    @Test fun combine_preampIsHalfOfHighestPositive_limiterCatchesTheRest() {
        val c = combine(curve(2f, 1f), curve(4f, 0f))
        assertEquals(-3f, c.preampDb, 0.001f)
    }

    @Test fun combine_allNegative_preampIsZero() {
        val c = combine(curve(-3f, -1f), curve(-2f))
        assertEquals(0f, c.preampDb, 0.001f)
    }

    @Test fun curve_requiresTenBands() {
        assertThrows(IllegalArgumentException::class.java) { EqCurve(List(9) { 0f }) }
    }
}
