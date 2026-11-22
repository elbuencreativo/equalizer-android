package com.equalizer.app.engine

/** Qué está sonando en una app, según su sesión de medios (requiere acceso a notificaciones). */
data class Track(val title: String?, val artist: String?)
