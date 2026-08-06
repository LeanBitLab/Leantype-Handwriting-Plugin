// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.handwriting

import android.content.Context

interface ModelDownloadListener {
    fun onProgress(progress: Float)
    fun onComplete(success: Boolean)
}

interface HandwritingRecognizer {
    fun init(context: Context)
    fun setLanguage(language: String): Boolean
    fun isLanguageReady(language: String): Boolean
    fun downloadModel(language: String, listener: ModelDownloadListener)
    fun recognize(strokes: List<FloatArray>): List<String>?
    // ponytail: allow deleting downloaded models, default false for backward compatibility
    fun removeModel(language: String): Boolean = false
}
