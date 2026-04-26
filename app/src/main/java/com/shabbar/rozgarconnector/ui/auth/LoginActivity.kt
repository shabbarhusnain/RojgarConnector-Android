package com.shabbar.rozgarconnector.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.shabbar.rozgarconnector.databinding.ActivityLoginBinding
import com.shabbar.rozgarconnector.ui.home.ProviderHomeActivity
import com.shabbar.rozgarconnector.ui.home.SeekerHomeActivity
import com.shabbar.rozgarconnector.ui.role.RoleSelectionActivity
import com.shabbar.rozgarconnector.ui.profile.EducatedWorkerProfileActivity
import com.shabbar.rozgarconnector.ui.profile.UneducatedWorkerProfileActivity
import com.shabbar.rozgarconnector.ui.settings.MenuActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSettings?.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java).apply {
                putExtra(MenuActivity.EXTRA_AUTH_MENU, true)
            })
        }

        if (auth.currentUser != null) {
            updateFcmTokenAndNavigate(auth.currentUser!!.uid)
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

        // IMPROVED: Strict check to match Firebase identifiers
        val formattedEmail = if (cnicInput.contains("@")) {
            cnicInput 
        } else {
            // Hum isay "@rozgar.com" keh rahy thy lekin console mein shayad truncated hai ya different hai.
            // Hum strictly cnic@rozgar.com try krty hain.
            "$cnicInput@rozgar.com"
        }

        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "VERIFYING..."

        auth.signInWithEmailAndPassword(formattedEmail, password)
            .addOnSuccessListener { result ->
                updateFcmTokenAndNavigate(result.user!!.uid)
            }
            .addOnFailureListener { e ->
                resetButton()
                Log.e("LOGIN_ERROR", "Email tried: $formattedEmail")
                Toast.makeText(this, "Login Failed. Check CNIC/Password.", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateFcmTokenAndNavigate(uid: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                db.collection("users").document(uid).update("fcmToken", token)
                    .addOnSuccessListener { Log.d("FCM", "Token updated: $token") }
            }
            checkUserStatusAndNavigate(uid)
        }
    }

    private fun checkUserStatusAndNavigate(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists() && !isFinishing) {
                    val role = (doc.getString("role") ?: "pending").lowercase()
                    val isVerified = doc.getBoolean("isVerified") ?: false
                    val profileCompleted = doc.getBoolean("profileCompleted") ?: false
                    val workerType = (doc.getString("workerType") ?: "none").lowercase()

                    when {
                        role == "admin" -> {
                            Toast.makeText(this, "Admin access is disabled", Toast.LENGTH_SHORT).show()
                            resetButton()
                        }
                        
                        role == "seeker" -> {
                            if (!isVerified) navigateTo(PendingApprovalActivity::class.java)
                            else navigateTo(SeekerHomeActivity::class.java)
                        }

                        role == "worker" || role == "provider" -> {
                            if (!isVerified) navigateTo(PendingApprovalActivity::class.java)
                            else if (!profileCompleted) {
                                val target = if (workerType == "educated") EducatedWorkerProfileActivity::class.java
                                else UneducatedWorkerProfileActivity::class.java
                                navigateTo(target)
                            } else navigateTo(ProviderHomeActivity::class.java)
                        }

                        else -> navigateTo(RoleSelectionActivity::class.java)
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