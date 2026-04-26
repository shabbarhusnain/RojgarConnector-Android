package com.shabbar.rozgarconnector.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.FragmentProfileBinding
import com.shabbar.rozgarconnector.models.UserModel
import com.shabbar.rozgarconnector.ui.profile.EducatedWorkerProfileActivity
import com.shabbar.rozgarconnector.ui.profile.UneducatedWorkerProfileActivity
import com.shabbar.rozgarconnector.ui.settings.MenuActivity
import com.shabbar.rozgarconnector.utils.loadBase64Image

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var currentUserWorkerType: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(requireContext(), MenuActivity::class.java))
        }

        binding.btnEditPortfolio.setOnClickListener {
            if (currentUserWorkerType == "educated") {
                startActivity(Intent(requireContext(), EducatedWorkerProfileActivity::class.java))
            } else {
                startActivity(Intent(requireContext(), UneducatedWorkerProfileActivity::class.java))
            }
        }

        loadUserProfile()
    }

    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return

        // Use a One-time get for profile to avoid unwanted real-time flickering issues
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (isAdded && doc != null && doc.exists()) {
                val user = doc.toObject(UserModel::class.java)
                if (user != null) {
                    binding.tvProfileName.text = user.fullName
                    currentUserWorkerType = user.workerType

                    val isEducated = user.workerType == "educated"
                    val isProvider = user.role?.lowercase() != "seeker"

                    // Logic for Role Tag
                    binding.tvProfileRole.text = when {
                        !isProvider -> "Service Seeker"
                        isEducated -> "Educated Provider"
                        else -> "Skilled Provider"
                    }

                    binding.profileRatingBar.rating = user.averageRating

                    // CRITICAL FIX: Explicitly load the DP of the current user
                    loadBase64Image(requireContext(), user.dpBase64, binding.imgProfile, R.drawable.ic_profile)

                    // Basic Details
                    binding.tvFatherName.text = "Father: ${user.fatherName ?: "N/A"}"
                    binding.tvCnic.text = "CNIC: ${user.cnic ?: "N/A"}"
                    binding.tvDob.text = "DOB: ${user.dob ?: "N/A"}"
                    binding.tvPhone.text = "Phone: ${user.phone ?: "N/A"}"
                    binding.tvLocationFull.text = "Address: ${user.city ?: ""}, ${user.district ?: ""}"

                    // --- PROVIDER SPECIFIC CARDS ---
                    if (isProvider) {
                        binding.portfolioCard.visibility = View.VISIBLE
                        binding.cardExpSkill.visibility = View.VISIBLE
                        binding.cardProfileEdu.visibility = if (isEducated) View.VISIBLE else View.GONE

                        val experienceText = if (!user.experienceYears.isNullOrEmpty()) "Experience: ${user.experienceYears} Years\n\n" else ""
                        val toolsText = if (user.hasOwnTools == true) "✅ Has own tools/equipment\n\n" else ""
                        binding.tvDescription.text = "$experienceText$toolsText${user.professionalDescription ?: "No biography provided."}"

                        binding.tvExperienceInfo.text = user.experienceYears ?: "0 Years"
                        binding.tvExpertise.text = user.professionalSkill ?: "N/A"

                        if (isEducated) {
                            binding.tvEducation.text = "${user.degreeName ?: ""}\n${user.lastDegree ?: ""}"
                        }
                    } else {
                        binding.portfolioCard.visibility = View.GONE
                        binding.cardExpSkill.visibility = View.GONE
                        binding.cardProfileEdu.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}