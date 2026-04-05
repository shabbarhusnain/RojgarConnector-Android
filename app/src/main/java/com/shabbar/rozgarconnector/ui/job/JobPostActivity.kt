package com.shabbar.rozgarconnector.ui.job

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.ActivityJobPostBinding

class JobPostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJobPostBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJobPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinners()
        
        binding.rgWorkerType.setOnCheckedChangeListener { _, _ ->
            updateCategorySpinner()
        }
        
        updateCategorySpinner()

        binding.btnPostJob.setOnClickListener { 
            checkSeekerVerificationAndPost()
        }
    }

    private fun setupSpinners() {
        binding.spinnerWorkplaceType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, resources.getStringArray(R.array.workplace_types))
        binding.spinnerDistrict.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, resources.getStringArray(R.array.pakistan_districts))
        binding.spinnerDurationUnit.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, resources.getStringArray(R.array.duration_units))
        binding.spinnerPayUnit.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, resources.getStringArray(R.array.pay_units))
    }

    private fun updateCategorySpinner() {
        val isEducated = binding.rbEducated.isChecked
        val categories = if (isEducated) {
            resources.getStringArray(R.array.educated_categories)
        } else {
            resources.getStringArray(R.array.skill_categories)
        }
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        binding.spinnerJobCategory.adapter = adapter
    }

    private fun checkSeekerVerificationAndPost() {
        val uid = auth.currentUser?.uid ?: return
        
        // Logical Fix: Only verified seekers can post jobs
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val isVerified = doc.getBoolean("isVerified") ?: false
            if (isVerified) {
                validateAndPost()
            } else {
                Toast.makeText(this, "Your account must be verified by Admin to post jobs.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun validateAndPost() {
        val title = binding.etJobTitle.text.toString().trim()
        val payAmount = binding.etPayAmount.text.toString().trim()
        val desc = binding.etJobDescription.text.toString().trim()
        val workplaceName = binding.etWorkplaceName.text.toString().trim()
        val workplaceAddress = binding.etWorkplaceAddress.text.toString().trim()
        val durationValue = binding.etDurationValue.text.toString().trim()
        
        val category = binding.spinnerJobCategory.selectedItem?.toString() ?: ""
        val district = binding.spinnerDistrict.selectedItem?.toString() ?: ""
        val workerType = if (binding.rbEducated.isChecked) "educated" else "uneducated"

        if (title.isEmpty() || category.isEmpty() || payAmount.isEmpty() || workplaceAddress.isEmpty()) {
            Toast.makeText(this, "Please fill all mandatory fields (Title, Category, Pay, Address)", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnPostJob.isEnabled = false
        binding.btnPostJob.text = "POSTING..."

        val jobData = hashMapOf(
            "seekerId" to auth.currentUser?.uid,
            "jobTitle" to title,
            "category" to category,
            "workerType" to workerType,
            "district" to district,
            "workplaceName" to workplaceName,
            "workplaceType" to binding.spinnerWorkplaceType.selectedItem.toString(),
            "workplaceAddress" to workplaceAddress,
            "payAmount" to payAmount,
            "payUnit" to binding.spinnerPayUnit.selectedItem.toString(),
            "durationValue" to durationValue,
            "durationUnit" to binding.spinnerDurationUnit.selectedItem.toString(),
            "jobDescription" to desc,
            "status" to "open",
            "timestamp" to Timestamp.now()
        )

        db.collection("jobs").add(jobData)
            .addOnSuccessListener {
                Toast.makeText(this, "Job Posted Successfully!", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener {
                binding.btnPostJob.isEnabled = true
                binding.btnPostJob.text = "POST JOB NOW"
                Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}