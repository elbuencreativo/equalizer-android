package com.equalizer.app

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.ContextCompat
import com.equalizer.app.device.DeviceRegistry
import com.equalizer.app.device.deviceDataStore
import com.equalizer.app.engine.EqRuntime
import com.equalizer.app.engine.EqService
import com.equalizer.app.engine.Settings
import com.equalizer.app.engine.settingsDataStore
import com.equalizer.app.ui.MainScreen
import com.equalizer.app.ui.MainViewModel
import com.equalizer.app.ui.theme.EqTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EqTheme {
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                    ContextCompat.startForegroundService(this, Intent(this, EqService::class.java))
                }
                LaunchedEffect(Unit) {
                    launcher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.BLUETOOTH_CONNECT))
                }
                val scope = rememberCoroutineScope()
                val vm = remember {
                    MainViewModel(
                        scope, EqRuntime.output, EqRuntime.engine.sessions, EqRuntime.engine.failed,
                        DeviceRegistry(deviceDataStore), Settings(settingsDataStore),
                    ) { EqRuntime.detector?.refresh() }
                }
                val state by vm.uiState.collectAsState()
                MainScreen(state, vm::onTogglePower, vm::onSelectPreset, vm::onAssignType, vm::onBandChanged)
            }
        }
    }
}
