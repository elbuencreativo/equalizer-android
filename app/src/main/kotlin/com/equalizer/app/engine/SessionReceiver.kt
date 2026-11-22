package com.equalizer.app.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect

class SessionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, AudioEffect.ERROR)
        if (id == AudioEffect.ERROR || id == 0) return
        val pkg = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME) ?: "desconocida"
        try {
            when (intent.action) {
                AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> EqRuntime.engine.onSessionOpened(id, pkg)
                AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> EqRuntime.engine.onSessionClosed(id)
            }
        } catch (e: Exception) {
            android.util.Log.w("SessionReceiver", "ignored: $e")
        }
    }
}
