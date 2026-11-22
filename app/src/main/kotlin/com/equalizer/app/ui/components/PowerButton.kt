package com.equalizer.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
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
        val tint = if (enabled) Tokens.OnBrand else Tokens.TextSecondary
        Canvas(Modifier.size(24.dp)) {
            val w = 2.5.dp.toPx()
            val d = size.width * 0.8f
            drawArc(
                color = tint, startAngle = -60f, sweepAngle = 300f, useCenter = false,
                topLeft = Offset(size.width * 0.1f, size.height * 0.15f), size = Size(d, d),
                style = Stroke(width = w, cap = StrokeCap.Round),
            )
            drawLine(tint, Offset(size.width / 2, w), Offset(size.width / 2, size.height * 0.5f), w, StrokeCap.Round)
        }
    }
}
