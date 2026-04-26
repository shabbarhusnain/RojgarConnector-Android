package com.shabbar.rozgarconnector.ui.seeker

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.ActivitySeekerDetailBinding
import com.shabbar.rozgarconnector.models.UserModel
import com.shabbar.rozgarconnector.utils.loadBase64Image

class SeekerDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySeekerDetailBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeekerDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val seekerId = intent.getStringExtra("SEEKER_ID")
        if (seekerId == null) {
            finish()
            return
        }

        binding.btnBack.setOnClickListener { finish() }

        loadSeekerDetails(seekerId)
    }

    private fun loadSeekerDetails(id: String) {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("users").document(id).get().addOnSuccessListener { doc ->
            binding.progressBar.visibility = View.GONE
            val seeker = doc.toObject(UserModel::class.java)
            if (seeker != null) {
                binding.tvSeekerName.text = seeker.fullName ?: "Seeker"
                binding.tvLocation.text = "${seeker.district ?: "N/A"}, ${seeker.city ?: ""}"
                binding.tvMemberSince.text = "Role: ${seeker.role?.uppercase()}"
                
                // Bind Seeker Rating Stars
                binding.seekerRatingBar.rating = seeker.averageRating
                
                loadBase64Image(this, seeker.dpBase64, binding.imgSeekerProfile, R.drawable.ic_profile)

                if (seeker.isVerified) {
                    binding.ivVerifyIcon.setImageResource(R.drawable.ic_notification)
                    binding.ivVerifyIcon.setColorFilter(getColor(R.color.primary_green))
                    binding.tvVerificationStatus.text = "Verified Seeker"
                } else {
                    binding.tvVerificationStatus.text = "Not Verified"
                    binding.ivVerifyIcon.setColorFilter(getColor(R.color.grey))
                }
            }
        }.addOnFailureListener {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, "Failed to load info", Toast.LENGTH_SHORT).show()
        }
    }
}