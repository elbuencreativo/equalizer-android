package com.equalizer.app.engine

data class PlaybackSession(val uid: Int, val sessionId: Int)

private val LINE = Regex("""u/pid:(\d+)/\d+ state:(\w+) attr:AudioAttributes: usage=(\w+).*?sessionId:(-?\d+)""")
private val MEDIA_USAGES = setOf("USAGE_MEDIA", "USAGE_GAME", "USAGE_UNKNOWN")
private const val FIRST_APP_UID = 10000

/** Sesiones de reproducción activas de apps de usuario, leídas de la salida de `dumpsys audio`. */
fun activeMediaSessions(dump: String, ownUid: Int): List<PlaybackSession> =
    dump.lineSequence()
        .mapNotNull { LINE.find(it) }
        .map { Triple(it.groupValues, it.groupValues[2], it.groupValues[3]) }
        .filter { (g, state, usage) ->
            state == "started" && usage in MEDIA_USAGES &&
                g[1].toInt() >= FIRST_APP_UID && g[1].toInt() != ownUid
        }
        .map { (g, _, _) -> PlaybackSession(g[1].toInt(), g[4].toInt().coerceAtLeast(0)) }
        .distinct()
        .toList()

private val ANY_SESSION = Regex("""u/pid:(\d+)/\d+ .*?sessionId:(-?\d+)""")

/** Ids de sesión presentes en el volcado en cualquier estado (sonando, en pausa o inactivas). */
fun presentSessionIds(dump: String, ownUid: Int): Set<Int> =
    dump.lineSequence()
        .mapNotNull { ANY_SESSION.find(it) }
        .filter { it.groupValues[1].toInt() >= FIRST_APP_UID && it.groupValues[1].toInt() != ownUid }
        .map { it.groupValues[2].toInt().coerceAtLeast(0) }
        .toSet()
