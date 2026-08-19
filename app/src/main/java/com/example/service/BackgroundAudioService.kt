package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.core.audio.AudioPipeline
import com.example.core.audio.KovaAcousticWakeWordDetector
import com.example.core.audio.WakeWordDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class BackgroundAudioService : Service() {

    companion object {
        private const val TAG = "KovaBgService"
        const val CHANNEL_ID = "kova_voice_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.service.START_LISTENING"
        const val ACTION_STOP = "com.example.service.STOP_LISTENING"

        private val _wakeWordTriggerFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val wakeWordTriggerFlow: SharedFlow<Unit> = _wakeWordTriggerFlow.asSharedFlow()

        fun startService(context: Context) {
            val intent = Intent(context, BackgroundAudioService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, BackgroundAudioService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var wakeWordDetector: WakeWordDetector
    private var audioPipeline: AudioPipeline? = null

    inner class LocalBinder : Binder() {
        fun getService(): BackgroundAudioService = this@BackgroundAudioService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "BackgroundAudioService onCreate")
        createNotificationChannel()

        wakeWordDetector = KovaAcousticWakeWordDetector()
        audioPipeline = AudioPipeline(
            wakeWordDetector = wakeWordDetector,
            onWakeWordDetected = {
                Log.i(TAG, "Wake word triggered inside Background Service")
                serviceScope.launch {
                    _wakeWordTriggerFlow.emit(Unit)
                }
            },
            onBargeInDetected = {
                Log.i(TAG, "Barge in detected in background service")
            },
            onPcmChunkCaptured = { /* no-op in background standby until woken */ }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildForegroundNotification("Kova is on standby. Say \"Kova\" to activate.")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Start low-power wake word listening
        audioPipeline?.startRecording(serviceScope)

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Kova Voice Assistant Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Kova listening for the local 'Kova' wake word"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(statusText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Kova AI Assistant")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_kova_logo)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "BackgroundAudioService onDestroy")
        audioPipeline?.stopRecording()
        audioPipeline?.release()
        audioPipeline = null
        serviceScope.cancel()
    }
}
