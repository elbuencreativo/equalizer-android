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
import com.equalizer.app.core.effective
import com.equalizer.app.device.DeviceRegistry
import com.equalizer.app.device.OutputDetector
import com.equalizer.app.device.deviceDataStore
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class EqService : Service() {
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main +
            CoroutineExceptionHandler { _, e -> android.util.Log.e("EqService", "coroutine failed", e) },
    )
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

        discovery = SessionDiscovery(this, EqRuntime.engine, scope).also { d ->
            d.start()
            scope.launch { d.dumpAvailable.collect { EqRuntime.dumpAvailable.value = it } }
            scope.launch { d.playing.collect { EqRuntime.playing.value = it } }
        }

        scope.launch {
            combine(detector.active, settings.preset, settings.offsets) { out, preset, offsets ->
                effective(out.deviceType, preset, offsets)
            }.collect { EqRuntime.engine.setCurve(it) }
        }
        scope.launch { detector.active.collect { EqRuntime.output.value = it } }
        scope.launch { settings.method.collect { EqRuntime.engine.setMethod(it) } }
        scope.launch { settings.enabled.collect { EqRuntime.engine.setEnabled(it) } }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onDestroy() {
        discovery?.stop()
        unregisterReceiver(receiver)
        EqRuntime.detector?.stop()
        EqRuntime.detector = null
        EqRuntime.engine.setEnabled(false)
        EqRuntime.engine.clearSessions()
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
