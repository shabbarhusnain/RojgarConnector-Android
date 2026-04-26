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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.adapters.WorkerAdapter
import com.shabbar.rozgarconnector.databinding.FragmentSeekerHomeBinding
import com.shabbar.rozgarconnector.models.UserModel
import com.shabbar.rozgarconnector.ui.settings.NotificationsActivity

class SeekerHomeFragment : Fragment() {

    private var _binding: FragmentSeekerHomeBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private var workerListener: ListenerRegistration? = null
    
    private var fullWorkerList = mutableListOf<UserModel>()
    private var filteredList = mutableListOf<UserModel>()
    private lateinit var adapter: WorkerAdapter
    private var isEducatedSelected = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSeekerHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilters()
        setupSearch()
        setupToggleLogic()

        updateToggleUI()
        startWorkerListener(isEducatedSelected)

        binding.swipeRefresh.setOnRefreshListener {
            startWorkerListener(isEducatedSelected)
        }

        binding.btnAnnouncements.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationsActivity::class.java))
        }
    }

    private fun startWorkerListener(isEducated: Boolean) {
        workerListener?.remove()
        
        val typeToFilter = if (isEducated) "educated" else "uneducated"
        binding.swipeRefresh.isRefreshing = true

        // WORKFLOW REPAIR: Simplified role query to only 'provider'
        workerListener = db.collection("users")
            .whereEqualTo("role", "provider")
            .whereEqualTo("workerType", typeToFilter)
            .whereEqualTo("isVerified", true)
            .addSnapshotListener { snapshots, e ->
                if (!isAdded) return@addSnapshotListener
                binding.swipeRefresh.isRefreshing = false

                if (snapshots != null) {
                    fullWorkerList.clear()
                    snapshots.forEach { doc ->
                        val worker = doc.toObject(UserModel::class.java).apply { uid = doc.id }
                        fullWorkerList.add(worker)
                    }
                    filterData()
                }
            }
    }

    private fun setupToggleLogic() {
        binding.btnEducated.setOnClickListener {
            if (!isEducatedSelected) {
                isEducatedSelected = true
                updateToggleUI()
                updateCategorySpinner(true)
                startWorkerListener(true)
            }
        }

        binding.btnUneducated.setOnClickListener {
            if (isEducatedSelected) {
                isEducatedSelected = false
                updateToggleUI()
                updateCategorySpinner(false)
                startWorkerListener(false)
            }
        }
    }

    private fun updateToggleUI() {
        if (isEducatedSelected) {
            binding.btnEducated.setBackgroundResource(R.drawable.toggle_selected_bg)
            binding.btnEducated.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_green))
            binding.btnUneducated.setBackgroundResource(0)
            binding.btnUneducated.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_grey))
        } else {
            binding.btnUneducated.setBackgroundResource(R.drawable.toggle_selected_bg)
            binding.btnUneducated.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_green))
            binding.btnEducated.setBackgroundResource(0)
            binding.btnEducated.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_grey))
        }
    }

    private fun setupRecyclerView() {
        adapter = WorkerAdapter(filteredList)
        binding.rvWorkers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWorkers.adapter = adapter
    }

    private fun setupFilters() {
        val districts = resources.getStringArray(R.array.pakistan_districts).toMutableList()
        if (!districts.contains("All Districts")) districts.add(0, "All Districts")
        binding.spinnerDistrict.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, districts)

        val filterListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) { filterData() }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
        binding.spinnerDistrict.onItemSelectedListener = filterListener
        binding.spinnerSkill.onItemSelectedListener = filterListener
        updateCategorySpinner(isEducatedSelected)
    }

    private fun updateCategorySpinner(isEducated: Boolean) {
        val categories = if (isEducated) {
            resources.getStringArray(R.array.educated_job_categories).toMutableList()
        } else {
            resources.getStringArray(R.array.uneducated_skill_categories).toMutableList()
        }
        if (!categories.contains("All Categories")) categories.add(0, "All Categories")
        binding.spinnerSkill.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
    }

    private fun setupSearch() {
        binding.etSearchWorker.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filterData() }
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    private fun filterData() {
        val query = binding.etSearchWorker.text.toString().lowercase().trim()
        val district = binding.spinnerDistrict.selectedItem?.toString() ?: "All Districts"
        val category = binding.spinnerSkill.selectedItem?.toString() ?: "All Categories"

        filteredList.clear()
        for (worker in fullWorkerList) {
            val nameMatch = worker.fullName?.lowercase()?.contains(query) ?: false
            val districtMatch = district == "All Districts" || worker.district == district
            
            // WORKFLOW REPAIR: Check both 'professionalSkill' and 'skills' fields
            val skillField = "${worker.professionalSkill ?: ""} ${worker.skills ?: ""} ${worker.degreeName ?: ""}".lowercase()
            val categoryMatch = if (category == "All Categories") true else skillField.contains(category.lowercase())

            if (nameMatch && districtMatch && categoryMatch) {
                filteredList.add(worker)
            }
        }
        adapter.notifyDataSetChanged()
        binding.llEmptyState.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        workerListener?.remove()
        super.onDestroyView()
        _binding = null
    }
}