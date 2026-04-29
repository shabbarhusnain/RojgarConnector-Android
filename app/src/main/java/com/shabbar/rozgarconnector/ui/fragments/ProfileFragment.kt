package com.shabbar.rozgarconnector.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.FragmentProfileBinding
import com.shabbar.rozgarconnector.models.UserModel
import com.shabbar.rozgarconnector.ui.profile.EducatedWorkerProfileActivity
import com.shabbar.rozgarconnector.ui.profile.UneducatedWorkerProfileActivity
import com.shabbar.rozgarconnector.ui.settings.MenuActivity
import com.shabbar.rozgarconnector.utils.TranslatorUtil
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

        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (isAdded && doc != null && doc.exists()) {
                val user = doc.toObject(UserModel::class.java)
                if (user != null) {
                    binding.tvProfileName.text = user.fullName
                    currentUserWorkerType = user.workerType

                    val isEducated = user.workerType == "educated"
                    val isProvider = user.role?.lowercase() != "seeker"
                    val isUrdu = TranslatorUtil.isUrduEnabled(requireContext())

                    // Logic for Role Tag
                    val roleRaw = when {
                        !isProvider -> "Service Seeker"
                        isEducated -> "Educated Provider"
                        else -> "Skilled Provider"
                    }
                    setTextOrTranslate(binding.tvProfileRole, roleRaw, isUrdu)

                    binding.profileRatingBar.rating = user.averageRating

                    loadBase64Image(requireContext(), user.dpBase64, binding.imgProfile, R.drawable.ic_profile)

                    // Basic Details with Label Translation
                    val labelFather = if (isUrdu) "والد:" else "Father:"
                    val labelCnic = if (isUrdu) "شناختی کارڈ:" else "CNIC:"
                    val labelDob = if (isUrdu) "پیدائش:" else "DOB:"
                    val labelPhone = if (isUrdu) "فون:" else "Phone:"
                    val labelAddress = if (isUrdu) "پتہ:" else "Address:"

                    binding.tvFatherName.text = "$labelFather ${user.fatherName ?: "N/A"}"
                    binding.tvCnic.text = "$labelCnic ${user.cnic ?: "N/A"}"
                    binding.tvDob.text = "$labelDob ${user.dob ?: "N/A"}"
                    binding.tvPhone.text = "$labelPhone ${user.phone ?: "N/A"}"
                    
                    val addrRaw = "${user.city ?: ""}, ${user.district ?: ""}"
                    if (isUrdu) {
                        TranslatorUtil.translateText(addrRaw) { binding.tvLocationFull.text = "$labelAddress $it" }
                    } else {
                        binding.tvLocationFull.text = "$labelAddress $addrRaw"
                    }

                    // --- PROVIDER SPECIFIC CARDS ---
                    if (isProvider) {
                        binding.portfolioCard.visibility = View.VISIBLE
                        binding.cardExpSkill.visibility = View.VISIBLE
                        binding.cardProfileEdu.visibility = if (isEducated) View.VISIBLE else View.GONE

                        val expText = if (isUrdu) "تجربہ: ${user.experienceYears} سال\n\n" else "Experience: ${user.experienceYears} Years\n\n"
                        val toolsText = if (user.hasOwnTools == true) (if (isUrdu) "✅ ذاتی اوزار موجود ہیں\n\n" else "✅ Has own tools/equipment\n\n") else ""
                        
                        if (isUrdu) {
                            TranslatorUtil.translateText(user.professionalDescription ?: "") { 
                                binding.tvDescription.text = "$expText$toolsText$it" 
                            }
                        } else {
                            binding.tvDescription.text = "$expText$toolsText${user.professionalDescription ?: "No biography provided."}"
                        }

                        binding.tvExperienceInfo.text = if (isUrdu) "${user.experienceYears ?: "0"} سال" else "${user.experienceYears ?: "0"} Years"
                        setTextOrTranslate(binding.tvExpertise, user.professionalSkill ?: "N/A", isUrdu)

                        if (isEducated) {
                            val eduRaw = "${user.degreeName ?: ""}\n${user.lastDegree ?: ""}"
                            setTextOrTranslate(binding.tvEducation, eduRaw, isUrdu)
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

    private fun setTextOrTranslate(textView: TextView, text: String, isUrdu: Boolean) {
        if (isUrdu) {
            TranslatorUtil.translateText(text) { textView.text = it }
        } else {
            textView.text = text
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}