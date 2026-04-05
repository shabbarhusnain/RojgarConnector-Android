package com.shabbar.rozgarconnector.ui.profile

import android.content.Intent
import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUneducatedWorkerProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinners()
        setupListeners()

        binding.btnSaveProfile.setOnClickListener { validateAndSave() }
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
            "experienceYears" to exp,
            "lastWorkPlace" to place,
            "professionalDescription" to desc,
            "profileCompleted" to true,
            "role" to "Worker",
            "workerType" to "uneducated"
            // Logical Fix: Removed "isVerified" to false. User is already verified by admin.
        )

        db.collection("users").document(uid).update(profile)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile Saved Successfully!", Toast.LENGTH_SHORT).show()
                // Logical Fix: Go to Home Activity instead of Pending screen
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