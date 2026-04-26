package com.shabbar.rozgarconnector.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.adapters.JobAdapter
import com.shabbar.rozgarconnector.databinding.FragmentProviderHomeBinding
import com.shabbar.rozgarconnector.models.JobModel
import com.shabbar.rozgarconnector.ui.job.JobDetailActivity

class ProviderHomeFragment : Fragment() {

    private var _binding: FragmentProviderHomeBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private lateinit var adapter: JobAdapter
    private val allJobs = mutableListOf<JobModel>()
    private val filteredJobs = mutableListOf<JobModel>()
    private var currentUserWorkerType: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProviderHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFilters()
        setupSearch()
        fetchUserTypeAndLoadJobs()

        binding.swipeRefresh.setOnRefreshListener {
            fetchUserTypeAndLoadJobs()
        }
    }

    private fun setupRecyclerView() {
        adapter = JobAdapter(filteredJobs) { job ->
            startActivity(Intent(requireContext(), JobDetailActivity::class.java).apply {
                putExtra("JOB_ID", job.jobId)
            })
        }
        binding.rvJobs.layoutManager = LinearLayoutManager(requireContext())
        binding.rvJobs.adapter = adapter
    }

    private fun setupFilters() {
        // District Filter
        val districts = resources.getStringArray(R.array.pakistan_districts).toMutableList()
        if (!districts.contains("All Districts")) districts.add(0, "All Districts")
        binding.spinnerDistrictFilter.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, districts)

        // Skill Filter
        updateCategorySpinner()

        val filterListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) { filterData() }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
        binding.spinnerDistrictFilter.onItemSelectedListener = filterListener
        binding.spinnerSkillFilter.onItemSelectedListener = filterListener
    }

    private fun updateCategorySpinner() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (isAdded) {
                val type = doc.getString("workerType") ?: "educated"
                val categories = if (type == "educated") {
                    resources.getStringArray(R.array.educated_job_categories).toMutableList()
                } else {
                    resources.getStringArray(R.array.uneducated_skill_categories).toMutableList()
                }
                if (!categories.contains("All Categories")) categories.add(0, "All Categories")
                binding.spinnerSkillFilter.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
            }
        }
    }

    private fun setupSearch() {
        binding.etSearchJob.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filterData() }
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    private fun fetchUserTypeAndLoadJobs() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (isAdded) {
                currentUserWorkerType = doc.getString("workerType") ?: "educated"
                loadOpenJobsForMe()
            }
        }
    }

    private fun loadOpenJobsForMe() {
        if (currentUserWorkerType == null) return
        binding.swipeRefresh.isRefreshing = true
        
        db.collection("jobs")
            .whereEqualTo("status", "open")
            .whereEqualTo("workerType", currentUserWorkerType)
            .addSnapshotListener { snapshots, _ ->
                if (!isAdded) return@addSnapshotListener
                binding.swipeRefresh.isRefreshing = false
                allJobs.clear()
                snapshots?.forEach { doc ->
                    val job = doc.toObject(JobModel::class.java).apply { jobId = doc.id }
                    allJobs.add(job)
                }
                filterData()
            }
    }

    private fun filterData() {
        val query = binding.etSearchJob.text.toString().lowercase().trim()
        val district = binding.spinnerDistrictFilter.selectedItem?.toString() ?: "All Districts"
        val category = binding.spinnerSkillFilter.selectedItem?.toString() ?: "All Categories"

        filteredJobs.clear()
        for (job in allJobs) {
            val titleMatch = job.jobTitle?.lowercase()?.contains(query) ?: false
            val districtMatch = district == "All Districts" || job.district == district
            val categoryMatch = category == "All Categories" || job.category == category

            if (titleMatch && districtMatch && categoryMatch) {
                filteredJobs.add(job)
            }
        }
        adapter.notifyDataSetChanged()
        binding.llEmptyState.visibility = if (filteredJobs.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}