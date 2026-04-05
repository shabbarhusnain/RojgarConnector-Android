package com.shabbar.rozgarconnector.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.FragmentProviderHomeBinding
import com.shabbar.rozgarconnector.models.JobModel
import com.shabbar.rozgarconnector.adapters.JobAdapter
import com.shabbar.rozgarconnector.ui.job.JobDetailActivity

class HomeFragment : Fragment(R.layout.fragment_provider_home) {

    private var _binding: FragmentProviderHomeBinding? = null
    private val binding get() = _binding

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private var allJobsList = mutableListOf<JobModel>()
    private var filteredJobsList = mutableListOf<JobModel>()
    private lateinit var adapter: JobAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProviderHomeBinding.bind(view)

        setupRecyclerView()
        setupFilters()
        setupSearch()
        loadJobsForProvider()
    }

    private fun setupRecyclerView() {
        if (!isAdded) return
        adapter = JobAdapter(filteredJobsList) { job ->
            val intent = Intent(requireContext(), JobDetailActivity::class.java)
            intent.putExtra("JOB_ID", job.jobId)
            startActivity(intent)
        }
        binding?.rvJobs?.layoutManager = LinearLayoutManager(requireContext())
        binding?.rvJobs?.adapter = adapter
    }

    private fun setupFilters() {
        if (!isAdded) return
        val districts = resources.getStringArray(R.array.pakistan_districts).toMutableList()
        if (!districts.contains("All Districts")) districts.add(0, "All Districts")
        binding?.spinnerDistrict?.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, districts)

        val categories = resources.getStringArray(R.array.educated_categories).toMutableList()
        if (!categories.contains("All Categories")) categories.add(0, "All Categories")
        binding?.spinnerCategory?.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)

        val filterListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) { filterJobs() }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
        binding?.spinnerDistrict?.onItemSelectedListener = filterListener
        binding?.spinnerCategory?.onItemSelectedListener = filterListener
    }

    private fun setupSearch() {
        binding?.etSearchJob?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filterJobs() }
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    private fun loadJobsForProvider() {
        val uid = auth.currentUser?.uid ?: return
        
        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            if (!isAdded || _binding == null) return@addOnSuccessListener
            
            val workerType = (userDoc.getString("workerType") ?: "educated").lowercase()
            binding?.tvHeaderTitle?.text = if (workerType == "educated") "Educated Service Provider" else "Uneducated Service Provider"
            
            binding?.progressBar?.visibility = View.VISIBLE
            
            // Logical Fix: Fetch open jobs matching worker type
            db.collection("jobs")
                .whereEqualTo("workerType", workerType)
                .whereEqualTo("status", "open")
                .addSnapshotListener { snapshots, e ->
                    if (!isAdded || _binding == null) return@addSnapshotListener
                    binding?.progressBar?.visibility = View.GONE
                    
                    if (e != null) {
                        Log.e("HOME_FRAGMENT", "Error: ${e.message}")
                        return@addSnapshotListener
                    }

                    allJobsList.clear()
                    snapshots?.forEach { doc ->
                        try {
                            val job = doc.toObject(JobModel::class.java)
                            job.jobId = doc.id
                            allJobsList.add(job)
                        } catch (ex: Exception) {
                            Log.e("HOME_FRAGMENT", "Parse Error: ${ex.message}")
                        }
                    }
                    filterJobs()
                }
        }
    }

    private fun filterJobs() {
        if (!isAdded || _binding == null) return
        val query = binding?.etSearchJob?.text?.toString()?.lowercase()?.trim() ?: ""
        val district = binding?.spinnerDistrict?.selectedItem?.toString() ?: "All Districts"
        val category = binding?.spinnerCategory?.selectedItem?.toString() ?: "All Categories"

        filteredJobsList.clear()
        for (job in allJobsList) {
            val title = (job.jobTitle ?: "").lowercase()
            val desc = (job.jobDescription ?: "").lowercase()
            
            val matchesSearch = title.contains(query) || desc.contains(query)
            val matchesDistrict = district == "All Districts" || job.district == district
            val matchesCategory = category == "All Categories" || job.category == category

            if (matchesSearch && matchesDistrict && matchesCategory) {
                filteredJobsList.add(job)
            }
        }
        
        adapter.notifyDataSetChanged()
        binding?.tvNoJobs?.visibility = if (filteredJobsList.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}