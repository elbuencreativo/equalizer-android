package com.equalizer.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.equalizer.app.ui.theme.Tokens

/** Botón de encendido con forma de vinilo: gira despacio mientras el ecualizador está encendido. */
@Composable
fun PowerButton(enabled: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier, size: Dp = 120.dp) {
    // Se mueve con el reloj de cuadros y no con las animaciones del sistema: si el usuario las
    // desactivó en Ajustes, el vinilo igual gira (es parte del indicador de encendido).
    var rotation by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(enabled) {
        if (enabled) {
            var last = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                rotation = (rotation + (now - last) / 1_000_000_000f * 90f) % 360f // 4 s por vuelta
                last = now
            }
        }
    }
    Canvas(
        modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onToggle)
            .semantics {
                role = Role.Switch
                contentDescription = "Encender o apagar"
                stateDescription = if (enabled) "Encendido" else "Apagado"
            },
    ) {
        val r = this.size.minDimension / 2f
        val c = center
        val line = 1.dp.toPx()
        // Disco: gira.
        rotate(if (enabled) rotation else 0f, c) {
            drawCircle(if (enabled) Color(0xFF161616) else Tokens.BgSurface, r, c)
            drawCircle(if (enabled) Tokens.BrandPrimary else Tokens.Border, r - line / 2, c, style = Stroke(line * 1.5f))
            listOf(0.92f, 0.82f, 0.72f, 0.62f).forEach {
                drawCircle(if (enabled) Color(0xFF2C2C2C) else Tokens.Border.copy(alpha = 0.5f), r * it, c, style = Stroke(line))
            }
            // Brillos: hacen visible el giro.
            listOf(20f, 200f).forEach { start ->
                drawArc(
                    Color.White.copy(alpha = if (enabled) 0.16f else 0.05f), start, 40f, false,
                    topLeft = Offset(c.x - r * 0.77f, c.y - r * 0.77f), size = Size(r * 1.54f, r * 1.54f),
                    style = Stroke(r * 0.2f),
                )
            }
            drawCircle(if (enabled) Tokens.BrandPrimary else Tokens.ControlOff, r * 0.38f, c)
            drawCircle(Tokens.OnBrand.copy(alpha = 0.7f), r * 0.035f, Offset(c.x + r * 0.3f, c.y))
        }
        // Símbolo de encendido: queda quieto.
        val tint = if (enabled) Tokens.OnBrand else Tokens.TextPrimary
        val w = r * 0.045f
        val d = r * 0.34f
        drawArc(
            tint, -60f, 300f, false, topLeft = Offset(c.x - d / 2, c.y - d / 2 + r * 0.02f), size = Size(d, d),
            style = Stroke(w, cap = StrokeCap.Round),
        )
        drawLine(tint, Offset(c.x, c.y - d / 2 - r * 0.03f), Offset(c.x, c.y - r * 0.02f), w, StrokeCap.Round)
    }
}
