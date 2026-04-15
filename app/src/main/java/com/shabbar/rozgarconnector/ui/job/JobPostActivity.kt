package com.shabbar.rozgarconnector.ui.job

import android.os.Bundle
import android.view.View
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

        // Step 1: Logic for Safety Hazards visibility
        binding.cbSafetyHazards.setOnCheckedChangeListener { _, isChecked ->
            binding.etHazardsDescription.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        binding.btnPostJob.setOnClickListener { 
            checkSeekerVerificationAndPost()
        }
    }

    private fun setupSpinners() {
        binding.spinnerWorkplaceType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, resources.getStringArray(R.array.workplace_types))
        binding.spinnerDistrict.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, resources.getStringArray(R.array.pakistan_districts))
        binding.spinnerDurationUnit.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, resources.getStringArray(R.array.duration_units))
        binding.spinnerPayUnit.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, resources.getStringArray(R.array.pay_units))
        
        // Step 1: Tools Provider Spinner
        val toolOptions = arrayOf("Seeker (I will provide)", "Worker (Provider must bring)", "Shared / Discuss in Chat")
        binding.spinnerToolsProvided.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, toolOptions)
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
        
        // Step 1 Logical Validations
        val isEthicalTermsAccepted = binding.cbEthicalTerms.isChecked
        val isHazardsChecked = binding.cbSafetyHazards.isChecked
        val hazardsDesc = binding.etHazardsDescription.text.toString().trim()

        if (title.isEmpty() || payAmount.isEmpty() || workplaceAddress.isEmpty()) {
            Toast.makeText(this, "Title, Budget, and Address are required.", Toast.LENGTH_SHORT).show()
            return
        }

        if (isHazardsChecked && hazardsDesc.isEmpty()) {
            Toast.makeText(this, "Please describe the safety hazards.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isEthicalTermsAccepted) {
            Toast.makeText(this, "You must agree to the Ethical Terms to post.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnPostJob.isEnabled = false
        binding.btnPostJob.text = "POSTING..."

        // Fixed type mismatch by explicitly providing Map entries
        val jobData = mutableMapOf<String, Any?>()
        jobData["seekerId"] = auth.currentUser?.uid
        jobData["jobTitle"] = title
        jobData["category"] = binding.spinnerJobCategory.selectedItem?.toString() ?: ""
        jobData["workerType"] = if (binding.rbEducated.isChecked) "educated" else "uneducated"
        jobData["district"] = binding.spinnerDistrict.selectedItem?.toString() ?: ""
        jobData["workplaceName"] = workplaceName
        jobData["workplaceType"] = binding.spinnerWorkplaceType.selectedItem.toString()
        jobData["workplaceAddress"] = workplaceAddress
        jobData["payAmount"] = payAmount
        jobData["payUnit"] = binding.spinnerPayUnit.selectedItem.toString()
        jobData["durationValue"] = durationValue
        jobData["durationUnit"] = binding.spinnerDurationUnit.selectedItem.toString()
        jobData["jobDescription"] = desc
        jobData["status"] = "open"
        jobData["timestamp"] = Timestamp.now()
        
        // Step 1: New Ethical Fields
        jobData["isNegotiable"] = binding.cbIsNegotiable.isChecked
        jobData["isVisitRequired"] = binding.cbVisitRequired.isChecked
        jobData["toolsProvidedBy"] = binding.spinnerToolsProvided.selectedItem.toString()
        jobData["hasSafetyHazards"] = isHazardsChecked
        jobData["hazardsDescription"] = hazardsDesc
        jobData["ethicalTermsAccepted"] = true

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