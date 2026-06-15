// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.handwriting.plugin

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import helium314.keyboard.latin.handwriting.HandwritingRecognizer
import helium314.keyboard.latin.handwriting.ModelDownloadListener
import java.util.concurrent.TimeUnit

class HandwritingRecognizerImpl : HandwritingRecognizer {

    private lateinit var appContext: Context
    private val modelManager = RemoteModelManager.getInstance()
    
    private var currentModel: DigitalInkRecognitionModel? = null
    private var currentRecognizer: DigitalInkRecognizer? = null
    private var currentLanguageTag: String? = null

    override fun init(context: Context) {
        this.appContext = context.applicationContext
    }

    override fun setLanguage(language: String): Boolean {
        if (currentLanguageTag == language && currentRecognizer != null) {
            return true
        }

        try {
            val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag(language)
                ?: return false
            
            val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
            val recognizer = DigitalInkRecognition.getClient(
                DigitalInkRecognizerOptions.builder(model).build()
            )
            
            this.currentModel = model
            this.currentRecognizer = recognizer
            this.currentLanguageTag = language
            return true
        } catch (e: Exception) {
            android.util.Log.e("HandwritingRecognizer", "Failed to set language: $language", e)
        }
        return false
    }

    override fun isLanguageReady(language: String): Boolean {
        try {
            val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag(language)
                ?: return false
            val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
            
            val checkTask = modelManager.isModelDownloaded(model)
            return Tasks.await(checkTask, 5, TimeUnit.SECONDS)
        } catch (e: Exception) {
            android.util.Log.e("HandwritingRecognizer", "Failed to check model download status", e)
        }
        return false
    }

    override fun downloadModel(language: String, listener: ModelDownloadListener) {
        try {
            val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag(language)
            if (modelIdentifier == null) {
                listener.onComplete(false)
                return
            }

            val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
            val conditions = DownloadConditions.Builder().build()
            
            listener.onProgress(0f)
            
            modelManager.download(model, conditions)
                .addOnSuccessListener {
                    listener.onProgress(1f)
                    listener.onComplete(true)
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("HandwritingRecognizer", "Model download failed", e)
                    listener.onComplete(false)
                }
        } catch (e: Exception) {
            android.util.Log.e("HandwritingRecognizer", "Failed to start model download", e)
            listener.onComplete(false)
        }
    }

    override fun recognize(strokes: List<FloatArray>): List<String>? {
        val recognizer = currentRecognizer ?: return null
        if (strokes.isEmpty()) return null

        try {
            val inkBuilder = Ink.builder()
            for (strokeArray in strokes) {
                val strokeBuilder = Ink.Stroke.builder()
                var i = 0
                while (i < strokeArray.size) {
                    if (i + 2 < strokeArray.size) {
                        val x = strokeArray[i]
                        val y = strokeArray[i + 1]
                        val t = strokeArray[i + 2].toLong()
                        strokeBuilder.addPoint(Ink.Point.create(x, y, t))
                    }
                    i += 3
                }
                inkBuilder.addStroke(strokeBuilder.build())
            }

            val ink = inkBuilder.build()
            val task = recognizer.recognize(ink)
            
            val result = Tasks.await(task, 10, TimeUnit.SECONDS)
            return result.candidates.map { it.text }
        } catch (e: Exception) {
            android.util.Log.e("HandwritingRecognizer", "Recognition failed", e)
        }
        return null
    }
}
