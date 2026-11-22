package com.equalizer.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BalanceAndLegacyTest {
    @Test fun centeredBalance_leavesBothChannelsAtPreamp() {
        assertEquals(Pair(-3f, -3f), channelGainsDb(-3f, 0f))
    }

    @Test fun balanceToTheRight_attenuatesOnlyLeft() {
        val (l, r) = channelGainsDb(0f, 0.5f)
        assertEquals(-6.02f, l, 0.01f)
        assertEquals(0f, r, 0.001f)
    }

    @Test fun balanceToTheLeft_attenuatesOnlyRight_andKeepsPreamp() {
        val (l, r) = channelGainsDb(-2f, -0.5f)
        assertEquals(-2f, l, 0.001f)
        assertEquals(-8.02f, r, 0.01f)
    }

    @Test fun fullBalance_isCappedInsteadOfMinusInfinity() {
        val (l, _) = channelGainsDb(0f, 1f)
        assertEquals(-40f, l, 0.001f)
    }

    @Test fun legacyLevels_flatCurveGivesPreampOnEveryDeviceBand() {
        val levels = legacyBandLevelsMb(listOf(60_000, 230_000, 910_000, 3_600_000, 14_000_000), List(10) { 0f }, -2f, -1500, 1500)
        assertEquals(List(5) { (-200).toShort() }, levels)
    }

    @Test fun legacyLevels_bandOnOneOfOurFrequencies_takesThatGain() {
        val bands = List(10) { it.toFloat() } // 1 kHz es la banda 5
        val levels = legacyBandLevelsMb(listOf(1_000_000), bands, 0f, -1500, 1500)
        assertEquals(500.toShort(), levels[0])
    }

    @Test fun legacyLevels_areClampedToTheDeviceRange() {
        val levels = legacyBandLevelsMb(listOf(1_000_000), List(10) { 12f }, 0f, -1500, 500)
        assertEquals(500.toShort(), levels[0])
        assertTrue(levels.size == 1)
    }

    @Test fun snapBalance_roundsToFivePercent_andClamps() {
        assertEquals(0.35f, snapBalance(0.3721f), 0.0001f)
        assertEquals(-1f, snapBalance(-4f), 0.0001f)
        assertEquals(0f, snapBalance(0.02f), 0.0001f)
    }

    @Test fun balanceLabel_isPlainSpanish() {
        assertEquals("Centro", balanceLabel(0f))
        assertEquals("Derecha 30 %", balanceLabel(0.3f))
        assertEquals("Izquierda 45 %", balanceLabel(-0.45f))
    }
}
