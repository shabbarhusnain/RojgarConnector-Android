package com.shabbar.rozgarconnector.ui.admin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.ActivityAdminDashboardBinding
import com.shabbar.rozgarconnector.ui.auth.LoginActivity

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        
        binding.btnLogout.setOnClickListener {
            handleLogout()
        }

        // Default Fragment: Stats
        if (savedInstanceState == null) {
            loadFragment(StatsFragment())
        }
    }

    private fun setupNavigation() {
        binding.adminBottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_stats -> {
                    loadFragment(StatsFragment())
                    true
                }
                R.id.nav_verifications -> {
                    loadFragment(VerificationsFragment())
                    true
                }
                R.id.nav_users -> {
                    loadFragment(UsersFragment())
                    true
                }
                R.id.nav_feedback -> {
                    loadFragment(FeedbackFragment())
                    true
                }
                R.id.nav_support -> {
                    loadFragment(SupportFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.admin_fragment_container, fragment)
            .commit()
    }

    private fun handleLogout() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}