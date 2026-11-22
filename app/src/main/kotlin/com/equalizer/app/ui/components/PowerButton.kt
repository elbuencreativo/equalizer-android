package com.equalizer.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.equalizer.app.ui.theme.Tokens

@Composable
fun PowerButton(enabled: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(if (enabled) Tokens.BrandPrimary else Tokens.BgSurface)
            .border(BorderStroke(1.dp, if (enabled) Tokens.BrandPrimary else Tokens.Border), CircleShape)
            .clickable(onClick = onToggle)
            .semantics {
                contentDescription = "Encender o apagar"
                stateDescription = if (enabled) "Encendido" else "Apagado"
            },
        contentAlignment = Alignment.Center,
    ) {
        Text("⏻", fontSize = 24.sp, color = if (enabled) Tokens.OnBrand else Tokens.TextSecondary)
    }
}
