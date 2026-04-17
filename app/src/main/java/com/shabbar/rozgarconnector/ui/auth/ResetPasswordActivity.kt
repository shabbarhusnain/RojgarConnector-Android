package com.shabbar.rozgarconnector.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.ActivityResetPasswordBinding

/**
 * Allows the user to set a new password after successful OTP verification.
 * Requires reauthentication with the old password before updating.
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

            // Show dialog to ask for old password (for reauthentication)
            showOldPasswordDialog(newPass)
        }
    }

    private fun showOldPasswordDialog(newPassword: String) {
        val dialogView = layoutInflater.inflate(R.layout.layout_step_password, null)
        val etOldPassword = dialogView.findViewById<TextInputEditText>(R.id.etPasswordInput)
        val btnContinue = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnVerifyPassword)

        val tvTitle = dialogView.findViewById<android.widget.TextView>(R.id.tvHeaderTitle) ?: 
            dialogView.findViewById<android.widget.TextView>(R.id.tvHeaderTitle)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnContinue.text = "VERIFY & CONTINUE"
        btnContinue.setOnClickListener {
            val oldPassword = etOldPassword.text.toString().trim()

            if (oldPassword.isEmpty()) {
                Toast.makeText(this, "Please enter your current password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnContinue.isEnabled = false
            btnContinue.text = "VERIFYING..."

            // Reauthenticate with old password
            reauthenticateAndUpdatePassword(oldPassword, newPassword, btnContinue)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun reauthenticateAndUpdatePassword(oldPassword: String, newPassword: String, 
                                                btnContinue: com.google.android.material.button.MaterialButton) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Session expired. Please try again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Construct email from CNIC
        val email = "$mCnic@rozgar.com"

        // Create credential with old password for reauthentication
        val credential = EmailAuthProvider.getCredential(email, oldPassword)

        // Reauthenticate
        user.reauthenticate(credential)
            .addOnSuccessListener {
                // Reauthentication successful - now update password
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        Toast.makeText(this, "✅ Password Updated Successfully!", Toast.LENGTH_LONG).show()
                        // Sign out and redirect to login
                        auth.signOut()
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        btnContinue.isEnabled = true
                        btnContinue.text = "UPDATE PASSWORD"
                        val errorMsg = when {
                            e.message?.contains("blocked") == true -> 
                                "Too many attempts. Try again later."
                            else -> "Failed to update password: ${e.message}"
                        }
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                btnContinue.isEnabled = true
                btnContinue.text = "VERIFY & CONTINUE"
                val errorMsg = when {
                    e.message?.contains("INVALID_PASSWORD") == true -> 
                        "Incorrect password. Please try again."
                    e.message?.contains("invalid") == true -> 
                        "Invalid credentials."
                    e.message?.contains("blocked") == true || 
                    e.message?.contains("unusual activity") == true -> 
                        "Too many attempts. Try again later."
                    e.message?.contains("network") == true ->
                        "Network error. Check your connection."
                    else -> "Reauthentication failed: ${e.message}"
                }
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            }
    }
}