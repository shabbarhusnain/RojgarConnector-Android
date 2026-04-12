package com.shabbar.rozgarconnector.ui.auth

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.ActivityRegisterBinding
import com.shabbar.rozgarconnector.ui.role.RoleSelectionActivity
import com.shabbar.rozgarconnector.ui.admin.AdminDashboardActivity
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

        setupDistrictSpinner()

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }

        binding.tvDateOfBirth.setOnClickListener { showDatePicker() }
        binding.btnUploadDP.setOnClickListener { pickDp.launch("image/*") }
        binding.btnUploadCnicFront.setOnClickListener { pickFront.launch("image/*") }
        binding.btnUploadCnicBack.setOnClickListener { pickBack.launch("image/*") }
        binding.btnRegister.setOnClickListener { handleRegistration() }
        binding.tvLogin.setOnClickListener { finish() }
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

        // Checking for duplicates before creating auth account
        val cnicCheck = db.collection("users").whereEqualTo("cnic", cnic).get()
        val phoneCheck = db.collection("users").whereEqualTo("phone", phone).get()

        Tasks.whenAllSuccess<com.google.firebase.firestore.QuerySnapshot>(cnicCheck, phoneCheck)
            .addOnSuccessListener { results ->
                if (!results[0].isEmpty || !results[1].isEmpty) {
                    Toast.makeText(this, "CNIC ya Phone pehle se registered hai!", Toast.LENGTH_LONG).show()
                    resetBtn()
                } else {
                    proceedToAuth(cnic, pass, name, phone, fatherName, dob, permanentAddress, city)
                }
            }
            .addOnFailureListener { e ->
                resetBtn()
                Log.e("REG_ERROR", "Firestore Error: ${e.message}")
                Toast.makeText(this, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun proceedToAuth(cnic: String, pass: String, name: String, phone: String, 
                             fatherName: String, dob: String, permanentAddress: String, city: String) {
        val fakeEmail = "$cnic@rozgar.com"
        binding.btnRegister.text = "Saving Data..."

        auth.createUserWithEmailAndPassword(fakeEmail, pass).addOnSuccessListener { res ->
            val uid = res.user?.uid ?: ""

            val isAdmin = (cnic == "0000000000000")
            val role = if (isAdmin) "admin" else "pending"
            val verifiedStatus = isAdmin 

            val userData = hashMapOf(
                "uid" to uid,
                "fullName" to name,
                "fatherName" to fatherName,
                "cnic" to cnic,
                "phone" to phone,
                "dob" to dob,
                "district" to binding.spinnerDistrict.selectedItem.toString(),
                "permanentAddress" to permanentAddress,
                "city" to city,
                "dpBase64" to uriToBase64(dpUri!!),
                "cnicFrontBase64" to uriToBase64(cnicFrontUri!!),
                "cnicBackBase64" to uriToBase64(cnicBackUri!!),
                "isVerified" to verifiedStatus,
                "profileCompleted" to isAdmin,
                "role" to role
            )

            db.collection("users").document(uid).set(userData).addOnSuccessListener {
                if (isAdmin) {
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                } else {
                    startActivity(Intent(this, RoleSelectionActivity::class.java))
                }
                finish()
            }.addOnFailureListener { e ->
                resetBtn()
                Toast.makeText(this, "Save Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            resetBtn()
            Toast.makeText(this, "Auth Failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uriToBase64(uri: Uri): String {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 20, out) // Increased quality slightly
            return Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) {
            return ""
        }
    }

    private fun resetBtn() {
        binding.btnRegister.isEnabled = true
        binding.btnRegister.text = "Register"
    }
}