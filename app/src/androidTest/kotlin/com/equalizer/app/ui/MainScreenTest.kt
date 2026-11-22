package com.equalizer.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.equalizer.app.core.DeviceType
import com.equalizer.app.core.Preset
import com.equalizer.app.core.recommendedFor
import com.equalizer.app.device.ActiveOutput
import com.equalizer.app.device.OutputKind
import com.equalizer.app.device.TypeSource
import com.equalizer.app.ui.theme.EqTheme
import org.junit.Rule
import org.junit.Test

class MainScreenTest {
    @get:Rule val rule = createComposeRule()

    @Test fun showsDeviceNowPlayingPowerButtonAndSettings() {
        val state = MainUiState(
            output = ActiveOutput("bt", "Buds X", OutputKind.BLUETOOTH, DeviceType.BUDS, TypeSource.ASSIGNED),
            enabled = true,
            preset = Preset.FLAT,
            recommended = recommendedFor(DeviceType.BUDS),
            bands = List(10) { 0f },
            apps = emptyList(),
            failedCount = 0,
        )
        rule.setContent {
            EqTheme { MainScreen(state, {}, {}, {}, { _, _ -> }) }
        }
        rule.onNodeWithText("Buds X", substring = true).assertExists()
        rule.onNodeWithContentDescription("Encender o apagar").assertExists()
        rule.onNodeWithText("Ahora suena").assertExists()
        rule.onNodeWithText("Configuración").performClick()
        rule.onNodeWithText("Método de ecualización").assertExists()
    }
}
