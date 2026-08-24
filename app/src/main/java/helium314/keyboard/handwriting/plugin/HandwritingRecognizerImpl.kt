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
    
    @Volatile private var currentModel: DigitalInkRecognitionModel? = null
    @Volatile private var currentRecognizer: DigitalInkRecognizer? = null
    @Volatile private var currentLanguageTag: String? = null

    override fun init(context: Context) {
        this.appContext = context.applicationContext
        loadNativeLibrary(this.appContext)
        ensureWorkManagerInitialized(this.appContext)
        ensureMlKitInitialized(this.appContext)
        modelManager = RemoteModelManager.getInstance()
    }

    private fun ensureWorkManagerInitialized(ctx: Context) {
        try {
            androidx.work.WorkManager.getInstance(ctx)
        } catch (_: Throwable) {
            try {
                val config = androidx.work.Configuration.Builder().build()
                androidx.work.WorkManager.initialize(ctx, config)
                android.util.Log.i("HandwritingRecognizer", "WorkManager successfully initialized in plugin")
            } catch (e: Throwable) {
                android.util.Log.w("HandwritingRecognizer", "Failed to initialize WorkManager in plugin", e)
            }
        }
    }

    private fun loadNativeLibrary(ctx: Context) {
        try {
            System.loadLibrary("digitalink")
            android.util.Log.i("HandwritingRecognizer", "Loaded digitalink via System.loadLibrary")
        } catch (e: Throwable) {
            try {
                val libFile = java.io.File(ctx.filesDir, "plugin_libs/handwriting/libdigitalink.so")
                if (libFile.exists()) {
                    System.load(libFile.absolutePath)
                    android.util.Log.i("HandwritingRecognizer", "Loaded digitalink via System.load: ${libFile.absolutePath}")
                } else {
                    android.util.Log.e("HandwritingRecognizer", "libdigitalink.so not found at ${libFile.absolutePath}")
                }
            } catch (e2: Throwable) {
                android.util.Log.e("HandwritingRecognizer", "Failed to load digitalink library", e2)
            }
        }
    }

    private fun ensureMlKitInitialized(ctx: Context) {
        try {
            val mlKitContextClass = Class.forName("com.google.mlkit.common.sdkinternal.MlKitContext")
            try {
                val field = mlKitContextClass.getDeclaredField("zzb")
                field.isAccessible = true
                field.set(null, null)
            } catch (_: Throwable) {}

            val registrars = listOf<ComponentRegistrar>(
                CommonComponentRegistrar(),
                DigitalInkRecognitionRegistrar()
            )
            val initMethod = mlKitContextClass.getDeclaredMethod(
                "initialize",
                Context::class.java,
                List::class.java
            )
            initMethod.invoke(null, ctx, registrars)
            android.util.Log.i("HandwritingRecognizer", "MlKitContext successfully initialized with DigitalInkRecognitionRegistrar")
        } catch (e: Throwable) {
            android.util.Log.w("HandwritingRecognizer", "ensureMlKitInitialized fallback", e)
        }
    }

    private fun getSupportedLanguageTag(language: String): String? {
        val allSupported = HashSet<String>()
        try {
            for (id in DigitalInkRecognitionModelIdentifier.allModelIdentifiers()) {
                val tag = id.languageTag
                if (!tag.contains("-x-gesture", ignoreCase = true)) {
                    allSupported.add(tag)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("HandwritingRecognizer", "Failed to list all model identifiers", e)
        }

        if (allSupported.isEmpty()) {
            return language
        }

        if (allSupported.contains(language)) {
            return language
        }

        val parts = ArrayList<String>()
        var start = 0
        while (true) {
            val idx = language.indexOf('-', start)
            if (idx == -1) {
                parts.add(language.substring(start))
                break
            }
            parts.add(language.substring(start, idx))
            start = idx + 1
        }
        if (parts.isEmpty()) return null

        val lang = parts[0]
        val part1 = if (parts.size > 1) parts[1] else null
        val part2 = if (parts.size > 2) parts[2] else null
        val script = part1?.takeIf { it.length == 4 }
        val region = part1?.takeIf { it.length in 2..3 } ?: part2?.takeIf { it.length in 2..3 }

        val candidates = ArrayList<String>()
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
            for (supported in allSupported) {
                if (supported.equals(candidate, ignoreCase = true)) {
                    return supported
                }
            }
        }

        for (supported in allSupported) {
            if (supported.startsWith("$lang-", ignoreCase = true)) {
                return supported
            }
        }

        return null
    }

    private val LANG_TO_SCRIPT = mapOf(
        "ar" to "arabic", "fa" to "arabic", "ur" to "arabic", "ps" to "arabic",
        "hy" to "armenian",
        "bn" to "bengali", "as" to "bengali",
        "zh" to "chinese",
        "ru" to "cyrillic", "uk" to "cyrillic", "be" to "cyrillic", "bg" to "cyrillic", "mk" to "cyrillic", "sr" to "cyrillic", "kk" to "cyrillic", "ky" to "cyrillic", "tg" to "cyrillic", "mn" to "cyrillic",
        "hi" to "devanagari", "mr" to "devanagari", "ne" to "devanagari", "sa" to "devanagari", "kok" to "devanagari", "mai" to "devanagari", "bho" to "devanagari",
        "am" to "ethiopic", "ti" to "ethiopic",
        "ka" to "georgian",
        "el" to "greek",
        "gu" to "gujarati",
        "he" to "hebrew", "iw" to "hebrew", "yi" to "hebrew",
        "ja" to "japanese",
        "kn" to "kannada",
        "km" to "khmer",
        "ko" to "korean",
        "lo" to "lao",
        "ml" to "malayalam",
        "my" to "myanmar",
        "or" to "odia",
        "pa" to "punjabi",
        "si" to "sinhala",
        "ta" to "tamil",
        "te" to "telugu",
        "th" to "thai",
        "bo" to "tibetan",
        "vi" to "vietnamese"
    )

    override fun setLanguage(language: String): Boolean {
        val supportedLanguage = getSupportedLanguageTag(language) ?: language
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
            
            // Close previous recognizer to prevent native C++ memory leaks
            currentRecognizer?.close()

            // Direct Sideload/Import JNI Init
            val ctx = if (::appContext.isInitialized) appContext else null
            if (ctx != null) {
                val baseLang = supportedLanguage.substringBefore('-').lowercase()
                val script = LANG_TO_SCRIPT[baseLang] ?: "latin"
                val baseDir = ctx.noBackupFilesDir ?: ctx.filesDir
                val tagsToCheck = setOf(supportedLanguage, language.replace('_', '-'), baseLang, language)
                var modelDir: java.io.File? = null
                for (tag in tagsToCheck) {
                    val f = java.io.File(baseDir, "com.google.mlkit.models/$tag/DIGITAL_INK/0")
                    if (java.io.File(f, "model.tflite").exists() && java.io.File(f, "model.tflite").length() > 0) {
                        modelDir = f
                        break
                    }
                }

                if (modelDir != null) {
                    try {
                        val localRecospec = java.io.File(modelDir, "recospec")
                        val recospecFile = if (localRecospec.exists() && localRecospec.length() > 0) {
                            localRecospec
                        } else {
                            val assetRecospec = java.io.File(ctx.cacheDir, "hw_$script.recospec")
                            if (!assetRecospec.exists() || assetRecospec.length() == 0L) {
                                ctx.assets.open("recospecs/$script.recospec").use { input ->
                                    java.io.FileOutputStream(assetRecospec).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                            assetRecospec
                        }

                        val modelFile = java.io.File(modelDir, "model.tflite")
                        val localFst = java.io.File(modelDir, "fst.compact")
                        val emptyFstFile = java.io.File(ctx.cacheDir, "empty.fst").apply {
                            if (!exists()) createNewFile()
                        }
                        val fstFile = if (localFst.exists() && localFst.length() > 0) localFst else emptyFstFile

                        val jniField = recognizer.javaClass.getDeclaredField("zzb")
                        jniField.isAccessible = true
                        val jniRef = jniField.get(recognizer) as? java.util.concurrent.atomic.AtomicReference<*>
                        val jni = jniRef?.get() as? com.google.mlkit.vision.digitalink.recognition.internal.DigitalInkRecognizerJni
                        if (jni != null && recospecFile.exists() && modelFile.exists()) {
                            java.io.FileInputStream(recospecFile).use { recospecIn ->
                                java.io.FileInputStream(modelFile).use { modelIn ->
                                    java.io.FileInputStream(fstFile).use { fstIn ->
                                        val nativeHandle = jni.initNativeRecognizer(recospecIn, modelIn, fstIn)
                                        jni.zza.set(nativeHandle)
                                        android.util.Log.i("HandwritingRecognizer", "Directly initialized native recognizer handle=$nativeHandle for $supportedLanguage (script=$script, fstSize=${fstFile.length()})")
                                    }
                                }
                            }
                        }
                    } catch (e: Throwable) {
                        android.util.Log.w("HandwritingRecognizer", "Failed direct JNI init, fallback to standard", e)
                    }
                }
            }

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
        val supportedLanguage = getSupportedLanguageTag(language) ?: language
        val baseLang = language.substringBefore('-').lowercase()
        val normalizedTag = language.replace('_', '-')
        
        // 1. Direct file check on disk (reliable for imported models)
        val ctx = appContext
        if (ctx != null) {
            val baseDir = ctx.noBackupFilesDir ?: ctx.filesDir
            val tagsToCheck = setOf(supportedLanguage, normalizedTag, baseLang, language)
            for (tag in tagsToCheck) {
                val modelFile = java.io.File(baseDir, "com.google.mlkit.models/$tag/DIGITAL_INK/0/model.tflite")
                if (modelFile.exists() && modelFile.length() > 0) {
                    return true
                }
            }
        }

        // 2. Fallback to modelManager
        if (!::modelManager.isInitialized) return false
        try {
            val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag(supportedLanguage)
                ?: return false
            val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
            val checkTask = modelManager.isModelDownloaded(model)
            return Tasks.await(checkTask, 2, TimeUnit.SECONDS)
        } catch (e: Exception) {
            android.util.Log.e("HandwritingRecognizer", "Failed to check model download status for $supportedLanguage (requested: $language)", e)
        }
        return false
    }

    override fun downloadModel(language: String, listener: ModelDownloadListener) {
        if (!::modelManager.isInitialized) {
            listener.onComplete(false)
            return
        }
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
            val list = ArrayList<String>()
            for (candidate in result.candidates) {
                list.add(candidate.text)
            }
            return list
        } catch (e: Exception) {
            android.util.Log.e("HandwritingRecognizer", "Recognition failed", e)
        }
        return null
    }

    // ponytail: implement model deletion using ML Kit RemoteModelManager
    override fun removeModel(language: String): Boolean {
        if (!::modelManager.isInitialized) return false
        val supportedLanguage = getSupportedLanguageTag(language) ?: return false
        try {
            val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag(supportedLanguage)
                ?: return false
            val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
            val deleteTask = modelManager.deleteDownloadedModel(model)
            Tasks.await(deleteTask, 5, TimeUnit.SECONDS)
            if (currentLanguageTag == supportedLanguage) {
                currentRecognizer?.close()
                currentModel = null
                currentRecognizer = null
                currentLanguageTag = null
            }
            return true
        } catch (e: Exception) {
            android.util.Log.e("HandwritingRecognizer", "Failed to delete model for $supportedLanguage", e)
        }
        return false
    }
}
