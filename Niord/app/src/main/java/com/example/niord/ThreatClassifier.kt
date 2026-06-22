package com.example.niord
import android.content.Context
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textclassifier.TextClassifier
import com.google.mediapipe.tasks.text.textclassifier.TextClassifier.TextClassifierOptions

class ThreatClassifier(context: Context) {

    private var textClassifier: TextClassifier? = null

    init {
        try {

            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("distilbert/distilbert_quant_meta.tflite")
                .build()

            val options = TextClassifierOptions.builder()
                .setBaseOptions(baseOptions)
                .build()

            textClassifier = TextClassifier.createFromOptions(context, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isActiveThreat(text: String, confidenceThreshold: Float = 0.85f): Boolean {
        val classifierResult = textClassifier?.classify(text) ?: return false

        val classificationResultHead = classifierResult.classificationResult()

        for (classifications in classificationResultHead.classifications()) {
            for (category in classifications.categories()) {
                if (category.categoryName() == "LABEL_1" && category.score() >= confidenceThreshold) {
                    return true
                }
            }
        }
        return false
    }

    fun close() {
        textClassifier?.close()
        textClassifier = null
    }
}