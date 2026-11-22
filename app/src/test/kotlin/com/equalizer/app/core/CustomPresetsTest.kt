package com.equalizer.app.core

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CustomPresetsTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun store() = CustomPresets(PreferenceDataStoreFactory.create { tmp.newFolder().resolve("c.preferences_pb") })
    private fun curve(first: Float) = EqCurve(List(10) { if (it == 0) first else 0f })

    @Test fun save_thenListed() = runTest {
        val s = store()
        s.save(CustomPreset("Mío", curve(3f)))
        assertEquals(listOf(CustomPreset("Mío", curve(3f))), s.all.first())
    }

    @Test fun save_sameNameOverwrites() = runTest {
        val s = store()
        s.save(CustomPreset("Mío", curve(3f)))
        s.save(CustomPreset("Mío", curve(5f)))
        assertEquals(listOf(CustomPreset("Mío", curve(5f))), s.all.first())
    }

    @Test fun delete_removes() = runTest {
        val s = store()
        s.save(CustomPreset("A", curve(1f)))
        s.delete("A")
        assertTrue(s.all.first().isEmpty())
    }

    @Test fun save_blankName_isRejected() = runTest {
        try { store().save(CustomPreset("  ", curve(1f))); fail("debía rechazar nombre vacío") }
        catch (e: IllegalArgumentException) { /* esperado */ }
    }

    @Test fun save_nameWithSeparator_roundTrips() = runTest {
        val s = store()
        s.save(CustomPreset("a|b\nc", curve(2f)))
        assertEquals(1, s.all.first().size)
        assertEquals(curve(2f), s.all.first()[0].curve)
    }
}
