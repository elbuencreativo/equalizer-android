package com.equalizer.app.engine

import com.equalizer.app.device.ActiveOutput
import com.equalizer.app.device.OutputDetector
import kotlinx.coroutines.flow.MutableStateFlow

/** Estado compartido entre servicio y UI (app personal de un solo proceso). */
object EqRuntime {
    val engine = EqEngine(EqMethod.GLOBAL, { LegacyEffectHandle(it) }) { DynamicsEffectHandle(it) }

    /** Salida activa, reenviada por el servicio para la UI. */
    val output = MutableStateFlow(ActiveOutput.NONE)

    /** Falso si falta el permiso DUMP y no se pueden descubrir sesiones. */
    val dumpAvailable = MutableStateFlow(true)

    @Volatile var detector: OutputDetector? = null
}
