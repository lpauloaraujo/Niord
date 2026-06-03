package com.example.niord
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class GoogleOfflineSTTManager(
    private val context: Context,
    private val onResultReady: (String) -> Unit,
    private val onErrorOccurred: (String, code: Int) -> Unit
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isRecording = false
    private lateinit var recognizerIntent: Intent

    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        initializeRecognizer()
    }

    private fun initializeRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

            recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-BR")

                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)

                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            setupCallbacks()
        } else {
            onErrorOccurred("Speech Recognition not available on this device.", 0)
        }
    }

    private fun setupCallbacks() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                //End of speech encompasses errors already
                Log.d("STT_GOOGLE", "End of speech")
                restartListening()
            }

            override fun onError(error: Int) {
                if(error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    Log.d("STT_GOOGLE", "Restarting on error")
                    //restartListening()
                }else {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_NETWORK -> "Network error (Check offline pack installation)"
                        else -> "Unknown recognition error: $error"
                    }
                    onErrorOccurred(errorMessage, error)
                    if(error == SpeechRecognizer.ERROR_CLIENT){
                        restartListening()
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onResultReady(matches[0]) // The first element has the highest confidence
                }
                //restartListening()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    //onResultReady(matches[0])
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun restartListening(){
        if (isRecording) {
            //speechRecognizer?.cancel()

            mainHandler.removeCallbacksAndMessages(null)

            mainHandler.postDelayed({
                if (isRecording) {
                    try {
                        speechRecognizer?.startListening(recognizerIntent)
                    } catch (e: Exception) {
                        Log.e("STT_LOOP", "Failed to restart: ${e.message}")
                    }
                }
            }, 100)
        }
    }

    fun startListening() {
        isRecording = true
        speechRecognizer?.startListening(recognizerIntent)
    }

    fun stopListening() {
        isRecording = false
        speechRecognizer?.stopListening()
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    fun isRecognizerNull(): Boolean{
        return speechRecognizer == null
    }
}