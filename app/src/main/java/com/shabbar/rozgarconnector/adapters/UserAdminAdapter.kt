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
    private val onApproveClick: (UserModel) -> Unit
) : RecyclerView.Adapter<UserAdminAdapter.AdminViewHolder>() {

    class AdminViewHolder(val binding: ItemUserAdminBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminViewHolder {
        val binding = ItemUserAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AdminViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdminViewHolder, position: Int) {
        val user = userList[position]

        holder.binding.tvUserName.text = user.fullName ?: "No Name"
        holder.binding.tvUserCnic.text = "CNIC: ${user.cnic ?: "N/A"}"
        holder.binding.tvUserRole.text = "Role: ${user.role ?: "Worker"}"

        // Crash Prevention: Check if image string is valid
        val base64Img = user.cnicFrontBase64
        if (!base64Img.isNullOrEmpty() && base64Img.length > 100) {
            try {
                val imageBytes = Base64.decode(base64Img, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.binding.imgUserCnic.setImageBitmap(bitmap)
            } catch (e: Exception) {
                // holder.binding.imgUserCnic.setImageResource(R.drawable.ic_error_placeholder)
            }
        }

        holder.binding.btnApproveUser.setOnClickListener { onApproveClick(user) }
    }

    override fun getItemCount() = userList.size
}