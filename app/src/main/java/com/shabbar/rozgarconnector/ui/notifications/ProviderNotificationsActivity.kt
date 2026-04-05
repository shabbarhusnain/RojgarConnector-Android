package com.shabbar.rozgarconnector.ui.notifications

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.shabbar.rozgarconnector.adapters.NotificationAdapter
import com.shabbar.rozgarconnector.databinding.ActivityProviderNotificationsBinding
import com.shabbar.rozgarconnector.models.NotificationModel

class ProviderNotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProviderNotificationsBinding
    private lateinit var adapter: NotificationAdapter
    private val notificationList = mutableListOf<NotificationModel>()
    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProviderNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadNotifications()
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(notificationList) { notification ->
            if (notification.type == "hire" && notification.status == "pending") {
                showActionDialog(notification)
            } else {
                markAsRead(notification.notificationId)
            }
        }
        binding.notificationsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.notificationsRecyclerView.adapter = adapter
    }

    private fun showActionDialog(notification: NotificationModel) {
        AlertDialog.Builder(this)
            .setTitle("Hire Request")
            .setMessage(notification.message)
            .setPositiveButton("Accept") { _, _ -> updateRequestStatus(notification, "accepted") }
            .setNegativeButton("Reject") { _, _ -> updateRequestStatus(notification, "rejected") }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun updateRequestStatus(notification: NotificationModel, newStatus: String) {
        val notificationId = notification.notificationId
        if (notificationId.isEmpty()) return

        db.collection("notifications").document(notificationId)
            .update("status", newStatus, "isRead", true)
            .addOnSuccessListener {
                // Also update the hire_requests collection if needed (optional but good for tracking)
                sendResponseToSeeker(notification, newStatus)
                Toast.makeText(this, "Request $newStatus", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendResponseToSeeker(notification: NotificationModel, status: String) {
        // We need to know who the Seeker was. For simplicity, we can store seekerId in notification model
        // If not available, we find the request in hire_requests and notify the seeker
        db.collection("hire_requests")
            .whereEqualTo("workerId", uid)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    val seekerId = doc.getString("seekerId") ?: continue
                    doc.reference.update("status", status)

                    // Notify Seeker
                    val workerName = doc.getString("workerName") ?: "A Worker"
                    val responseMsg = if (status == "accepted") "$workerName has ACCEPTED your hire request!" else "$workerName has rejected the request."
                    
                    val notifySeeker = hashMapOf(
                        "userId" to seekerId,
                        "message" to responseMsg,
                        "type" to "hire_response",
                        "status" to status,
                        "timestamp" to Timestamp.now(),
                        "isRead" to false
                    )
                    db.collection("notifications").add(notifySeeker)
                }
            }
    }

    private fun markAsRead(notificationId: String) {
        if (notificationId.isEmpty()) return
        db.collection("notifications").document(notificationId).update("isRead", true)
    }

    private fun loadNotifications() {
        if (uid == null) return

        binding.progressBar.visibility = View.VISIBLE
        db.collection("notifications")
            .whereEqualTo("userId", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (isFinishing) return@addSnapshotListener
                binding.progressBar.visibility = View.GONE
                if (e != null) return@addSnapshotListener

                notificationList.clear()
                snapshots?.forEach { doc ->
                    val notification = doc.toObject(NotificationModel::class.java).copy(notificationId = doc.id)
                    notificationList.add(notification)
                }
                adapter.notifyDataSetChanged()
                binding.tvNoNotifications.visibility = if (notificationList.isEmpty()) View.VISIBLE else View.GONE
            }
    }
}