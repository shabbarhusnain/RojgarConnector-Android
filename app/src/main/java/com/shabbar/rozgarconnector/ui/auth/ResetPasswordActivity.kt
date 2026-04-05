package com.shabbar.rozgarconnector.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.shabbar.rozgarconnector.databinding.ActivityResetPasswordBinding

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResetPasswordBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnResetPassword.setOnClickListener {
            val newPass = binding.etNewPassword.text.toString().trim()
            val confirmPass = binding.etConfirmNewPassword.text.toString().trim()

            if (newPass.length < 6) {
                binding.etNewPassword.error = "Minimum 6 characters"
                return@setOnClickListener
            }
            if (newPass != confirmPass) {
                binding.etConfirmNewPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            updatePassword(newPass)
        }
    }

    private fun updatePassword(pass: String) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Error: User not authenticated. Please try again.", Toast.LENGTH_LONG).show()
            // Optional: Redirect to login
            return
        }

        binding.btnResetPassword.isEnabled = false
        binding.btnResetPassword.text = "UPDATING..."

        user.updatePassword(pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Password updated successfully! Please login with your new password.", Toast.LENGTH_LONG).show()
                auth.signOut() // Sign out to force re-login
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                binding.btnResetPassword.isEnabled = true
                binding.btnResetPassword.text = "RESET PASSWORD"
                Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}