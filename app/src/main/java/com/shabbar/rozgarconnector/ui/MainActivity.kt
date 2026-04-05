package com.shabbar.rozgarconnector.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.ActivityMainBinding
import com.shabbar.rozgarconnector.ui.auth.LoginActivity
import com.shabbar.rozgarconnector.ui.fragments.HomeFragment
import com.shabbar.rozgarconnector.ui.fragments.MessagesFragment
import com.shabbar.rozgarconnector.ui.fragments.NotificationsFragment
import com.shabbar.rozgarconnector.ui.fragments.ProfileFragment
import com.shabbar.rozgarconnector.ui.job.JobPostActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Register for Notifications
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                auth.currentUser?.uid?.let { uid ->
                    db.collection("users").document(uid).update("fcmToken", token)
                        .addOnSuccessListener { Log.d("FCM", "Token updated successfully") }
                }
            }
        }

        replaceFragment(HomeFragment())

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> replaceFragment(HomeFragment())
                R.id.nav_messages -> replaceFragment(MessagesFragment())
                R.id.nav_notifications -> replaceFragment(NotificationsFragment())
                R.id.nav_profile -> replaceFragment(ProfileFragment())
            }
            true
        }

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, JobPostActivity::class.java))
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}