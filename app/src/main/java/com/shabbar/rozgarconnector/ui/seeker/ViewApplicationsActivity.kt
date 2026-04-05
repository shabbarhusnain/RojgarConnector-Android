package com.shabbar.rozgarconnector.ui.seeker

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.adapters.ApplicationAdapter
import com.shabbar.rozgarconnector.databinding.ActivityViewApplicationsBinding
import com.shabbar.rozgarconnector.models.ApplicationModel

class ViewApplicationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewApplicationsBinding
    private val db = FirebaseFirestore.getInstance()
    private var applicationList = mutableListOf<ApplicationModel>()
    private lateinit var adapter: ApplicationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewApplicationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val jobId = intent.getStringExtra("JOB_ID") ?: return

        // Setup Adapter - Fixed: Now only takes one argument
        adapter = ApplicationAdapter(applicationList)

        binding.rvApplications.layoutManager = LinearLayoutManager(this)
        binding.rvApplications.adapter = adapter

        loadApplications(jobId)
    }

    private fun loadApplications(jobId: String) {
        db.collection("applications")
            .whereEqualTo("jobId", jobId)
            .get()
            .addOnSuccessListener { snapshots ->
                applicationList.clear()
                for (doc in snapshots) {
                    val app = doc.toObject(ApplicationModel::class.java)
                    app.applicationId = doc.id
                    applicationList.add(app)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}