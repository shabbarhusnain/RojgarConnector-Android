package com.shabbar.rozgarconnector.utils

import android.content.Context
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

object TranslatorUtil {
    private const val PREFS_NAME = "rozgar_prefs"
    private const val KEY_LANG_URDU = "is_urdu_enabled"
    private var translator: Translator? = null
    private var isInitializing = false
    private var isModelReady = false

    fun isUrduEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_LANG_URDU, false)
    }

    fun setUrduEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_LANG_URDU, enabled).apply()
        if (enabled && !isModelReady) initTranslator({}, {})
    }

    fun initTranslator(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        if (isModelReady) { onSuccess(); return }
        if (isInitializing) return

        isInitializing = true
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.URDU)
            .build()
        
        val client = Translation.getClient(options)
        val conditions = DownloadConditions.Builder().build()

        client.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                translator = client
                isInitializing = false
                isModelReady = true
                onSuccess()
            }
            .addOnFailureListener { exception ->
                isInitializing = false
                onFailure(exception)
            }
    }

    fun translateText(text: String, onResult: (String) -> Unit) {
        if (text.isEmpty()) { onResult(text); return }
        
        val currentTranslator = translator
        if (currentTranslator == null || !isModelReady) {
            // Auto-trigger init if needed
            initTranslator({}, {})
            onResult(text) // Return original for now
            return
        }

        currentTranslator.translate(text)
            .addOnSuccessListener { onResult(it) }
            .addOnFailureListener { onResult(text) }
    }

    fun close() {
        translator?.close()
        translator = null
        isModelReady = false
    }
}