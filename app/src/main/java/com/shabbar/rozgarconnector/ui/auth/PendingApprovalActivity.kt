package com.shabbar.rozgarconnector.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
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

        listenForStatus()

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            navigateToLogin()
        }

        binding.btnReApply.setOnClickListener {
            // Take user back to RegisterActivity in "Update Mode" to fix rejected details
            val intent = Intent(this, RegisterActivity::class.java)
            intent.putExtra("IS_UPDATE_MODE", true)
            startActivity(intent)
            // Note: We don't finish() here because we want them to come back to pending state after update
        }
    }

    private fun listenForStatus() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null || isFinishing) return@addSnapshotListener

                if (snapshot != null && snapshot.exists()) {
                    val isVerified = snapshot.getBoolean("isVerified") ?: false
                    val isRejected = snapshot.getBoolean("isRejected") ?: false
                    val rejectionReason = snapshot.getString("rejectionReason") ?: "Documents quality was not sufficient."

                    // Handle Rejection UI
                    if (isRejected) {
                        binding.llPendingView.visibility = View.GONE
                        binding.llRejectedView.visibility = View.VISIBLE
                        binding.tvRejectionReason.text = "Reason: $rejectionReason"
                        return@addSnapshotListener
                    } else {
                        binding.llPendingView.visibility = View.VISIBLE
                        binding.llRejectedView.visibility = View.GONE
                    }

                    // Handle Approval Navigation
                    if (isVerified) {
                        val role = (snapshot.getString("role") ?: "").lowercase()
                        val workerType = (snapshot.getString("workerType") ?: "").lowercase()
                        val profileCompleted = snapshot.getBoolean("profileCompleted") ?: false

                        when {
                            role == "seeker" -> navigateTo(SeekerHomeActivity::class.java)
                            role == "worker" || role == "provider" -> {
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