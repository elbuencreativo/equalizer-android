package com.equalizer.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.equalizer.app.core.MAX_GAIN_DB
import com.equalizer.app.ui.theme.Tokens
import kotlin.math.roundToInt

fun formatFreq(hz: Int): String = if (hz >= 1000) "${hz / 1000}k" else "$hz"

/** Convierte la posición vertical (0 = arriba) en dB, con paso de 0,5 dB y límite ±[MAX_GAIN_DB]. */
fun yToGainDb(y: Float, heightPx: Float): Float {
    if (heightPx <= 0f) return 0f
    val raw = MAX_GAIN_DB - (y / heightPx) * 2f * MAX_GAIN_DB
    return ((raw * 2f).roundToInt() / 2f).coerceIn(-MAX_GAIN_DB, MAX_GAIN_DB)
}

private fun formatGain(gainDb: Float): String {
    val r = gainDb.roundToInt()
    return if (r > 0) "+$r" else "$r"
}

/**
 * Banda vertical con el cero en el centro: lo que sube se rellena en cian hacia arriba,
 * lo que baja en ámbar hacia abajo.
 */
@Composable
fun BandSlider(freqHz: Int, gainDb: Float, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val latestOnChange by rememberUpdatedState(onChange)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            formatGain(gainDb),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (gainDb == 0f) Tokens.TextSecondary else Tokens.TextPrimary,
        )
        Box(
            Modifier
                .width(32.dp)
                .height(180.dp)
                .semantics {
                    contentDescription = "Banda ${formatFreq(freqHz)} Hz"
                    stateDescription = "${formatGain(gainDb)} dB"
                    progressBarRangeInfo = ProgressBarRangeInfo(gainDb, -MAX_GAIN_DB..MAX_GAIN_DB)
                    setProgress { target ->
                        latestOnChange(target.coerceIn(-MAX_GAIN_DB, MAX_GAIN_DB)); true
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { latestOnChange(yToGainDb(it.y, size.height.toFloat())) }
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, _ ->
                        change.consume()
                        latestOnChange(yToGainDb(change.position.y, size.height.toFloat()))
                    }
                },
        ) {
            Canvas(Modifier.width(32.dp).height(180.dp)) {
                val trackW = 8.dp.toPx()
                val thumbR = 10.dp.toPx()
                val top = thumbR
                val bottom = size.height - thumbR
                val mid = (top + bottom) / 2f
                val x = size.width / 2f
                val frac = (MAX_GAIN_DB - gainDb) / (2f * MAX_GAIN_DB)
                val thumbY = top + (bottom - top) * frac

                drawRoundRect(
                    Tokens.ControlOff.copy(alpha = 0.5f),
                    topLeft = Offset(x - trackW / 2, top), size = Size(trackW, bottom - top),
                    cornerRadius = CornerRadius(trackW / 2),
                )
                if (gainDb != 0f) {
                    val up = gainDb > 0f
                    val y0 = if (up) thumbY else mid
                    val y1 = if (up) mid else thumbY
                    val brush = if (up) {
                        Brush.verticalGradient(listOf(Tokens.Amber, Tokens.BrandPrimary), startY = y0, endY = y1)
                    } else {
                        Brush.verticalGradient(listOf(Tokens.BrandPrimary, Tokens.Amber), startY = y0, endY = y1)
                    }
                    drawRoundRect(
                        brush,
                        topLeft = Offset(x - trackW / 2, y0), size = Size(trackW, y1 - y0),
                        cornerRadius = CornerRadius(trackW / 2),
                    )
                }
                drawLine(
                    Tokens.TextTertiary, Offset(x - 12.dp.toPx(), mid), Offset(x + 12.dp.toPx(), mid), strokeWidth = 1.dp.toPx(),
                )
                drawCircle(Color.White, radius = thumbR, center = Offset(x, thumbY))
                drawCircle(Tokens.BrandPrimary, radius = thumbR - 3.dp.toPx(), center = Offset(x, thumbY))
            }
        }
        Text(formatFreq(freqHz), style = MaterialTheme.typography.labelMedium, color = Tokens.TextSecondary)
    }
}
