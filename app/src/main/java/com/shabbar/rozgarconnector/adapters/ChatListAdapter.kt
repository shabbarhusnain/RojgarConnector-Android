package com.shabbar.rozgarconnector.adapters

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.models.ChatListModel
import com.shabbar.rozgarconnector.utils.TranslatorUtil
import java.text.SimpleDateFormat
import java.util.*

class ChatListAdapter(
    private val chatList: List<ChatListModel>,
    private val onClick: (ChatListModel) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivUser: ImageView = view.findViewById(R.id.imgProfile)
        val tvName: TextView = view.findViewById(R.id.tvUserName)
        val tvLastMsg: TextView = view.findViewById(R.id.tvLastMessage)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val viewUnreadDot: View = view.findViewById(R.id.viewUnreadDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat = chatList[position]
        val context = holder.itemView.context
        
        holder.tvName.text = chat.userName
        
        if (TranslatorUtil.isUrduEnabled(context)) {
            TranslatorUtil.translateText(chat.lastMessage) { translated ->
                holder.tvLastMsg.text = translated
            }
        } else {
            holder.tvLastMsg.text = chat.lastMessage
        }
        
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        holder.tvTime.text = timeFormat.format(Date(chat.timestamp))

        holder.viewUnreadDot.visibility = if (chat.hasUnread) View.VISIBLE else View.GONE

        // Handle Base64 Image
        if (!chat.profileImage.isNullOrEmpty()) {
            try {
                val decodedString = Base64.decode(chat.profileImage, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                holder.ivUser.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.ivUser.setImageResource(R.drawable.ic_profile)
            }
        } else {
            holder.ivUser.setImageResource(R.drawable.ic_profile)
        }

        holder.itemView.setOnClickListener { onClick(chat) }
    }

    override fun getItemCount(): Int = chatList.size
}