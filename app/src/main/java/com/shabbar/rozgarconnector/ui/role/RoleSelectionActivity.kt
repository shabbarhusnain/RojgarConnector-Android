package com.shabbar.rozgarconnector.ui.role

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.databinding.ActivityRoleSelectionBinding
import com.shabbar.rozgarconnector.ui.auth.PendingApprovalActivity
import com.shabbar.rozgarconnector.ui.settings.MenuActivity

class RoleSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoleSelectionBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var selectedRole = "" // seeker or provider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoleSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }

        // Seeker Card Click
        binding.cardSeeker.setOnClickListener {
            selectedRole = "seeker"
            binding.llProviderType.visibility = View.GONE
            binding.btnConfirmRole.visibility = View.VISIBLE
            binding.cardSeeker.setStrokeColor(android.graphics.Color.GREEN)
            binding.cardProvider.setStrokeColor(android.graphics.Color.LTGRAY)
        }

        // Provider Card Click
        binding.cardProvider.setOnClickListener {
            selectedRole = "provider"
            binding.llProviderType.visibility = View.VISIBLE
            binding.btnConfirmRole.visibility = View.VISIBLE
            binding.cardProvider.setStrokeColor(android.graphics.Color.GREEN)
            binding.cardSeeker.setStrokeColor(android.graphics.Color.LTGRAY)
        }

        binding.btnConfirmRole.setOnClickListener {
            if (selectedRole.isEmpty()) {
                Toast.makeText(this, "Please select a role first!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var category = "none"
            if (selectedRole == "provider") {
                category = if (binding.radioEducated.isChecked) "educated" else "uneducated"
            }

            updateUserRole(selectedRole, category)
        }
    }

    private fun updateUserRole(role: String, category: String) {
        val uid = auth.currentUser?.uid ?: return
        
        // NO MORE EMPTY FIELDS: We save both role and workerCategory
        val updates = hashMapOf<String, Any>(
            "role" to role,
            "workerCategory" to category,
            "profileCompleted" to true
        )

        db.collection("users").document(uid).update(updates)
            .addOnSuccessListener {
                // After selecting role, new users ALWAYS go to Pending Approval
                startActivity(Intent(this, PendingApprovalActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}