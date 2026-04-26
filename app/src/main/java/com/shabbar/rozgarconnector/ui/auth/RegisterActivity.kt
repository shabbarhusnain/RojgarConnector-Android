package com.shabbar.rozgarconnector.ui.auth

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.ActivityRegisterBinding
import com.shabbar.rozgarconnector.ui.role.RoleSelectionActivity
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.TimeUnit

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var dpUri: Uri? = null
    private var cnicFrontUri: Uri? = null
    private var cnicBackUri: Uri? = null
    
    private var verificationId: String? = null
    private var otpDialog: AlertDialog? = null
    private var pendingData: RegistrationData? = null

    data class RegistrationData(
        val name: String, val fatherName: String, val cnic: String,
        val phone: String, val dob: String, val pass: String,
        val address: String, val city: String, val district: String
    )

    private val pickDp = registerForActivityResult(ActivityResultContracts.GetContent()) { dpUri = it; if (it != null) binding.btnUploadDP.text = "DP Ready ✅" }
    private val pickFront = registerForActivityResult(ActivityResultContracts.GetContent()) { cnicFrontUri = it; if (it != null) binding.btnUploadCnicFront.text = "Front Ready ✅" }
    private val pickBack = registerForActivityResult(ActivityResultContracts.GetContent()) { cnicBackUri = it; if (it != null) binding.btnUploadCnicBack.text = "Back Ready ✅" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDistrictSpinner()
        binding.tvDateOfBirth.setOnClickListener { showDatePicker() }
        binding.btnUploadDP.setOnClickListener { pickDp.launch("image/*") }
        binding.btnUploadCnicFront.setOnClickListener { pickFront.launch("image/*") }
        binding.btnUploadCnicBack.setOnClickListener { pickBack.launch("image/*") }
        binding.btnRegister.setOnClickListener { validateAndStartAuth() }
    }

    private fun setupDistrictSpinner() {
        val districts = resources.getStringArray(R.array.pakistan_districts)
        binding.spinnerDistrict.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, districts)
    }

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d -> binding.tvDateOfBirth.text = "$d/${m+1}/$y" },
            c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun validateAndStartAuth() {
        val name = binding.etFullName.text.toString().trim()
        val cnic = binding.etCNIC.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val pass = binding.etPassword.text.toString().trim()
        val confirmPass = binding.etConfirmPassword.text.toString().trim()

        if (name.isEmpty() || cnic.length != 13 || phone.length < 10 || dpUri == null || cnicFrontUri == null || cnicBackUri == null) {
            Toast.makeText(this, "Tamam maloomat aur images lazmi hain!", Toast.LENGTH_SHORT).show()
            return
        }

        if (pass.length < 6 || pass != confirmPass) {
            Toast.makeText(this, "Passwords mismatch!", Toast.LENGTH_SHORT).show()
            return
        }

        pendingData = RegistrationData(name, binding.etFatherName.text.toString().trim(), cnic, phone, binding.tvDateOfBirth.text.toString(), pass, binding.etPermanentAddress.text.toString().trim(), binding.etCity.text.toString().trim(), binding.spinnerDistrict.selectedItem.toString())
        sendOtp(formatPhoneNumber(phone))
    }

    private fun sendOtp(phoneNumber: String) {
        binding.btnRegister.isEnabled = false
        binding.btnRegister.text = "Sending OTP..."

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber).setTimeout(60L, TimeUnit.SECONDS).setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) { completeRegistration(credential) }
                override fun onVerificationFailed(e: FirebaseException) {
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "Register"
                    Toast.makeText(this@RegisterActivity, "OTP Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
                override fun onCodeSent(verId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = verId
                    showOtpDialog(phoneNumber)
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "Register"
                }
            }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun showOtpDialog(phone: String) {
        val view = LayoutInflater.from(this).inflate(R.layout.layout_step_otp_verify, null)
        val etOtp = view.findViewById<TextInputEditText>(R.id.etOtpInput)
        val btnVerify = view.findViewById<MaterialButton>(R.id.btnVerifyOtp)
        view.findViewById<TextView>(R.id.tvOtpDescription).text = "Verify $phone"

        otpDialog = AlertDialog.Builder(this).setView(view).setCancelable(true).create()
        btnVerify.setOnClickListener {
            val code = etOtp.text.toString().trim()
            if (code.length == 6) {
                btnVerify.isEnabled = false
                btnVerify.text = "Verifying..."
                val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
                completeRegistration(credential, btnVerify)
            } else {
                Toast.makeText(this, "Enter 6-digit code", Toast.LENGTH_SHORT).show()
            }
        }
        otpDialog?.show()
    }

    private fun completeRegistration(phoneCredential: PhoneAuthCredential, btnVerify: MaterialButton? = null) {
        val data = pendingData ?: return
        val email = "${data.cnic}@rozgar.com"
        
        auth.signInWithCredential(phoneCredential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = task.result?.user
                val emailCredential = EmailAuthProvider.getCredential(email, data.pass)
                
                user?.linkWithCredential(emailCredential)?.addOnCompleteListener { linkTask ->
                    if (linkTask.isSuccessful) {
                        saveToFirestore(user.uid, data)
                    } else {
                        btnVerify?.isEnabled = true
                        btnVerify?.text = "Verify OTP"
                        Toast.makeText(this, "Link Error: ${linkTask.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                btnVerify?.isEnabled = true
                btnVerify?.text = "Verify OTP"
                Toast.makeText(this, "Wrong OTP! Please try again.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveToFirestore(uid: String, data: RegistrationData) {
        val userMap = hashMapOf(
            "uid" to uid, 
            "fullName" to data.name, 
            "fatherName" to data.fatherName,
            "cnic" to data.cnic, 
            "phone" to data.phone, 
            "dob" to data.dob,
            "permanentAddress" to data.address, 
            "city" to data.city, 
            "district" to data.district,
            "dpBase64" to uriToBase64(dpUri!!), 
            "cnicFrontBase64" to uriToBase64(cnicFrontUri!!),
            "cnicBackBase64" to uriToBase64(cnicBackUri!!), 
            "isVerified" to false,
            "role" to "pending"
        )
        db.collection("users").document(uid).set(userMap).addOnSuccessListener {
            otpDialog?.dismiss()
            Toast.makeText(this, "✅ Registration Successful!", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, RoleSelectionActivity::class.java))
            finish()
        }
    }

    private fun uriToBase64(uri: Uri): String {
        return try {
            val stream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(stream)
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 20, out)
            Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) { "" }
    }

    private fun formatPhoneNumber(phone: String): String {
        var p = phone.replace(" ", "").replace("-", "")
        if (p.startsWith("0")) p = "+92" + p.substring(1)
        if (!p.startsWith("+")) p = "+92$p"
        return p
    }
}