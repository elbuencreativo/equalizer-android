package com.equalizer.app.device

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.equalizer.app.core.DeviceType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DeviceRegistryTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun registry(): DeviceRegistry {
        val file = tmp.newFolder().resolve("devices.preferences_pb")
        return DeviceRegistry(PreferenceDataStoreFactory.create { file })
    }

    @Test fun registry_assignThenLookupByKey() = runTest {
        val r = registry()
        r.assign("AA:BB", "Buds X", DeviceType.BUDS)
        assertEquals(DeviceType.BUDS, r.typeFor("AA:BB", "otro nombre"))
    }

    @Test fun registry_fallsBackToNameWhenKeyChanges() = runTest {
        val r = registry()
        r.assign("AA:BB", "Buds X", DeviceType.BUDS)
        assertEquals(DeviceType.BUDS, r.typeFor("CC:DD", "Buds X"))
    }

    @Test fun registry_unknownDevice_returnsNull() = runTest {
        assertNull(registry().typeFor("ZZ", "Nada"))
    }
}
