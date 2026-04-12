package com.shabbar.rozgarconnector

import android.app.Application
import com.shabbar.rozgarconnector.utils.TranslatorUtil

class RozgarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Translator globally if Urdu is enabled
        if (TranslatorUtil.isUrduEnabled(this)) {
            TranslatorUtil.initTranslator(
                onSuccess = { /* Ready */ },
                onFailure = { /* Handle error */ }
            )
        }
    }
}