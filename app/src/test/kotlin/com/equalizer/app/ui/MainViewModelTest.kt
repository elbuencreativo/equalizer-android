package com.equalizer.app.ui

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.equalizer.app.core.ContentType
import com.equalizer.app.core.CustomPresets
import com.equalizer.app.core.DeviceType
import com.equalizer.app.core.Preset
import com.equalizer.app.core.recommendedFor
import com.equalizer.app.device.ActiveOutput
import com.equalizer.app.device.DeviceRegistry
import com.equalizer.app.device.OutputCandidate
import com.equalizer.app.device.OutputKind
import com.equalizer.app.device.TypeSource
import com.equalizer.app.device.buildActive
import com.equalizer.app.engine.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @get:Rule val tmp = TemporaryFolder()

    private val bt = OutputCandidate("bt", "Buds X", OutputKind.BLUETOOTH)
    private val btOutput = buildActive(bt, null, false)

    private class Fixture(
        val output: MutableStateFlow<ActiveOutput>,
        val registry: DeviceRegistry,
        val settings: Settings,
        val vm: MainViewModel,
        val dump: MutableStateFlow<Boolean>,
    )

    private fun fixture(scope: CoroutineScope, initial: ActiveOutput, candidate: OutputCandidate = bt): Fixture {
        val output = MutableStateFlow(initial)
        val registry = DeviceRegistry(PreferenceDataStoreFactory.create { tmp.newFolder().resolve("d.preferences_pb") })
        val settings = Settings(PreferenceDataStoreFactory.create { tmp.newFolder().resolve("s.preferences_pb") })
        val dump = MutableStateFlow(true)
        val custom = CustomPresets(PreferenceDataStoreFactory.create { tmp.newFolder().resolve("c.preferences_pb") })
        val vm = MainViewModel(
            scope, output, MutableStateFlow(emptyMap()), MutableStateFlow(emptySet()), dump, registry, settings, custom,
        ) {
            output.value = buildActive(candidate, runBlocking { registry.typeFor(candidate.key, candidate.displayName) }, false)
        }
        return Fixture(output, registry, settings, vm, dump)
    }

    @Test fun viewModel_unassignedBluetooth_exposesUnassignedSource() = runTest(UnconfinedTestDispatcher()) {
        val f = fixture(backgroundScope, btOutput)
        assertEquals(TypeSource.UNASSIGNED, f.vm.uiState.first { it.output.key == "bt" }.output.typeSource)
    }

    @Test fun viewModel_assignType_updatesOutputAndCurve() = runTest(UnconfinedTestDispatcher()) {
        val f = fixture(backgroundScope, btOutput)
        f.vm.onAssignType(DeviceType.BUDS)
        val s = f.vm.uiState.first { it.output.deviceType == DeviceType.BUDS }
        assertEquals(TypeSource.ASSIGNED, s.output.typeSource)
        assertEquals(9f, s.bands[1], 0.001f)
    }

    @Test fun viewModel_selectPreset_changesCombinedBands() = runTest(UnconfinedTestDispatcher()) {
        val f = fixture(backgroundScope, btOutput)
        f.vm.onSelectPreset(Preset.BASS_BOOST)
        assertEquals(6f, f.vm.uiState.first { it.preset == Preset.BASS_BOOST }.bands[1], 0.001f)
    }

    @Test fun viewModel_togglePower_persistsDisabled() = runTest(UnconfinedTestDispatcher()) {
        val f = fixture(backgroundScope, btOutput)
        f.vm.onTogglePower()
        f.vm.uiState.first { !it.enabled }
    }

    @Test fun viewModel_recommendedMatchesDeviceType() = runTest(UnconfinedTestDispatcher()) {
        val spk = OutputCandidate("builtin", "Parlante del celular", OutputKind.BUILT_IN_SPEAKER)
        val f = fixture(backgroundScope, buildActive(spk, null, false), spk)
        val s = f.vm.uiState.first { it.output.key == "builtin" }
        assertEquals(recommendedFor(DeviceType.PHONE_SPEAKER), s.recommended)
        assertEquals(Preset.PODCAST, s.recommended[ContentType.PODCAST])
    }

    @Test fun viewModel_bandChanged_clampsTo12() = runTest(UnconfinedTestDispatcher()) {
        val f = fixture(backgroundScope, btOutput)
        f.vm.onBandChanged(0, 20f)
        assertEquals(12f, f.vm.uiState.first { it.bands[0] == 12f }.bands[0], 0.001f)
    }

    @Test fun viewModel_selectPreset_resetsManualBandEdits() = runTest(UnconfinedTestDispatcher()) {
        val f = fixture(backgroundScope, btOutput)
        f.vm.onBandChanged(0, 5f)
        f.vm.uiState.first { it.bands[0] == 5f }
        f.vm.onSelectPreset(Preset.FLAT)
        assertEquals(0f, f.vm.uiState.first { it.bands[0] == 0f }.bands[0], 0.001f)
    }

    @Test fun viewModel_saveCustom_thenApply_restoresBands() = runTest(UnconfinedTestDispatcher()) {
        val f = fixture(backgroundScope, btOutput)
        f.vm.onBandChanged(0, 5f)
        f.vm.uiState.first { it.bands[0] == 5f }
        f.vm.onSaveCustom("Mío")
        f.vm.uiState.first { s -> s.customPresets.any { it.name == "Mío" } }
        f.vm.onSelectPreset(Preset.PODCAST)
        f.vm.uiState.first { it.preset == Preset.PODCAST && it.bands[0] == -12f }
        f.vm.onApplyCustom(f.vm.uiState.value.customPresets.first { it.name == "Mío" })
        val s = f.vm.uiState.first { it.preset == Preset.FLAT && it.bands[0] == 5f }
        assertEquals(5f, s.bands[0], 0.001f)
    }

    @Test fun viewModel_dumpUnavailable_isExposedInState() = runTest(UnconfinedTestDispatcher()) {
        val f = fixture(backgroundScope, btOutput)
        f.dump.value = false
        f.vm.uiState.first { !it.dumpAvailable }
    }
}
