package com.equalizer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.equalizer.app.core.DeviceType
import com.equalizer.app.device.ActiveOutput
import com.equalizer.app.device.OutputKind
import com.equalizer.app.device.TypeSource
import com.equalizer.app.ui.theme.Tokens

fun kindLabel(k: OutputKind) = when (k) {
    OutputKind.BUILT_IN_SPEAKER -> "Parlante integrado"
    OutputKind.WIRED -> "Cable"
    OutputKind.BLUETOOTH -> "Bluetooth"
    OutputKind.UNKNOWN -> "Sin salida"
}

fun sourceLabel(s: TypeSource) = when (s) {
    TypeSource.ASSIGNED -> "Asignado por ti"
    TypeSource.DETECTED -> "Detectado por la salida"
    TypeSource.UNASSIGNED -> "Sin asignar — elige el tipo"
}

@Composable
fun DeviceCard(output: ActiveOutput, onAssignType: (DeviceType) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    var menu by remember { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxWidth()
            .background(Tokens.BgSurface, shape)
            .border(1.dp, Tokens.Border, shape)
            .padding(16.dp),
    ) {
        Text(output.displayName, style = MaterialTheme.typography.titleMedium, color = Tokens.TextPrimary)
        Text(kindLabel(output.kind), style = MaterialTheme.typography.bodySmall, color = Tokens.TextSecondary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tipo: ${output.deviceType?.displayName ?: "sin asignar"}",
            style = MaterialTheme.typography.bodyMedium,
            color = Tokens.TextPrimary,
        )
        Text(
            sourceLabel(output.typeSource),
            style = MaterialTheme.typography.bodySmall,
            color = if (output.typeSource == TypeSource.UNASSIGNED) Tokens.Warning else Tokens.TextSecondary,
        )
        TextButton(onClick = { menu = true }) { Text("Cambiar tipo", color = Tokens.BrandPrimary) }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DeviceType.values().forEach { t ->
                DropdownMenuItem(text = { Text(t.displayName) }, onClick = { menu = false; onAssignType(t) })
            }
        }
    }
}
