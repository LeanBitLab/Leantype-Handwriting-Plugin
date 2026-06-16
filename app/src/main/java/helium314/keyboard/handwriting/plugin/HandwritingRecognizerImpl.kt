// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.handwriting.plugin

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.components.ComponentRegistrar
import com.google.mlkit.common.internal.CommonComponentRegistrar
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.common.sdkinternal.MlKitContext
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import com.google.mlkit.vision.digitalink.recognition.internal.DigitalInkRecognitionRegistrar
import helium314.keyboard.latin.handwriting.HandwritingRecognizer
import helium314.keyboard.latin.handwriting.ModelDownloadListener
import java.util.concurrent.TimeUnit

class HandwritingRecognizerImpl : HandwritingRecognizer {

    private lateinit var appContext: Context
    private lateinit var modelManager: RemoteModelManager
    
    private var currentModel: DigitalInkRecognitionModel? = null
    private var currentRecognizer: DigitalInkRecognizer? = null
    private var currentLanguageTag: String? = null

    override fun init(context: Context) {
        this.appContext = context.applicationContext

        try {
            val registrars = listOf<ComponentRegistrar>(
                CommonComponentRegistrar(),
                DigitalInkRecognitionRegistrar()
            )
            MlKitContext.initialize(this.appContext, registrars)
        } catch (e: Exception) {
            android.util.Log.e("HandwritingRecognizer", "Failed to initialize MlKitContext", e)
        }
        modelManager = RemoteModelManager.getInstance()
    }

    private fun getSupportedLanguageTag(language: String): String? {
        val allSupported = try {
            DigitalInkRecognitionModelIdentifier.allModelIdentifiers().map { it.languageTag }.toSet()
        } catch (e: Exception) {
            android.util.Log.e("HandwritingRecognizer", "Failed to list all model identifiers", e)
            emptySet()
        }

        if (allSupported.isEmpty()) {
            return language
        }

        if (allSupported.contains(language)) {
            return language
        }

        val parts = language.split("-")
        if (parts.isEmpty()) return null

        val lang = parts[0]
        val script = parts.getOrNull(1)?.takeIf { it.length == 4 }
        val region = parts.getOrNull(1)?.takeIf { it.length in 2..3 }
            ?: parts.getOrNull(2)?.takeIf { it.length in 2..3 }

        val candidates = mutableListOf<String>()
        if (script != null) {
            if (region != null) {
                candidates.add("$lang-$script-$region")
            }
            candidates.add("$lang-$script")
        } else if (region != null) {
            candidates.add("$lang-$region")
        }
        candidates.add(lang)

        for (candidate in candidates) {
            val matched = allSupported.firstOrNull { it.equals(candidate, ignoreCase = true) }
            if (matched != null) {
                return matched
            }
        }

        val prefixMatch = allSupported.firstOrNull { it.startsWith("$lang-", ignoreCase = true) }
        if (prefixMatch != null) {
            return prefixMatch
        }

        return null
    }

    override fun setLanguage(language: String): Boolean {
        val supportedLanguage = getSupportedLanguageTag(language) ?: return false
        if (currentLanguageTag == supportedLanguage && currentRecognizer != null) {
            return true
        }

        try {
            val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag(supportedLanguage)
                ?: return false
            
            val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
            val recognizer = DigitalInkRecognition.getClient(
                DigitalInkRecognizerOptions.builder(model).build()
            )
            
            this.currentModel = model
            this.currentRecognizer = recognizer
            this.currentLanguageTag = supportedLanguage
            return true
        } catch (e: Exception) {
            android.util.Log.e("HandwritingRecognizer", "Failed to set language: $supportedLanguage (requested: $language)", e)
        }
        return false
    }

    override fun isLanguageReady(language: String): Boolean {
        val supportedLanguage = getSupportedLanguageTag(language) ?: return false
        try {
            val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag(supportedLanguage)
                ?: return false
            val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
            
            val checkTask = modelManager.isModelDownloaded(model)
            return Tasks.await(checkTask, 5, TimeUnit.SECONDS)
        } catch (e: Exception) {
            android.util.Log.e("HandwritingRecognizer", "Failed to check model download status for $supportedLanguage (requested: $language)", e)
        }
        return false
    }

    override fun downloadModel(language: String, listener: ModelDownloadListener) {
        val supportedLanguage = getSupportedLanguageTag(language)
        if (supportedLanguage == null) {
            listener.onComplete(false)
            return
        }
        try {
            val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag(supportedLanguage)
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
                    android.util.Log.e("HandwritingRecognizer", "Model download failed for $supportedLanguage (requested: $language)", e)
                    listener.onComplete(false)
                }
        } catch (e: Exception) {
            android.util.Log.e("HandwritingRecognizer", "Failed to start model download for $supportedLanguage (requested: $language)", e)
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
