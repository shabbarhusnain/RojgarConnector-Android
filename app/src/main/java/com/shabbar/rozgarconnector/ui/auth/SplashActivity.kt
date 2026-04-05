package com.shabbar.rozgarconnector.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.ui.admin.AdminDashboardActivity
import com.shabbar.rozgarconnector.ui.home.SeekerHomeActivity
import com.shabbar.rozgarconnector.ui.home.ProviderHomeActivity
import com.shabbar.rozgarconnector.ui.role.RoleSelectionActivity
import com.shabbar.rozgarconnector.ui.profile.EducatedWorkerProfileActivity
import com.shabbar.rozgarconnector.ui.profile.UneducatedWorkerProfileActivity

class SplashActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserStatus()
        }, 2000)
    }

    private fun checkUserStatus() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val role = (doc.getString("role") ?: "").lowercase()
                        val isVerified = doc.getBoolean("isVerified") ?: false
                        val workerType = (doc.getString("workerType") ?: "").lowercase()
                        val profileCompleted = doc.getBoolean("profileCompleted") ?: false

                        // Logical Fix: Admin bypasses verification check
                        if (role == "admin") {
                            startActivity(Intent(this, AdminDashboardActivity::class.java))
                        } else if (!isVerified) {
                            startActivity(Intent(this, PendingApprovalActivity::class.java))
                        } else {
                            // Professional Routing based on Role
                            when {
                                role == "seeker" -> {
                                    startActivity(Intent(this, SeekerHomeActivity::class.java))
                                }
                                role == "worker" || workerType == "educated" || workerType == "uneducated" -> {
                                    if (profileCompleted) {
                                        startActivity(Intent(this, ProviderHomeActivity::class.java))
                                    } else {
                                        // Force Portfolio making
                                        val intent = if (workerType == "educated") {
                                            Intent(this, EducatedWorkerProfileActivity::class.java)
                                        } else {
                                            Intent(this, UneducatedWorkerProfileActivity::class.java)
                                        }
                                        startActivity(intent)
                                    }
                                }
                                else -> {
                                    // Only new users who haven't picked a role yet
                                    startActivity(Intent(this, RoleSelectionActivity::class.java))
                                }
                            }
                        }
                    } else {
                        auth.signOut()
                        startActivity(Intent(this, LoginActivity::class.java))
                    }
                    finish()
                }
                .addOnFailureListener {
                    auth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
        }
    }
}