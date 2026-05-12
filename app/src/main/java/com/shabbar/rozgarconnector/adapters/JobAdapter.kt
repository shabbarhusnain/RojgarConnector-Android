package com.shabbar.rozgarconnector.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.models.JobModel
import com.shabbar.rozgarconnector.utils.TranslatorUtil

class JobAdapter(
    private var jobList: MutableList<JobModel>,
    private val onJobClick: (JobModel) -> Unit
) : RecyclerView.Adapter<JobAdapter.JobViewHolder>() {

    class JobViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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
        val context = holder.itemView.context

        // 1. Title Fix: Isay hamesha set karein takay gayab na ho
        val originalTitle = job.jobTitle ?: "No Title"
        holder.title?.text = originalTitle
        if (TranslatorUtil.isUrduEnabled(context)) {
            TranslatorUtil.translateText(originalTitle) { holder.title?.text = it }
        }
        
        // 2. Workplace / Category
        val wpName = if (job.workplaceName.isNullOrEmpty()) "Local" else job.workplaceName
        val wpType = if(job.workerType == "educated") "Professional" else "Skilled/Manual"
        holder.workplace?.text = "$wpName • $wpType"
        
        // 3. --- CLEAN SALARY TAG ---
        val amount = job.payAmount ?: ""
        val unit = job.payUnit ?: ""
        
        // Agar "Depend" ka lafz kahin bhi hai toh chota text show karein
        if (amount.contains("Depend", true) || unit.contains("Depend", true)) {
            holder.salary?.text = if (TranslatorUtil.isUrduEnabled(context)) "قابلِ تبادلہ" else "Negotiable"
        } else {
            // Normal salary ke liye
            holder.salary?.text = "Rs. $amount"
        }
        
        // 4. Location Fix
        val jobLoc = if (!job.workplaceAddress.isNullOrEmpty()) job.workplaceAddress else (job.district ?: "N/A")
        holder.location?.text = jobLoc
        
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
