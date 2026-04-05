package com.shabbar.rozgarconnector.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.models.MessageModel
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(private val messageList: List<MessageModel>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messageList[position].senderId == auth.currentUser?.uid) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    class SentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvSentMsg)
        val tvTime: TextView = view.findViewById(R.id.tvSentTime)
    }

    class ReceivedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvReceivedMsg)
        val tvTime: TextView = view.findViewById(R.id.tvReceivedTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_sent, parent, false)
            SentViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_received, parent, false)
            ReceivedViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messageList[position]
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val time = timeFormat.format(Date(message.timestamp))

        if (holder is SentViewHolder) {
            holder.tvMessage.text = message.message
            holder.tvTime.text = time
        } else if (holder is ReceivedViewHolder) {
            holder.tvMessage.text = message.message
            holder.tvTime.text = time
        }
    }

    override fun getItemCount(): Int = messageList.size
}