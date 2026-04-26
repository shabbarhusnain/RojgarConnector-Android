package com.shabbar.rozgarconnector.ui.job

import android.app.DatePickerDialog
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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.ActivityJobPostBinding
import com.shabbar.rozgarconnector.models.JobModel
import com.shabbar.rozgarconnector.utils.loadBase64Image
import java.io.ByteArrayOutputStream
import java.util.*

class JobPostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJobPostBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var jobPhotoBase64: String? = null
    private var editJobId: String? = null

    private val pickJobPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            binding.ivJobPoster.setImageURI(uri)
            binding.ivJobPoster.setPadding(0, 0, 0, 0)
            jobPhotoBase64 = uriToBase64(uri)
            binding.ivRemovePhoto.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJobPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDistricts()
        setupCategories()
        
        editJobId = intent.getStringExtra("EDIT_JOB_ID")
        if (editJobId != null) {
            binding.tvTitleHeader.text = "Edit Job Post"
            binding.btnPublishJob.text = "Update Job"
            loadJobDataForEdit(editJobId!!)
        }
        
        binding.btnBack.setOnClickListener { finish() }
        binding.etLastDate.setOnClickListener { showDatePicker() }
        binding.btnUploadJobPhoto.setOnClickListener { pickJobPhoto.launch("image/*") }
        binding.btnPublishJob.setOnClickListener { validateAndPublish() }

        binding.ivRemovePhoto.setOnClickListener {
            jobPhotoBase64 = null
            binding.ivJobPoster.setImageResource(R.drawable.ic_add_circle)
            binding.ivJobPoster.setPadding(24, 24, 24, 24)
            binding.ivRemovePhoto.visibility = View.GONE
        }
    }

    private fun loadJobDataForEdit(id: String) {
        db.collection("jobs").document(id).get().addOnSuccessListener { doc ->
            val job = doc.toObject(JobModel::class.java)
            job?.let {
                binding.etJobTitle.setText(it.jobTitle)
                binding.etWorkplaceAddress.setText(it.workplaceAddress)
                binding.etWorkplaceName.setText(it.workplaceName)
                binding.etCompanyIntro.setText(it.companyIntro)
                binding.etJobDescription.setText(it.jobDescription)
                binding.etQualifications.setText(it.qualifications)
                binding.etPayAmount.setText(it.payAmount)
                binding.etBenefits.setText(it.benefits)
                binding.etLastDate.setText(it.lastDateToApply)
                binding.cbIsNegotiable.isChecked = it.isNegotiable
                
                jobPhotoBase64 = it.jobPhotoBase64
                if (!jobPhotoBase64.isNullOrEmpty()) {
                    loadBase64Image(this, jobPhotoBase64, binding.ivJobPoster, R.drawable.ic_add_circle)
                    binding.ivJobPoster.setPadding(0, 0, 0, 0)
                    binding.ivRemovePhoto.visibility = View.VISIBLE
                }

                // Set Spinners
                (binding.spinnerDistrict.adapter as? ArrayAdapter<String>)?.let { adapter ->
                    val pos = adapter.getPosition(it.district)
                    if (pos >= 0) binding.spinnerDistrict.setSelection(pos)
                }
                (binding.spinnerJobCategory.adapter as? ArrayAdapter<String>)?.let { adapter ->
                    val pos = adapter.getPosition(it.category)
                    if (pos >= 0) binding.spinnerJobCategory.setSelection(pos)
                }
            }
        }
    }

    private fun setupDistricts() {
        val districts = resources.getStringArray(R.array.pakistan_districts)
        binding.spinnerDistrict.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, districts)
    }

    private fun setupCategories() {
        val categories = resources.getStringArray(R.array.educated_job_categories)
        binding.spinnerJobCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
    }

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d -> binding.etLastDate.setText("$d/${m + 1}/$y") },
            c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun validateAndPublish() {
        val title = binding.etJobTitle.text.toString().trim()
        val amount = binding.etPayAmount.text.toString().trim()
        
        if (title.isEmpty() || amount.isEmpty()) {
            Toast.makeText(this, "Please fill main fields", Toast.LENGTH_SHORT).show()
            return
        }

        val id = editJobId ?: db.collection("jobs").document().id
        val jobData = JobModel().apply {
            this.jobId = id; this.seekerId = auth.currentUser?.uid; this.jobTitle = title
            this.district = binding.spinnerDistrict.selectedItem.toString()
            this.category = binding.spinnerJobCategory.selectedItem.toString()
            this.workerType = "educated"; this.workplaceAddress = binding.etWorkplaceAddress.text.toString()
            this.workplaceName = binding.etWorkplaceName.text.toString()
            this.companyIntro = binding.etCompanyIntro.text.toString()
            this.jobDescription = binding.etJobDescription.text.toString()
            this.qualifications = binding.etQualifications.text.toString()
            this.payAmount = amount; this.benefits = binding.etBenefits.text.toString()
            this.lastDateToApply = binding.etLastDate.text.toString()
            this.isNegotiable = binding.cbIsNegotiable.isChecked
            this.jobPhotoBase64 = jobPhotoBase64
            this.status = "open"; this.timestamp = Timestamp.now()
        }

        db.collection("jobs").document(id).set(jobData).addOnSuccessListener {
            Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun uriToBase64(uri: Uri): String {
        return try {
            val stream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(stream)
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 25, out)
            Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) { "" }
    }
}