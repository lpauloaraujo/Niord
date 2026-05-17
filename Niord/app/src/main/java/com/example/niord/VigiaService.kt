package com.example.niord

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

// Foreground service that keeps Niord Vigia monitoring alive while the app is in background.
// The actual audio capture + NLP pipeline lives in startMonitoring()/stopMonitoring(); for
// now we only hold the foreground microphone slot and notify the user that vigia is active.
class VigiaService : android.app.Service() {

    companion object {
        const val CHANNEL_ID = "niord_vigia_channel"
        const val NOTIFICATION_ID = 4205
        const val ACTION_START = "com.example.niord.action.VIGIA_START"
        const val ACTION_STOP = "com.example.niord.action.VIGIA_STOP"

        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, VigiaService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, VigiaService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopMonitoring()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                isRunning = false
                return START_NOT_STICKY
            }
            else -> {
                ensureChannel()
                val notification = buildNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                isRunning = true
                startMonitoring()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopMonitoring()
        isRunning = false
        super.onDestroy()
    }

    // TODO(US-005 follow-up): capture audio chunks with MediaRecorder/AudioRecord and stream
    // them to the backend NLP pipeline for threat detection. Keeping a no-op stub for now so
    // the activation flow can be exercised end-to-end.
    private fun startMonitoring() {}

    private fun stopMonitoring() {}

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Niord Vigia",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Monitoramento de áudio do Niord Vigia"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.plt_vigia)
            .setContentTitle("Niord Vigia ativo")
            .setContentText("Monitorando áudio em segundo plano")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
