package com.example.niord

import android.content.Context
import android.util.Log
import org.vosk.Model
import org.vosk.android.StorageService

class VoiceRecognitionManager(
    private val context: Context
) {

    private var model: Model? = null

    fun loadModel() {

        StorageService.unpack(
            context,
            "vosk-model-small-pt-0.3",
            "model",
            { loadedModel ->
                model = loadedModel
                Log.d("VOSK", "Modelo carregado")
            },
            { exception ->
                Log.e("VOSK", "Erro ao carregar modelo", exception)
            }
        )
    }
}