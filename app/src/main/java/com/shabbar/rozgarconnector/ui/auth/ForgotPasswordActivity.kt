package com.shabbar.rozgarconnector.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.databinding.ActivityForgotPasswordBinding
import java.util.concurrent.TimeUnit

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var verificationId: String? = null
    private var targetCnic: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnSendOtp.setOnClickListener {
            val rawPhone = binding.etPhone.text.toString().trim()
            if (rawPhone.length >= 9) {
                // Normalize for DB search: 0344...
                val dbPhone = if (rawPhone.startsWith("0")) rawPhone else "0$rawPhone"
                findUserAndSendOtp(dbPhone)
            } else {
                Toast.makeText(this, "Enter valid phone number", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnVerifyOtp.setOnClickListener {
            val code = binding.etOtp.text.toString().trim()
            if (code.length == 6 && verificationId != null) verifyOtp(code)
        }

        binding.btnResetPassword.setOnClickListener {
            val newPass = binding.etNewPassword.text.toString().trim()
            val confirmPass = binding.etConfirmPassword.text.toString().trim()
            if (newPass.length >= 6 && newPass == confirmPass) finalizeResetAndLink(newPass)
            else Toast.makeText(this, "Passwords mismatch or too short", Toast.LENGTH_SHORT).show()
        }
    }

    private fun findUserAndSendOtp(phoneForDb: String) {
        binding.btnSendOtp.isEnabled = false
        binding.btnSendOtp.text = "Searching..."

        db.collection("users").whereEqualTo("phone", phoneForDb).get().addOnSuccessListener { res ->
            if (!res.isEmpty) {
                targetCnic = res.documents[0].getString("cnic")
                val internationalPhone = if (phoneForDb.startsWith("0")) "+92${phoneForDb.substring(1)}" else "+92$phoneForDb"
                sendOtp(internationalPhone)
            } else {
                binding.btnSendOtp.isEnabled = true
                binding.btnSendOtp.text = "SEND VERIFICATION CODE"
                Toast.makeText(this, "No user found with this number", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener {
            binding.btnSendOtp.isEnabled = true
            Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendOtp(phone: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone).setTimeout(60L, TimeUnit.SECONDS).setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    verifyOtpWithCredential(credential)
                }
                override fun onVerificationFailed(e: FirebaseException) {
                    binding.btnSendOtp.isEnabled = true
                    Toast.makeText(this@ForgotPasswordActivity, "OTP Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
                override fun onCodeSent(verId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = verId
                    binding.layoutPhoneInput.visibility = View.GONE
                    binding.layoutOtpInput.visibility = View.VISIBLE
                }
            }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyOtp(code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
        verifyOtpWithCredential(credential)
    }

    private fun verifyOtpWithCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                binding.layoutOtpInput.visibility = View.GONE
                binding.layoutResetInput.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun finalizeResetAndLink(newPass: String) {
        val user = auth.currentUser ?: return
        val email = "$targetCnic@rozgar.com"
        val emailCredential = EmailAuthProvider.getCredential(email, newPass)

        // The "Merge" Magic: Try to link the Email account to this Phone session
        user.linkWithCredential(emailCredential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // accounts are now MERGED into one UID
                Toast.makeText(this, "✅ Password Updated & Linked!", Toast.LENGTH_LONG).show()
                auth.signOut()
                finish()
            } else {
                // If linking fails (collision), we update the password directly
                user.updatePassword(newPass).addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        Toast.makeText(this, "✅ Password Updated! Please login.", Toast.LENGTH_LONG).show()
                        auth.signOut()
                        finish()
                    } else {
                        Toast.makeText(this, "Error: ${updateTask.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}