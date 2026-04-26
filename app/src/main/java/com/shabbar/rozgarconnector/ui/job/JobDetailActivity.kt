package com.shabbar.rozgarconnector.ui.job

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.ActivityJobDetailBinding
import com.shabbar.rozgarconnector.models.ActivitiesModel
import com.shabbar.rozgarconnector.models.JobModel
import com.shabbar.rozgarconnector.utils.loadBase64Image

class JobDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJobDetailBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var jobData: JobModel? = null
    private var currentUserName: String? = null
    private var currentUserRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJobDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val jobId = intent.getStringExtra("JOB_ID")
        if (jobId == null) { finish(); return }

        fetchCurrentUserDetails()
        loadJobDetails(jobId)

        binding.btnConfirmApply.setOnClickListener { applyForJob() }
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun fetchCurrentUserDetails() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            currentUserName = doc.getString("fullName")
            currentUserRole = doc.getString("role")?.lowercase()
        }
    }

    private fun loadJobDetails(id: String) {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("jobs").document(id).get().addOnSuccessListener { doc ->
            if (!isFinishing) {
                binding.progressBar.visibility = View.GONE
                binding.scrollView.visibility = View.VISIBLE
                
                jobData = doc.toObject(JobModel::class.java)
                jobData?.let { job ->
                    binding.tvDetailTitle.text = job.jobTitle
                    binding.tvDetailBudget.text = "Rs. ${job.payAmount}"
                    binding.tvDetailCategory.text = if(job.workerType == "educated") "Professional" else "Skilled/Manual"
                    binding.tvCompanyName.text = job.workplaceName ?: "Local Job"
                    binding.tvDetailDesc.text = job.jobDescription
                    binding.tvDetailAddress.text = "Location: ${job.district ?: "N/A"}"
                    binding.tvDetailDeadline.text = "Date: ${job.lastDateToApply ?: "N/A"}"
                    binding.tvDetailTools.text = "Tools Provided By: ${job.toolsProvidedBy ?: "N/A"}"
                    binding.tvDetailNegotiable.text = "Negotiable: ${if(job.isNegotiable) "Yes" else "No"}"
                    binding.tvBenefits.text = job.benefits ?: "Not mentioned"
                    binding.tvQualifications.text = job.qualifications ?: "General"
                    
                    // REPAIR: Button visibility logic based on status and role
                    if (job.status != "open" || currentUserRole == "seeker") {
                        binding.btnConfirmApply.visibility = View.GONE
                    } else {
                        binding.btnConfirmApply.visibility = View.VISIBLE
                    }

                    // Handle missing image gracefully
                    if (!job.jobPhotoBase64.isNullOrEmpty()) {
                        loadBase64Image(this, job.jobPhotoBase64, binding.ivJobPoster, R.drawable.header_gradient_curved)
                    } else {
                        binding.ivJobPoster.setImageResource(R.drawable.header_gradient_curved)
                    }
                }
            }
        }.addOnFailureListener {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyForJob() {
        val currentUid = auth.currentUser?.uid ?: return
        val job = jobData ?: return
        
        if (job.seekerId == currentUid) {
            Toast.makeText(this, "This is your post!", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnConfirmApply.isEnabled = false
        db.collection("notifications").whereEqualTo("jobId", job.jobId).whereEqualTo("senderId", currentUid).get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    Toast.makeText(this, "Already applied", Toast.LENGTH_SHORT).show()
                    binding.btnConfirmApply.isEnabled = true
                } else {
                    submitApplication(currentUid, job)
                }
            }
    }

    private fun submitApplication(currentUid: String, job: JobModel) {
        val nid = db.collection("notifications").document().id
        val app = ActivitiesModel().apply {
            notificationId = nid; jobId = job.jobId; senderId = currentUid; receiverId = job.seekerId
            senderName = currentUserName ?: "Worker"; title = "Job Application"; taskTitle = job.jobTitle
            budget = job.payAmount; location = job.district ?: "N/A"; type = "job"; status = "pending"
            timestamp = Timestamp.now()
        }
        db.collection("notifications").document(nid).set(app).addOnSuccessListener {
            Toast.makeText(this, "Applied Successfully!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}