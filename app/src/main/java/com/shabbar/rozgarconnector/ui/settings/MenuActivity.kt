package com.shabbar.rozgarconnector.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.firebase.auth.FirebaseAuth
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.ActivityMenuBinding
import com.shabbar.rozgarconnector.ui.auth.SplashActivity
import com.shabbar.rozgarconnector.utils.TranslatorUtil

class MenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding
    private val auth = FirebaseAuth.getInstance()
    private var isUpdatingLocale = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadSettings()

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.switchUrdu.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingLocale) return@setOnCheckedChangeListener
            
            val currentIsUrdu = TranslatorUtil.isUrduEnabled(this)
            if (isChecked == currentIsUrdu) return@setOnCheckedChangeListener

            // Save preference
            TranslatorUtil.setUrduEnabled(this, isChecked)
            
            // Set App Locale
            val localeList = if (isChecked) {
                LocaleListCompat.forLanguageTags("ur")
            } else {
                LocaleListCompat.forLanguageTags("en")
            }
            
            isUpdatingLocale = true
            AppCompatDelegate.setApplicationLocales(localeList)
            
            // Optional: Initialize translator in background without blocking UI
            if (isChecked) {
                binding.tvStatus.visibility = View.VISIBLE
                binding.tvStatus.text = getString(R.string.pending)
                
                TranslatorUtil.initTranslator(
                    onSuccess = {
                        runOnUiThread { binding.tvStatus.text = getString(R.string.finish_job) }
                    },
                    onFailure = { 
                        runOnUiThread { binding.tvStatus.text = "Error" }
                    }
                )
            }
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, SplashActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun loadSettings() {
        val isUrdu = TranslatorUtil.isUrduEnabled(this)
        isUpdatingLocale = true
        binding.switchUrdu.isChecked = isUrdu
        isUpdatingLocale = false
        
        if (isUrdu) {
            binding.tvStatus.visibility = View.VISIBLE
            binding.tvStatus.text = getString(R.string.finish_job)
        }
    }
}