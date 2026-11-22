package com.equalizer.app

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import com.equalizer.app.ui.mergeNowPlaying
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import com.equalizer.app.ui.OnboardingScreen
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.ContextCompat
import com.equalizer.app.core.CustomPresets
import com.equalizer.app.core.customPresetsDataStore
import com.equalizer.app.device.DeviceRegistry
import com.equalizer.app.device.deviceDataStore
import com.equalizer.app.engine.EqRuntime
import com.equalizer.app.engine.EqService
import com.equalizer.app.engine.Settings as EqSettings
import com.equalizer.app.engine.settingsDataStore
import com.equalizer.app.ui.MainScreen
import com.equalizer.app.ui.UpdateSection
import com.equalizer.app.update.Installer
import com.equalizer.app.update.RealHttp
import com.equalizer.app.update.UpdateChecker
import com.equalizer.app.update.UpdateController
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
                val nowPlaying = remember {
                    kotlinx.coroutines.flow.combine(EqRuntime.playing, EqRuntime.tracks) { playing, tracks ->
                        mergeNowPlaying(playing, tracks) { pkg ->
                            try {
                                packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
                            } catch (e: Exception) { pkg }
                        }
                    }.stateIn(scope, SharingStarted.Eagerly, emptyList())
                }
                val vm = remember {
                    MainViewModel(
                        scope, EqRuntime.output, EqRuntime.engine.sessions, EqRuntime.engine.failed, EqRuntime.dumpAvailable,
                        DeviceRegistry(deviceDataStore), EqSettings(settingsDataStore), CustomPresets(customPresetsDataStore),
                        EqRuntime.engine.status,
                        nowPlaying,
                    ) { EqRuntime.detector?.refresh() }
                }
                val updater = remember {
                    val installer = Installer(applicationContext)
                    UpdateController(
                        scope, BuildConfig.VERSION_NAME,
                        { UpdateChecker(RealHttp, BuildConfig.GITHUB_OWNER, BuildConfig.GITHUB_REPO).latest() },
                        { installer.downloadAndInstall(it) },
                    )
                }
                val updateState by updater.state.collectAsState()
                val state by vm.uiState.collectAsState()
                var trackAccess by remember { mutableStateOf(hasTrackAccess()) }
                val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
                androidx.compose.runtime.DisposableEffect(lifecycle) {
                    val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
                        if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) trackAccess = hasTrackAccess()
                    }
                    lifecycle.addObserver(obs)
                    onDispose { lifecycle.removeObserver(obs) }
                }
                val settings = remember { EqSettings(settingsDataStore) }
                val onboardingDone by settings.onboardingDone.collectAsState(initial = null)
                var showGuide by remember { mutableStateOf(false) }
                if (onboardingDone == null) return@EqTheme
                if (onboardingDone == false || showGuide) {
                    OnboardingScreen {
                        showGuide = false
                        scope.launch { settings.setOnboardingDone(true) }
                    }
                    return@EqTheme
                }
                MainScreen(
                    state, vm::onTogglePower, vm::onSelectPreset, vm::onAssignType, vm::onBandChanged,
                    onSaveCustom = vm::onSaveCustom, onApplyCustom = vm::onApplyCustom, onDeleteCustom = vm::onDeleteCustom, onSelectMethod = vm::onSelectMethod, onShowGuide = { showGuide = true },
                    trackAccess = trackAccess,
                    onEnableTrackAccess = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                    footer = { UpdateSection(updater.currentVersion, updateState, updater::check, updater::install) },
                )
            }
        }
    }

    private fun hasTrackAccess() =
        androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
}
