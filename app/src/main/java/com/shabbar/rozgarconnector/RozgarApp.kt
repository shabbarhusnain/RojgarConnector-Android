package com.shabbar.rozgarconnector

import android.app.Application
import com.shabbar.rozgarconnector.utils.TranslatorUtil

class RozgarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // --- BACKGROUND PRE-DOWNLOAD ---
        // We trigger the translator initialization immediately on app launch.
        // This ensures that the Urdu model starts downloading in the background
        // so it's ready by the time the user explores the app.
        TranslatorUtil.initTranslator(
            onSuccess = { 
                // Model is ready and cached on the device
            },
            onFailure = { 
                // Will retry automatically when needed
            }
        )
    }
}