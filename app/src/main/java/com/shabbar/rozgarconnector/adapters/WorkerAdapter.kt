package com.shabbar.rozgarconnector.adapters

import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.ItemWorkerBinding
import com.shabbar.rozgarconnector.models.UserModel
import com.shabbar.rozgarconnector.ui.worker.WorkerDetailActivity

class WorkerAdapter(
    private val workerList: List<UserModel>
) : RecyclerView.Adapter<WorkerAdapter.WorkerViewHolder>() {

    class WorkerViewHolder(val binding: ItemWorkerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkerViewHolder {
        val binding = ItemWorkerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WorkerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkerViewHolder, position: Int) {
        val worker = workerList[position]

        holder.binding.tvName.text = worker.fullName
        holder.binding.tvLocation.text = worker.district ?: "Unknown Location"

        // Display Average Rating
        holder.binding.ratingBar.rating = worker.averageRating

        // Smart Skill Display
        val displayInfo = if (worker.workerType == "educated") {
            worker.degreeName ?: "Educated Worker"
        } else {
            worker.professionalSkill ?: "General Worker"
        }
        holder.binding.tvSkill.text = displayInfo

        // Verification Badge
        holder.binding.imgVerified.visibility = if (worker.isVerified) View.VISIBLE else View.GONE

        // --- Load Profile Image (Base64 Support) ---
        if (!worker.dpBase64.isNullOrEmpty()) {
            try {
                val decodedString = Base64.decode(worker.dpBase64, Base64.DEFAULT)
                val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                holder.binding.imgProfile.setImageBitmap(decodedByte)
            } catch (e: Exception) {
                holder.binding.imgProfile.setImageResource(R.drawable.ic_profile)
            }
        } else {
            holder.binding.imgProfile.setImageResource(R.drawable.ic_profile)
        }

        // View Portfolio Button
        holder.binding.btnViewDetails.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, WorkerDetailActivity::class.java).apply {
                putExtra("WORKER_ID", worker.uid)
            }
            context.startActivity(intent)
        }

        // Card Click also opens detail
        holder.itemView.setOnClickListener {
            holder.binding.btnViewDetails.performClick()
        }
    }

    override fun getItemCount() = workerList.size
}