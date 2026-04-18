package com.shabbar.rozgarconnector.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.shabbar.rozgarconnector.databinding.ActivityResetPasswordBinding

/**
 * Allows the user to set a new password after successful OTP verification.
 * The user reaches this screen only after Firebase phone authentication succeeds.
 */
class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResetPasswordBinding
    private val auth = FirebaseAuth.getInstance()
    private var mCnic: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve the CNIC passed from ForgotPasswordActivity
        mCnic = intent.getStringExtra("CNIC")

        if (mCnic == null) {
            Toast.makeText(this, "Error: User context lost.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnResetPassword.setOnClickListener {
            val newPass = binding.etNewPassword.text.toString().trim()
            val confirmPass = binding.etConfirmPassword.text.toString().trim()

            if (newPass.isEmpty() || newPass.length < 6) {
                binding.etNewPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            if (newPass != confirmPass) {
                binding.etConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            binding.btnResetPassword.isEnabled = false
            binding.btnResetPassword.text = "UPDATING..."
            updatePassword(newPass)
        }
    }

    private fun updatePassword(newPassword: String) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Session expired. Please try again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        user.updatePassword(newPassword)
            .addOnSuccessListener {
                Toast.makeText(this, "✅ Password Updated Successfully!", Toast.LENGTH_LONG).show()
                auth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                binding.btnResetPassword.isEnabled = true
                binding.btnResetPassword.text = "UPDATE PASSWORD"
                val errorMsg = when {
                    e.message?.contains("blocked") == true || 
                    e.message?.contains("unusual activity") == true -> 
                        "Too many attempts. Try again later."
                    e.message?.contains("network") == true ->
                        "Network error. Check your connection."
                    else -> "Failed to update password: ${e.message}"
                }
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            }
    }
}
