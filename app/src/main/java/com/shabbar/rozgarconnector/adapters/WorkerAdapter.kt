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
import com.shabbar.rozgarconnector.utils.TranslatorUtil

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
        val context = holder.itemView.context

        // Name stays in original (usually English/Roman)
        holder.binding.tvName.text = worker.fullName

        // Check if Urdu is enabled to translate other fields
        if (TranslatorUtil.isUrduEnabled(context)) {
            // Translate Location
            worker.district?.let { district ->
                TranslatorUtil.translateText(district) { translated ->
                    holder.binding.tvLocation.text = translated
                }
            }

            // Translate Skill/Degree
            val skillToTranslate = if (worker.workerType == "educated") {
                worker.degreeName ?: "Educated Worker"
            } else {
                worker.professionalSkill ?: "General Worker"
            }
            
            TranslatorUtil.translateText(skillToTranslate) { translated ->
                holder.binding.tvSkill.text = translated
            }
        } else {
            // Default English display
            holder.binding.tvLocation.text = worker.district ?: "Location"
            holder.binding.tvSkill.text = if (worker.workerType == "educated") {
                worker.degreeName ?: "Educated Worker"
            } else {
                worker.professionalSkill ?: "General Worker"
            }
        }

        // Display Average Rating
        holder.binding.ratingBar.rating = worker.averageRating

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

        // View Portfolio Button (Hidden but usable for logic)
        holder.binding.btnViewDetails.setOnClickListener {
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