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
import androidx.compose.ui.util.fastJoinToString
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

    private var resultsQueue = ArrayDeque<String>(20)
    private var lastPartialResult = ""

    private val dangerWords: Set<String> =
        setOf(
            "assalto", "perdeu", "reage", "armado",
            "peça", "calado", "cala",
            "quieto", "grita", "morrer",
            "atirar",

            "passa", "desce", "sai",
            "entra", "destrava", "abre",

            "carro", "moto", "chave",
            "celular", "carteira",

            "trás", "costas", "frente", "chão"
        )

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
                    //onResultReady(matches[0]) // The first element has the highest confidence
                    Log.d("STT_RESULT", matches[0])
                    lastPartialResult = ""
                }
                //restartListening()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val result = matches[0]
                    val newWords = extractNewWords(splitResult(result))
                    updateLastWord(splitResult(result))
                    if (newWords.isNotEmpty()) {
                        addWordsToQueue(newWords)
                        lastPartialResult = result
                        if(hasDangerWord(resultsQueue)){
                            onResultReady(resultsQueue.fastJoinToString(" "))
                        }
                    }
                    Log.d("STT_PARTIAL", result)
                    Log.d("STT_PARTIAL", resultsQueue.toString())
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

    private fun getPrevious(): List<String>{
        return splitResult(lastPartialResult)
    }

    private fun splitResult(fullPhrase: String): List<String>{
        return fullPhrase.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
    }
    private fun extractNewWords(current: List<String>): List<String> {
        val previous = getPrevious()
        return if (current.size > previous.size) current.drop(previous.size) else emptyList()
    }

    private fun updateLastWord(current: List<String>){
        //If the size is the same update the last word, ensuring full word result
        //partial 1: good mor
        //partial 2: good mornin
        //partial 3: good morning
        val previous = getPrevious()
        if(previous.size == current.size){
            resultsQueue.removeLast()
            resultsQueue.add(current.last())
        }
    }

    private fun addWordsToQueue(words: List<String>) {
        for (word in words) {
            if (resultsQueue.size >= 20) resultsQueue.removeFirst()
            resultsQueue.addLast(word)
        }
    }

    private fun hasDangerWord(queue: ArrayDeque<String>): Boolean{
        return queue.takeLast(10).any {it.lowercase() in dangerWords}
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