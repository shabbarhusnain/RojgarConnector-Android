package com.shabbar.rozgarconnector.ui.worker

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.ActivityWorkerDetailBinding
import com.shabbar.rozgarconnector.models.UserModel

class WorkerDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkerDetailBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var workerId: String? = null
    private var workerName: String? = null
    private var workerType: String? = null
    private var notificationId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkerDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        workerId = intent.getStringExtra("WORKER_ID")
        notificationId = intent.getStringExtra("NOTIFICATION_ID")

        if (workerId == null) {
            finish()
            return
        }

        loadWorkerDetails()
        checkNotificationStatus()

        binding.btnBack.setOnClickListener { finish() }
        binding.btnReport.setOnClickListener { showReportDialog() }
        binding.btnHireNow.setOnClickListener { showHiringForm() }
        
        binding.btnAcceptApp.setOnClickListener { handleDirectDecision("accepted") }
        binding.btnDeclineApp.setOnClickListener { handleDirectDecision("rejected") }
    }

    private fun checkNotificationStatus() {
        if (notificationId == null) {
            binding.btnHireNow.visibility = View.VISIBLE
            binding.llApplicationActions.visibility = View.GONE
            return
        }

        db.collection("notifications").document(notificationId!!).get().addOnSuccessListener { doc ->
            val status = doc.getString("status")?.lowercase() ?: "pending"
            val currentUserId = auth.currentUser?.uid
            val senderId = doc.getString("senderId")

            binding.btnHireNow.visibility = View.GONE
            binding.llApplicationActions.visibility = View.VISIBLE

            if (status == "pending") {
                // If I received it, show Accept/Decline. If I sent it, show nothing here.
                if (senderId != currentUserId) {
                    binding.btnAcceptApp.visibility = View.VISIBLE
                    binding.btnDeclineApp.visibility = View.VISIBLE
                    binding.btnAcceptApp.text = "Accept Offer"
                } else {
                    binding.btnAcceptApp.visibility = View.GONE
                    binding.btnDeclineApp.visibility = View.GONE
                }
            } else if (status == "accepted") {
                // Only Seeker (sender) can Finish the job in this logic, or both. 
                // Let's allow the one who opened it to Finish if they want.
                binding.btnDeclineApp.visibility = View.GONE
                binding.btnAcceptApp.visibility = View.VISIBLE
                binding.btnAcceptApp.text = "Finish & Close Job"
                binding.btnAcceptApp.setOnClickListener { showFinishJobDialog() }
            } else {
                binding.llApplicationActions.visibility = View.GONE
            }
        }
    }

    private fun showFinishJobDialog() {
        AlertDialog.Builder(this)
            .setTitle("Complete Job")
            .setMessage("Has the work been completed successfully?")
            .setPositiveButton("Yes, Completed") { _, _ -> handleDirectDecision("completed") }
            .setNegativeButton("No", null)
            .show()
    }

    private fun handleDirectDecision(status: String) {
        if (notificationId == null) return
        val currentUserId = auth.currentUser?.uid ?: return

        // FIX: Instead of creating a NEW notification, we update the EXISTING one.
        // This prevents "Doubling" in the list.
        val updates = hashMapOf<String, Any>(
            "status" to status,
            "timestamp" to Timestamp.now(),
            "isRead" to false, // Set to false so the OTHER person sees it as new
            "lastActionBy" to currentUserId
        )

        db.collection("notifications").document(notificationId!!)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Status updated to ${status.uppercase()}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun loadWorkerDetails() {
        db.collection("users").document(workerId!!).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val worker = doc.toObject(UserModel::class.java)
                if (worker != null) {
                    workerName = worker.fullName
                    workerType = worker.workerType
                    binding.tvWorkerName.text = worker.fullName
                    loadBase64Image(worker.dpBase64, binding.imgWorkerProfile, R.drawable.ic_profile)
                    binding.tvSkill.text = if(worker.workerType == "educated") worker.degreeName else worker.professionalSkill
                    binding.tvExp.text = "${worker.experienceYears ?: "0"} Years"
                    binding.tvDesc.text = worker.professionalDescription ?: "No description provided."

                    if (worker.workerType == "educated") {
                        binding.eduSection.visibility = View.VISIBLE
                        binding.tvEdu.text = "${worker.lastDegree} in ${worker.degreeName}"
                        if (notificationId != null) {
                            binding.degreeSection.visibility = View.VISIBLE
                            loadBase64Image(worker.degreePhotoBase64, binding.imgDegree, R.drawable.ic_profile_placeholder)
                        } else {
                            binding.degreeSection.visibility = View.GONE
                        }
                    } else {
                        binding.eduSection.visibility = View.GONE
                        binding.degreeSection.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun loadBase64Image(base64Str: String?, imageView: android.widget.ImageView, placeholder: Int) {
        if (!base64Str.isNullOrEmpty()) {
            try {
                val decodedString = Base64.decode(base64Str, Base64.DEFAULT)
                val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                imageView.setImageBitmap(decodedByte)
            } catch (e: Exception) {
                imageView.setImageResource(placeholder)
            }
        } else {
            imageView.setImageResource(placeholder)
        }
    }

    private fun showHiringForm() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_hire_details, null)
        val etTask = view.findViewById<EditText>(R.id.etJobTask)
        val etDuration = view.findViewById<EditText>(R.id.etJobDuration)
        val etLocation = view.findViewById<EditText>(R.id.etJobLocation)
        val etBudget = view.findViewById<EditText>(R.id.etJobBudget)
        val etDeadline = view.findViewById<EditText>(R.id.etJobDeadline)
        val etContact = view.findViewById<EditText>(R.id.etJobContact)
        val spinnerEngagement = view.findViewById<Spinner>(R.id.spinnerEngagement)
        val tilDuration = view.findViewById<TextInputLayout>(R.id.tilJobDuration)

        if (workerType == "educated") {
            tilDuration.visibility = View.GONE
            val adapter = ArrayAdapter.createFromResource(this, R.array.engagement_types, android.R.layout.simple_spinner_item)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerEngagement.adapter = adapter
        }

        AlertDialog.Builder(this)
            .setTitle("Send Hiring Offer")
            .setView(view)
            .setPositiveButton("Send") { _, _ ->
                val task = etTask.text.toString().trim()
                val loc = etLocation.text.toString().trim()
                val budget = etBudget.text.toString().trim()
                val duration = if (workerType == "educated") spinnerEngagement.selectedItem.toString() else etDuration.text.toString().trim()
                if (task.isNotEmpty()) sendHireRequest(task, duration, loc, budget, etDeadline.text.toString(), etContact.text.toString())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendHireRequest(task: String, duration: String, location: String, budget: String, deadline: String, contact: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        db.collection("users").document(currentUserId).get().addOnSuccessListener { seekerDoc ->
            val seekerName = seekerDoc.getString("fullName") ?: "Seeker"
            val message = "Hiring Offer:\n• Task: $task\n• Budget: Rs. $budget\n• Location: $location\n• Duration: $duration"
            
            val notificationData = hashMapOf(
                "receiverId" to workerId,
                "senderId" to currentUserId,
                "senderName" to seekerName,
                "message" to message,
                "type" to "hire",
                "status" to "pending",
                "timestamp" to Timestamp.now(),
                "isRead" to false,
                "lastActionBy" to currentUserId
            )
            db.collection("notifications").add(notificationData).addOnSuccessListener { finish() }
        }
    }

    private fun showReportDialog() {
        val options = arrayOf("Fake Profile", "Harassment", "Other")
        AlertDialog.Builder(this).setItems(options) { _, which -> submitReport(options[which]) }.show()
    }

    private fun submitReport(reason: String) {
        db.collection("reports").add(hashMapOf("reportedId" to workerId, "reason" to reason, "timestamp" to Timestamp.now()))
    }
}