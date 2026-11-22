package com.equalizer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.equalizer.app.core.BAND_FREQS_HZ
import com.equalizer.app.core.CustomPreset
import com.equalizer.app.core.DeviceType
import com.equalizer.app.core.Preset
import com.equalizer.app.core.balanceLabel
import com.equalizer.app.engine.EqMethod
import com.equalizer.app.ui.components.BandSlider
import com.equalizer.app.ui.components.DeviceCard
import com.equalizer.app.ui.components.NowPlayingCard
import com.equalizer.app.ui.components.PowerButton
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
    onSelectMethod: (EqMethod) -> Unit = {},
    onShowGuide: () -> Unit = {},
    onBalanceChanged: (Float) -> Unit = {},
    trackAccess: Boolean = true,
    onEnableTrackAccess: () -> Unit = {},
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
        Text(
            "Equalizer", style = MaterialTheme.typography.titleLarge, color = Tokens.TextPrimary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        )
        PrimaryTabRow(selectedTabIndex = tab, containerColor = Tokens.BgScreen, contentColor = Tokens.BrandPrimary) {
            listOf("Ecualizador", "Configuración").forEachIndexed { i, title ->
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
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PowerButton(state.enabled, onTogglePower, size = 96.dp)
                        Text(
                            if (state.enabled) "Encendido" else "Apagado",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (state.enabled) Tokens.BrandPrimary else Tokens.TextSecondary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    NowPlayingCard(state.nowPlaying, trackAccess, onEnableTrackAccess, Modifier.weight(1f))
                }
                if (state.fellBack) {
                    Text(
                        if (state.activeMethod == EqMethod.LEGACY)
                            "El método «Global» no funciona en este teléfono; la app usa el ecualizador «Clásico», con menos bandas y sin balance. Mira los detalles en Configuración."
                        else
                            "Los métodos que ecualizan todo el sonido dejaron de funcionar y la app cambió sola al modo «Por app». Mira los detalles en Configuración.",
                        color = Tokens.Warning, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                DeviceCard(state.output, onAssignType)
                Column(Modifier.alpha(if (state.enabled) 1f else 0.4f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PresetDropdown(
                        state, recommendedPresets, onSelectPreset, onApplyCustom,
                        onAskDelete = { deleting = it }, onSave = { saving = true },
                    )
                    Section("Bandas (dB)") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            BAND_FREQS_HZ.forEachIndexed { i, hz ->
                                BandSlider(hz, state.bands[i], { onBandChanged(i, it) })
                            }
                        }
                    }
                    BalanceSection(state.balance, state.activeMethod != EqMethod.LEGACY, onBalanceChanged)
                }
            } else {
                MethodSection(state, onSelectMethod)
                if (state.activeMethod == EqMethod.PER_APP && !state.dumpAvailable) {
                    Section("Falta un permiso") {
                        Text(
                            "Para que el ecualizador encuentre apps como Pocket Casts o Voice, que no avisan cuando suenan, falta un permiso. " +
                                "Conecta el teléfono al computador por USB y ejecuta este comando:",
                            color = Tokens.Warning,
                        )
                        Text("adb shell pm grant com.equalizer.app android.permission.DUMP", color = Tokens.TextPrimary)
                    }
                }
                if (state.failedCount > 0) {
                    Section("Apps que no aceptaron el ecualizador") {
                        Text(
                            "No se pudo aplicar el ecualizador en ${state.failedCount} app(s). Esas apps no lo permitieron; " +
                                "no es un fallo de Equalizer. Prueba cerrarlas y abrirlas de nuevo" +
                                if (state.activeMethod == EqMethod.PER_APP) " o cambia al método «Global»." else ".",
                            color = Tokens.Warning, style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                TextButton(onClick = onShowGuide) { Text("Ver guía de uso", color = Tokens.BrandPrimary) }
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

/** Lista desplegable grande con todos los presets; la ★ marca los recomendados para este aparato. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetDropdown(
    state: MainUiState,
    recommended: Set<Preset>,
    onSelectPreset: (Preset) -> Unit,
    onApplyCustom: (CustomPreset) -> Unit,
    onAskDelete: (String) -> Unit,
    onSave: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = (if (state.preset in recommended) "★ " else "") + state.preset.displayName,
            onValueChange = {}, readOnly = true, singleLine = true,
            label = { Text("Preset") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            textStyle = MaterialTheme.typography.titleMedium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Tokens.TextPrimary, unfocusedTextColor = Tokens.TextPrimary,
                focusedBorderColor = Tokens.BrandPrimary, unfocusedBorderColor = Tokens.Border,
                focusedLabelColor = Tokens.BrandPrimary, unfocusedLabelColor = Tokens.TextSecondary,
                focusedContainerColor = Tokens.BgSurface, unfocusedContainerColor = Tokens.BgSurface,
            ),
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = Tokens.BgSurface) {
            Preset.values().forEach { p ->
                DropdownMenuItem(
                    text = {
                        Text(
                            (if (p in recommended) "★ " else "") + p.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (p == state.preset) Tokens.BrandPrimary else Tokens.TextPrimary,
                        )
                    },
                    onClick = { open = false; onSelectPreset(p) },
                )
            }
            state.customPresets.forEach { c ->
                DropdownMenuItem(
                    text = { Text(c.name, style = MaterialTheme.typography.titleMedium, color = Tokens.TextPrimary) },
                    trailingIcon = {
                        Text(
                            "✕", color = Tokens.TextSecondary,
                            modifier = Modifier.clickable { open = false; onAskDelete(c.name) }.padding(8.dp),
                        )
                    },
                    onClick = { open = false; onApplyCustom(c) },
                )
            }
            DropdownMenuItem(
                text = { Text("+ Guardar como…", style = MaterialTheme.typography.titleMedium, color = Tokens.BrandPrimary) },
                onClick = { open = false; onSave() },
            )
        }
    }
    if (recommended.isNotEmpty()) {
        Text("★ recomendado para el aparato que estás usando", style = MaterialTheme.typography.bodySmall, color = Tokens.TextTertiary)
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

@Composable
private fun BalanceSection(balance: Float, supported: Boolean, onChange: (Float) -> Unit) {
    Section("Balance") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Izq.", color = Tokens.TextSecondary, style = MaterialTheme.typography.labelLarge)
            Slider(
                value = balance, onValueChange = onChange, valueRange = -1f..1f, enabled = supported,
                colors = SliderDefaults.colors(
                    thumbColor = Tokens.BrandPrimary, activeTrackColor = Tokens.BrandPrimary,
                    inactiveTrackColor = Tokens.ControlOff,
                ),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            Text("Der.", color = Tokens.TextSecondary, style = MaterialTheme.typography.labelLarge)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (supported) balanceLabel(balance) else "El método «Clásico» no permite ajustar el balance.",
                color = if (supported) Tokens.TextPrimary else Tokens.TextSecondary,
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
            )
            if (supported && balance != 0f) {
                TextButton(onClick = { onChange(0f) }) { Text("Centrar", color = Tokens.BrandPrimary) }
            }
        }
    }
}

@Composable
private fun MethodSection(state: MainUiState, onSelectMethod: (EqMethod) -> Unit) {
    Section("Método de ecualización") {
        listOf(
            Triple(EqMethod.GLOBAL, "Global (recomendado)", "Ecualiza todo el sonido del teléfono, incluidos los navegadores como Brave. Es el que funciona con más apps y el único con todas las funciones."),
            Triple(EqMethod.LEGACY, "Clásico", "Para teléfonos viejos donde «Global» no funciona. Ecualiza todo el sonido, pero con menos bandas y sin balance."),
            Triple(EqMethod.PER_APP, "Por app", "Ecualiza cada app por separado. Es el método oficial de Android, pero no alcanza a los navegadores."),
        ).forEach { (m, title, desc) ->
            Row(
                Modifier.fillMaxWidth().clickable { onSelectMethod(m) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                RadioButton(selected = state.method == m, onClick = { onSelectMethod(m) })
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(title, color = Tokens.TextPrimary, style = MaterialTheme.typography.bodyLarge)
                    Text(desc, color = Tokens.TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (state.fellBack) {
            Text(
                when (state.activeMethod) {
                    EqMethod.LEGACY ->
                        "El método «Global» no funciona en este teléfono (su sistema de audio lo rechaza). " +
                            "Para que sigas escuchando con ecualizador, la app cambió sola al método «Clásico». " +
                            "Tiene menos bandas, así que el ajuste es menos fino, y no permite balance. El ecualizador no está roto: solo cambió la forma de aplicarlo."
                    else ->
                        "Los métodos «Global» y «Clásico» dejaron de funcionar en este teléfono. Lo más probable es que una actualización de Android los haya bloqueado. " +
                            "Para que sigas escuchando con ecualizador, la app cambió sola al método «Por app». " +
                            "Con este método los navegadores no se ecualizan. El ecualizador no está roto: solo cambió la forma de aplicarlo."
                },
                color = Tokens.Warning, style = MaterialTheme.typography.bodySmall,
            )
        } else if (state.activeMethod == EqMethod.PER_APP && state.sessionlessApp) {
            Text(
                "Hay una app sonando (por ejemplo un navegador) que el método «Por app» no puede ecualizar. " +
                    "Cambia al método «Global» para incluirla.",
                color = Tokens.Warning, style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            "Si al cambiar de preset no notas diferencia en alguna app, prueba otro método.",
            color = Tokens.TextTertiary, style = MaterialTheme.typography.bodySmall,
        )
    }
}
