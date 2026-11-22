package com.equalizer.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfilesTest {
    @Test fun flat_isAllZero() {
        assertEquals(List(10) { 0f }, presetCurve(Preset.FLAT).gainsDb)
    }

    @Test fun nullDevice_isFlat() {
        assertEquals(EqCurve.FLAT, baseProfile(null))
    }

    @Test fun inEar_bassAtLeast6dbAboveOverEar_at62hz() {
        val diff = baseProfile(DeviceType.IN_EAR_WIRED).gainsDb[1] - baseProfile(DeviceType.OVER_EAR).gainsDb[1]
        assertTrue("diff=$diff", diff >= 6f)
    }

    @Test fun clips_bassBoostAtLeast4db_at62hz() {
        assertTrue(baseProfile(DeviceType.CLIPS).gainsDb[1] >= 4f)
    }

    @Test fun phoneSpeaker_cutsLowEnd() {
        val g = baseProfile(DeviceType.PHONE_SPEAKER).gainsDb
        assertTrue(g[0] <= -6f && g[1] <= -6f)
    }

    @Test fun podcast_rollsOffBelow100AndLiftsPresence() {
        val g = presetCurve(Preset.PODCAST).gainsDb
        assertTrue(g[1] <= -6f)
        assertTrue("2k=${g[6]}", g[6] in 2f..3f)
    }

    @Test fun bassBoost_liftsLowBands() {
        val g = presetCurve(Preset.BASS_BOOST).gainsDb
        assertTrue(g[1] >= 4f && g[2] > 0f)
    }

    @Test fun everyTypeAndPreset_hasTenBands() {
        DeviceType.values().forEach { assertEquals(10, baseProfile(it).gainsDb.size) }
        Preset.values().forEach { assertEquals(10, presetCurve(it).gainsDb.size) }
    }
}
