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
    private var fullJobList = mutableListOf<JobModel>()
    private var filteredList = mutableListOf<JobModel>()
    private lateinit var adapter: JobAdapter
    private var currentUserType: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProviderHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        fetchUserTypeAndLoadJobs()
        setupSearch()
    }

    private fun setupRecyclerView() {
        adapter = JobAdapter(filteredList) { job ->
            val intent = Intent(requireContext(), JobDetailActivity::class.java)
            intent.putExtra("JOB_ID", job.jobId)
            startActivity(intent)
        }
        binding.rvJobs.layoutManager = LinearLayoutManager(requireContext())
        binding.rvJobs.adapter = adapter
    }

    private fun fetchUserTypeAndLoadJobs() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (isAdded && doc.exists()) {
                currentUserType = doc.getString("workerType") ?: ""
                setupFilters(currentUserType)
                loadJobs(currentUserType)
            }
        }
    }

    private fun setupFilters(type: String) {
        // District Filter
        val districts = resources.getStringArray(R.array.pakistan_districts).toMutableList()
        districts.add(0, "All Districts")
        binding.spinnerDistrict.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, districts)

        // Category Filter based on worker type
        val categories = mutableListOf<String>()
        categories.add("All Categories")
        if (type == "educated") {
            categories.addAll(resources.getStringArray(R.array.educated_categories))
        } else {
            categories.addAll(resources.getStringArray(R.array.skill_categories))
        }
        
        binding.spinnerCategory.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories.distinct())

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) { filterData() }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
        binding.spinnerDistrict.onItemSelectedListener = listener
        binding.spinnerCategory.onItemSelectedListener = listener
    }

    private fun setupSearch() {
        binding.etSearchJob.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filterData() }
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    private fun loadJobs(type: String) {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("jobs")
            .whereEqualTo("status", "open")
            .whereEqualTo("workerType", type) // Filter jobs based on worker's type (educated/uneducated)
            .addSnapshotListener { snapshots, e ->
                if (!isAdded) return@addSnapshotListener
                binding.progressBar.visibility = View.GONE
                if (e != null || snapshots == null) return@addSnapshotListener

                fullJobList.clear()
                snapshots.forEach { doc ->
                    val job = doc.toObject(JobModel::class.java)
                    job.jobId = doc.id
                    fullJobList.add(job)
                }
                filterData()
            }
    }

    private fun filterData() {
        val query = binding.etSearchJob.text.toString().lowercase().trim()
        val district = binding.spinnerDistrict.selectedItem?.toString() ?: "All Districts"
        val category = binding.spinnerCategory.selectedItem?.toString() ?: "All Categories"

        filteredList.clear()
        for (job in fullJobList) {
            val matchesSearch = job.jobTitle.lowercase().contains(query) || job.jobDescription.lowercase().contains(query)
            val matchesDistrict = district == "All Districts" || job.district == district
            val matchesCategory = category == "All Categories" || job.category == category

            if (matchesSearch && matchesDistrict && matchesCategory) {
                filteredList.add(job)
            }
        }
        adapter.notifyDataSetChanged()
        binding.tvNoJobs.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}