package com.equalizer.app.ui

import com.equalizer.app.engine.Track

data class NowPlayingItem(val appName: String, val title: String?, val artist: String?)

/**
 * Une las apps que suenan (por paquete) con la información de pista, si la hay.
 * Una pista sin app detectada también se muestra, por si falta el permiso DUMP.
 */
fun mergeNowPlaying(playing: List<String>, tracks: Map<String, Track>, labelOf: (String) -> String): List<NowPlayingItem> =
    (playing + tracks.keys).distinct().map { pkg ->
        val t = tracks[pkg]
        NowPlayingItem(labelOf(pkg), t?.title?.takeIf { it.isNotBlank() }, t?.artist?.takeIf { it.isNotBlank() })
    }
