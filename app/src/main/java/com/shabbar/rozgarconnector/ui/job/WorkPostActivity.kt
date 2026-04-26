package com.shabbar.rozgarconnector.ui.job

import android.app.DatePickerDialog
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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.ActivityWorkPostBinding
import com.shabbar.rozgarconnector.models.JobModel
import com.shabbar.rozgarconnector.utils.loadBase64Image
import java.io.ByteArrayOutputStream
import java.util.*

class WorkPostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkPostBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var workPhotoBase64: String? = null
    private var editJobId: String? = null

    private val pickWorkPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            binding.ivWorkPhoto.setImageURI(uri)
            binding.ivWorkPhoto.setPadding(0, 0, 0, 0)
            workPhotoBase64 = uriToBase64(uri)
            binding.ivRemovePhoto.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinners()
        
        editJobId = intent.getStringExtra("EDIT_JOB_ID")
        if (editJobId != null) {
            binding.tvTitleHeader.text = "Edit Manual Work"
            binding.btnPublishWork.text = "Update Post"
            loadWorkDataForEdit(editJobId!!)
        }
        
        binding.btnBack.setOnClickListener { finish() }
        binding.etWorkDate.setOnClickListener { showDatePicker() }
        binding.btnUploadWorkPhoto.setOnClickListener { pickWorkPhoto.launch("image/*") }
        binding.btnPublishWork.setOnClickListener { validateAndPublish() }

        binding.ivRemovePhoto.setOnClickListener {
            workPhotoBase64 = null
            binding.ivWorkPhoto.setImageResource(R.drawable.ic_add_circle)
            binding.ivWorkPhoto.setPadding(40, 40, 40, 40)
            binding.ivRemovePhoto.visibility = View.GONE
        }
    }

    private fun loadWorkDataForEdit(id: String) {
        db.collection("jobs").document(id).get().addOnSuccessListener { doc ->
            val job = doc.toObject(JobModel::class.java)
            job?.let {
                binding.etWorkTitle.setText(it.jobTitle)
                binding.etWorkDescription.setText(it.jobDescription)
                binding.etWorkAddress.setText(it.workplaceAddress)
                binding.etWorkBudget.setText(it.payAmount)
                binding.etWorkDate.setText(it.lastDateToApply)
                binding.cbIsNegotiable.isChecked = it.isNegotiable
                binding.cbIsUrgent.isChecked = it.qualifications == "URGENT"
                
                if (it.toolsProvidedBy == "Seeker") binding.rbToolsSeeker.isChecked = true 
                else binding.rbToolsWorker.isChecked = true

                workPhotoBase64 = it.jobPhotoBase64
                if (!workPhotoBase64.isNullOrEmpty()) {
                    loadBase64Image(this, workPhotoBase64, binding.ivWorkPhoto, R.drawable.ic_add_circle)
                    binding.ivWorkPhoto.setPadding(0, 0, 0, 0)
                    binding.ivRemovePhoto.visibility = View.VISIBLE
                }

                // Set Spinners
                (binding.spinnerDistrict.adapter as? ArrayAdapter<String>)?.let { adapter ->
                    val pos = adapter.getPosition(it.district)
                    if (pos >= 0) binding.spinnerDistrict.setSelection(pos)
                }
                (binding.spinnerWorkCategory.adapter as? ArrayAdapter<String>)?.let { adapter ->
                    val pos = adapter.getPosition(it.category)
                    if (pos >= 0) binding.spinnerWorkCategory.setSelection(pos)
                }
            }
        }
    }

    private fun setupSpinners() {
        val districts = resources.getStringArray(R.array.pakistan_districts)
        binding.spinnerDistrict.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, districts)

        val categories = resources.getStringArray(R.array.uneducated_skill_categories)
        binding.spinnerWorkCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        val strategies = arrayOf("Fixed Price", "Quote Requested", "Per hour Rate", "Daily Wage")
        binding.spinnerPaymentType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, strategies)

        binding.spinnerPaymentType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                if (position == 1) {
                    binding.etWorkBudget.visibility = View.GONE
                    binding.cbIsNegotiable.visibility = View.GONE
                } else {
                    binding.etWorkBudget.visibility = View.VISIBLE
                    binding.cbIsNegotiable.visibility = View.VISIBLE
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d -> binding.etWorkDate.setText("$d/${m + 1}/$y") },
            c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun validateAndPublish() {
        val title = binding.etWorkTitle.text.toString().trim()
        if (title.isEmpty()) { Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show(); return }

        val id = editJobId ?: db.collection("jobs").document().id
        val toolsBy = if (binding.rbToolsSeeker.isChecked) "Seeker" else "Worker"
        
        val jobData = JobModel().apply {
            this.jobId = id; this.seekerId = auth.currentUser?.uid; this.jobTitle = title
            this.jobDescription = binding.etWorkDescription.text.toString()
            this.district = binding.spinnerDistrict.selectedItem.toString()
            this.category = binding.spinnerWorkCategory.selectedItem.toString()
            this.workerType = "uneducated"; this.workplaceAddress = binding.etWorkAddress.text.toString()
            this.payAmount = if (binding.spinnerPaymentType.selectedItemPosition == 1) "Quote Requested" else binding.etWorkBudget.text.toString()
            this.payUnit = binding.spinnerPaymentType.selectedItem.toString()
            this.toolsProvidedBy = toolsBy; this.lastDateToApply = binding.etWorkDate.text.toString()
            this.isNegotiable = binding.cbIsNegotiable.isChecked; this.jobPhotoBase64 = workPhotoBase64
            this.status = "open"; this.timestamp = Timestamp.now()
            this.qualifications = if (binding.cbIsUrgent.isChecked) "URGENT" else ""
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