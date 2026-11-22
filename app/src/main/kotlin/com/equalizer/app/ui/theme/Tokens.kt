package com.equalizer.app.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

object Tokens {
    val BgScreen = Color(0xFF0E0E0E)
    val BgSurface = Color(0xFF2D2D2D)
    val BgSelected = Color(0xFF0B3A50)
    val BrandPrimary = Color(0xFF00B2FF)
    val OnBrand = Color(0xFF001820)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB8B8B8)
    val TextTertiary = Color(0xFF9E9E9E)
    val Border = Color(0xFF464646)
    val ControlOff = Color(0xFF5A5A5A)
    val Amber = Color(0xFFF5B400)
    val Warning = Color(0xFFFFB74D)
}

/** Espaciado en dp sobre base de 4. */
object Sp {
    const val S2 = 2; const val S4 = 4; const val S8 = 8; const val S12 = 12; const val S16 = 16
    const val S20 = 20; const val S24 = 24; const val S32 = 32; const val S40 = 40
}

private fun channel(c: Float): Double {
    val v = c.toDouble()
    return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
}

private fun luminance(c: Color): Double =
    0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)

/** Relación de contraste WCAG entre dos colores. */
fun contrastRatio(fg: Color, bg: Color): Double {
    val a = luminance(fg); val b = luminance(bg)
    return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
}
