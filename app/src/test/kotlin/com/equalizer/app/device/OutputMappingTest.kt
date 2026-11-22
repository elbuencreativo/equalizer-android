package com.equalizer.app.device

import android.media.AudioDeviceInfo
import com.equalizer.app.core.DeviceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OutputMappingTest {
    @Test fun mapKind_a2dp_isBluetooth() =
        assertEquals(OutputKind.BLUETOOTH, mapKind(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP))

    @Test fun mapKind_wiredHeadset_isWired() =
        assertEquals(OutputKind.WIRED, mapKind(AudioDeviceInfo.TYPE_WIRED_HEADSET))

    @Test fun mapKind_builtinSpeaker_isBuiltIn() =
        assertEquals(OutputKind.BUILT_IN_SPEAKER, mapKind(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER))

    @Test fun defaultType_bluetooth_isNull() = assertNull(defaultTypeFor(OutputKind.BLUETOOTH, false))

    @Test fun defaultType_builtIn_dependsOnTablet() {
        assertEquals(DeviceType.PHONE_SPEAKER, defaultTypeFor(OutputKind.BUILT_IN_SPEAKER, false))
        assertEquals(DeviceType.TABLET_SPEAKER, defaultTypeFor(OutputKind.BUILT_IN_SPEAKER, true))
    }

    @Test fun pick_bluetoothOverWiredOverBuiltIn() {
        val bt = OutputCandidate("bt", "Buds", OutputKind.BLUETOOTH)
        val wired = OutputCandidate("w", "Cable", OutputKind.WIRED)
        val spk = OutputCandidate("builtin", "Teléfono", OutputKind.BUILT_IN_SPEAKER)
        assertEquals(bt, pickActive(listOf(spk, wired, bt)))
        assertEquals(wired, pickActive(listOf(spk, wired)))
        assertEquals(spk, pickActive(listOf(spk)))
        assertNull(pickActive(emptyList()))
    }

    @Test fun buildActive_marksTypeSource() {
        val bt = OutputCandidate("bt", "Buds", OutputKind.BLUETOOTH)
        assertEquals(TypeSource.UNASSIGNED, buildActive(bt, null, false).typeSource)
        assertEquals(TypeSource.ASSIGNED, buildActive(bt, DeviceType.BUDS, false).typeSource)
        val spk = OutputCandidate("builtin", "Teléfono", OutputKind.BUILT_IN_SPEAKER)
        val a = buildActive(spk, null, false)
        assertEquals(TypeSource.DETECTED, a.typeSource)
        assertEquals(DeviceType.PHONE_SPEAKER, a.deviceType)
    }
}
