package com.shabbar.rozgarconnector.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.firebase.auth.FirebaseAuth
import com.shabbar.rozgarconnector.databinding.ActivityMenuBinding
import com.shabbar.rozgarconnector.ui.auth.SplashActivity
import com.shabbar.rozgarconnector.ui.help.ChatWithAdminActivity
import com.shabbar.rozgarconnector.ui.help.HelpSupportActivity
import com.shabbar.rozgarconnector.utils.TranslatorUtil

class MenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding
    private val auth = FirebaseAuth.getInstance()
    private var isUpdatingLocale = false
    private val isAuthMenu by lazy {
        intent.getBooleanExtra(EXTRA_AUTH_MENU, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyMenuMode()
        loadSettings()

        binding.btnBack.setOnClickListener {
            finish()
        }

        // --- ACCOUNT SETTINGS (Navigate to New Screen) ---
        binding.btnAccountSettings.setOnClickListener {
            // Hum abhi naya Activity banayein ge
            val intent = Intent(this, AccountSettingsActivity::class.java)
            startActivity(intent)
        }

        // --- LANGUAGE SWITCH ---
        binding.switchUrdu.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingLocale) return@setOnCheckedChangeListener
            
            val currentIsUrdu = TranslatorUtil.isUrduEnabled(this)
            if (isChecked == currentIsUrdu) return@setOnCheckedChangeListener

            TranslatorUtil.setUrduEnabled(this, isChecked)
            
            val localeList = if (isChecked) {
                LocaleListCompat.forLanguageTags("ur")
            } else {
                LocaleListCompat.forLanguageTags("en")
            }
            
            isUpdatingLocale = true
            AppCompatDelegate.setApplicationLocales(localeList)
        }

        // --- CUSTOMER SERVICE ---
        binding.btnChatAdmin.setOnClickListener {
            startActivity(Intent(this, ChatWithAdminActivity::class.java))
        }

        // --- HELP & SUPPORT ---
        binding.btnHelpSupport.setOnClickListener {
            startActivity(Intent(this, HelpSupportActivity::class.java))
        }

        // --- SOS EMERGENCY ---
        binding.btnSOS.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:15")
            startActivity(intent)
        }

        // --- LOGOUT ---
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, SplashActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun applyMenuMode() {
        if (!isAuthMenu) return

        binding.tvMenuTitle.text = "Quick Menu"
        binding.btnAccountSettings.visibility = android.view.View.GONE
        binding.btnChatAdmin.visibility = android.view.View.GONE
        binding.helpDivider.visibility = android.view.View.GONE
        binding.btnSOS.visibility = android.view.View.GONE
        binding.btnLogout.visibility = android.view.View.GONE
    }

    private fun loadSettings() {
        val isUrdu = TranslatorUtil.isUrduEnabled(this)
        isUpdatingLocale = true
        binding.switchUrdu.isChecked = isUrdu
        isUpdatingLocale = false
    }

    companion object {
        const val EXTRA_AUTH_MENU = "extra_auth_menu"
    }
}
