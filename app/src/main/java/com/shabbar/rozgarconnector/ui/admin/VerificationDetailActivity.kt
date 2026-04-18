package com.shabbar.rozgarconnector.ui.admin

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.databinding.ActivityVerificationDetailBinding
import com.shabbar.rozgarconnector.models.UserModel
import com.shabbar.rozgarconnector.utils.decodeBase64BitmapAsync

class VerificationDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerificationDetailBinding
    private val db = FirebaseFirestore.getInstance()
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerificationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getStringExtra("USER_ID")
        
        binding.btnBack.setOnClickListener { finish() }
        
        if (userId != null) {
            loadUserDetails(userId!!)
        }

        binding.btnApprove.setOnClickListener {
            approveUser()
        }

        binding.btnReject.setOnClickListener {
            rejectUser()
        }
    }

    private fun loadUserDetails(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val user = doc.toObject(UserModel::class.java)
                if (user != null) {
                    displayUser(user)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayUser(user: UserModel) {
        binding.apply {
            // Fix: Use 'fullName' to match the updated UserModel
            tvFullName.text = if (user.fullName.isNotEmpty()) user.fullName else "N/A"
            tvCnic.text = user.cnic ?: "N/A"
            tvFatherName.text = user.fatherName ?: "N/A"
            tvPhone.text = user.phone ?: "N/A"
            tvDob.text = user.dob ?: "N/A"
            tvAddress.text = user.permanentAddress ?: "N/A"
            tvRoleBadge.text = user.role.uppercase()

            // Load Basic Images
            loadBase64Image(user.dpBase64, imgProfile)
            loadBase64Image(user.cnicFrontBase64, imgCnicFront)
            loadBase64Image(user.cnicBackBase64, imgCnicBack)

            // Handle Professional Data for Workers
            if (user.role.equals("Worker", ignoreCase = true)) {
                layoutProfessionalInfo.visibility = View.VISIBLE
                
                if (user.workerType == "educated") {
                    tvEducation.text = "${user.lastDegree} (${user.degreeName})"
                    tvSkills.text = user.professionalSkill ?: "N/A"
                    tvExperience.text = user.experienceYears ?: "N/A"
                    tvDailyRate.text = "N/A (Monthly/Project)"
                    
                    // Show Degree Photo
                    layoutDegreeDoc.visibility = View.VISIBLE
                    loadBase64Image(user.degreePhotoBase64, imgDegreeDoc)
                    
                    rowDailyRate.visibility = View.GONE
                } else {
                    // Uneducated Worker
                    tvEducation.text = "N/A"
                    tvSkills.text = user.skills ?: "N/A"
                    tvExperience.text = user.experience ?: "N/A"
                    tvDailyRate.text = "Rs. ${user.dailyRate ?: "0"}"
                    
                    layoutDegreeDoc.visibility = View.GONE
                    rowEducation.visibility = View.GONE
                    rowDailyRate.visibility = View.VISIBLE
                }
            } else {
                layoutProfessionalInfo.visibility = View.GONE
            }
            
            // If user is already verified, hide buttons
            if (user.isVerified) {
                btnApprove.visibility = View.GONE
                btnReject.text = "BLOCK USER"
            }
            
            if (user.isRejected) {
                btnReject.visibility = View.GONE
                btnApprove.text = "RE-ACTIVATE USER"
            }
        }
    }

    private fun loadBase64Image(base64Str: String?, imageView: android.widget.ImageView) {
        if (!base64Str.isNullOrEmpty()) {
            decodeBase64BitmapAsync(base64Str, {
                imageView.setImageBitmap(it)
            }, {
                // Keep default placeholder
            })
        }
    }

    private fun approveUser() {
        userId?.let { uid ->
            val updates = mapOf(
                "isVerified" to true,
                "isRejected" to false
            )
            db.collection("users").document(uid)
                .update(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "User Approved Successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun rejectUser() {
        userId?.let { uid ->
            val updates = mapOf(
                "isVerified" to false,
                "isRejected" to true
            )
            db.collection("users").document(uid)
                .update(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "User Account Rejected", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
