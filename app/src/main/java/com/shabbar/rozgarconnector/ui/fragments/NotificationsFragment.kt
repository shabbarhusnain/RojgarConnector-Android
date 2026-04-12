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
    private val binding get() = _binding

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

        binding?.btnSettings?.setOnClickListener {
            startActivity(Intent(requireContext(), MenuActivity::class.java))
        }
    }

    private fun fetchUserRole() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            userRole = doc.getString("role") ?: ""
            updateChipLabels()
        }
    }

    private fun updateChipLabels() {
        if (!isAdded || _binding == null) return
        val role = userRole.lowercase()
        when (role) {
            "seeker" -> {
                binding?.chipOffers?.text = getString(R.string.sent_offers)
                binding?.chipApplications?.text = getString(R.string.apps_received)
                binding?.chipActive?.text = getString(R.string.active_contracts)
            }
            "provider" -> {
                binding?.chipOffers?.text = getString(R.string.job_offers)
                binding?.chipApplications?.text = getString(R.string.my_apps)
                binding?.chipActive?.text = getString(R.string.work_in_progress_chip)
            }
        }
    }

    private fun setupFilters() {
        binding?.chipGroupFilters?.setOnCheckedStateChangeListener { _, checkedIds ->
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
            val isSeeker = if (notif.type == "hire") notif.senderId == currentUid else notif.receiverId == currentUid
            
            // Logic Fix: hasUserFinished must be true if current user (Seeker/Worker) has confirmed completion
            val hasUserFinished = if (isSeeker) notif.seekerConfirmed else notif.workerConfirmed
            val isFullyCompleted = notif.status == "completed"

            when (filterType) {
                // Active Work only shows jobs that are Accepted AND not yet finished by the current user
                "active" -> (notif.status == "accepted" || notif.status == "approved") && !hasUserFinished
                
                // History shows fully completed jobs OR jobs the user has personally marked as finished
                "history" -> isFullyCompleted || hasUserFinished
                
                "hire" -> notif.type == "hire" && notif.status == "pending"
                "job" -> notif.type == "job" && notif.status == "pending"
                "all" -> notif.status == "rejected" || notif.status == "cancelled"
                else -> true
            }
        }
        
        groupNotificationsByDate(filtered, filterType)
    }

    private fun groupNotificationsByDate(list: List<NotificationModel>, filterType: String) {
        displayList.clear()
        if (list.isEmpty()) {
            adapter.updateData(displayList)
            updateEmptyState()
            return
        }

        when (filterType) {
            "active" -> displayList.add(getString(R.string.active_work_header))
            "history" -> displayList.add(getString(R.string.job_history_header))
            "hire" -> displayList.add(getString(R.string.hiring_offers_header))
            "job" -> displayList.add(getString(R.string.job_apps_header))
            else -> displayList.add(getString(R.string.notif_header))
        }
        
        displayList.addAll(list.sortedByDescending { it.timestamp })
        adapter.updateData(displayList)
        updateEmptyState()
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(displayList) { notification ->
            db.collection("notifications").document(notification.notificationId).update("isRead", true)
            if (notification.status != "completed") openWorkerProfile(notification)
        }
        binding?.rvNotifications?.layoutManager = LinearLayoutManager(requireContext())
        binding?.rvNotifications?.adapter = adapter
    }

    private fun loadNotifications() {
        val uid = auth.currentUser?.uid ?: return
        binding?.progressBar?.visibility = View.VISIBLE
        db.collection("notifications").addSnapshotListener { snapshots, e ->
            if (!isAdded || _binding == null) return@addSnapshotListener
            binding?.progressBar?.visibility = View.GONE
            if (e != null) return@addSnapshotListener
            
            allNotifications.clear()
            snapshots?.forEach { doc ->
                val rId = doc.getString("receiverId") ?: ""
                val sId = doc.getString("senderId") ?: ""
                val type = doc.getString("type") ?: ""
                if (rId == uid || (sId == uid && (type == "hire" || type == "job"))) {
                    val notif = doc.toObject(NotificationModel::class.java).apply { notificationId = doc.id }
                    allNotifications.add(notif)
                }
            }
            applyFilter(currentFilter)
        }
    }

    private fun updateEmptyState() {
        binding?.tvNoNotifications?.visibility = if (displayList.isEmpty()) View.VISIBLE else View.GONE
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