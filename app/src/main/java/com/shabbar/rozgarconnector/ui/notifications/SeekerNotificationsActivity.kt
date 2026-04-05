package com.shabbar.rozgarconnector.ui.notifications

import android.content.Intent
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
import com.shabbar.rozgarconnector.databinding.ActivitySeekerNotificationsBinding
import com.shabbar.rozgarconnector.models.NotificationModel
import com.shabbar.rozgarconnector.ui.worker.WorkerDetailActivity

class SeekerNotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySeekerNotificationsBinding
    private lateinit var adapter: NotificationAdapter
    private val notificationList = mutableListOf<NotificationModel>()
    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeekerNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadNotifications()
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(notificationList) { notification ->
            if (notification.type == "job" && notification.status == "pending") {
                showActionDialog(notification)
            } else {
                markAsRead(notification.notificationId)
            }
        }
        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = adapter
    }

    private fun showActionDialog(notification: NotificationModel) {
        val options = arrayOf("View Portfolio", "Approve Application", "Reject Application")
        AlertDialog.Builder(this)
            .setTitle("Application Received")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openPortfolio(notification.senderId)
                    1 -> updateApplicationStatus(notification, "approved")
                    2 -> updateApplicationStatus(notification, "rejected")
                }
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun openPortfolio(senderId: String) {
        if (senderId.isEmpty()) {
            Toast.makeText(this, "Worker details not available", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, WorkerDetailActivity::class.java)
        intent.putExtra("WORKER_ID", senderId)
        startActivity(intent)
    }

    private fun updateApplicationStatus(notification: NotificationModel, newStatus: String) {
        val notificationId = notification.notificationId
        if (notificationId.isEmpty()) return

        db.collection("notifications").document(notificationId)
            .update("status", newStatus, "isRead", true)
            .addOnSuccessListener {
                sendResponseToWorker(notification, newStatus)
                Toast.makeText(this, "Action: $newStatus", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendResponseToWorker(notification: NotificationModel, status: String) {
        // Notify Worker using naye fields
        db.collection("applications")
            .whereEqualTo("seekerId", uid)
            .whereEqualTo("providerId", notification.senderId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    doc.reference.update("status", status)

                    val responseMsg = if (status == "approved") "Congratulations! Your job application has been APPROVED!" 
                                      else "Your application was not accepted."
                    
                    val notifyWorker = hashMapOf(
                        "receiverId" to notification.senderId,
                        "senderId" to uid,
                        "message" to responseMsg,
                        "title" to "Job Response",
                        "type" to "job_response",
                        "status" to status,
                        "timestamp" to Timestamp.now(),
                        "isRead" to false
                    )
                    db.collection("notifications").add(notifyWorker)
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
            .whereEqualTo("receiverId", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (isFinishing) return@addSnapshotListener
                binding.progressBar.visibility = View.GONE
                if (e != null) return@addSnapshotListener

                notificationList.clear()
                snapshots?.forEach { doc ->
                    val notification = doc.toObject(NotificationModel::class.java).apply {
                        notificationId = doc.id
                    }
                    notificationList.add(notification)
                }
                adapter.notifyDataSetChanged()
                binding.tvNoNotifications.visibility = if (notificationList.isEmpty()) View.VISIBLE else View.GONE
            }
    }
}