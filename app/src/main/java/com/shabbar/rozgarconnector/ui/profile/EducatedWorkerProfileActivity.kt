package com.shabbar.rozgarconnector.ui.profile

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.ActivityEducatedWorkerProfileBinding
import com.shabbar.rozgarconnector.ui.home.ProviderHomeActivity
import java.io.ByteArrayOutputStream

class EducatedWorkerProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEducatedWorkerProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var degreeImageUri: Uri? = null

    private val pickDegree = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        degreeImageUri = uri
        if (uri != null) {
            binding.imgDegreePreview.visibility = View.VISIBLE
            binding.imgDegreePreview.setImageURI(uri)
            binding.btnUploadLastDegreeImage.text = "Degree Photo Selected ✅"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEducatedWorkerProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinners()

        binding.btnUploadLastDegreeImage.setOnClickListener { pickDegree.launch("image/*") }
        binding.btnSubmitPortfolio.setOnClickListener { handleSubmit() }
        
        // Hide/Show "Last place of work" based on experience years
        binding.etExperienceYears.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val years = binding.etExperienceYears.text.toString().trim()
                if (years.isNotEmpty() && years.toInt() > 0) {
                    binding.etLastWorkPlace.visibility = View.VISIBLE
                } else {
                    binding.etLastWorkPlace.visibility = View.GONE
                }
            }
        }
    }

    private fun setupSpinners() {
        // Updated Last Degree Spinner (Simplified as per request)
        val degrees = arrayOf("Metric", "Intermediate", "BS", "Master", "PhD")
        binding.spinnerLastDegree.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, degrees).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Skills Spinner (from resources)
        val skills = resources.getStringArray(R.array.educated_categories).toMutableList()
        if (!skills.contains("Other")) skills.add("Other")
        
        binding.spinnerSkillCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, skills).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerSkillCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                if (skills[p2] == "Other") {
                    binding.etCustomSkill.visibility = View.VISIBLE
                } else {
                    binding.etCustomSkill.visibility = View.GONE
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun handleSubmit() {
        val degreeTitle = binding.etDegreeTitle.text.toString().trim()
        val board = binding.etBoardUniversity.text.toString().trim()
        val percentage = binding.etPercentage.text.toString().trim()
        val description = binding.etProfessionalDescription.text.toString().trim()
        val years = binding.etExperienceYears.text.toString().trim()
        val lastPlace = binding.etLastWorkPlace.text.toString().trim()
        val skill = if (binding.spinnerSkillCategory.selectedItem == "Other") {
            binding.etCustomSkill.text.toString().trim()
        } else {
            binding.spinnerSkillCategory.selectedItem.toString()
        }

        if (degreeTitle.isEmpty() || board.isEmpty() || percentage.isEmpty() || description.isEmpty() || skill.isEmpty() || degreeImageUri == null) {
            Toast.makeText(this, "Please fill all fields and upload degree image!", Toast.LENGTH_SHORT).show()
            return
        }

        if (!binding.cbSurety.isChecked) {
            Toast.makeText(this, "Please check the surety box!", Toast.LENGTH_SHORT).show()
            return
        }

        val portfolioData = hashMapOf(
            "lastDegree" to binding.spinnerLastDegree.selectedItem.toString(),
            "degreeName" to degreeTitle, // This is what the user asked for
            "boardUniversity" to board,
            "percentageCGPA" to percentage,
            "professionalSkill" to skill,
            "experienceYears" to years,
            "lastWorkPlace" to lastPlace,
            "professionalDescription" to description,
            "degreePhotoBase64" to uriToBase64(degreeImageUri!!),
            "profileCompleted" to true,
            "role" to "Worker",
            "workerType" to "educated"
        )

        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update(portfolioData as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(this, "Portfolio Completed!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, ProviderHomeActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uriToBase64(uri: Uri): String {
        val inputStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 15, out)
        return Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
    }
}