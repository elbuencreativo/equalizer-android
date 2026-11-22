package com.equalizer.app

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.equalizer.app.core.Preset
import com.equalizer.app.device.ActiveOutput
import com.equalizer.app.engine.EqRuntime
import com.equalizer.app.engine.EqService
import com.equalizer.app.engine.Settings
import com.equalizer.app.engine.settingsDataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/** Pantalla de prototipo: salida activa, sesiones detectadas, encendido y un preset de prueba. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { PrototypeScreen() } }
    }

    @Composable
    private fun PrototypeScreen() {
        val settings = Settings(settingsDataStore)
        val scope = rememberCoroutineScope()
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            ContextCompat.startForegroundService(this, Intent(this, EqService::class.java))
        }
        LaunchedEffect(Unit) {
            launcher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.BLUETOOTH_CONNECT))
        }
        val enabled by settings.enabled.collectAsState(initial = true)
        val preset by settings.preset.collectAsState(initial = Preset.FLAT)
        val sessions by EqRuntime.engine.sessions.collectAsState()
        val failed by EqRuntime.engine.failed.collectAsState()
        val output by remember { outputFlow() }.collectAsState(initial = ActiveOutput.NONE)

        Column(Modifier.padding(24.dp)) {
            Text("Salida: ${output.displayName} (${output.kind}, tipo ${output.deviceType ?: "sin asignar"})")
            Switch(checked = enabled, onCheckedChange = { scope.launch { settings.setEnabled(it) } })
            Text("Preset actual: ${preset.displayName}")
            Button(onClick = { scope.launch { settings.setPreset(Preset.BASS_BOOST) } }) { Text("Probar Bass Boost") }
            Button(onClick = { scope.launch { settings.setPreset(Preset.FLAT) } }) { Text("Plano") }
            Button(onClick = { EqRuntime.engine.setCurve(com.equalizer.app.core.Combined(List(10) { -12f }, -12f)) }) { Text("PRUEBA: bajar todo -24 dB") }
            Button(onClick = { EqRuntime.engine.setCurve(com.equalizer.app.core.Combined(List(10) { 0f }, 0f)) }) { Text("PRUEBA: restaurar 0 dB") }
            Text("Sesiones detectadas: ${sessions.size}")
            sessions.forEach { (id, pkg) -> Text("• $pkg (sesión $id)${if (id in failed) " — falló" else ""}") }
        }
    }

    /** El detector lo crea el servicio; se sigue su salida cuando ya existe. */
    private fun outputFlow() = flow {
        while (true) {
            val d = EqRuntime.detector
            if (d != null) d.active.collect { emit(it) } else { emit(ActiveOutput.NONE); delay(500) }
        }
    }
}
