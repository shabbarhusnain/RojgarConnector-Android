package com.shabbar.rozgarconnector.ui.profile

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.ActivityUneducatedWorkerProfileBinding
import com.shabbar.rozgarconnector.ui.home.ProviderHomeActivity

class UneducatedWorkerProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUneducatedWorkerProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var platformCommission: Double = 10.0 // Default 10%

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUneducatedWorkerProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fetchCommissionRate()
        setupSpinners()
        setupListeners()

        binding.btnSaveProfile.setOnClickListener { validateAndSave() }
    }

    private fun fetchCommissionRate() {
        db.collection("settings").document("rates").get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    platformCommission = doc.getDouble("commission") ?: 10.0
                }
            }
    }

    private fun setupListeners() {
        // Experience logic (Hide/Show Last Place)
        binding.etExperienceYears.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val yearsStr = binding.etExperienceYears.text.toString().trim()
                if (yearsStr.isNotEmpty() && yearsStr.toInt() > 0) {
                    binding.etLastWorkPlace.visibility = View.VISIBLE
                } else {
                    binding.etLastWorkPlace.visibility = View.GONE
                }
            }
        }

        // Live Commission Calculation
        binding.etDailyRate.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calculateNetIncome(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun calculateNetIncome(rateStr: String) {
        if (rateStr.isEmpty()) {
            binding.tvCommissionInfo.text = "Platform fee ($platformCommission%) will be deducted."
            return
        }

        try {
            val rate = rateStr.toDouble()
            val commissionAmount = (rate * platformCommission) / 100
            val netIncome = rate - commissionAmount
            
            binding.tvCommissionInfo.text = String.format(
                "Fee: Rs. %.0f | You will receive: Rs. %.0f",
                commissionAmount, netIncome
            )
        } catch (e: Exception) {
            binding.tvCommissionInfo.text = "Enter valid rate."
        }
    }

    private fun setupSpinners() {
        val skills = resources.getStringArray(R.array.skill_categories).toMutableList()
        if (!skills.contains("Other")) skills.add("Other")
        
        val skillAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, skills)
        binding.spinnerSkill.adapter = skillAdapter

        binding.spinnerSkill.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (skills[position] == "Other") {
                    binding.etCustomSkill.visibility = View.VISIBLE
                } else {
                    binding.etCustomSkill.visibility = View.GONE
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun validateAndSave() {
        val rate = binding.etDailyRate.text.toString().trim()
        val expYears = binding.etExperienceYears.text.toString().trim()
        val lastPlace = binding.etLastWorkPlace.text.toString().trim()
        val description = binding.etProfessionalDescription.text.toString().trim()
        val commitment = binding.cbTerms.isChecked

        val finalSkill = if (binding.spinnerSkill.selectedItem.toString() == "Other") {
            binding.etCustomSkill.text.toString().trim()
        } else {
            binding.spinnerSkill.selectedItem.toString()
        }

        if (finalSkill.isEmpty() || rate.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields!", Toast.LENGTH_SHORT).show()
            return
        }

        if (!commitment) {
            Toast.makeText(this, "Please agree to the work guarantee!", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSaveProfile.isEnabled = false
        binding.btnSaveProfile.text = "SAVING..."

        saveToFirestore(finalSkill, rate, expYears, lastPlace, description)
    }

    private fun saveToFirestore(skill: String, rate: String, exp: String, place: String, desc: String) {
        val uid = auth.currentUser?.uid ?: return
        
        val profile = mapOf(
            "professionalSkill" to skill,
            "dailyRate" to rate,
            "platformCommission" to platformCommission, // Save snapshot of commission at time of profile
            "experienceYears" to exp,
            "lastWorkPlace" to place,
            "professionalDescription" to desc,
            "profileCompleted" to true,
            "role" to "Worker",
            "workerType" to "uneducated"
        )

        db.collection("users").document(uid).update(profile)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile Saved Successfully!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, ProviderHomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
            .addOnFailureListener { e ->
                binding.btnSaveProfile.isEnabled = true
                binding.btnSaveProfile.text = "SAVE PROFILE"
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}