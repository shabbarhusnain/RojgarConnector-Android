package com.shabbar.rozgarconnector.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.models.JobModel

class JobAdapter(
    private var jobList: MutableList<JobModel>,
    private val onJobClick: (JobModel) -> Unit
) : RecyclerView.Adapter<JobAdapter.JobViewHolder>() {

    class JobViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Safe binding using try-catch or null checks
        val title: TextView? = view.findViewById(R.id.tvJobTitle)
        val workplace: TextView? = view.findViewById(R.id.tvWorkplace)
        val salary: TextView? = view.findViewById(R.id.tvSalaryTag)
        val location: TextView? = view.findViewById(R.id.tvLocation)
        val time: TextView? = view.findViewById(R.id.tvTimeAgo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_job, parent, false)
        return JobViewHolder(view)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        val job = jobList[position]

        // Crash Prevention Logic
        holder.title?.text = job.jobTitle ?: "No Title"
        
        val wpName = job.workplaceName ?: "Company"
        val wpType = job.workplaceType ?: "Remote"
        holder.workplace?.text = "$wpName • $wpType"
        
        val amount = job.payAmount ?: "0"
        val unit = job.payUnit ?: "Job"
        holder.salary?.text = "Rs. $amount / $unit"
        
        holder.location?.text = job.district ?: "Not Specified"
        holder.time?.text = "Active Now"

        holder.itemView.setOnClickListener {
            onJobClick(job)
        }
    }

    override fun getItemCount() = jobList.size

    fun updateList(newList: List<JobModel>) {
        jobList = newList.toMutableList()
        notifyDataSetChanged()
    }
}