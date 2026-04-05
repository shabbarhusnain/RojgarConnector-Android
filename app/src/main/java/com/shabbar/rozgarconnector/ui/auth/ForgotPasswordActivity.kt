package com.shabbar.rozgarconnector.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.databinding.ActivityForgotPasswordBinding
import java.util.concurrent.TimeUnit

/**
 * Handles the password reset flow using Phone and OTP verification.
 * This is a two-step process: first find the account via CNIC, then verify ownership via OTP.
 */
class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

    // Firebase instances
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Stores the verification ID from the OTP sending process.
    private var verificationId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // The main button's function changes based on the current state of the flow.
        binding.btnNextAction.setOnClickListener {
            val input = binding.etForgotCNIC.text.toString().trim()

            if (binding.btnNextAction.text == "FIND ACCOUNT") {
                // Step 1: Find the user's account using their CNIC.
                if (input.length == 13) {
                    verifyCnicAndInitiateOtp(input)
                } else {
                    binding.etForgotCNIC.error = "CNIC must be 13 digits"
                }
            } else if (binding.btnNextAction.text == "VERIFY OTP") {
                // Step 2: Verify the OTP code entered by the user.
                if (input.length == 6) {
                    signInWithPhoneAuthCredential(input)
                } else {
                    binding.etForgotCNIC.error = "OTP must be 6 digits"
                }
            }
        }

        binding.tvBackToLogin.setOnClickListener { finish() }
    }

    /**
     * Searches for a user in Firestore with the given CNIC and sends an OTP to their phone number.
     * @param cnic The CNIC number to search for.
     */
    private fun verifyCnicAndInitiateOtp(cnic: String) {
        binding.btnNextAction.isEnabled = false
        binding.btnNextAction.text = "SEARCHING..."

        db.collection("users").whereEqualTo("cnic", cnic).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // User found, get their phone number and send OTP.
                    val phone = documents.documents[0].getString("phone") ?: ""
                    val formattedPhone = if (phone.startsWith("0")) "+92${phone.substring(1)}" else phone
                    sendVerificationCode(formattedPhone)
                } else {
                    // No user found with that CNIC.
                    binding.btnNextAction.isEnabled = true
                    binding.btnNextAction.text = "FIND ACCOUNT"
                    Toast.makeText(this, "No account is linked to this CNIC.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                binding.btnNextAction.isEnabled = true
                binding.btnNextAction.text = "FIND ACCOUNT"
                Toast.makeText(this, "Error checking CNIC: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Uses Firebase Phone Authentication to send an OTP to the user's phone.
     * @param phoneNumber The formatted phone number to send the OTP to.
     */
    private fun sendVerificationCode(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = id
                    // Transition the UI to OTP entry mode.
                    binding.tvForgotDescription.text = "We have sent a 6-digit code to $phoneNumber"
                    binding.etForgotCNIC.setText("")
                    binding.etForgotCNIC.hint = "Enter 6-digit OTP"
                    binding.etForgotCNIC.filters = arrayOf(android.text.InputFilter.LengthFilter(6))
                    binding.btnNextAction.text = "VERIFY OTP"
                    binding.btnNextAction.isEnabled = true
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    binding.btnNextAction.isEnabled = true
                    binding.btnNextAction.text = "FIND ACCOUNT"
                    Toast.makeText(this@ForgotPasswordActivity, "Verification failed: ${e.message}", Toast.LENGTH_LONG).show()
                }

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // This is called when OTP is auto-detected. We can pre-fill the field.
                    binding.etForgotCNIC.setText(credential.smsCode)
                }
            }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    /**
     * Signs the user in using the verification ID and the OTP code.
     * This proves ownership of the account and allows them to proceed to reset their password.
     * @param code The 6-digit OTP code entered by the user.
     */
    private fun signInWithPhoneAuthCredential(code: String) {
        binding.btnNextAction.isEnabled = false
        binding.btnNextAction.text = "VERIFYING..."

        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                Toast.makeText(this, "OTP Verified Successfully!", Toast.LENGTH_SHORT).show()
                // Navigate to the screen where they can set a new password.
                val intent = Intent(this, ResetPasswordActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                binding.btnNextAction.isEnabled = true
                binding.btnNextAction.text = "VERIFY OTP"
                Toast.makeText(this, "Invalid OTP. Please try again.", Toast.LENGTH_SHORT).show()
            }
    }
}