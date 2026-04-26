package com.shabbar.rozgarconnector.ui.role

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.ActivityRoleSelectionBinding
import com.shabbar.rozgarconnector.ui.auth.PendingApprovalActivity
import com.shabbar.rozgarconnector.ui.home.SeekerHomeActivity
import com.shabbar.rozgarconnector.ui.home.ProviderHomeActivity
import com.shabbar.rozgarconnector.ui.profile.EducatedWorkerProfileActivity
import com.shabbar.rozgarconnector.ui.profile.UneducatedWorkerProfileActivity

class RoleSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoleSelectionBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var selectedRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoleSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cardSeeker.setOnClickListener {
            selectedRole = "seeker"
            binding.llProviderType.visibility = View.GONE
            binding.btnConfirmRole.visibility = View.VISIBLE
            updateSelectionUI()
        }

        binding.cardProvider.setOnClickListener {
            selectedRole = "provider"
            binding.llProviderType.visibility = View.VISIBLE
            binding.btnConfirmRole.visibility = View.VISIBLE
            updateSelectionUI()
        }

        binding.btnConfirmRole.setOnClickListener {
            saveRoleAndNavigate()
        }
    }

    private fun updateSelectionUI() {
        val green = ContextCompat.getColor(this, R.color.primary_green)
        val white = Color.WHITE
        val black = Color.BLACK
        val grey = Color.parseColor("#80000000") // 50% Alpha Black

        if (selectedRole == "seeker") {
            // Seeker Selected
            applyCardStyle(binding.cardSeeker, green, white, white)
            applyCardStyle(binding.cardProvider, white, black, grey)
        } else {
            // Provider Selected
            applyCardStyle(binding.cardSeeker, white, black, grey)
            applyCardStyle(binding.cardProvider, green, white, white)
        }
    }

    private fun applyCardStyle(card: MaterialCardView, bgColor: Int, titleColor: Int, subTitleColor: Int) {
        card.setCardBackgroundColor(bgColor)
        card.strokeWidth = if (bgColor == Color.WHITE) 2 else 0
        
        // Find TextViews inside the first child (LinearLayout)
        val layout = card.getChildAt(0) as? android.widget.LinearLayout ?: return
        val tvTitle = layout.getChildAt(0) as? TextView
        val tvSub = layout.getChildAt(1) as? TextView
        
        tvTitle?.setTextColor(titleColor)
        tvSub?.setTextColor(subTitleColor)
        tvSub?.alpha = if (bgColor == Color.WHITE) 0.5f else 0.9f
    }

    private fun saveRoleAndNavigate() {
        val uid = auth.currentUser?.uid ?: return
        val role = selectedRole ?: return
        
        val updateData = mutableMapOf<String, Any>("role" to role)
        
        if (role == "provider") {
            val type = if (binding.radioEducated.isChecked) "educated" else if (binding.radioUneducated.isChecked) "uneducated" else null
            if (type == null) {
                Toast.makeText(this, "Please select worker type", Toast.LENGTH_SHORT).show()
                return
            }
            updateData["workerType"] = type
        }

        binding.btnConfirmRole.isEnabled = false
        db.collection("users").document(uid).update(updateData).addOnSuccessListener {
            checkStatusAndNavigate(uid)
        }
    }

    private fun checkStatusAndNavigate(uid: String) {
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val isVerified = doc.getBoolean("isVerified") ?: false
            if (!isVerified) {
                startActivity(Intent(this, PendingApprovalActivity::class.java))
            } else {
                val role = doc.getString("role") ?: ""
                val profileCompleted = doc.getBoolean("profileCompleted") ?: false
                val workerType = doc.getString("workerType") ?: ""

                when (role) {
                    "seeker" -> startActivity(Intent(this, SeekerHomeActivity::class.java))
                    "provider" -> {
                        if (profileCompleted) startActivity(Intent(this, ProviderHomeActivity::class.java))
                        else {
                            val target = if (workerType == "educated") EducatedWorkerProfileActivity::class.java
                            else UneducatedWorkerProfileActivity::class.java
                            startActivity(Intent(this, target))
                        }
                    }
                }
            }
            finish()
        }
    }
}