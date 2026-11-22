package com.equalizer.app.core

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

private const val MAX_BALANCE_ATTENUATION_DB = -40f

/**
 * Ganancia de entrada (izquierda, derecha) en dB. [balance] va de -1 (todo a la izquierda)
 * a +1 (todo a la derecha); el canal opuesto se atenúa, el otro queda en el nivel base.
 */
fun channelGainsDb(preampDb: Float, balance: Float): Pair<Float, Float> {
    val b = balance.coerceIn(-1f, 1f)
    val att = if (b == 0f) 0f else (20f * log10(1f - abs(b))).coerceAtLeast(MAX_BALANCE_ATTENUATION_DB)
    return Pair(preampDb + if (b > 0f) att else 0f, preampDb + if (b < 0f) att else 0f)
}

/** Pasos de 5 % para que el control no quede en valores raros. */
fun snapBalance(v: Float): Float = ((v.coerceIn(-1f, 1f) * 20f).roundToInt() / 20f)

fun balanceLabel(b: Float): String {
    val pct = (abs(b) * 100f).roundToInt()
    return when {
        pct == 0 -> "Centro"
        b > 0f -> "Derecha $pct %"
        else -> "Izquierda $pct %"
    }
}

/**
 * Niveles (en milibelios) para las bandas fijas del ecualizador clásico del teléfono, a partir de
 * nuestra curva de 10 bandas. Interpola en escala logarítmica de frecuencia y suma el preamp
 * (el ecualizador clásico no tiene preamp; así se evita saturar).
 */
fun legacyBandLevelsMb(centerFreqsMilliHz: List<Int>, bandsDb: List<Float>, preampDb: Float, minMb: Int, maxMb: Int): List<Short> {
    val xs = BAND_FREQS_HZ.map { ln(it.toDouble()) }
    return centerFreqsMilliHz.map { mhz ->
        val x = ln((mhz / 1000.0).coerceAtLeast(1.0))
        val gain = when {
            x <= xs.first() -> bandsDb.first().toDouble()
            x >= xs.last() -> bandsDb.last().toDouble()
            else -> {
                val hi = xs.indexOfFirst { it >= x }
                val lo = hi - 1
                val f = (x - xs[lo]) / (xs[hi] - xs[lo])
                bandsDb[lo] + (bandsDb[hi] - bandsDb[lo]) * f
            }
        }
        (((gain + preampDb) * 100.0).roundToInt()).coerceIn(minMb, maxMb).toShort()
    }
}
