package com.shabbar.rozgarconnector.ui.worker

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.ActivityWorkerDetailBinding
import com.shabbar.rozgarconnector.models.UserModel
import com.shabbar.rozgarconnector.utils.decodeBase64BitmapAsync

class WorkerDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkerDetailBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var workerId: String? = null
    private var workerName: String? = null
    private var workerType: String? = null
    private var notificationId: String? = null
    private var baseRate: Double = 0.0
    private var commissionPct: Double = 10.0 // Default

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

        fetchSystemRates()
        loadWorkerDetails()
        checkNotificationStatus()

        binding.btnBack.setOnClickListener { finish() }
        binding.btnReport.setOnClickListener { showReportDialog() }
        binding.btnHireNow.setOnClickListener { showHiringForm() }
        
        binding.btnAcceptApp.setOnClickListener { handleDirectDecision("accepted") }
        binding.btnDeclineApp.setOnClickListener { handleDirectDecision("rejected") }
    }

    private fun fetchSystemRates() {
        db.collection("settings").document("rates").get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                commissionPct = doc.getDouble("commission") ?: 10.0
            }
        }
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
                if (senderId != currentUserId) {
                    binding.btnAcceptApp.visibility = View.VISIBLE
                    binding.btnDeclineApp.visibility = View.VISIBLE
                    binding.btnAcceptApp.text = "Accept Offer"
                } else {
                    binding.btnAcceptApp.visibility = View.GONE
                    binding.btnDeclineApp.visibility = View.GONE
                }
            } else if (status == "accepted") {
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

        val updates = hashMapOf<String, Any>(
            "status" to status,
            "timestamp" to Timestamp.now(),
            "isRead" to false,
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
                    baseRate = (worker.dailyRate ?: "0").toDoubleOrNull() ?: 0.0
                    
                    binding.tvWorkerName.text = worker.fullName
                    loadBase64Image(worker.dpBase64, binding.imgWorkerProfile, R.drawable.ic_profile)
                    
                    // Display Skill + Price (Transparent Pricing)
                    val totalPrice = baseRate // Rate is already including commission for Seeker view
                    binding.tvSkill.text = "${if(worker.workerType == "educated") worker.degreeName else worker.professionalSkill} | Rs. $totalPrice"
                    
                    binding.tvExp.text = "${worker.experienceYears ?: "0"} Years"
                    binding.tvDesc.text = worker.professionalDescription ?: "No description provided."

                    binding.imgVerifiedBadge.visibility = if (worker.isVerified) View.VISIBLE else View.GONE
                    binding.workerRatingBar.rating = worker.averageRating

                    if (worker.workerType == "educated") {
                        binding.eduSection.visibility = View.VISIBLE
                        binding.tvEdu.text = "${worker.lastDegree} in ${worker.degreeName}"
                    } else {
                        binding.eduSection.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun loadBase64Image(base64Str: String?, imageView: android.widget.ImageView, placeholder: Int) {
        if (!base64Str.isNullOrEmpty()) {
            decodeBase64BitmapAsync(base64Str, {
                imageView.setImageBitmap(it)
            }, {
                imageView.setImageResource(placeholder)
            })
        } else {
            imageView.setImageResource(placeholder)
        }
    }

    private fun showHiringForm() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_hire_details, null)
        val etTask = view.findViewById<EditText>(R.id.etJobTask)
        val etLocation = view.findViewById<EditText>(R.id.etJobLocation)
        val etBudget = view.findViewById<EditText>(R.id.etJobBudget)
        val etDeadline = view.findViewById<EditText>(R.id.etJobDeadline)
        
        // Auto-fill suggested budget
        etBudget.setText(baseRate.toString())

        val cbTools = view.findViewById<CheckBox>(R.id.cbToolsProvided)
        val cbSafety = view.findViewById<CheckBox>(R.id.cbSafetyConfirmed)
        val cbPayment = view.findViewById<CheckBox>(R.id.cbPaymentPrompt)

        AlertDialog.Builder(this)
            .setTitle("Work Agreement & Offer")
            .setView(view)
            .setPositiveButton("Send Official Offer") { _, _ ->
                val task = etTask.text.toString().trim()
                val loc = etLocation.text.toString().trim()
                val budget = etBudget.text.toString().trim()
                val deadline = etDeadline.text.toString().trim()
                
                if (task.isEmpty() || budget.isEmpty() || loc.isEmpty()) {
                    Toast.makeText(this, "Please fill all required agreement fields.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val message = "OFFICIAL WORK OFFER:\n" +
                        "• Task: $task\n" +
                        "• Budget: Rs. $budget\n" +
                        "• Deadline: $deadline\n" +
                        "• Location: $loc\n" +
                        "• Tools Provided: ${if(cbTools.isChecked) "Yes (By Seeker)" else "No (By Worker)"}\n" +
                        "• Safety Guaranteed: ${if(cbSafety.isChecked) "Yes" else "Under Discussion"}\n" +
                        "• Payment Terms: ${if(cbPayment.isChecked) "Prompt after work" else "Discussed"}"

                sendHireRequest(message)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendHireRequest(message: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        db.collection("users").document(currentUserId).get().addOnSuccessListener { seekerDoc ->
            val seekerName = seekerDoc.getString("fullName") ?: "Seeker"
            
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
            db.collection("notifications").add(notificationData).addOnSuccessListener { 
                Toast.makeText(this, "Official Offer Sent Successfully!", Toast.LENGTH_SHORT).show()
                finish() 
            }
        }
    }

    private fun showReportDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_report_user, null)
        val etDesc = view.findViewById<EditText>(R.id.etReportDescription)
        val spReason = view.findViewById<Spinner>(R.id.spReportReason)
        
        val reasons = arrayOf("Fraudulent Activity", "Harassment", "Poor Work Quality", "Suspicious Documents", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, reasons)
        spReason.adapter = adapter

        AlertDialog.Builder(this)
            .setTitle("Report Professional")
            .setView(view)
            .setPositiveButton("Submit Report") { _, _ ->
                val reason = spReason.selectedItem.toString()
                val description = etDesc.text.toString().trim()
                
                if (description.isEmpty()) {
                    Toast.makeText(this, "Please provide some details for the admin.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                submitReport(reason, description)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submitReport(reason: String, description: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        
        val reportData = hashMapOf(
            "reporterId" to currentUserId,
            "reportedUserId" to workerId,
            "title" to reason,
            "description" to description,
            "category" to reason.lowercase().replace(" ", "_"),
            "priority" to if (reason == "Harassment") "high" else "medium",
            "status" to "pending",
            "createdAt" to Timestamp.now()
        )

        db.collection("reports").add(reportData).addOnSuccessListener {
            Toast.makeText(this, "Report submitted. Admin will review it shortly.", Toast.LENGTH_LONG).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to submit report. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }
}
