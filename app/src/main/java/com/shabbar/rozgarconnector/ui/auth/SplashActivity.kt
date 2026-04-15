package com.shabbar.rozgarconnector.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.ui.home.ProviderHomeActivity
import com.shabbar.rozgarconnector.ui.home.SeekerHomeActivity
import com.shabbar.rozgarconnector.ui.role.RoleSelectionActivity
import com.shabbar.rozgarconnector.ui.profile.EducatedWorkerProfileActivity
import com.shabbar.rozgarconnector.ui.profile.UneducatedWorkerProfileActivity

class SplashActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            checkAuth()
        }, 2000)
    }

    private fun checkAuth() {
        val user = auth.currentUser
        if (user != null) {
            fetchUserRole(user.uid)
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun fetchUserRole(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists() && !isFinishing) {
                    val role = (doc.getString("role") ?: "pending").lowercase()
                    
                    // SECURITY: If an Admin tries to stay logged in, force logout them
                    if (role == "admin") {
                        auth.signOut()
                        navigateTo(LoginActivity::class.java)
                        return@addOnSuccessListener
                    }

                    val isVerified = doc.getBoolean("isVerified") ?: false
                    val profileCompleted = doc.getBoolean("profileCompleted") ?: false
                    val workerType = (doc.getString("workerType") ?: "none").lowercase()

                    when {
                        role == "seeker" -> {
                            if (!isVerified) navigateTo(PendingApprovalActivity::class.java)
                            else navigateTo(SeekerHomeActivity::class.java)
                        }

                        role == "worker" || role == "provider" -> {
                            if (!isVerified) {
                                navigateTo(PendingApprovalActivity::class.java)
                            } else if (!profileCompleted) {
                                val target = if (workerType == "educated") EducatedWorkerProfileActivity::class.java
                                else UneducatedWorkerProfileActivity::class.java
                                navigateTo(target)
                            } else {
                                navigateTo(ProviderHomeActivity::class.java)
                            }
                        }

                        else -> navigateTo(RoleSelectionActivity::class.java)
                    }
                } else {
                    navigateTo(LoginActivity::class.java)
                }
            }
            .addOnFailureListener {
                navigateTo(LoginActivity::class.java)
            }
    }

    private fun navigateTo(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
        finish()
    }
}