package com.equalizer.app.engine

data class ReconcilePlan(val open: Set<Int>, val close: Set<Int>)

/**
 * Decide qué sesiones abrir o cerrar comparando las que conoce el motor con lo que hay en `dumpsys`.
 * Abre las que están sonando y no se conocen. Cierra solo las que desaparecen del volcado
 * (no al pausar) y únicamente tras [graceMisses] ausencias seguidas, para tolerar carreras.
 */
class SessionReconciler(private val graceMisses: Int = 2) {
    private val misses = HashMap<Int, Int>()

    fun reconcile(known: Set<Int>, started: Set<Int>, present: Set<Int>): ReconcilePlan {
        misses.keys.retainAll(known)
        val close = HashSet<Int>()
        for (id in known) {
            if (id in present) {
                misses.remove(id)
            } else {
                val n = (misses[id] ?: 0) + 1
                if (n >= graceMisses) { close.add(id); misses.remove(id) } else misses[id] = n
            }
        }
        return ReconcilePlan(open = started - known, close = close)
    }
}
