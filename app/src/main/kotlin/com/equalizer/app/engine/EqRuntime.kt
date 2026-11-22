package com.equalizer.app.engine

import com.equalizer.app.device.OutputDetector

/** Estado compartido entre servicio y UI (app personal de un solo proceso). */
object EqRuntime {
    val engine = EqEngine { DynamicsEffectHandle(it) }

    @Volatile var detector: OutputDetector? = null
}
