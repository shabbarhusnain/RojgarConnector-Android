package com.shabbar.rozgarconnector.ui.job

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.ActivityJobDetailBinding
import com.shabbar.rozgarconnector.models.ActivitiesModel
import com.shabbar.rozgarconnector.models.JobModel
import com.shabbar.rozgarconnector.utils.TranslatorUtil
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

        fetchCurrentUserDetails {
            loadJobDetails(jobId)
        }

        binding.btnConfirmApply.setOnClickListener { applyForJob() }
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun fetchCurrentUserDetails(onComplete: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            currentUserName = doc.getString("fullName")
            currentUserRole = doc.getString("role")?.lowercase()
            onComplete()
        }.addOnFailureListener { onComplete() }
    }

    private fun loadJobDetails(id: String) {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("jobs").document(id).get().addOnSuccessListener { doc ->
            if (!isFinishing) {
                binding.progressBar.visibility = View.GONE
                binding.scrollView.visibility = View.VISIBLE
                
                jobData = doc.toObject(JobModel::class.java)
                jobData?.let { job ->
                    val isUrdu = TranslatorUtil.isUrduEnabled(this)

                    setTextOrTranslate(binding.tvDetailTitle, job.jobTitle, isUrdu)
                    binding.tvDetailBudget.text = "Rs. ${job.payAmount}"
                    
                    val category = if(job.workerType == "educated") "Professional" else "Skilled/Manual"
                    setTextOrTranslate(binding.tvDetailCategory, category, isUrdu)
                    
                    setTextOrTranslate(binding.tvCompanyName, job.workplaceName ?: "Local Job", isUrdu)
                    setTextOrTranslate(binding.tvDetailDesc, job.jobDescription, isUrdu)
                    
                    val address = "Location: ${job.district ?: "N/A"}"
                    setTextOrTranslate(binding.tvDetailAddress, address, isUrdu)
                    
                    val deadline = "Date: ${job.lastDateToApply ?: "N/A"}"
                    setTextOrTranslate(binding.tvDetailDeadline, deadline, isUrdu)
                    
                    val tools = "Tools Provided By: ${job.toolsProvidedBy ?: "N/A"}"
                    setTextOrTranslate(binding.tvDetailTools, tools, isUrdu)
                    
                    val negotiable = "Negotiable: ${if(job.isNegotiable) "Yes" else "No"}"
                    setTextOrTranslate(binding.tvDetailNegotiable, negotiable, isUrdu)
                    
                    setTextOrTranslate(binding.tvBenefits, job.benefits ?: "Not mentioned", isUrdu)
                    setTextOrTranslate(binding.tvQualifications, job.qualifications ?: "General", isUrdu)
                    
                    if (job.status != "open" || currentUserRole == "seeker") {
                        binding.btnConfirmApply.visibility = View.GONE
                    } else {
                        binding.btnConfirmApply.visibility = View.VISIBLE
                    }

                    // --- Image Loading Fix ---
                    if (!job.jobPhotoBase64.isNullOrEmpty()) {
                        binding.ivJobPoster.setPadding(0, 0, 0, 0)
                        binding.ivJobPoster.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                        loadBase64Image(this, job.jobPhotoBase64, binding.ivJobPoster, R.drawable.header_gradient_curved)
                    } else {
                        binding.ivJobPoster.setImageResource(R.drawable.header_gradient_curved)
                        binding.ivJobPoster.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                        binding.ivJobPoster.setPadding(0,0,0,0)
                    }

                    trackJobView(job)
                }
            }
        }.addOnFailureListener {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, "Failed to load details", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setTextOrTranslate(textView: TextView, text: String?, isUrdu: Boolean) {
        if (text == null) return
        if (isUrdu) {
            TranslatorUtil.translateText(text) { translated ->
                textView.text = translated
            }
        } else {
            textView.text = text
        }
    }

    private fun trackJobView(job: JobModel) {
        val currentUid = auth.currentUser?.uid ?: return
        if (job.seekerId == currentUid) return
        if (currentUserRole != "seeker") {
            if (!job.viewedBy.contains(currentUid)) {
                db.collection("jobs").document(job.jobId!!).update(
                    "viewsCount", FieldValue.increment(1),
                    "viewedBy", FieldValue.arrayUnion(currentUid)
                )
            }
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
