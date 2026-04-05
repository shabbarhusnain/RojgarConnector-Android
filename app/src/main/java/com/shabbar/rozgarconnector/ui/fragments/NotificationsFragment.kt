package com.shabbar.rozgarconnector.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.adapters.NotificationAdapter
import com.shabbar.rozgarconnector.databinding.FragmentNotificationsBinding
import com.shabbar.rozgarconnector.models.NotificationModel
import com.shabbar.rozgarconnector.ui.worker.WorkerDetailActivity
import java.util.*

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
                binding?.chipOffers?.text = "Sent Offers"
                binding?.chipApplications?.text = "Applications Received"
                binding?.chipActive?.text = "Active Contracts"
            }
            "provider" -> {
                binding?.chipOffers?.text = "Job Offers"
                binding?.chipApplications?.text = "My Applications"
                binding?.chipActive?.text = "Work In Progress"
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
            val hasUserFinished = if (isSeeker) notif.seekerConfirmed else notif.workerConfirmed

            when (filterType) {
                "active" -> (notif.status == "accepted" || notif.status == "approved") && !hasUserFinished
                "history" -> notif.status == "completed" || hasUserFinished
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
            "active" -> displayList.add("Current Active Work 🛠️")
            "history" -> displayList.add("Job History Record 📜")
            "hire" -> displayList.add("Hiring Offers 💼")
            "job" -> displayList.add("Job Applications 📝")
            else -> displayList.add("Notifications 🔔")
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