package com.shabbar.rozgarconnector.adapters

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shabbar.rozgarconnector.databinding.ItemUserAdminBinding
import com.shabbar.rozgarconnector.models.UserModel

class UserAdminAdapter(
    private val userList: List<UserModel>,
    private val onItemClick: (UserModel) -> Unit
) : RecyclerView.Adapter<UserAdminAdapter.AdminViewHolder>() {

    class AdminViewHolder(val binding: ItemUserAdminBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminViewHolder {
        val binding = ItemUserAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AdminViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdminViewHolder, position: Int) {
        val user = userList[position]

        // Fix: Use 'fullName' instead of 'fullname'
        holder.binding.tvUserName.text = if (user.fullName.isNotEmpty()) user.fullName else "No Name"
        holder.binding.tvUserCnic.text = if (user.cnic.isNotEmpty()) user.cnic else "CNIC: N/A"
        holder.binding.tvUserRole.text = user.role.uppercase()

        if (!user.dpBase64.isNullOrEmpty()) {
            try {
                val imageBytes = Base64.decode(user.dpBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.binding.imgUserThumb.setImageBitmap(bitmap)
            } catch (e: Exception) {}
        }

        holder.itemView.setOnClickListener { onItemClick(user) }
    }

    override fun getItemCount() = userList.size
}