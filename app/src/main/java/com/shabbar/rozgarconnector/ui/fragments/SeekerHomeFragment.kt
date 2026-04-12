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
import com.shabbar.rozgarconnector.adapters.WorkerAdapter
import com.shabbar.rozgarconnector.databinding.FragmentSeekerHomeBinding
import com.shabbar.rozgarconnector.models.UserModel
import com.shabbar.rozgarconnector.ui.settings.MenuActivity
import com.shabbar.rozgarconnector.utils.TranslatorUtil

class SeekerHomeFragment : Fragment() {

    private var _binding: FragmentSeekerHomeBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private var fullWorkerList = mutableListOf<UserModel>()
    private var filteredList = mutableListOf<UserModel>()
    private lateinit var adapter: WorkerAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSeekerHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilters()
        setupSearch()

        // Initial Load (Educated by default or current state)
        val initialEducated = binding.btnEducated.isChecked
        updateCategorySpinner(initialEducated)
        loadWorkers(initialEducated)

        binding.workerToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val isEducated = (checkedId == R.id.btnEducated)
                updateCategorySpinner(isEducated)
                loadWorkers(isEducated)
            }
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadWorkers(binding.btnEducated.isChecked)
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(requireContext(), MenuActivity::class.java))
        }

        // --- UI TRANSLATION ---
        if (TranslatorUtil.isUrduEnabled(requireContext())) {
            TranslatorUtil.initTranslator(
                onSuccess = { translateUI() },
                onFailure = { }
            )
        }
    }

    private fun translateUI() {
        if (!isAdded) return
        TranslatorUtil.translateText("Service Seeker") { binding.tvHeaderTitle.text = it }
        TranslatorUtil.translateText("Find top rated workers near you") { binding.tvSubtitle.text = it }
        TranslatorUtil.translateText("Educated Worker") { binding.btnEducated.text = it }
        TranslatorUtil.translateText("Uneducated Worker") { binding.btnUneducated.text = it }
        TranslatorUtil.translateText("Search by name or category...") { binding.etSearchWorker.hint = it }
    }

    private fun setupRecyclerView() {
        adapter = WorkerAdapter(filteredList)
        binding.rvWorkers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWorkers.adapter = adapter
    }

    private fun setupFilters() {
        // District Filter
        val districts = resources.getStringArray(R.array.pakistan_districts).toMutableList()
        if (!districts.contains("All Districts")) districts.add(0, "All Districts")
        binding.spinnerDistrict.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, districts)

        val filterListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) { filterData() }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
        binding.spinnerDistrict.onItemSelectedListener = filterListener
        binding.spinnerSkill.onItemSelectedListener = filterListener
    }

    private fun updateCategorySpinner(isEducated: Boolean) {
        val categories = if (isEducated) {
            resources.getStringArray(R.array.educated_categories).toMutableList()
        } else {
            resources.getStringArray(R.array.skill_categories).toMutableList()
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

    private fun loadWorkers(isEducated: Boolean) {
        val typeToFilter = if (isEducated) "educated" else "uneducated"
        binding.swipeRefresh.isRefreshing = true

        // Logical Optimization: Fetch all verified workers of this type, filter locally for speed
        db.collection("users")
            .whereEqualTo("role", "Worker")
            .whereEqualTo("workerType", typeToFilter)
            .whereEqualTo("isVerified", true)
            .get()
            .addOnSuccessListener { snapshots ->
                if (!isAdded) return@addOnSuccessListener
                binding.swipeRefresh.isRefreshing = false

                fullWorkerList.clear()
                snapshots?.forEach { doc ->
                    val worker = doc.toObject(UserModel::class.java)
                    worker.uid = doc.id
                    fullWorkerList.add(worker)
                }
                filterData()
            }
            .addOnFailureListener {
                binding.swipeRefresh.isRefreshing = false
            }
    }

    private fun filterData() {
        val query = binding.etSearchWorker.text.toString().lowercase().trim()
        val district = binding.spinnerDistrict.selectedItem?.toString() ?: "All Districts"
        val category = binding.spinnerSkill.selectedItem?.toString() ?: "All Categories"

        filteredList.clear()
        for (worker in fullWorkerList) {
            // 1. Search Logic (Name)
            val nameMatch = worker.fullName.lowercase().contains(query)
            
            // 2. District Filter
            val districtMatch = district == "All Districts" || worker.district == district
            
            // 3. Category/Skill Smart Filter
            val skillField = if (worker.workerType == "educated") {
                // For educated, we check both their skill and degree name for relevance
                "${worker.professionalSkill} ${worker.degreeName}".lowercase()
            } else {
                worker.professionalSkill?.lowercase() ?: ""
            }
            
            val categoryMatch = if (category == "All Categories") true 
                               else skillField.contains(category.lowercase())

            if (nameMatch && districtMatch && categoryMatch) {
                filteredList.add(worker)
            }
        }
        adapter.notifyDataSetChanged()
        binding.tvNoResults.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}