package com.shabbar.rozgarconnector.adapters

import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shabbar.rozgarconnector.databinding.ItemUserAdminBinding
import com.shabbar.rozgarconnector.models.UserModel
import com.shabbar.rozgarconnector.utils.decodeBase64BitmapAsync

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

        // Fix: Added null safety check for Strings
        holder.binding.tvUserName.text = if (user.fullName?.isNotEmpty() == true) user.fullName else "No Name"
        holder.binding.tvUserCnic.text = if (user.cnic?.isNotEmpty() == true) user.cnic else "CNIC: N/A"
        holder.binding.tvUserRole.text = user.role?.uppercase() ?: "USER"

        val profileBase64 = user.dpBase64
        if (!profileBase64.isNullOrEmpty()) {
            decodeBase64BitmapAsync(profileBase64, {
                holder.binding.imgUserThumb.setImageBitmap(it)
            })
        }

        holder.itemView.setOnClickListener { onItemClick(user) }
    }

    override fun getItemCount() = userList.size
}
