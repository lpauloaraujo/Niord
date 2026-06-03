package com.example.niord

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import org.vosk.android.RecognitionListener
import java.lang.Exception

class VigiaService : android.app.Service(), RecognitionListener {

    private lateinit var voiceRecognition: VoiceRecognitionManager
    private val mainHandler = Handler(Looper.getMainLooper())

    val themedContext = ContextThemeWrapper(this, R.style.Theme_Niord)
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


    override fun onCreate() {
        voiceRecognition = VoiceRecognitionManager(this)
    }


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

    private fun startMonitoring() {
        voiceRecognition.loadModel {
            mainHandler.post {
                voiceRecognition.startListening(this)
            }
        }
    }

    private fun stopMonitoring() {
        voiceRecognition.stopListening()
    }

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

    override fun onPartialResult(hypothesis: String?) {
        val text = JSONObject(hypothesis ?: "{}").optString("text")
        if(text.isNotEmpty()) {
            Log.d("VOSK_DEBUG", "Parcial: $hypothesis")
        }
    }

    override fun onResult(hypothesis: String?) {
        Log.d("VOSK_DEBUG", "Resultado: $hypothesis")

        val text = JSONObject(hypothesis ?: "{}").optString("text")
        if(text.isNotEmpty()) {
            val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(
                themedContext,
                R.style.CustomAlertDialog
            )
                .setTitle("Fala detectada")
                .setMessage("Você falou: $text.")
                .setPositiveButton("Ok") { _, _ -> }
                .create()
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            dialog.show()
        }
    }

    override fun onFinalResult(hypothesis: String?) {
        Log.d("VOSK_DEBUG", "Final: $hypothesis")
    }

    override fun onError(exception: Exception?) {
        Log.e("VOSK_DEBUG", "Erro: ${exception?.message}")
    }

    override fun onTimeout() {
        Log.d("VOSK_DEBUG", "Timeout")
    }
}
