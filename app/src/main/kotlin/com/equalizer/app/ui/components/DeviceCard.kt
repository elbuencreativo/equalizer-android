package com.equalizer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
    OutputKind.EXTERNAL -> "USB / externo"
    OutputKind.UNKNOWN -> "Sin salida"
}

fun sourceLabel(s: TypeSource) = when (s) {
    TypeSource.ASSIGNED -> "Asignado por ti"
    TypeSource.DETECTED -> "Detectado por la salida"
    TypeSource.UNASSIGNED -> "Sin asignar — elige el tipo"
}

/** Texto que deja claro si la elección de tipo queda guardada. */
fun savedHint(o: ActiveOutput): String = when (o.typeSource) {
    TypeSource.ASSIGNED -> "✓ Guardado: la app recordará este tipo para este dispositivo."
    TypeSource.DETECTED -> "Detectado automáticamente por la salida de audio. Si no es correcto, cámbialo y quedará guardado."
    TypeSource.UNASSIGNED -> "Elige el tipo y la app lo recordará para este dispositivo."
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(Tokens.BgSelected, CircleShape), contentAlignment = Alignment.Center) {
                Icon(deviceIcon(output.deviceType), contentDescription = null, tint = Tokens.BrandPrimary)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(output.displayName, style = MaterialTheme.typography.titleMedium, color = Tokens.TextPrimary)
                Text(
                    // Si el nombre ya dice el tipo, mostrar solo la salida evita repetirlo.
                    if (output.displayName == output.deviceType?.displayName) kindLabel(output.kind)
                    else "${kindLabel(output.kind)} · ${output.deviceType?.displayName ?: "sin tipo"}",
                    style = MaterialTheme.typography.bodySmall, color = Tokens.TextSecondary,
                )
            }
            Box {
                TextButton(onClick = { menu = true }) {
                    Text(if (output.deviceType == null) "Elegir tipo" else "Cambiar", color = Tokens.BrandPrimary)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DeviceType.values().forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t.displayName) },
                            leadingIcon = { Icon(deviceIcon(t), contentDescription = null) },
                            onClick = { menu = false; onAssignType(t) },
                        )
                    }
                }
            }
        }
        Text(
            savedHint(output),
            style = MaterialTheme.typography.bodySmall,
            color = if (output.typeSource == TypeSource.UNASSIGNED) Tokens.Warning else Tokens.TextSecondary,
        )
    }
}
