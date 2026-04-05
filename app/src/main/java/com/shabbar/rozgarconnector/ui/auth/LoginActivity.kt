package com.shabbar.rozgarconnector.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.databinding.ActivityLoginBinding
import com.shabbar.rozgarconnector.ui.admin.AdminDashboardActivity
import com.shabbar.rozgarconnector.ui.home.ProviderHomeActivity
import com.shabbar.rozgarconnector.ui.home.SeekerHomeActivity
import com.shabbar.rozgarconnector.ui.role.RoleSelectionActivity
import com.shabbar.rozgarconnector.ui.profile.EducatedWorkerProfileActivity
import com.shabbar.rozgarconnector.ui.profile.UneducatedWorkerProfileActivity
import com.shabbar.rozgarconnector.ui.settings.SettingsActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Null check for settings button (Safe implementation)
        binding.btnSettings?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        if (auth.currentUser != null) {
            checkUserStatusAndNavigate(auth.currentUser!!.uid)
        }

        binding.btnLogin.setOnClickListener { handleLogin() }
        binding.tvRegisterLink.setOnClickListener { startActivity(Intent(this, RegisterActivity::class.java)) }
        binding.tvForgotPassword.setOnClickListener { startActivity(Intent(this, ForgotPasswordActivity::class.java)) }
    }

    private fun handleLogin() {
        val cnicInput = binding.etLoginCNIC.text.toString().trim()
        val password = binding.etLoginPassword.text.toString().trim()

        if (cnicInput.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val formattedEmail = if (cnicInput.contains("@")) cnicInput else "$cnicInput@rozgar.com"

        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "VERIFYING..."

        auth.signInWithEmailAndPassword(formattedEmail, password)
            .addOnSuccessListener { result ->
                checkUserStatusAndNavigate(result.user!!.uid)
            }
            .addOnFailureListener { e ->
                resetButton()
                Toast.makeText(this, "Auth Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun checkUserStatusAndNavigate(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists() && !isFinishing) {
                    val role = doc.getString("role") ?: "pending"
                    val isVerified = doc.getBoolean("isVerified") ?: false
                    val profileCompleted = doc.getBoolean("profileCompleted") ?: false
                    val workerType = doc.getString("workerType") ?: "none"

                    when {
                        role.equals("Admin", ignoreCase = true) -> {
                            navigateTo(AdminDashboardActivity::class.java)
                        }
                        
                        role.equals("Seeker", ignoreCase = true) -> {
                            if (!isVerified) {
                                navigateTo(PendingApprovalActivity::class.java)
                            } else {
                                navigateTo(SeekerHomeActivity::class.java)
                            }
                        }

                        role.equals("Worker", ignoreCase = true) -> {
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

                        role == "pending" -> {
                            navigateTo(RoleSelectionActivity::class.java)
                        }
                        
                        else -> {
                            Toast.makeText(this, "Unknown Role: $role", Toast.LENGTH_SHORT).show()
                            resetButton()
                        }
                    }
                }
            }
            .addOnFailureListener {
                resetButton()
                Toast.makeText(this, "Database Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateTo(activityClass: Class<*>) {
        if (!isFinishing) {
            startActivity(Intent(this, activityClass).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    private fun resetButton() {
        binding.btnLogin.isEnabled = true
        binding.btnLogin.text = "LOGIN"
    }
}