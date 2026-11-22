package com.equalizer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.equalizer.app.core.BAND_FREQS_HZ
import com.equalizer.app.core.CustomPreset
import com.equalizer.app.core.DeviceType
import com.equalizer.app.core.Preset
import com.equalizer.app.ui.components.BandSlider
import com.equalizer.app.ui.components.DeviceCard
import com.equalizer.app.ui.components.PowerButton
import com.equalizer.app.ui.components.PresetChip
import com.equalizer.app.ui.theme.Tokens

@Composable
fun MainScreen(
    state: MainUiState,
    onTogglePower: () -> Unit,
    onSelectPreset: (Preset) -> Unit,
    onAssignType: (DeviceType) -> Unit,
    onBandChanged: (Int, Float) -> Unit,
    onSaveCustom: (String) -> Unit = {},
    onApplyCustom: (CustomPreset) -> Unit = {},
    onDeleteCustom: (String) -> Unit = {},
    footer: @Composable () -> Unit = {},
) {
    var saving by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<String?>(null) }
    val recommendedPresets = state.recommended.values.toSet()
    Column(
        Modifier
            .fillMaxSize()
            .background(Tokens.BgScreen)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Equalizer", style = MaterialTheme.typography.headlineMedium, color = Tokens.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            PowerButton(state.enabled, onTogglePower)
        }

        Column(Modifier.alpha(if (state.enabled) 1f else 0.4f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DeviceCard(state.output, onAssignType)

            Section("Recomendado para este dispositivo") {
                state.recommended.forEach { (content, preset) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPreset(preset) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(content.displayName, color = Tokens.TextSecondary, modifier = Modifier.weight(1f))
                        Text(
                            preset.displayName,
                            color = if (preset == state.preset) Tokens.BrandPrimary else Tokens.TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            Section("Presets") {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Preset.values().forEach { p ->
                        PresetChip(p.displayName, p == state.preset, p in recommendedPresets, { onSelectPreset(p) })
                    }
                    state.customPresets.forEach { c ->
                        PresetChip(c.name, false, false, { onApplyCustom(c) }, onLongClick = { deleting = c.name })
                    }
                    PresetChip("+ Guardar como…", false, false, { saving = true })
                }
            }

            Section("Bandas (dB)") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    BAND_FREQS_HZ.forEachIndexed { i, hz ->
                        BandSlider(hz, state.bands[i], { onBandChanged(i, it) })
                    }
                }
            }
        }

        Section("Apps con audio") {
            if (state.apps.isEmpty()) {
                Text("Ninguna app está reproduciendo audio.", color = Tokens.TextSecondary)
            } else {
                state.apps.forEach { Text("• $it", color = Tokens.TextPrimary) }
            }
            if (state.failedCount > 0) {
                Text(
                    "${state.failedCount} sesión(es) no aceptaron el ecualizador.",
                    color = Tokens.Warning, style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        footer()
        Spacer(Modifier.height(16.dp))
    }

    if (saving) NameDialog(onConfirm = { onSaveCustom(it); saving = false }, onDismiss = { saving = false })
    deleting?.let { name ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("¿Borrar «$name»?") },
            confirmButton = { TextButton(onClick = { onDeleteCustom(name); deleting = null }) { Text("Borrar") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun NameDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nombre del preset") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Tokens.TextPrimary)
        content()
    }
}
