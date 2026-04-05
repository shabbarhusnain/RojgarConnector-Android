package com.shabbar.rozgarconnector.ui.provider

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.adapters.ApplicationAdapter
import com.shabbar.rozgarconnector.databinding.ActivityProviderApplicationsBinding
import com.shabbar.rozgarconnector.models.ApplicationModel

class ProviderApplicationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProviderApplicationsBinding
    private lateinit var adapter: ApplicationAdapter
    private var applicationList = mutableListOf<ApplicationModel>()
    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProviderApplicationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar fix
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        loadApplications()
    }

    private fun setupRecyclerView() {
        // Fix: ApplicationAdapter now only takes the list as an argument
        adapter = ApplicationAdapter(applicationList)
        
        binding.applicationsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.applicationsRecyclerView.adapter = adapter
    }

    private fun loadApplications() {
        if (uid == null) return

        db.collection("applications")
            .whereEqualTo("providerId", uid)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                applicationList.clear()
                snapshots?.forEach { doc ->
                    val app = doc.toObject(ApplicationModel::class.java)
                    app.applicationId = doc.id
                    applicationList.add(app)
                }
                adapter.notifyDataSetChanged()
            }
    }
}