package com.shabbar.rozgarconnector.adapters

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.models.ApplicationModel
import com.shabbar.rozgarconnector.ui.worker.WorkerDetailActivity
import java.text.SimpleDateFormat
import java.util.*

class ApplicationAdapter(
    private val applicationList: List<ApplicationModel>
) : RecyclerView.Adapter<ApplicationAdapter.ApplicationViewHolder>() {

    class ApplicationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvNotificationTitle)
        val timestamp: TextView = itemView.findViewById(R.id.tvNotificationTimestamp)
        val statusLabel: TextView = itemView.findViewById(R.id.tvStatusLabel)
        val llActions: View = itemView.findViewById(R.id.llActions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_activities, parent, false)
        return ApplicationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ApplicationViewHolder, position: Int) {
        val app = applicationList[position]
        val context = holder.itemView.context
        
        holder.title.text = "Applied by: ${app.workerName}"
        holder.statusLabel.text = app.status.uppercase()
        
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        holder.timestamp.text = app.timestamp?.toDate()?.let { sdf.format(it) } ?: "Just now"

        val status = app.status.lowercase()
        val statusColor = when (status) {
            "accepted", "approved" -> Color.parseColor("#4CAF50")
            "rejected" -> Color.parseColor("#F44336")
            else -> Color.parseColor("#FF9800")
        }
        holder.statusLabel.backgroundTintList = ColorStateList.valueOf(statusColor)

        // Hide inline buttons - we want the seeker to view the profile first
        holder.llActions.visibility = View.GONE

        holder.itemView.setOnClickListener {
            val intent = Intent(context, WorkerDetailActivity::class.java).apply {
                putExtra("WORKER_ID", app.providerId)
                putExtra("APPLICATION_ID", app.applicationId)
                putExtra("JOB_ID", app.jobId)
                putExtra("APP_STATUS", app.status)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = applicationList.size
}