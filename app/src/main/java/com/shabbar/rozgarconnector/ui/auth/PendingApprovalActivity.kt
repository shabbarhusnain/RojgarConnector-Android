package com.shabbar.rozgarconnector.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.databinding.ActivityPendingApprovalBinding
import com.shabbar.rozgarconnector.ui.home.SeekerHomeActivity
import com.shabbar.rozgarconnector.ui.home.ProviderHomeActivity
import com.shabbar.rozgarconnector.ui.role.RoleSelectionActivity
import com.shabbar.rozgarconnector.ui.profile.EducatedWorkerProfileActivity
import com.shabbar.rozgarconnector.ui.profile.UneducatedWorkerProfileActivity
import com.shabbar.rozgarconnector.ui.settings.MenuActivity

class PendingApprovalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPendingApprovalBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPendingApprovalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listenForApproval()

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            navigateToLogin()
        }
    }

    private fun listenForApproval() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null || isFinishing) return@addSnapshotListener

                if (snapshot != null && snapshot.exists()) {
                    val isVerified = snapshot.getBoolean("isVerified") ?: false
                    val role = (snapshot.getString("role") ?: "").lowercase()
                    val workerType = (snapshot.getString("workerType") ?: "").lowercase()
                    val profileCompleted = snapshot.getBoolean("profileCompleted") ?: false

                    if (isVerified) {
                        // Routing Logic (Matching Splash Screen)
                        when {
                            role == "seeker" -> navigateTo(SeekerHomeActivity::class.java)
                            role == "worker" || workerType == "educated" || workerType == "uneducated" -> {
                                if (profileCompleted) {
                                    navigateTo(ProviderHomeActivity::class.java)
                                } else {
                                    val intent = if (workerType == "educated") {
                                        Intent(this, EducatedWorkerProfileActivity::class.java)
                                    } else {
                                        Intent(this, UneducatedWorkerProfileActivity::class.java)
                                    }
                                    startActivity(intent)
                                    finish()
                                }
                            }
                            else -> navigateTo(RoleSelectionActivity::class.java)
                        }
                    }
                }
            }
    }

    private fun navigateTo(cls: Class<*>) {
        val intent = Intent(this, cls)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}