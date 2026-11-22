package com.equalizer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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

@OptIn(ExperimentalLayoutApi::class)
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
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val recommendedPresets = state.recommended.values.toSet()
    Column(
        Modifier
            .fillMaxSize()
            .background(Tokens.BgScreen)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Equalizer", style = MaterialTheme.typography.headlineMedium, color = Tokens.TextPrimary)
                Text(
                    "${state.output.displayName} · ${state.output.deviceType?.displayName ?: "sin tipo"}",
                    style = MaterialTheme.typography.bodyMedium, color = Tokens.TextSecondary,
                )
            }
            PowerButton(state.enabled, onTogglePower)
        }
        PrimaryTabRow(selectedTabIndex = tab, containerColor = Tokens.BgScreen, contentColor = Tokens.BrandPrimary) {
            listOf("Ecualizador", "Dispositivo").forEachIndexed { i, title ->
                Tab(
                    selected = tab == i, onClick = { tab = i },
                    text = { Text(title, color = if (tab == i) Tokens.BrandPrimary else Tokens.TextSecondary) },
                )
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (tab == 0) {
                Column(Modifier.alpha(if (state.enabled) 1f else 0.4f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Section("Presets") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Preset.values().forEach { p ->
                                PresetChip(p.displayName, p == state.preset, p in recommendedPresets, { onSelectPreset(p) })
                            }
                            state.customPresets.forEach { c ->
                                PresetChip(c.name, false, false, { onApplyCustom(c) }, onLongClick = { deleting = c.name })
                            }
                            PresetChip("+ Guardar como…", false, false, { saving = true })
                        }
                        if (recommendedPresets.isNotEmpty()) {
                            Text("★ recomendado para este dispositivo", style = MaterialTheme.typography.bodySmall, color = Tokens.TextTertiary)
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
            } else {
                Column(Modifier.alpha(if (state.enabled) 1f else 0.4f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    DeviceCard(state.output, onAssignType)
                    Section("Recomendado para este dispositivo") {
                        state.recommended.forEach { (content, preset) ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectPreset(preset); tab = 0 }
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
                }
                if (!state.dumpAvailable) {
                    Section("Falta un permiso") {
                        Text(
                            "Sin el permiso DUMP no se detectan apps como Pocket Casts o Voice. Conecta el teléfono por USB y ejecuta:",
                            color = Tokens.Warning,
                        )
                        Text("adb shell pm grant com.equalizer.app android.permission.DUMP", color = Tokens.TextPrimary)
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
            }
            Spacer(Modifier.height(16.dp))
        }
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
