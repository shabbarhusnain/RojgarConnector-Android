package com.shabbar.rozgarconnector.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.ActivityNotificationsBinding
import com.shabbar.rozgarconnector.models.ActivitiesModel
import java.text.SimpleDateFormat
import java.util.*

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notificationsList = mutableListOf<ActivitiesModel>()
    private lateinit var adapter: SimpleNotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        setupRecyclerView()
        loadNotifications()
    }

    private fun setupRecyclerView() {
        adapter = SimpleNotificationAdapter(notificationsList)
        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = adapter
    }

    private fun loadNotifications() {
        val uid = auth.currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE

        db.collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (isFinishing || isDestroyed) return@addSnapshotListener
                
                binding.progressBar.visibility = View.GONE
                if (e != null || snapshots == null) return@addSnapshotListener

                notificationsList.clear()
                snapshots.forEach { doc ->
                    val type = doc.getString("type")?.lowercase() ?: ""
                    val titleText = doc.getString("title")?.lowercase() ?: ""
                    val rId = doc.getString("receiverId") ?: ""
                    
                    val isBroadcast = type == "broadcast" || titleText.contains("broadcast") || doc.getString("senderId").isNullOrEmpty()

                    if (isBroadcast && (rId == "all" || rId == "" || rId == uid)) {
                        val notif = doc.toObject(ActivitiesModel::class.java).apply { notificationId = doc.id }
                        notificationsList.add(notif)
                    }
                }

                if (notificationsList.isEmpty()) {
                    binding.llEmpty.visibility = View.VISIBLE
                    binding.rvNotifications.visibility = View.GONE
                } else {
                    binding.llEmpty.visibility = View.GONE
                    binding.rvNotifications.visibility = View.VISIBLE
                }
                adapter.notifyDataSetChanged()
            }
    }

    // Dedicated Simple Adapter for Announcements/Notifications
    inner class SimpleNotificationAdapter(private val items: List<ActivitiesModel>) :
        RecyclerView.Adapter<SimpleNotificationAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notifications, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            // Fix: Added null safety for title, type, and notificationId
            holder.tvTitle.text = if (item.title.isNullOrEmpty()) "System Notification" else item.title
            holder.tvMessage.text = item.message ?: ""
            holder.tvType.text = item.type?.uppercase() ?: "INFO"
            
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            holder.tvTime.text = item.timestamp?.toDate()?.let { sdf.format(it) } ?: "Just now"

            // Simple click to mark as read
            holder.itemView.setOnClickListener {
                val nid = item.notificationId
                if (!nid.isNullOrEmpty()) {
                    db.collection("notifications").document(nid).update("isRead", true)
                }
            }
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val tvTitle: TextView = v.findViewById(R.id.tvTitle)
            val tvMessage: TextView = v.findViewById(R.id.tvMessage)
            val tvTime: TextView = v.findViewById(R.id.tvTime)
            val tvType: TextView = v.findViewById(R.id.tvType)
            val ivIcon: ImageView = v.findViewById(R.id.ivIcon)
        }
    }
}
