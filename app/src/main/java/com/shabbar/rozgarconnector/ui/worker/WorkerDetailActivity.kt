package com.shabbar.rozgarconnector.ui.worker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.ActivityWorkerDetailBinding
import com.shabbar.rozgarconnector.models.ActivitiesModel
import com.shabbar.rozgarconnector.models.UserModel
import com.shabbar.rozgarconnector.ui.messaging.ChatActivity
import com.shabbar.rozgarconnector.utils.loadBase64Image

class WorkerDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkerDetailBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private var workerId: String? = null
    private var applicationId: String? = null 
    private var jobId: String? = null
    private var workerName: String? = null
    private var currentSeekerName: String? = "Service Seeker"
    private var isContractActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkerDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        workerId = intent.getStringExtra("WORKER_ID")
        applicationId = intent.getStringExtra("APPLICATION_ID")
        jobId = intent.getStringExtra("JOB_ID")

        if (workerId == null) { finish(); return }

        binding.btnBack.setOnClickListener { finish() }
        fetchCurrentSeekerName()
        checkIfAlreadyContracted()
        loadWorkerDetails(workerId!!)
    }

    private fun fetchCurrentSeekerName() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { 
            currentSeekerName = it.getString("fullName") ?: "Service Seeker"
        }
    }

    private fun checkIfAlreadyContracted() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("notifications")
            .whereIn("status", listOf("accepted", "approved"))
            .addSnapshotListener { snapshots, _ ->
                snapshots?.forEach { doc ->
                    val sender = doc.getString("senderId")
                    val receiver = doc.getString("receiverId")
                    if ((sender == uid && receiver == workerId) || (sender == workerId && receiver == uid)) {
                        isContractActive = true
                    }
                }
                setupBottomButtons()
            }
    }

    private fun setupBottomButtons() {
        if (applicationId != null) {
            binding.btnHireNow.visibility = View.GONE
            binding.llApplicationActions.visibility = View.VISIBLE
            binding.btnAcceptApp.setOnClickListener { processApplication("accepted") }
            binding.btnRejectApp.setOnClickListener { processApplication("rejected") }
        } else if (isContractActive) {
            binding.btnHireNow.visibility = View.VISIBLE
            binding.btnHireNow.text = "OPEN CHAT"
            binding.btnHireNow.setOnClickListener { openChat() }
        } else {
            binding.btnHireNow.visibility = View.VISIBLE
            binding.btnHireNow.text = "HIRE THIS WORKER"
            binding.btnHireNow.setOnClickListener { showHireDialog() }
        }
    }

    private fun loadWorkerDetails(id: String) {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("users").document(id).get().addOnSuccessListener { doc ->
            binding.progressBar.visibility = View.GONE
            binding.scrollView.visibility = View.VISIBLE
            val worker = doc.toObject(UserModel::class.java)
            if (worker != null) {
                workerName = worker.fullName
                binding.tvWorkerName.text = worker.fullName
                val isEducated = worker.workerType == "educated"
                binding.tvWorkerType.text = if (isEducated) "Educated Provider" else "Skilled Provider"
                binding.workerRatingBar.rating = worker.averageRating
                loadBase64Image(this, worker.dpBase64, binding.imgWorkerProfile, R.drawable.ic_profile)
                
                // REPAIR: Sync with Educated Portfolio Details
                if (isEducated) {
                    binding.cardEducation.visibility = View.VISIBLE
                    binding.cardServices.visibility = View.VISIBLE // Reusing this for Expertise
                    binding.tvEducationInfo.text = "Degree: ${worker.degreeName ?: "N/A"}\nLevel: ${worker.lastDegree ?: "N/A"}"
                    binding.tvServicesInfo.text = "Expertise: ${worker.professionalSkill ?: "N/A"}\nExperience: ${worker.experienceYears ?: "N/A"}"
                } else {
                    binding.cardEducation.visibility = View.GONE
                    binding.cardServices.visibility = View.VISIBLE
                    binding.tvServicesInfo.text = "Skills: ${worker.professionalSkill ?: "Manual Work"}\nExperience: ${worker.experienceYears ?: "1+"} Years"
                }

                binding.tvBioInfo.text = if (!worker.professionalDescription.isNullOrEmpty() && worker.professionalDescription != "not yet") 
                    worker.professionalDescription else "No biography provided yet."
                
                val history = StringBuilder()
                if (!worker.jobTitle.isNullOrEmpty()) history.append("Title: ${worker.jobTitle}\n")
                if (!worker.lastWorkPlace.isNullOrEmpty()) history.append("Place: ${worker.lastWorkPlace}\n")
                if (!worker.employmentDuration.isNullOrEmpty()) history.append("Duration: ${worker.employmentDuration}")
                binding.tvHistoryInfo.text = if(history.isNotEmpty()) history.toString() else "No history provided."
                
                binding.tvCertsInfo.text = if (!worker.certifications.isNullOrEmpty()) worker.certifications else "No certifications added."
                
                if (!worker.projectPhotoBase64.isNullOrEmpty()) {
                    binding.imgProjectPortfolio.visibility = View.VISIBLE
                    loadBase64Image(this, worker.projectPhotoBase64, binding.imgProjectPortfolio, R.drawable.ic_messages)
                } else {
                    binding.imgProjectPortfolio.visibility = View.GONE
                }
            }
        }
    }

    private fun processApplication(newStatus: String) {
        if (applicationId == null) return
        val batch = db.batch()
        val notifRef = db.collection("notifications").document(applicationId!!)
        batch.update(notifRef, "status", newStatus)
        batch.update(notifRef, "title", if(newStatus == "accepted") "Contract Started" else "Application Rejected")

        if (newStatus == "accepted" && jobId != null) {
            val jobRef = db.collection("jobs").document(jobId!!)
            batch.update(jobRef, "status", "accepted") 
        }

        batch.commit().addOnSuccessListener {
            if (newStatus == "accepted") openChat()
            Toast.makeText(this, "Process Complete", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showHireDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_hire_details, null)
        val dialog = AlertDialog.Builder(this).setView(view).create()
        view.findViewById<Button>(R.id.btnSendOffer).setOnClickListener {
            val task = view.findViewById<TextInputEditText>(R.id.etJobTask).text.toString().trim()
            val budget = view.findViewById<TextInputEditText>(R.id.etJobBudget).text.toString().trim()
            val loc = view.findViewById<TextInputEditText>(R.id.etJobLocation).text.toString().trim()
            if (task.isEmpty() || budget.isEmpty()) return@setOnClickListener
            createDirectHireContract(task, budget, loc)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun createDirectHireContract(task: String, budget: String, location: String) {
        val nid = db.collection("notifications").document().id
        val contract = ActivitiesModel().apply {
            notificationId = nid; senderId = auth.currentUser?.uid; receiverId = workerId
            senderName = currentSeekerName ?: "Seeker"; title = "Direct Hire Offer"
            taskTitle = task; this.budget = budget; this.location = location
            type = "hire"; status = "pending"; timestamp = Timestamp.now()
        }
        db.collection("notifications").document(nid).set(contract).addOnSuccessListener {
            Toast.makeText(this, "Offer Sent!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun openChat() {
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("RECEIVER_ID", workerId); putExtra("RECEIVER_NAME", workerName)
        }
        startActivity(intent)
    }
}