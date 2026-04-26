package com.shabbar.rozgarconnector.ui.profile

import android.content.Intent
import android.os.Bundle
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
        
        binding.btnSaveProfile.setOnClickListener { validateAndSave() }
    }

    private fun setupSpinners() {
        val skills = resources.getStringArray(R.array.uneducated_skill_categories).toMutableList()
        val skillAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, skills)
        binding.spinnerSkill.adapter = skillAdapter
    }

    private fun validateAndSave() {
        val expYears = binding.etExperienceYears.text.toString().trim()
        val description = binding.etProfessionalDescription.text.toString().trim()
        val commitment = binding.cbTerms.isChecked
        val hasTools = binding.rbToolsYes.isChecked

        val finalSkill = binding.spinnerSkill.selectedItem.toString()

        if (finalSkill == "Select Skill Category" || description.isEmpty()) {
            Toast.makeText(this, "Please provide your skill and bio!", Toast.LENGTH_SHORT).show()
            return
        }

        if (!commitment) {
            Toast.makeText(this, "Please agree to the accuracy guarantee!", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSaveProfile.isEnabled = false
        binding.btnSaveProfile.text = "SAVING..."

        saveToFirestore(finalSkill, expYears, hasTools, description)
    }

    private fun saveToFirestore(skill: String, exp: String, tools: Boolean, desc: String) {
        val uid = auth.currentUser?.uid ?: return
        
        val profile = mapOf(
            "professionalSkill" to skill,
            "experienceYears" to exp,
            "hasOwnTools" to tools,
            "professionalDescription" to desc,
            "profileCompleted" to true,
            "role" to "provider",
            "workerType" to "uneducated"
        )

        db.collection("users").document(uid).update(profile)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile Saved Successfully!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, ProviderHomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { exception ->
                binding.btnSaveProfile.isEnabled = true
                binding.btnSaveProfile.text = "FINISH & SAVE"
                Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}