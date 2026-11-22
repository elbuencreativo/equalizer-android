package com.equalizer.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.equalizer.app.ui.theme.Tokens
import com.equalizer.app.update.UpdateState

@Composable
fun UpdateSection(
    currentVersion: String,
    state: UpdateState,
    onCheck: () -> Unit,
    onInstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Versión $currentVersion", style = MaterialTheme.typography.bodySmall, color = Tokens.TextSecondary)
        when (state) {
            UpdateState.Idle, UpdateState.UpToDate -> {
                if (state == UpdateState.UpToDate) Text("Estás al día.", color = Tokens.TextSecondary)
                TextButton(onClick = onCheck) { Text("Buscar actualización", color = Tokens.BrandPrimary) }
            }
            UpdateState.Checking -> Text("Buscando…", color = Tokens.TextSecondary)
            is UpdateState.Available -> {
                Text("Hay una versión nueva: ${state.release.version}", color = Tokens.TextPrimary)
                TextButton(onClick = onInstall) { Text("Descargar e instalar", color = Tokens.BrandPrimary) }
            }
            UpdateState.Working -> Text("Descargando…", color = Tokens.TextSecondary)
            is UpdateState.Failed -> {
                Text(state.reason, color = Tokens.Warning)
                TextButton(onClick = onCheck) { Text("Reintentar", color = Tokens.BrandPrimary) }
            }
        }
    }
}
