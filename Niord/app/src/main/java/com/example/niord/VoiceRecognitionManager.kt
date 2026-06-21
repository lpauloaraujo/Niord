package com.example.niord

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.vosk.Model
import org.vosk.android.StorageService
import java.io.File


class VoiceRecognitionManager(
    private val context: Context
) {

    private var model: Model? = null
        get() = Companion.model

    companion object {
        @Volatile
        private var model: Model? = null
        private var isLoading = false

        private const val MODEL_FOLDER_NAME = "vosk-model-small-pt-0.3"
        private const val TARGET_DIR = "model"
    }

    fun loadModel() {
        Log.d("VOSK_DEBUG", "Loading Model")
        if(model != null) return

        if(isLoading) return
        isLoading = true


        val modelDir = File(context.getExternalFilesDir(null), "$TARGET_DIR/$MODEL_FOLDER_NAME")


        if (modelDir.exists()) {
            Log.d("VOSK_DEBUG", "Loading Model from disk at ${modelDir.absolutePath}")
            try {
                model = Model(modelDir.absolutePath)
                isLoading = false
                Log.d("VOSK_DEBUG", "Modelo carregado do disco")
            } catch (e: Exception) {
                isLoading = false
                Log.e("VOSK_DEBUG", "Erro ao carregar modelo do disco", e)
                unpackModel()
            }
        }else {
            unpackModel()
        }
    }

    private fun unpackModel(){
        Log.d("VOSK_DEBUG", "Unpacking Model")
        StorageService.unpack(
            context,
            "vosk-model-small-pt-0.3",
            "model",
            { loadedModel ->
                model = loadedModel
                isLoading = false
                Log.d("VOSK", "Modelo carregado")
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