package com.shabbar.rozgarconnector.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.models.ApplicationModel
import java.text.SimpleDateFormat
import java.util.*

class ApplicationAdapter(
    private val applicationList: List<ApplicationModel>
) : RecyclerView.Adapter<ApplicationAdapter.ApplicationViewHolder>() {

    class ApplicationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvNotificationTitle)
        val timestamp: TextView = itemView.findViewById(R.id.tvNotificationTimestamp)
        val statusLabel: TextView = itemView.findViewById(R.id.tvStatusLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_activities, parent, false)
        return ApplicationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ApplicationViewHolder, position: Int) {
        val app = applicationList[position]
        
        holder.title.text = "Job Application: ${app.jobTitle}"
        
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        val dateStr = app.timestamp?.toDate()?.let { sdf.format(it) } ?: "Just now"
        holder.timestamp.text = "Applied on: $dateStr"

        // Updated Badge UI (Fixing logical duplication)
        val status = app.status.lowercase()
        val statusColor = when (status) {
            "approved", "accepted" -> Color.parseColor("#4CAF50") // Green
            "rejected" -> Color.parseColor("#F44336") // Red
            "completed" -> Color.parseColor("#2196F3") // Blue
            else -> Color.parseColor("#FF9800") // Orange for Pending
        }

        holder.statusLabel.text = status.uppercase()
        holder.statusLabel.backgroundTintList = ColorStateList.valueOf(statusColor)
        
        // Hide action area buttons for this simple list
        holder.itemView.findViewById<View>(R.id.llActionArea).visibility = View.GONE
    }

    override fun getItemCount() = applicationList.size
}
