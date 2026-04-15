package com.shabbar.rozgarconnector.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.FragmentAdminStatsBinding
import com.shabbar.rozgarconnector.databinding.ItemAdminChatListBinding
import com.shabbar.rozgarconnector.models.NotificationModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatsFragment : Fragment() {

    private var _binding: FragmentAdminStatsBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val activityLog = mutableListOf<NotificationModel>()
    private lateinit var logAdapter: ActivityLogAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupActivityLog()
        loadStats()
        loadActivityLog()
    }

    private fun setupActivityLog() {
        logAdapter = ActivityLogAdapter(activityLog)
        binding.rvActivityLog.layoutManager = LinearLayoutManager(requireContext())
        binding.rvActivityLog.adapter = logAdapter
    }

    private fun loadStats() {
        db.collection("users").addSnapshotListener { snapshots, _ ->
            if (!isAdded) return@addSnapshotListener
            binding.tvTotalUsers.text = (snapshots?.size() ?: 0).toString()
            
            var online = 0
            var rejected = 0
            snapshots?.forEach { doc ->
                if (doc.getBoolean("isOnline") == true) online++
                if (doc.getBoolean("isRejected") == true) rejected++
            }
            binding.tvOnlineUsers.text = online.toString()
            binding.tvRejectedUsers.text = rejected.toString()
        }

        db.collection("notifications").addSnapshotListener { snapshots, _ ->
            if (!isAdded) return@addSnapshotListener
            binding.tvTotalApps.text = (snapshots?.size() ?: 0).toString()
        }
    }

    private fun loadActivityLog() {
        db.collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshots, _ ->
                if (!isAdded || snapshots == null) return@addSnapshotListener
                activityLog.clear()
                snapshots.forEach { doc ->
                    val item = doc.toObject(NotificationModel::class.java)
                    activityLog.add(item)
                }
                logAdapter.notifyDataSetChanged()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class ActivityLogAdapter(private val list: List<NotificationModel>) : RecyclerView.Adapter<ActivityLogAdapter.LogVH>() {
        // ViewHolder uses the layout's root View directly to avoid Type Casting errors
        inner class LogVH(val b: ItemAdminChatListBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogVH {
            return LogVH(ItemAdminChatListBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: LogVH, position: Int) {
            val item = list[position]
            holder.b.apply {
                tvUserName.text = item.senderName.ifEmpty { "System" }
                tvLastMessage.text = item.message
                val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                tvTime.text = item.timestamp?.toDate()?.let { sdf.format(it) } ?: ""
                
                // Set fixed icon for logs
                imgUserAvatar.setImageResource(R.drawable.ic_notification)
                tvUnreadBadge.visibility = View.GONE // No badges in log
            }
        }

        override fun getItemCount() = list.size
    }
}