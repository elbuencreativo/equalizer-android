package com.equalizer.app.engine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.audiofx.AudioEffect
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.equalizer.app.core.baseProfile
import com.equalizer.app.core.combine as combineCurves
import com.equalizer.app.core.presetCurve
import com.equalizer.app.device.DeviceRegistry
import com.equalizer.app.device.OutputDetector
import com.equalizer.app.device.deviceDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class EqService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val receiver = SessionReceiver()
    private var discovery: SessionDiscovery? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        val settings = Settings(settingsDataStore)
        val detector = OutputDetector(this, DeviceRegistry(deviceDataStore), scope)
        EqRuntime.detector = detector
        detector.start()

        val filter = IntentFilter().apply {
            addAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
            addAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        }
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_EXPORTED)

        discovery = SessionDiscovery(this, EqRuntime.engine, scope).also { it.start() }

        scope.launch {
            combine(detector.active, settings.preset) { out, preset ->
                combineCurves(baseProfile(out.deviceType), presetCurve(preset))
            }.collect { EqRuntime.engine.setCurve(it) }
        }
        scope.launch { settings.enabled.collect { EqRuntime.engine.setEnabled(it) } }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onDestroy() {
        discovery?.stop()
        unregisterReceiver(receiver)
        EqRuntime.detector?.stop()
        EqRuntime.detector = null
        EqRuntime.engine.setEnabled(false)
        scope.cancel()
        super.onDestroy()
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("eq", "Ecualizador", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "eq")
            .setContentTitle("Equalizer activo")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else startForeground(1, n)
    }
}
