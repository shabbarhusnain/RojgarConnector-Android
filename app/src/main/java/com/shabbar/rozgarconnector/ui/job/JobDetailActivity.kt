package com.shabbar.rozgarconnector.ui.job

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.databinding.ActivityJobDetailBinding
import com.shabbar.rozgarconnector.models.JobModel

class JobDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJobDetailBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var jobId: String? = null
    private var seekerId: String? = null
    private var jobTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJobDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        jobId = intent.getStringExtra("JOB_ID")
        if (jobId == null) {
            finish()
            return
        }

        loadJobDetails()

        binding.btnConfirmApply.setOnClickListener {
            checkExistingAndActiveJobs()
        }
    }

    private fun loadJobDetails() {
        db.collection("jobs").document(jobId!!).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val job = doc.toObject(JobModel::class.java)
                if (job != null) {
                    seekerId = job.seekerId
                    jobTitle = job.jobTitle
                    binding.tvDetailTitle.text = job.jobTitle
                    binding.tvDetailCategory.text = job.category
                    binding.tvDetailAddress.text = "${job.district}, ${job.workplaceAddress ?: ""}"
                    binding.tvDetailBudget.text = "Rs. ${job.payAmount} / ${job.payUnit}"
                    binding.tvDetailDesc.text = job.jobDescription
                    binding.tvDetailDeadline.text = "${job.durationValue} ${job.durationUnit}"
                    
                    if (auth.currentUser?.uid == seekerId) {
                        binding.btnConfirmApply.visibility = android.view.View.GONE
                    }
                }
            }
        }
    }

    private fun checkExistingAndActiveJobs() {
        val providerId = auth.currentUser?.uid ?: return

        if (providerId == seekerId) {
            Toast.makeText(this, "You cannot apply for your own job.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("applications")
            .whereEqualTo("jobId", jobId)
            .whereEqualTo("providerId", providerId)
            .get()
            .addOnSuccessListener { snapshots ->
                if (!snapshots.isEmpty) {
                    Toast.makeText(this, "You have already applied for this job.", Toast.LENGTH_SHORT).show()
                } else {
                    applyForJob()
                }
            }
    }

    private fun applyForJob() {
        val providerId = auth.currentUser?.uid ?: return
        binding.btnConfirmApply.isEnabled = false
        binding.btnConfirmApply.text = "APPLYING..."

        db.collection("users").document(providerId).get().addOnSuccessListener { providerDoc ->
            val workerName = providerDoc.getString("fullName") ?: "A Worker"
            val timestamp = Timestamp.now()

            val applicationData = hashMapOf(
                "jobId" to jobId,
                "jobTitle" to jobTitle,
                "providerId" to providerId,
                "workerName" to workerName,
                "seekerId" to seekerId,
                "status" to "pending",
                "timestamp" to timestamp
            )

            db.collection("applications").add(applicationData).addOnSuccessListener {
                // IMPORTANT: senderId MUST be the providerId (worker) so seeker can view their profile
                val notifySeeker = hashMapOf(
                    "receiverId" to seekerId,
                    "senderId" to providerId,
                    "senderName" to workerName,
                    "jobId" to jobId,
                    "jobTitle" to jobTitle,
                    "title" to "New Job Application",
                    "message" to "$workerName has applied for your job: $jobTitle",
                    "type" to "job",
                    "status" to "pending",
                    "timestamp" to timestamp,
                    "isRead" to false
                )
                db.collection("notifications").add(notifySeeker)

                // Notify the provider as well
                val notifyProvider = hashMapOf(
                    "receiverId" to providerId,
                    "senderId" to seekerId,
                    "jobId" to jobId,
                    "jobTitle" to jobTitle,
                    "title" to "Application Sent",
                    "message" to "You successfully applied for: $jobTitle",
                    "type" to "job_application_sent",
                    "status" to "pending",
                    "timestamp" to timestamp,
                    "isRead" to true
                )
                db.collection("notifications").add(notifyProvider).addOnSuccessListener {
                    Toast.makeText(this, "Application sent successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }.addOnFailureListener {
            binding.btnConfirmApply.isEnabled = true
            binding.btnConfirmApply.text = "SEND APPLICATION"
        }
    }
}