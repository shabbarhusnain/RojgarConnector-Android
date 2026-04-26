package com.shabbar.rozgarconnector.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

        checkMaintenanceMode()
    }

    private fun checkMaintenanceMode() {
        db.collection("settings").document("maintenanceMode").get()
            .addOnSuccessListener { doc ->
                if (doc.exists() && doc.getBoolean("value") == true) {
                    showMaintenanceDialog()
                } else {
                    Handler(Looper.getMainLooper()).postDelayed({ checkAuth() }, 2000)
                }
            }
            .addOnFailureListener { checkAuth() }
    }

    private fun showMaintenanceDialog() {
        AlertDialog.Builder(this)
            .setTitle("System Maintenance")
            .setMessage("RozgarConnector is currently undergoing maintenance.")
            .setCancelable(false)
            .setPositiveButton("Close") { _, _ -> finish() }.show()
    }

    private fun checkAuth() {
        val user = auth.currentUser
        if (user != null) fetchUserRole(user.uid)
        else navigateTo(LoginActivity::class.java)
    }

    private fun fetchUserRole(uid: String) {
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists() && !isFinishing) {
                if (doc.getBoolean("isBlocked") == true) {
                    showBlockedDialog(doc.getString("blockReason") ?: "Account blocked.")
                    return@addOnSuccessListener
                }

                // WORKFLOW REPAIR: Unified role check (lowercase only)
                val role = (doc.getString("role") ?: "pending").lowercase()
                val isVerified = doc.getBoolean("isVerified") ?: false
                val profileCompleted = doc.getBoolean("profileCompleted") ?: false
                val workerType = (doc.getString("workerType") ?: "").lowercase()

                if (!isVerified) {
                    navigateTo(PendingApprovalActivity::class.java)
                    return@addOnSuccessListener
                }

                when (role) {
                    "seeker" -> navigateTo(SeekerHomeActivity::class.java)
                    "provider", "worker" -> {
                        if (profileCompleted) {
                            navigateTo(ProviderHomeActivity::class.java)
                        } else {
                            val target = if (workerType == "educated") EducatedWorkerProfileActivity::class.java
                                        else UneducatedWorkerProfileActivity::class.java
                            navigateTo(target)
                        }
                    }
                    else -> navigateTo(RoleSelectionActivity::class.java)
                }
            } else {
                navigateTo(LoginActivity::class.java)
            }
        }.addOnFailureListener { navigateTo(LoginActivity::class.java) }
    }

    private fun showBlockedDialog(reason: String) {
        AlertDialog.Builder(this).setTitle("Account Blocked").setMessage(reason)
            .setCancelable(false).setPositiveButton("Logout") { _, _ ->
                auth.signOut()
                navigateTo(LoginActivity::class.java)
            }.show()
    }

    private fun navigateTo(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
        finish()
    }
}