package com.equalizer.app.engine

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService

/**
 * Solo existe para que Android deje leer las sesiones de medios (título y artista de lo que suena).
 * El usuario debe darle "acceso a notificaciones"; la app no lee ninguna notificación.
 */
class NowPlayingListener : NotificationListenerService() {
    private var manager: MediaSessionManager? = null
    private var controllers: List<MediaController> = emptyList()

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
    }
    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { setControllers(it.orEmpty()) }

    override fun onListenerConnected() {
        val m = getSystemService(MediaSessionManager::class.java)
        manager = m
        val me = ComponentName(this, NowPlayingListener::class.java)
        try {
            m.addOnActiveSessionsChangedListener(sessionsListener, me)
            setControllers(m.getActiveSessions(me))
        } catch (e: SecurityException) {
            android.util.Log.w("NowPlaying", "sin acceso a sesiones de medios: $e")
        }
    }

    override fun onListenerDisconnected() {
        controllers.forEach { it.unregisterCallback(controllerCallback) }
        controllers = emptyList()
        manager?.removeOnActiveSessionsChangedListener(sessionsListener)
        EqRuntime.tracks.value = emptyMap()
    }

    private fun setControllers(list: List<MediaController>) {
        controllers.forEach { it.unregisterCallback(controllerCallback) }
        controllers = list
        controllers.forEach { it.registerCallback(controllerCallback) }
        publish()
    }

    private fun publish() {
        EqRuntime.tracks.value = controllers
            .filter { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            .associate {
                val md = it.metadata
                it.packageName to Track(md?.getString(MediaMetadata.METADATA_KEY_TITLE), md?.getString(MediaMetadata.METADATA_KEY_ARTIST))
            }
    }
}
