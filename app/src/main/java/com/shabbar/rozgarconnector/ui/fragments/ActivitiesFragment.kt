package com.shabbar.rozgarconnector.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.adapters.ActivitiesAdapter
import com.shabbar.rozgarconnector.databinding.FragmentActivitiesBinding
import com.shabbar.rozgarconnector.models.ActivitiesModel
import com.shabbar.rozgarconnector.ui.worker.WorkerDetailActivity

class ActivitiesFragment : Fragment() {

    private var _binding: FragmentActivitiesBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val allActivities = mutableListOf<ActivitiesModel>()
    private val displayList = mutableListOf<Any>()
    private lateinit var adapter: ActivitiesAdapter
    private var currentFilter = "active"
    private var userRole = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentActivitiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilters()
        fetchUserRole()
        loadActivities()
        
        // Settings button logic removed from here as it was removed from layout
    }

    private fun fetchUserRole() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (isAdded && doc.exists()) {
                userRole = (doc.getString("role") ?: "").lowercase()
                updateChipLabels()
            }
        }
    }

    private fun updateChipLabels() {
        if (!isAdded) return
        when (userRole) {
            "seeker" -> {
                binding.chipOffers.text = getString(R.string.sent_offers)
                binding.chipApplications.text = getString(R.string.apps_received)
                binding.chipActive.text = getString(R.string.active_contracts)
            }
            "provider", "worker" -> {
                binding.chipOffers.text = getString(R.string.job_offers)
                binding.chipApplications.text = getString(R.string.my_apps)
                binding.chipActive.text = getString(R.string.work_in_progress_chip)
            }
        }
    }

    private fun setupFilters() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                R.id.chipActive -> applyFilter("active")
                R.id.chipOffers -> applyFilter("hire")
                R.id.chipApplications -> applyFilter("job")
                R.id.chipHistory -> applyFilter("history")
            }
        }
    }

    private fun applyFilter(filterType: String) {
        currentFilter = filterType
        val filtered = allActivities.filter { activity ->
            val status = activity.status.lowercase()
            val type = activity.type.lowercase()
            val hasUserFinished = if (userRole == "seeker") activity.seekerConfirmed else activity.workerConfirmed
            
            if (type == "broadcast" || activity.title.lowercase().contains("broadcast")) return@filter false

            when (filterType) {
                "active" -> (status == "accepted" || status == "approved") && !hasUserFinished
                "history" -> status == "completed" || status == "disputed" || status == "rejected" || status == "cancelled" || hasUserFinished
                "hire" -> type == "hire" && status == "pending"
                "job" -> type == "job" && status == "pending"
                else -> true
            }
        }.distinctBy { it.notificationId }
        
        groupActivitiesByDate(filtered, filterType)
        updateChipBadges()
    }

    private fun updateChipBadges() {
        if (!isAdded) return
        
        val activeUnread = allActivities.any { (it.status == "accepted" || it.status == "approved") && !it.isRead }
        val hireUnread = allActivities.any { it.type == "hire" && it.status == "pending" && !it.isRead }
        val jobUnread = allActivities.any { it.type == "job" && it.status == "pending" && !it.isRead }
        val historyUnread = allActivities.any { (it.status == "completed" || it.status == "rejected" || it.status == "cancelled") && !it.isRead }

        binding.chipActive.text = (if (activeUnread) "● " else "") + getString(if (userRole == "seeker") R.string.active_contracts else R.string.work_in_progress_chip)
        binding.chipOffers.text = (if (hireUnread) "● " else "") + getString(if (userRole == "seeker") R.string.sent_offers else R.string.job_offers)
        binding.chipApplications.text = (if (jobUnread) "● " else "") + getString(if (userRole == "seeker") R.string.apps_received else R.string.my_apps)
        binding.chipHistory.text = (if (historyUnread) "● " else "") + getString(R.string.history)
    }

    private fun groupActivitiesByDate(list: List<ActivitiesModel>, filterType: String) {
        displayList.clear()
        if (!isAdded) return
        
        if (list.isEmpty()) {
            adapter.updateData(displayList)
            updateEmptyState()
            return
        }

        val header = when (filterType) {
            "active" -> "Active Work ⚒️"
            "history" -> getString(R.string.job_history_header)
            "hire" -> getString(R.string.hiring_offers_header)
            "job" -> getString(R.string.job_apps_header)
            else -> "Work Activities 📋"
        }
        displayList.add(header)
        
        displayList.addAll(list.sortedByDescending { it.timestamp })
        adapter.updateData(displayList)
        updateEmptyState()
    }

    private fun setupRecyclerView() {
        adapter = ActivitiesAdapter(displayList) { activity ->
            db.collection("notifications").document(activity.notificationId).update("isRead", true)
            openWorkerProfile(activity)
        }
        binding.rvActivities.layoutManager = LinearLayoutManager(requireContext())
        binding.rvActivities.adapter = adapter
    }

    private fun loadActivities() {
        val uid = auth.currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE
        
        db.collection("notifications").addSnapshotListener { snapshots, e ->
            if (!isAdded) return@addSnapshotListener
            binding.progressBar.visibility = View.GONE
            if (e != null) return@addSnapshotListener
            
            allActivities.clear()
            snapshots?.forEach { doc ->
                val rId = doc.getString("receiverId") ?: ""
                val sId = doc.getString("senderId") ?: ""
                val type = doc.getString("type")?.lowercase() ?: ""
                
                if (type != "broadcast" && !doc.getString("title")?.lowercase()?.contains("broadcast").let { it == true }) {
                    if (rId == uid || sId == uid) {
                        val activity = doc.toObject(ActivitiesModel::class.java).apply { notificationId = doc.id }
                        allActivities.add(activity)
                    }
                }
            }
            applyFilter(currentFilter)
        }
    }

    private fun updateEmptyState() {
        if (_binding == null) return
        if (displayList.isEmpty()) {
            binding.llEmptyState.visibility = View.VISIBLE
            binding.rvActivities.visibility = View.GONE
        } else {
            binding.llEmptyState.visibility = View.GONE
            binding.rvActivities.visibility = View.VISIBLE
        }
    }

    private fun openWorkerProfile(activity: ActivitiesModel) {
        val currentUid = auth.currentUser?.uid ?: return
        val targetId = if (activity.senderId == currentUid) activity.receiverId else activity.senderId
        if (targetId.isEmpty()) return
        startActivity(Intent(requireContext(), WorkerDetailActivity::class.java).apply {
            putExtra("WORKER_ID", targetId)
            putExtra("NOTIFICATION_ID", activity.notificationId)
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}