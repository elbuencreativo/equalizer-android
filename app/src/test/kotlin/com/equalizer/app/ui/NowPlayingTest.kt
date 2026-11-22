package com.equalizer.app.ui

import com.equalizer.app.device.ActiveOutput
import com.equalizer.app.device.OutputKind
import com.equalizer.app.device.TypeSource
import com.equalizer.app.core.DeviceType
import com.equalizer.app.engine.Track
import com.equalizer.app.ui.components.savedHint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NowPlayingTest {
    private val labels = mapOf("a" to "Spotify", "b" to "Brave", "c" to "Pocket Casts")
    private val labelOf = { p: String -> labels[p] ?: p }

    @Test fun appWithoutTrackInfo_showsOnlyAppName() {
        assertEquals(listOf(NowPlayingItem("Brave", null, null)), mergeNowPlaying(listOf("b"), emptyMap(), labelOf))
    }

    @Test fun appWithTrack_includesTitleAndArtist() {
        val r = mergeNowPlaying(listOf("a"), mapOf("a" to Track("Song", "Band")), labelOf)
        assertEquals(listOf(NowPlayingItem("Spotify", "Song", "Band")), r)
    }

    @Test fun trackWithoutDetectedApp_isStillShown() {
        val r = mergeNowPlaying(emptyList(), mapOf("c" to Track("Episodio 5", null)), labelOf)
        assertEquals(listOf(NowPlayingItem("Pocket Casts", "Episodio 5", null)), r)
    }

    @Test fun duplicatesCollapse_andNothingPlayingIsEmpty() {
        assertEquals(1, mergeNowPlaying(listOf("a", "a"), emptyMap(), labelOf).size)
        assertTrue(mergeNowPlaying(emptyList(), emptyMap(), labelOf).isEmpty())
    }

    @Test fun savedHint_explainsWhetherTheChoiceIsRemembered() {
        val assigned = ActiveOutput("bt", "Buds X", OutputKind.BLUETOOTH, DeviceType.BUDS, TypeSource.ASSIGNED)
        assertTrue(savedHint(assigned).startsWith("✓ Guardado"))
        assertEquals("✓ Guardado: la app recordará este tipo para este dispositivo.", savedHint(assigned))
        val detected = assigned.copy(typeSource = TypeSource.DETECTED)
        assertTrue(savedHint(detected).contains("automáticamente"))
        val none = assigned.copy(deviceType = null, typeSource = TypeSource.UNASSIGNED)
        assertTrue(savedHint(none).contains("recordará"))
    }
}
