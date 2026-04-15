package com.shabbar.rozgarconnector.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.adapters.NotificationAdapter
import com.shabbar.rozgarconnector.databinding.FragmentNotificationsBinding
import com.shabbar.rozgarconnector.models.NotificationModel
import com.shabbar.rozgarconnector.ui.worker.WorkerDetailActivity
import com.shabbar.rozgarconnector.ui.settings.MenuActivity

class NotificationsFragment : Fragment(R.layout.fragment_notifications) {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val allNotifications = mutableListOf<NotificationModel>()
    private val displayList = mutableListOf<Any>()
    private lateinit var adapter: NotificationAdapter
    private var currentFilter = "active"
    private var userRole = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNotificationsBinding.bind(view)

        setupRecyclerView()
        setupFilters()
        fetchUserRole()
        loadNotifications()

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(requireContext(), MenuActivity::class.java))
        }
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
                R.id.chipAll -> applyFilter("all")
            }
        }
    }

    private fun applyFilter(filterType: String) {
        currentFilter = filterType
        val currentUid = auth.currentUser?.uid ?: return
        
        val filtered = allNotifications.filter { notif ->
            val status = notif.status.lowercase()
            val type = notif.type.lowercase()
            
            val hasUserFinished = if (userRole == "seeker") notif.seekerConfirmed else notif.workerConfirmed

            when (filterType) {
                "active" -> (status == "accepted" || status == "approved") && !hasUserFinished
                "history" -> status == "completed" || status == "disputed" || status == "rejected" || status == "cancelled" || hasUserFinished
                "hire" -> type == "hire" && status == "pending"
                "job" -> type == "job" && status == "pending"
                else -> true
            }
        }.distinctBy { it.notificationId }
        
        groupNotificationsByDate(filtered, filterType)
    }

    private fun groupNotificationsByDate(list: List<NotificationModel>, filterType: String) {
        displayList.clear()
        if (!isAdded) return
        
        if (list.isEmpty()) {
            adapter.updateData(displayList)
            updateEmptyState()
            return
        }

        val header = when (filterType) {
            "active" -> getString(R.string.active_work_header)
            "history" -> getString(R.string.job_history_header)
            "hire" -> getString(R.string.hiring_offers_header)
            "job" -> getString(R.string.job_apps_header)
            else -> getString(R.string.notif_header)
        }
        displayList.add(header)
        
        displayList.addAll(list.sortedByDescending { it.timestamp })
        adapter.updateData(displayList)
        updateEmptyState()
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(displayList) { notification ->
            db.collection("notifications").document(notification.notificationId).update("isRead", true)
            openWorkerProfile(notification)
        }
        binding.rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotifications.adapter = adapter
    }

    private fun loadNotifications() {
        val uid = auth.currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE
        
        db.collection("notifications").addSnapshotListener { snapshots, e ->
            if (!isAdded) return@addSnapshotListener
            binding.progressBar.visibility = View.GONE
            if (e != null) return@addSnapshotListener
            
            allNotifications.clear()
            snapshots?.forEach { doc ->
                val rId = doc.getString("receiverId") ?: ""
                val sId = doc.getString("senderId") ?: ""
                
                if (rId == uid || sId == uid) {
                    val notif = doc.toObject(NotificationModel::class.java).apply { notificationId = doc.id }
                    allNotifications.add(notif)
                }
            }
            applyFilter(currentFilter)
        }
    }

    private fun updateEmptyState() {
        if (_binding == null) return
        if (displayList.isEmpty()) {
            binding.llEmptyState.visibility = View.VISIBLE
            binding.rvNotifications.visibility = View.GONE
        } else {
            binding.llEmptyState.visibility = View.GONE
            binding.rvNotifications.visibility = View.VISIBLE
        }
    }

    private fun openWorkerProfile(notification: NotificationModel) {
        val currentUid = auth.currentUser?.uid ?: return
        val targetId = if (notification.senderId == currentUid) notification.receiverId else notification.senderId
        if (targetId.isEmpty()) return
        startActivity(Intent(requireContext(), WorkerDetailActivity::class.java).apply {
            putExtra("WORKER_ID", targetId)
            putExtra("NOTIFICATION_ID", notification.notificationId)
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}