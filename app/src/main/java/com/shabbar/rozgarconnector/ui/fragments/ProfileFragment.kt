package com.shabbar.rozgarconnector.ui.fragments

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.FragmentProfileBinding
import com.shabbar.rozgarconnector.ui.auth.LoginActivity
import com.shabbar.rozgarconnector.ui.auth.ForgotPasswordActivity
import com.shabbar.rozgarconnector.ui.profile.EducatedWorkerProfileActivity
import com.shabbar.rozgarconnector.ui.profile.UneducatedWorkerProfileActivity
import com.shabbar.rozgarconnector.ui.settings.MenuActivity

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var currentWorkerType = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        loadUserData()

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(requireContext(), MenuActivity::class.java))
        }

        binding.btnSignOut.setOnClickListener {
            signOutUser()
        }

        binding.btnForgotPassword.setOnClickListener {
            startActivity(Intent(requireContext(), ForgotPasswordActivity::class.java))
        }

        binding.btnEditPortfolio.setOnClickListener {
            if (currentWorkerType == "educated") {
                startActivity(Intent(requireContext(), EducatedWorkerProfileActivity::class.java))
            } else {
                startActivity(Intent(requireContext(), UneducatedWorkerProfileActivity::class.java))
            }
        }
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).addSnapshotListener { doc, _ ->
            if (isAdded && doc != null && doc.exists()) {
                // 1. Basic Info
                val name = doc.getString("fullName") ?: "No Name"
                val fatherName = doc.getString("fatherName") ?: "Not Provided"
                val cnic = doc.getString("cnic") ?: "Not Provided"
                val dob = doc.getString("dob") ?: "Not Provided"
                val phone = doc.getString("phone") ?: "Not Provided"
                val district = doc.getString("district") ?: ""
                val city = doc.getString("city") ?: ""
                val address = doc.getString("permanentAddress") ?: ""
                
                // Logical Fix: Use lowercase for role check
                val rawRole = doc.getString("role") ?: "seeker"
                val role = rawRole.lowercase()
                
                currentWorkerType = (doc.getString("workerType") ?: "").lowercase()

                // 2. Set UI Fields
                binding.tvProfileName.text = name
                binding.tvFatherName.text = fatherName
                binding.tvCnic.text = cnic
                binding.tvDob.text = dob
                binding.tvPhone.text = phone
                binding.tvLocationFull.text = "$address, $city, $district"
                binding.tvProfileRole.text = if (currentWorkerType.isNotEmpty()) "$currentWorkerType worker".uppercase() else role.uppercase()

                // 3. Profile Image Setup
                val dpBase64 = doc.getString("dpBase64")
                if (!dpBase64.isNullOrEmpty()) {
                    try {
                        val decodedString = Base64.decode(dpBase64, Base64.DEFAULT)
                        val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                        binding.imgProfile.setImageBitmap(decodedByte)
                    } catch (e: Exception) {
                        binding.imgProfile.setImageResource(R.drawable.ic_profile)
                    }
                }

                // 4. Portfolio Visibility Logic (Crucial Fix)
                if (role == "worker" || currentWorkerType.isNotEmpty()) {
                    binding.portfolioCard.visibility = View.VISIBLE
                    
                    val skill = doc.getString("professionalSkill") ?: "Not Provided"
                    val expYears = doc.getString("experienceYears") ?: "0"
                    val lastPlace = doc.getString("lastWorkPlace") ?: "None"
                    val description = doc.getString("professionalDescription") ?: "No description provided."
                    
                    binding.tvExpertise.text = skill
                    binding.tvExperienceInfo.text = if (lastPlace.isNotEmpty() && lastPlace != "None") "$expYears Years at $lastPlace" else "$expYears Years Experience"
                    binding.tvDescription.text = description

                    if (currentWorkerType == "educated") {
                        binding.tvPortfolioTitle.text = "Professional Portfolio"
                        binding.educationSection.visibility = View.VISIBLE
                        binding.degreePhotoSection.visibility = View.VISIBLE
                        
                        val lastDegree = doc.getString("lastDegree") ?: ""
                        val degreeName = doc.getString("degreeName") ?: ""
                        val university = doc.getString("boardUniversity") ?: ""
                        val percentageVal = doc.getString("percentageCGPA") ?: ""

                        binding.tvEducation.text = "$lastDegree in $degreeName\n$university ($percentageVal)"

                        val degreeBase64 = doc.getString("degreePhotoBase64")
                        if (!degreeBase64.isNullOrEmpty()) {
                            try {
                                val decodedString = Base64.decode(degreeBase64, Base64.DEFAULT)
                                val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                                binding.imgDegreeDoc.setImageBitmap(decodedByte)
                            } catch (e: Exception) {
                                binding.imgDegreeDoc.setImageResource(R.drawable.ic_profile_placeholder)
                            }
                        }
                    } else {
                        binding.tvPortfolioTitle.text = "Worker Skill Profile"
                        binding.educationSection.visibility = View.GONE
                        binding.degreePhotoSection.visibility = View.GONE
                    }
                } else {
                    // This is a Seeker, hide the portfolio card.
                    binding.portfolioCard.visibility = View.GONE
                }
            }
        }
    }

    private fun signOutUser() {
        auth.signOut()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}