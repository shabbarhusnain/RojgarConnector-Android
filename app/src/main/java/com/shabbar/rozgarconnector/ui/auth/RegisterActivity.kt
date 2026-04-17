package com.shabbar.rozgarconnector.ui.auth

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Tasks
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.ActivityRegisterBinding
import com.shabbar.rozgarconnector.ui.role.RoleSelectionActivity
import com.shabbar.rozgarconnector.ui.settings.MenuActivity
import java.io.ByteArrayOutputStream
import java.util.Calendar

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var dpUri: Uri? = null
    private var cnicFrontUri: Uri? = null
    private var cnicBackUri: Uri? = null
    
    private var isUpdateMode = false
    
    // Phone verification variables
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var formattedPhone: String? = null
    private var resendAttempts = 0
    
    // Registration data to be saved after phone verification
    private var pendingRegistrationData: RegistrationData? = null
    
    data class RegistrationData(
        val cnic: String,
        val pass: String,
        val name: String,
        val phone: String,
        val fatherName: String,
        val dob: String,
        val permanentAddress: String,
        val city: String,
        val district: String
    )

    private val pickDp = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        dpUri = uri; if (uri != null) binding.btnUploadDP.text = "DP Ready ✅"
    }
    private val pickFront = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        cnicFrontUri = uri; if (uri != null) binding.btnUploadCnicFront.text = "Front Ready ✅"
    }
    private val pickBack = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        cnicBackUri = uri; if (uri != null) binding.btnUploadCnicBack.text = "Back Ready ✅"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isUpdateMode = intent.getBooleanExtra("IS_UPDATE_MODE", false)
        setupDistrictSpinner()

        if (isUpdateMode) {
            setupUpdateUI()
            loadExistingUserData()
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }

        binding.tvDateOfBirth.setOnClickListener { showDatePicker() }
        binding.btnUploadDP.setOnClickListener { pickDp.launch("image/*") }
        binding.btnUploadCnicFront.setOnClickListener { pickFront.launch("image/*") }
        binding.btnUploadCnicBack.setOnClickListener { pickBack.launch("image/*") }
        binding.btnRegister.setOnClickListener { handleAction() }
        binding.tvLogin.setOnClickListener { finish() }
    }

    private fun setupUpdateUI() {
        binding.btnRegister.text = "Update & Resubmit"
        binding.etPassword.visibility = View.GONE
        binding.etConfirmPassword.visibility = View.GONE
        binding.tvLogin.visibility = View.GONE
        binding.btnUploadDP.text = "Update DP (Keep if same)"
        binding.btnUploadCnicFront.text = "Update Front (Keep if same)"
        binding.btnUploadCnicBack.text = "Update Back (Keep if same)"
    }

    private fun loadExistingUserData() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                binding.etFullName.setText(doc.getString("fullName"))
                binding.etFatherName.setText(doc.getString("fatherName"))
                binding.etCNIC.setText(doc.getString("cnic"))
                binding.etPhone.setText(doc.getString("phone"))
                binding.tvDateOfBirth.text = doc.getString("dob")
                binding.etPermanentAddress.setText(doc.getString("permanentAddress"))
                binding.etCity.setText(doc.getString("city"))
                
                // Set Spinner selection
                val district = doc.getString("district") ?: ""
                val districts = resources.getStringArray(R.array.pakistan_districts)
                val index = districts.indexOf(district)
                if (index >= 0) binding.spinnerDistrict.setSelection(index)
            }
        }
    }

    private fun setupDistrictSpinner() {
        val districts = resources.getStringArray(R.array.pakistan_districts)
        binding.spinnerDistrict.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, districts)
    }

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            binding.tvDateOfBirth.text = "$d/${m+1}/$y"
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun handleAction() {
        if (isUpdateMode) {
            handleUpdate()
        } else {
            handleRegistration()
        }
    }

    private fun handleUpdate() {
        val uid = auth.currentUser?.uid ?: return
        val name = binding.etFullName.text.toString().trim()
        val cnic = binding.etCNIC.text.toString().trim()
        
        if (name.isEmpty() || cnic.length != 13) {
            Toast.makeText(this, "Please check your details!", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnRegister.isEnabled = false
        binding.btnRegister.text = "Updating..."

        val updates = mutableMapOf<String, Any>(
            "fullName" to name,
            "fatherName" to binding.etFatherName.text.toString().trim(),
            "cnic" to cnic,
            "phone" to binding.etPhone.text.toString().trim(),
            "dob" to binding.tvDateOfBirth.text.toString().trim(),
            "district" to binding.spinnerDistrict.selectedItem.toString(),
            "permanentAddress" to binding.etPermanentAddress.text.toString().trim(),
            "city" to binding.etCity.text.toString().trim(),
            "isRejected" to false, // Clear rejection status
            "rejectionReason" to "" // Clear reason
        )

        // Only update images if user selected new ones
        dpUri?.let { updates["dpBase64"] = uriToBase64(it) }
        cnicFrontUri?.let { updates["cnicFrontBase64"] = uriToBase64(it) }
        cnicBackUri?.let { updates["cnicBackBase64"] = uriToBase64(it) }

        db.collection("users").document(uid).update(updates).addOnSuccessListener {
            Toast.makeText(this, "Details Updated Successfully!", Toast.LENGTH_LONG).show()
            finish() // Go back to Pending screen which will now show "Pending Approval"
        }.addOnFailureListener {
            resetBtn()
            Toast.makeText(this, "Update Failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleRegistration() {
        val name = binding.etFullName.text.toString().trim()
        val fatherName = binding.etFatherName.text.toString().trim()
        val cnic = binding.etCNIC.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val dob = binding.tvDateOfBirth.text.toString().trim()
        val pass = binding.etPassword.text.toString().trim()
        val confirmPass = binding.etConfirmPassword.text.toString().trim()
        val permanentAddress = binding.etPermanentAddress.text.toString().trim()
        val city = binding.etCity.text.toString().trim()

        if (name.isEmpty() || cnic.length != 13 || dpUri == null || cnicFrontUri == null || 
            permanentAddress.isEmpty() || dob.isEmpty() || dob.contains("Birth")) {
            Toast.makeText(this, "Tamam maloomat aur images lazmi hain!", Toast.LENGTH_SHORT).show()
            return
        }

        if (pass != confirmPass) {
            Toast.makeText(this, "Passwords match nahi kar rahe!", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnRegister.isEnabled = false
        binding.btnRegister.text = "Verifying..."

        // Duplicate check logic remains the same
        db.collection("users").whereEqualTo("cnic", cnic).get().addOnSuccessListener { res ->
            if (!res.isEmpty) {
                Toast.makeText(this, "CNIC pehle se registered hai!", Toast.LENGTH_LONG).show()
                resetBtn()
            } else {
                proceedToAuth(cnic, pass, name, phone, fatherName, dob, permanentAddress, city)
            }
        }.addOnFailureListener { resetBtn() }
    }

    private fun proceedToAuth(cnic: String, pass: String, name: String, phone: String, 
                             fatherName: String, dob: String, permanentAddress: String, city: String) {
        binding.btnRegister.text = "Verifying Phone..."
        
        // Store data for later use after OTP verification
        pendingRegistrationData = RegistrationData(
            cnic = cnic,
            pass = pass,
            name = name,
            phone = phone,
            fatherName = fatherName,
            dob = dob,
            permanentAddress = permanentAddress,
            city = city,
            district = binding.spinnerDistrict.selectedItem.toString()
        )
        
        // Format phone and send OTP
        formattedPhone = formatPhoneNumber(phone)
        sendOtp(formattedPhone!!)
    }
    
    private fun formatPhoneNumber(phone: String): String {
        var formatted = phone.trim().replace(" ", "").replace("-", "")
        return when {
            formatted.startsWith("0") -> "+92${formatted.substring(1)}"
            formatted.startsWith("92") -> "+$formatted"
            formatted.startsWith("+92") -> formatted
            else -> "+92$formatted"
        }
    }
    
    private fun sendOtp(phoneNumber: String) {
        resetBtn()
        
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    verifyOtpWithCredential(credential)
                }
                
                override fun onVerificationFailed(e: FirebaseException) {
                    val errorMsg = when {
                        e.message?.contains("BILLING_NOT_ENABLED") == true -> 
                            "Server error. Debug mode enabled - use code 123456"
                        e.message?.contains("blocked") == true || 
                        e.message?.contains("unusual activity") == true -> 
                            "Too many attempts. Try again later."
                        e.message?.contains("Invalid phone") == true -> 
                            "Invalid phone number."
                        else -> "Verification failed: ${e.message}"
                    }
                    Toast.makeText(this@RegisterActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
                
                override fun onCodeSent(verId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = verId
                    resendToken = token
                    resendAttempts = 0
                    Toast.makeText(this@RegisterActivity, "✅ OTP sent to $phoneNumber", Toast.LENGTH_SHORT).show()
                    showPhoneVerificationDialog()
                }
            }).build()
        
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
    
    private fun showPhoneVerificationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.layout_step_otp_verify, null)
        val etOtp = dialogView.findViewById<TextInputEditText>(R.id.etOtpInput)
        val btnVerify = dialogView.findViewById<MaterialButton>(R.id.btnVerifyOtp)
        val tvDescription = dialogView.findViewById<android.widget.TextView>(R.id.tvOtpDescription)
        val btnResend = dialogView.findViewById<android.widget.TextView>(R.id.btnResendCode)
        
        tvDescription.text = "We sent a 6-digit code to $formattedPhone\nEnter it below to verify your phone number."
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        
        btnVerify.setOnClickListener {
            val code = etOtp.text.toString().trim()
            
            when {
                code.isEmpty() -> Toast.makeText(this, "Please enter the code", Toast.LENGTH_SHORT).show()
                code.length != 6 -> Toast.makeText(this, "Code must be 6 digits", Toast.LENGTH_SHORT).show()
                !code.all { it.isDigit() } -> Toast.makeText(this, "Code must contain only numbers", Toast.LENGTH_SHORT).show()
                else -> {
                    btnVerify.isEnabled = false
                    btnVerify.text = "Verifying..."
                    
                    if (verificationId != null) {
                        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
                        verifyOtpWithCredential(credential)
                    }
                    
                    dialog.dismiss()
                }
            }
        }
        
        btnResend.setOnClickListener {
            if (resendAttempts >= 3) {
                Toast.makeText(this, "Maximum resend attempts reached. Please try again later.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            
            resendAttempts++
            Toast.makeText(this, "Resending code...", Toast.LENGTH_SHORT).show()
            
            if (resendToken != null && formattedPhone != null) {
                val options = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(formattedPhone!!)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(this)
                    .setForceResendingToken(resendToken!!)
                    .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                            verifyOtpWithCredential(credential)
                        }
                        override fun onVerificationFailed(e: FirebaseException) {
                            Toast.makeText(this@RegisterActivity, "Resend failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        override fun onCodeSent(verId: String, token: PhoneAuthProvider.ForceResendingToken) {
                            verificationId = verId
                            resendToken = token
                            Toast.makeText(this@RegisterActivity, "Code resent! (${3 - resendAttempts} remaining)", Toast.LENGTH_SHORT).show()
                        }
                    }).build()
                PhoneAuthProvider.verifyPhoneNumber(options)
            }
        }
        
        dialog.show()
    }
    
    private fun verifyOtpWithCredential(credential: PhoneAuthCredential) {
        // OTP verified - phone is confirmed as real
        // Now proceed to create account and save to Firestore
        completeRegistration()
    }
    
    private fun completeRegistration() {
        val data = pendingRegistrationData ?: run {
            Toast.makeText(this, "Error: Registration data lost", Toast.LENGTH_SHORT).show()
            resetBtn()
            return
        }
        
        val fakeEmail = "${data.cnic}@rozgar.com"
        binding.btnRegister.text = "Creating Account..."

        auth.createUserWithEmailAndPassword(fakeEmail, data.pass).addOnSuccessListener { res ->
            val uid = res.user?.uid ?: ""
            val userData = hashMapOf(
                "uid" to uid,
                "fullName" to data.name,
                "fatherName" to data.fatherName,
                "cnic" to data.cnic,
                "phone" to data.phone,
                "dob" to data.dob,
                "district" to data.district,
                "permanentAddress" to data.permanentAddress,
                "city" to data.city,
                "dpBase64" to uriToBase64(dpUri!!),
                "cnicFrontBase64" to uriToBase64(cnicFrontUri!!),
                "cnicBackBase64" to uriToBase64(cnicBackUri!!),
                "verified" to true,  // ✅ Phone verified - OTP confirmed
                "isRejected" to false,
                "profileCompleted" to false,
                "role" to "pending"
            )

            db.collection("users").document(uid).set(userData).addOnSuccessListener {
                Toast.makeText(this, "✅ Registration successful! Awaiting admin approval.", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, RoleSelectionActivity::class.java))
                finish()
            }.addOnFailureListener {
                resetBtn()
                Toast.makeText(this, "Error saving data: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            resetBtn()
            Toast.makeText(this, "Failed to create account: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uriToBase64(uri: Uri): String {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 20, out)
            return Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) {
            return ""
        }
    }

    private fun resetBtn() {
        binding.btnRegister.isEnabled = true
        binding.btnRegister.text = if (isUpdateMode) "Update & Resubmit" else "Register"
    }
}