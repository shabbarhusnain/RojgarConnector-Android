package com.shabbar.rozgarconnector.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

        // CORRECT PATH: settings -> maintenanceMode -> value
        checkMaintenanceMode()
    }

    private fun checkMaintenanceMode() {
        db.collection("settings").document("maintenanceMode").get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val isMaintenance = doc.getBoolean("value") ?: false
                    
                    if (isMaintenance) {
                        showMaintenanceDialog()
                        return@addOnSuccessListener
                    }
                }
                
                // If not in maintenance, proceed to auth check
                Handler(Looper.getMainLooper()).postDelayed({
                    checkAuth()
                }, 2000)
            }
            .addOnFailureListener {
                // If query fails, proceed normally
                checkAuth()
            }
    }

    private fun showMaintenanceDialog() {
        AlertDialog.Builder(this)
            .setTitle("System Maintenance")
            .setMessage("RozgarConnector is currently undergoing maintenance. Please check back shortly.")
            .setCancelable(false)
            .setPositiveButton("Close App") { _, _ -> finish() }
            .show()
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
                    
                    val isBlocked = doc.getBoolean("isBlocked") ?: false
                    if (isBlocked) {
                        val reason = doc.getString("blockReason") ?: "Account blocked due to policy violation."
                        showBlockedDialog(reason)
                        return@addOnSuccessListener
                    }

                    val role = (doc.getString("role") ?: "pending").lowercase()
                    
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

    private fun showBlockedDialog(reason: String) {
        AlertDialog.Builder(this)
            .setTitle("Account Blocked")
            .setMessage(reason)
            .setCancelable(false)
            .setPositiveButton("Logout") { _, _ -> 
                auth.signOut()
                navigateTo(LoginActivity::class.java)
            }
            .show()
    }

    private fun navigateTo(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
        finish()
    }
}