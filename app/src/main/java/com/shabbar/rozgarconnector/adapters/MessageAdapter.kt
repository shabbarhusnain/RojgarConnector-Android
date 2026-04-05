package com.shabbar.rozgarconnector.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.models.MessageModel
import de.hdodenhof.circleimageview.CircleImageView

class MessageAdapter(private val messageList: List<MessageModel>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Aap ki XML ki IDs ke mutabiq
        val tvSenderName: TextView = view.findViewById(R.id.sender_name)
        val tvLastMessage: TextView = view.findViewById(R.id.last_message)
        val tvTimestamp: TextView = view.findViewById(R.id.timestamp)
        val ivSenderImage: CircleImageView = view.findViewById(R.id.sender_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]

        // Data binding
        holder.tvLastMessage.text = message.message
        holder.tvTimestamp.text = message.timestamp.toString()
        holder.tvSenderName.text = "Chat Partner" // Isay hum baad mein dynamic karenge
    }

    override fun getItemCount() = messageList.size
}