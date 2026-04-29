package com.shabbar.rozgarconnector.adapters

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.models.ActivitiesModel
import com.shabbar.rozgarconnector.ui.messaging.ChatActivity
import com.shabbar.rozgarconnector.utils.TranslatorUtil
import java.text.SimpleDateFormat
import java.util.*

class ActivitiesAdapter(
    private var itemList: List<Any>,
    private val onNotificationClick: (ActivitiesModel) -> Unit,
    private val onEditJob: (ActivitiesModel) -> Unit = {},
    private val onDeleteJob: (ActivitiesModel) -> Unit = {},
    private val onFinishClick: (ActivitiesModel) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int = if (itemList[position] is String) TYPE_HEADER else TYPE_ITEM

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHeader: TextView = view.findViewById(R.id.tvHeaderTitle)
    }

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvNotificationTitle)
        val message: TextView = view.findViewById(R.id.tvNotificationMessage)
        val timestamp: TextView = view.findViewById(R.id.tvNotificationTimestamp)
        val statusLabel: TextView = view.findViewById(R.id.tvStatusLabel)
        val viewsCount: TextView = view.findViewById(R.id.tvViewsCount)
        val btnChat: Button = view.findViewById(R.id.btnChat)
        val btnCancel: Button = view.findViewById(R.id.btnCancel)
        val btnComplete: Button = view.findViewById(R.id.btnComplete)
        val btnEmergency: Button = view.findViewById(R.id.btnEmergency)
        val btnEdit: Button = view.findViewById(R.id.btnEdit)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
        val llActiveInfo: View = view.findViewById(R.id.llActiveInfo)
        val tvWorkInProgress: TextView = view.findViewById(R.id.tvWorkInProgress)
        val redDot: View = view.findViewById(R.id.redDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_activities, parent, false)
            ItemViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            val headerText = itemList[position] as String
            if (TranslatorUtil.isUrduEnabled(holder.itemView.context)) {
                TranslatorUtil.translateText(headerText) { holder.tvHeader.text = it }
            } else {
                holder.tvHeader.text = headerText
            }
        } else if (holder is ItemViewHolder) {
            val activity = itemList[position] as ActivitiesModel
            bindActivityItem(holder, activity)
        }
    }

    private fun bindActivityItem(holder: ItemViewHolder, activity: ActivitiesModel) {
        val currentUserId = auth.currentUser?.uid
        val context = holder.itemView.context
        val status = (activity.status ?: "").lowercase()
        val type = (activity.type ?: "").lowercase()
        val isSender = activity.senderId == currentUserId
        val isUrdu = TranslatorUtil.isUrduEnabled(context)

        holder.btnEdit.visibility = View.GONE
        holder.btnDelete.visibility = View.GONE
        holder.btnChat.visibility = View.GONE
        holder.btnEmergency.visibility = View.GONE
        holder.btnComplete.visibility = View.GONE
        holder.btnCancel.visibility = View.GONE
        holder.viewsCount.visibility = View.GONE
        holder.redDot.visibility = if (!activity.isRead) View.VISIBLE else View.GONE

        // --- Translation Logic for Titles and Messages ---
        val rawTitle = if (type == "myjob" || type == "mywork") (activity.taskTitle ?: activity.title) else (if (type == "hire") "Job Offer" else "Application")
        if (isUrdu) {
            TranslatorUtil.translateText(rawTitle ?: "") { holder.title.text = it }
        } else {
            holder.title.text = rawTitle
        }

        if (type == "myjob" || type == "mywork") {
            val rawMsg = "Status: ${status.uppercase()}\nBudget: Rs. ${activity.budget}\nLocation: ${activity.location}"
            if (isUrdu) {
                TranslatorUtil.translateText(rawMsg) { holder.message.text = it }
            } else {
                holder.message.text = rawMsg
            }
            holder.btnEdit.visibility = View.VISIBLE
            holder.btnDelete.visibility = View.VISIBLE
            holder.statusLabel.text = status.uppercase()
            holder.statusLabel.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF9800"))
            
            holder.viewsCount.visibility = View.VISIBLE
            holder.viewsCount.text = "${activity.viewsCount} Views"
        } else {
            val details = StringBuilder()
            details.append("OFFICIAL WORK DETAILS:\n")
            details.append("• Task: ${activity.taskTitle ?: "N/A"}\n")
            details.append("• Budget: Rs. ${activity.budget ?: "0"}\n")
            details.append("• Location: ${activity.location ?: "N/A"}")
            
            if (isUrdu) {
                TranslatorUtil.translateText(details.toString()) { holder.message.text = it }
            } else {
                holder.message.text = details.toString()
            }

            val isISeeker = if (type == "hire") activity.senderId == currentUserId else activity.receiverId == currentUserId
            val userHasFinished = if (isISeeker) activity.seekerConfirmed else activity.workerConfirmed
            
            when (status) {
                "pending" -> {
                    if (isSender) {
                        holder.btnCancel.visibility = View.VISIBLE
                        holder.btnCancel.text = if(isUrdu) "منسوخ کریں" else "CANCEL"
                    } else {
                        holder.btnComplete.visibility = View.VISIBLE
                        holder.btnComplete.text = if(isUrdu) "قبول کریں" else "ACCEPT"
                        holder.btnCancel.visibility = View.VISIBLE
                        holder.btnCancel.text = if(isUrdu) "مسترد کریں" else "REJECT"
                    }
                    holder.statusLabel.text = "PENDING"
                    holder.statusLabel.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF9800"))
                }
                "accepted", "approved" -> {
                    holder.btnChat.visibility = View.VISIBLE
                    holder.btnEmergency.visibility = View.VISIBLE
                    if (!userHasFinished) {
                        holder.btnComplete.visibility = View.VISIBLE
                        holder.btnComplete.text = if(isUrdu) "کام مکمل" else "FINISH JOB"
                    }
                    
                    if (userHasFinished) {
                        holder.statusLabel.text = "WAITING"
                        holder.statusLabel.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF9800"))
                        holder.tvWorkInProgress.text = if(isUrdu) "دوسرے فریق کا انتظار ہے" else "Waiting for other party"
                    } else {
                        holder.statusLabel.text = "ACTIVE"
                        holder.statusLabel.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
                        holder.tvWorkInProgress.text = if(isUrdu) "کام جاری ہے" else "Work in Progress"
                    }
                }
                "completed" -> {
                    holder.statusLabel.text = "COMPLETED"
                    holder.statusLabel.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2196F3"))
                    holder.tvWorkInProgress.text = if(isUrdu) "کام مکمل ہو گیا" else "✅ Job Finished"
                }
            }
        }

        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        holder.timestamp.text = activity.timestamp?.toDate()?.let { sdf.format(it) } ?: "Now"

        holder.btnComplete.setOnClickListener {
            if (holder.btnComplete.text.toString().contains("ACCEPT", true) || holder.btnComplete.text.toString().contains("قبول", true)) {
                val batch = db.batch()
                val notifRef = db.collection("notifications").document(activity.notificationId!!)
                batch.update(notifRef, "status", "accepted")
                batch.update(notifRef, "title", "Contract Started")
                
                if (!activity.jobId.isNullOrEmpty()) {
                    val jobRef = db.collection("jobs").document(activity.jobId!!)
                    batch.update(jobRef, "status", "accepted")
                }
                
                batch.commit().addOnSuccessListener {
                    Toast.makeText(context, if(isUrdu) "کام شروع ہو گیا!" else "Job Started!", Toast.LENGTH_SHORT).show()
                }
            } else {
                onFinishClick(activity)
            }
        }

        holder.btnCancel.setOnClickListener {
            val btnText = holder.btnCancel.text.toString()
            val newStatus = if (btnText.contains("REJECT", true) || btnText.contains("مسترد", true)) "rejected" else "cancelled"
            db.collection("notifications").document(activity.notificationId!!).update("status", newStatus)
        }

        holder.btnChat.setOnClickListener {
            val intent = Intent(context, ChatActivity::class.java).apply {
                val rId = if (activity.senderId == currentUserId) activity.receiverId else activity.senderId
                putExtra("RECEIVER_ID", rId)
                putExtra("RECEIVER_NAME", if(activity.senderId == currentUserId) "Provider" else activity.senderName)
            }
            context.startActivity(intent)
        }

        holder.itemView.setOnClickListener { onNotificationClick(activity) }
    }

    override fun getItemCount(): Int = itemList.size
    fun updateData(newList: List<Any>) { this.itemList = newList; notifyDataSetChanged() }
}