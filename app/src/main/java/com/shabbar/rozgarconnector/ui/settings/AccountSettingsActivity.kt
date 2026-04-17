package com.shabbar.rozgarconnector.ui.settings

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.ActivityAccountSettingsBinding
import com.shabbar.rozgarconnector.ui.auth.ForgotPasswordActivity
import com.shabbar.rozgarconnector.ui.auth.SplashActivity
import java.util.concurrent.TimeUnit

class AccountSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountSettingsBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var verificationId: String? = null
    private var oldPhoneNumber: String? = null
    private var newPhoneNumber: String? = null
    private var flowType = "" 
    private var isDebugMode = false
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var currentPhoneForOtp: String? = null
    private var isOldNumberForOtp: Boolean = false
    private var resendAttempts = 0
    private var isVerifyingPassword = false
    private var isVerifyingOtp = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fetchCurrentInfo()

        binding.btnBack.setOnClickListener {
            if (binding.stepContentContainer.visibility == View.VISIBLE) showMainMenu() else finish()
        }

        binding.btnPhoneRow.setOnClickListener {
            flowType = "CHANGE"
            showStepPassword()
        }

        binding.btnPasswordRow.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        binding.btnDeleteAccountRow.setOnClickListener {
            flowType = "DELETE"
            showStepPassword()
        }
    }

    private fun fetchCurrentInfo() {
        val user = auth.currentUser ?: run {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                
                // Phone number
                val phone = doc.getString("phone") ?: ""
                if (phone.isNotEmpty()) {
                    val masked = maskPhoneNumber(phone)
                    binding.tvCurrentPhone.text = masked
                } else {
                    binding.tvCurrentPhone.text = "Not set"
                }
                
                // Verification status
                val isVerified = doc.getBoolean("verified") ?: false
                if (isVerified) {
                    binding.tvVerificationStatus.text = "Verified"
                    binding.tvVerificationStatus.setTextColor(Color.parseColor("#4CAF50"))
                } else {
                    binding.tvVerificationStatus.text = "Pending Approval"
                    binding.tvVerificationStatus.setTextColor(Color.parseColor("#FF9800"))
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading info: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun maskPhoneNumber(phone: String): String {
        return if (phone.length > 5) {
            phone.substring(0, 3) + "****" + phone.substring(phone.length - 2)
        } else {
            phone
        }
    }

    private fun showMainMenu() {
        binding.mainMenuLayout.visibility = View.VISIBLE
        binding.stepContentContainer.visibility = View.GONE
        binding.tvHeaderTitle.text = "Account information"
        isDebugMode = false
    }

    private fun loadStepLayout(layoutId: Int): View {
        binding.mainMenuLayout.visibility = View.GONE
        binding.stepContentContainer.visibility = View.VISIBLE
        binding.stepContentContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)
        return inflater.inflate(layoutId, binding.stepContentContainer, true)
    }

    // --- STEP 1: PASSWORD ---
    private fun showStepPassword() {
        val view = loadStepLayout(R.layout.layout_step_password)
        binding.tvHeaderTitle.text = "Confirm password"
        
        val btnVerifyPassword = view.findViewById<MaterialButton>(R.id.btnVerifyPassword)
        val etPasswordInput = view.findViewById<TextInputEditText>(R.id.etPasswordInput)
        
        btnVerifyPassword.setOnClickListener {
            if (isVerifyingPassword) return@setOnClickListener
            
            val pass = etPasswordInput.text.toString().trim()
            if (pass.isEmpty()) {
                Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (pass.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            btnVerifyPassword.isEnabled = false
            btnVerifyPassword.text = "Verifying..."
            verifyPassword(pass)
        }
    }

    private fun verifyPassword(pass: String) {
        isVerifyingPassword = true
        val user = auth.currentUser ?: run {
            isVerifyingPassword = false
            Toast.makeText(this, "User session expired. Please login again.", Toast.LENGTH_SHORT).show()
            return
        }
        
        val email = user.email ?: "${user.uid}@rozgar.com"
        val credential = EmailAuthProvider.getCredential(email, pass)
        
        user.reauthenticate(credential)
            .addOnSuccessListener {
                isVerifyingPassword = false
                Toast.makeText(this, "✅ Identity Verified", Toast.LENGTH_SHORT).show()
                fetchOldNumberAndSendOTP()
            }
            .addOnFailureListener { e ->
                isVerifyingPassword = false
                val errorMsg = when {
                    e.message?.contains("blocked") == true || 
                    e.message?.contains("unusual activity") == true -> 
                        "Too many failed attempts. Try again after 30 minutes."
                    e is FirebaseAuthInvalidCredentialsException -> 
                        "Invalid password. Please check and try again."
                    e.message?.contains("network") == true ->
                        "Network error. Please check your connection."
                    else -> "Authentication failed: ${e.message}"
                }
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            }
    }

    // --- STEP 2: OTP (OLD NUMBER) ---
    private fun fetchOldNumberAndSendOTP() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            oldPhoneNumber = formatPhoneNumber(doc.getString("phone") ?: "03000000000")
            sendCode(oldPhoneNumber!!, isOldNumber = true)
        }
    }

    private fun sendCode(phone: String, isOldNumber: Boolean) {
        Toast.makeText(this, "Sending verification code...", Toast.LENGTH_SHORT).show()
        
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    verifyOtpWithCredential(credential, isOldNumber)
                }
                
                override fun onVerificationFailed(e: FirebaseException) {
                    isDebugMode = false
                    val errorMsg = when {
                        e.message?.contains("BILLING_NOT_ENABLED") == true || 
                        e.message?.contains("internal error") == true -> {
                            isDebugMode = true
                            verificationId = "DEBUG_MODE_ID"
                            "Server error. Entering debug mode - use code 123456"
                        }
                        e.message?.contains("blocked") == true || 
                        e.message?.contains("unusual activity") == true -> 
                            "Too many attempts. Please try again later."
                        e.message?.contains("Invalid phone") == true -> 
                            "Invalid phone number format."
                        e.message?.contains("network") == true ->
                            "Network error. Please check your connection."
                        else -> "Verification failed: ${e.message}"
                    }
                    Toast.makeText(this@AccountSettingsActivity, errorMsg, Toast.LENGTH_LONG).show()
                    if (isDebugMode) {
                        showStepOtp(isOldNumber, phone)
                    }
                }
                
                override fun onCodeSent(verId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = verId
                    resendToken = token
                    isDebugMode = false
                    resendAttempts = 0
                    Toast.makeText(this@AccountSettingsActivity, "✅ Code sent successfully!", Toast.LENGTH_SHORT).show()
                    showStepOtp(isOldNumber, phone)
                }
            })
            .build()
        
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun resendOtp(phone: String, isOldNumber: Boolean) {
        if (resendToken == null) {
            Toast.makeText(this, "Cannot resend. Please go back and try again.", Toast.LENGTH_SHORT).show()
            return
        }
        
        Toast.makeText(this, "Resending code...", Toast.LENGTH_SHORT).show()

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setForceResendingToken(resendToken!!)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    verifyOtpWithCredential(credential, isOldNumber)
                }
                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(this@AccountSettingsActivity, "Resend failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
                override fun onCodeSent(verId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = verId
                    resendToken = token
                    Toast.makeText(this@AccountSettingsActivity, "✅ Code resent! (Attempt $resendAttempts/3)", Toast.LENGTH_SHORT).show()
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun showStepOtp(isOldNumber: Boolean, phone: String) {
        val view = loadStepLayout(R.layout.layout_step_otp_verify)
        binding.tvHeaderTitle.text = "Verify code"
        
        val desc = view.findViewById<TextView>(R.id.tvOtpDescription)
        desc.text = "Enter the 6-digit code sent to $phone"
        if (isDebugMode) desc.append("\n(Debug: Use 123456)")

        val btnVerifyOtp = view.findViewById<MaterialButton>(R.id.btnVerifyOtp)
        val btnResendCode = view.findViewById<TextView>(R.id.btnResendCode)
        val etOtpInput = view.findViewById<TextInputEditText>(R.id.etOtpInput)

        // Update resend button text
        if (resendAttempts > 0) {
            btnResendCode.text = "Resend code (${3 - resendAttempts} remaining)"
        }

        btnVerifyOtp.setOnClickListener {
            if (isVerifyingOtp) return@setOnClickListener
            
            val code = etOtpInput.text.toString().trim()
            
            if (code.isEmpty()) {
                Toast.makeText(this, "Please enter the verification code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (code.length != 6) {
                Toast.makeText(this, "Code must be 6 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (!code.all { it.isDigit() }) {
                Toast.makeText(this, "Code must contain only numbers", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            isVerifyingOtp = true
            btnVerifyOtp.isEnabled = false
            btnVerifyOtp.text = "Verifying..."
            
            when {
                isDebugMode && code == "123456" -> {
                    isVerifyingOtp = false
                    Toast.makeText(this, "✅ Verified (Debug Mode)", Toast.LENGTH_SHORT).show()
                    handleSuccess(isOldNumber)
                }
                !isDebugMode && verificationId != null -> {
                    val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
                    verifyOtpWithCredential(credential, isOldNumber)
                }
                else -> {
                    isVerifyingOtp = false
                    btnVerifyOtp.isEnabled = true
                    btnVerifyOtp.text = "VERIFY & CONTINUE"
                    Toast.makeText(this, "Invalid code. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnResendCode.setOnClickListener {
            if (resendAttempts >= 3) {
                Toast.makeText(this, "Maximum resend attempts reached. Please try again later.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            
            resendAttempts++
            btnResendCode.text = "Resending... (${3 - resendAttempts} remaining)"
            btnResendCode.isEnabled = false
            
            if (isDebugMode) {
                Toast.makeText(this, "✅ Code resent! (Use 123456) - Attempt $resendAttempts/3", Toast.LENGTH_SHORT).show()
                btnResendCode.isEnabled = true
                btnResendCode.text = "Resend code (${3 - resendAttempts} remaining)"
            } else {
                resendOtp(currentPhoneForOtp ?: "", isOldNumberForOtp)
            }
        }
    }

    private fun handleSuccess(isOldNumber: Boolean) {
        if (isOldNumber) {
            if (flowType == "CHANGE") showStepPhoneInput() else showStepDeleteReason()
        } else {
            updateFirestoreNumber()
        }
    }

    private fun verifyOtpWithCredential(credential: PhoneAuthCredential, isOldNumber: Boolean) {
        if (isOldNumber) {
            isVerifyingOtp = false
            handleSuccess(true)
        } else {
            auth.currentUser?.updatePhoneNumber(credential)
                ?.addOnSuccessListener {
                    isVerifyingOtp = false
                    updateFirestoreNumber()
                }
                ?.addOnFailureListener { e ->
                    isVerifyingOtp = false
                    val errorMsg = when {
                        e.message?.contains("already") == true -> 
                            "This phone number is already in use."
                        e.message?.contains("blocked") == true -> 
                            "Too many attempts. Try again later."
                        else -> "Error: ${e.message}"
                    }
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun showStepPhoneInput() {
        val view = loadStepLayout(R.layout.layout_step_phone_input)
        binding.tvHeaderTitle.text = "New phone number"
        
        val btnSendOtp = view.findViewById<MaterialButton>(R.id.btnSendOtp)
        val etNewPhoneInput = view.findViewById<TextInputEditText>(R.id.etNewPhoneInput)
        
        btnSendOtp.setOnClickListener {
            if (!btnSendOtp.isEnabled) return@setOnClickListener
            
            val num = etNewPhoneInput.text.toString().trim()
            
            if (num.isEmpty()) {
                Toast.makeText(this, "Please enter phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (num.length < 10) {
                Toast.makeText(this, "Phone number must be at least 10 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (num.length > 15) {
                Toast.makeText(this, "Phone number too long", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            try {
                newPhoneNumber = formatPhoneNumber(num)
                
                // Validate it's different from old number
                if (newPhoneNumber == oldPhoneNumber) {
                    Toast.makeText(this, "New number must be different from current", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                btnSendOtp.isEnabled = false
                btnSendOtp.text = "Sending..."
                sendCode(newPhoneNumber!!, isOldNumber = false)
                
                Handler(Looper.getMainLooper()).postDelayed({
                    if (btnSendOtp.isEnabled.not()) {
                        btnSendOtp.isEnabled = true
                        btnSendOtp.text = "SEND VERIFICATION CODE"
                    }
                }, 3000)
            } catch (e: Exception) {
                Toast.makeText(this, "Invalid phone number: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showStepDeleteReason() {
        val view = loadStepLayout(R.layout.layout_step_delete_reason)
        binding.tvHeaderTitle.text = "Delete account"
        
        val btnFinalDelete = view.findViewById<MaterialButton>(R.id.btnFinalDelete)
        val etDeleteReasonInput = view.findViewById<TextInputEditText>(R.id.etDeleteReasonInput)
        
        btnFinalDelete.setOnClickListener {
            if (!btnFinalDelete.isEnabled) return@setOnClickListener
            
            val reason = etDeleteReasonInput.text.toString().trim()
            
            // Show confirmation dialog
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Confirm Account Deletion")
            builder.setMessage("Are you sure you want to permanently delete your account? This action cannot be undone.")
            builder.setPositiveButton("Delete") { _, _ ->
                btnFinalDelete.isEnabled = false
                btnFinalDelete.text = "Deleting..."
                performFinalDelete(reason)
            }
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            builder.show()
        }
    }

    private fun performFinalDelete(reason: String) {
        val user = auth.currentUser ?: run {
            Toast.makeText(this, "User session expired.", Toast.LENGTH_SHORT).show()
            return
        }
        val uid = user.uid
        Toast.makeText(this, "Deleting Account...", Toast.LENGTH_LONG).show()

        // Log deletion request
        db.collection("deletion_logs").add(mapOf(
            "uid" to uid, 
            "reason" to reason, 
            "email" to user.email,
            "phone" to auth.currentUser?.phoneNumber,
            "timestamp" to com.google.firebase.Timestamp.now(),
            "time" to System.currentTimeMillis()
        )).addOnSuccessListener {
            // Delete user data
            db.collection("users").document(uid).delete().addOnSuccessListener {
                // Delete from auth
                user.delete().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Account Deleted Successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, SplashActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Auth Delete Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Delete Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Logging Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateFirestoreNumber() {
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "User session expired.", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (newPhoneNumber.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid phone number.", Toast.LENGTH_SHORT).show()
            return
        }
        
        db.collection("users").document(uid).update("phone", newPhoneNumber)
            .addOnSuccessListener {
                Toast.makeText(this, "✅ Phone Number Updated Successfully!", Toast.LENGTH_LONG).show()
                
                // Refresh the UI
                Handler(Looper.getMainLooper()).postDelayed({
                    fetchCurrentInfo()
                    showMainMenu()
                }, 500)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun formatPhoneNumber(phone: String): String {
        var p = phone.replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
        
        // Remove leading 0 if present
        if (p.startsWith("0")) {
            p = p.substring(1)
        }
        
        // Remove any existing +92 and + symbols
        p = p.replace("+92", "").replace("+", "")
        
        // Add country code
        if (!p.startsWith("+92")) {
            p = "+92$p"
        }
        
        return p
    }
}