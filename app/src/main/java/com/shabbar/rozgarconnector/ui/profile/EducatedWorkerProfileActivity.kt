package com.shabbar.rozgarconnector.ui.profile

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
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

    private var projectPhotoUri: Uri? = null
    private var base64ProjectPhoto: String? = null

    private val pickProjectPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            projectPhotoUri = uri
            binding.ivProjectPhoto.setImageURI(uri)
            binding.ivProjectPhoto.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            binding.btnRemoveProjectPhoto.visibility = View.VISIBLE
            base64ProjectPhoto = uriToBase64(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEducatedWorkerProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinners()
        loadExistingData()

        binding.btnUploadProjectPhoto.setOnClickListener { pickProjectPhoto.launch("image/*") }
        
        binding.btnRemoveProjectPhoto.setOnClickListener {
            projectPhotoUri = null
            base64ProjectPhoto = null
            binding.ivProjectPhoto.setImageResource(R.drawable.ic_add_circle)
            binding.ivProjectPhoto.scaleType = android.widget.ImageView.ScaleType.CENTER
            binding.btnRemoveProjectPhoto.visibility = View.GONE
        }

        binding.btnSubmitPortfolio.setOnClickListener { handleSubmit() }
    }

    private fun loadExistingData() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                binding.etDegreeTitle.setText(doc.getString("degreeName"))
                binding.etProfessionalServices.setText(doc.getString("professionalSkill"))
                binding.etExperienceYears.setText(doc.getString("experienceYears"))
                binding.etLastWorkPlace.setText(doc.getString("lastWorkPlace"))
                binding.etJobTitle.setText(doc.getString("jobTitle"))
                binding.etDuration.setText(doc.getString("employmentDuration"))
                binding.etProfessionalDescription.setText(doc.getString("professionalDescription"))
                binding.etCertifications.setText(doc.getString("certifications"))
                
                val photo = doc.getString("projectPhotoBase64")
                if (!photo.isNullOrEmpty()) {
                    base64ProjectPhoto = photo
                    com.shabbar.rozgarconnector.utils.decodeBase64BitmapAsync(photo, {
                        binding.ivProjectPhoto.setImageBitmap(it)
                        binding.ivProjectPhoto.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                        binding.btnRemoveProjectPhoto.visibility = View.VISIBLE
                    }, {})
                }

                val lastDegree = doc.getString("lastDegree")
                if (lastDegree != null) {
                    val adapter = binding.spinnerLastDegree.adapter as? ArrayAdapter<String>
                    val pos = adapter?.getPosition(lastDegree) ?: -1
                    if (pos >= 0) binding.spinnerLastDegree.setSelection(pos)
                }

                val skillCat = doc.getString("skills")
                if (skillCat != null) {
                    val adapter = binding.spinnerSkillCategory.adapter as? ArrayAdapter<String>
                    val pos = adapter?.getPosition(skillCat) ?: -1
                    if (pos >= 0) binding.spinnerSkillCategory.setSelection(pos)
                }
            }
        }
    }

    private fun setupSpinners() {
        val degrees = arrayOf("Metric", "Intermediate", "BS", "Master", "PhD")
        binding.spinnerLastDegree.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, degrees).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val skills = resources.getStringArray(R.array.educated_job_categories).toMutableList()
        if (!skills.contains("Other")) skills.add("Other")
        
        binding.spinnerSkillCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, skills).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun handleSubmit() {
        val degreeTitle = binding.etDegreeTitle.text.toString().trim()
        val services = binding.etProfessionalServices.text.toString().trim()
        val description = binding.etProfessionalDescription.text.toString().trim()
        val years = binding.etExperienceYears.text.toString().trim()

        if (degreeTitle.isEmpty() || services.isEmpty() || description.isEmpty() || years.isEmpty()) {
            Toast.makeText(this, "Please fill main fields!", Toast.LENGTH_SHORT).show()
            return
        }

        val portfolioData = mutableMapOf<String, Any>(
            "lastDegree" to binding.spinnerLastDegree.selectedItem.toString(),
            "degreeName" to degreeTitle,
            "professionalSkill" to services,
            "skills" to binding.spinnerSkillCategory.selectedItem.toString(),
            "experienceYears" to years,
            "lastWorkPlace" to binding.etLastWorkPlace.text.toString().trim(),
            "jobTitle" to binding.etJobTitle.text.toString().trim(),
            "employmentDuration" to binding.etDuration.text.toString().trim(),
            "certifications" to binding.etCertifications.text.toString().trim(),
            "professionalDescription" to description,
            "profileCompleted" to true,
            "role" to "provider", // FIXED: Always save as provider (lowercase)
            "workerType" to "educated"
        )
        
        portfolioData["projectPhotoBase64"] = base64ProjectPhoto ?: ""

        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update(portfolioData)
            .addOnSuccessListener {
                Toast.makeText(this, "Portfolio Updated Successfully!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, ProviderHomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uriToBase64(uri: Uri): String {
        return try {
            val stream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(stream)
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 20, out)
            Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) { "" }
    }
}