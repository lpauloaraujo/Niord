package com.example.niord

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.io.IOException
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.File


class VoiceRecognitionManager(
    private val context: Context
) {
    private var speechService: SpeechService? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        @Volatile
        private var model: Model? = null
        private var isLoading = false

        private const val MODEL_FOLDER_NAME = "vosk-model-small-pt-0.3"
        private const val TARGET_DIR = "model"
    }

    fun startListening(listener: RecognitionListener) {
        val m = model ?: run {
            Log.w("VOSK_DEBUG", "Modelo ainda não carregado")
            return
        }
        try {
            val rec = Recognizer(m, 16000.0f)
            speechService = SpeechService(rec, 16000.0f)
            speechService!!.startListening(listener)
        } catch (e: IOException) {
            Log.e("VOSK_DEBUG", "Erro ao iniciar reconhecimento", e)
        }
    }

    fun stopListening(){
        speechService?.stop()
        speechService = null
    }
    fun loadModel(onReady: (() -> Unit)?) {
        Log.d("VOSK_DEBUG", "Loading Model")
        if(model != null){
            onReady?.invoke()
            return
        }

        if(isLoading) return
        isLoading = true

        val modelDir = File(context.applicationContext
            .getExternalFilesDir(null), "$TARGET_DIR/$MODEL_FOLDER_NAME")

        if (modelDir.exists()) {
            Log.d("VOSK_DEBUG", "Loading Model from disk at ${modelDir.absolutePath}")
            Thread {
                try {
                    model = Model(modelDir.absolutePath)
                    isLoading = false
                    Log.d("VOSK_DEBUG", "Modelo carregado do disco")
                    mainHandler.post{ onReady?.invoke() }
                } catch (e: Exception) {
                    isLoading = false
                    Log.e("VOSK_DEBUG", "Erro ao carregar modelo do disco", e)
                    mainHandler.post{ unpackModel(onReady) }
                }
            }.start()
        }else {
            unpackModel(onReady)
        }
    }

    private fun unpackModel(onReady: (() -> Unit)?){
        Log.d("VOSK_DEBUG", "Unpacking Model")
        StorageService.unpack(
            context.applicationContext,
            "vosk-model-small-pt-0.3",
            "model",
            { loadedModel ->
                model = loadedModel
                isLoading = false
                Log.d("VOSK", "Modelo carregado")
                onReady?.invoke()
            },
            { exception ->
                isLoading = false
                Log.e("VOSK", "Erro ao carregar modelo", exception)
            }
        )
    }

    fun releaseModel(){
        model?.close()
        model = null
        isLoading = false
    }
}