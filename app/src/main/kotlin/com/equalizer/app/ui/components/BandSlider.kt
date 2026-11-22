package com.equalizer.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.equalizer.app.core.MAX_GAIN_DB
import com.equalizer.app.ui.theme.Tokens
import kotlin.math.roundToInt

fun formatFreq(hz: Int): String = if (hz >= 1000) "${hz / 1000}k" else "$hz"

@Composable
fun BandSlider(freqHz: Int, gainDb: Float, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "${if (gainDb > 0) "+" else ""}${gainDb.roundToInt()}",
            style = MaterialTheme.typography.labelSmall,
            color = Tokens.TextSecondary,
        )
        Box(Modifier.width(32.dp).height(160.dp), contentAlignment = Alignment.Center) {
            Slider(
                value = gainDb,
                onValueChange = onChange,
                valueRange = -MAX_GAIN_DB..MAX_GAIN_DB,
                colors = SliderDefaults.colors(
                    thumbColor = Tokens.BrandPrimary,
                    activeTrackColor = Tokens.BrandPrimary,
                    inactiveTrackColor = Tokens.ControlOff,
                ),
                modifier = Modifier
                    .semantics { contentDescription = "Banda ${formatFreq(freqHz)} Hz" }
                    .graphicsLayer { rotationZ = 270f; transformOrigin = TransformOrigin(0f, 0f) }
                    .layout { measurable, constraints ->
                        val p = measurable.measure(
                            Constraints(
                                minWidth = constraints.minHeight, maxWidth = constraints.maxHeight,
                                minHeight = constraints.minWidth, maxHeight = constraints.maxHeight,
                            ),
                        )
                        layout(p.height, p.width) { p.place(-p.width, 0) }
                    }
                    .width(160.dp),
            )
        }
        Text(formatFreq(freqHz), style = MaterialTheme.typography.labelSmall, color = Tokens.TextSecondary)
    }
}
