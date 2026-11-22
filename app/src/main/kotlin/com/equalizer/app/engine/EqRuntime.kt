package com.equalizer.app.engine

import com.equalizer.app.device.ActiveOutput
import com.equalizer.app.device.OutputDetector
import kotlinx.coroutines.flow.MutableStateFlow

/** Estado compartido entre servicio y UI (app personal de un solo proceso). */
object EqRuntime {
    val engine = EqEngine(EqMethod.GLOBAL) { DynamicsEffectHandle(it) }

    /** Salida activa, reenviada por el servicio para la UI. */
    val output = MutableStateFlow(ActiveOutput.NONE)

    /** Falso si falta el permiso DUMP y no se pueden descubrir sesiones. */
    val dumpAvailable = MutableStateFlow(true)

    /** Paquetes de las apps que suenan ahora (de `dumpsys audio`). */
    val playing = MutableStateFlow<List<String>>(emptyList())

    /** Pista que suena por paquete; vacío si el usuario no dio acceso a notificaciones. */
    val tracks = MutableStateFlow<Map<String, Track>>(emptyMap())

    @Volatile var detector: OutputDetector? = null
}
