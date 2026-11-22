package com.equalizer.app.update

private fun parse(v: String): List<Int>? {
    val parts = v.trim().removePrefix("v").removePrefix("V").split(".")
    if (parts.isEmpty() || parts.size > 4) return null
    return parts.map { it.toIntOrNull() ?: return null }
}

/** Compara versiones `x.y.z`; cualquier texto inválido cuenta como "no más nueva". */
fun isNewer(current: String, latest: String): Boolean {
    val a = parse(current) ?: return false
    val b = parse(latest) ?: return false
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (y != x) return y > x
    }
    return false
}
