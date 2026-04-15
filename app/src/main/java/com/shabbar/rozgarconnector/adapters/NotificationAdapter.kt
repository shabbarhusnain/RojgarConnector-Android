package com.shabbar.rozgarconnector.adapters

import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.models.NotificationModel
import com.shabbar.rozgarconnector.ui.messaging.ChatActivity
import com.shabbar.rozgarconnector.utils.TranslatorUtil
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class NotificationAdapter(
    private var itemList: List<Any>,
    private val onNotificationClick: (NotificationModel) -> Unit
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
        val btnChat: Button = view.findViewById(R.id.btnChat)
        val btnCancel: Button = view.findViewById(R.id.btnCancel)
        val btnComplete: Button = view.findViewById(R.id.btnComplete)
        val btnEmergency: Button = view.findViewById(R.id.btnEmergency)
        val ivIcon: ImageView = view.findViewById(R.id.ivNotificationIcon)
        val llActiveInfo: View = view.findViewById(R.id.llActiveInfo)
        val tvDaysLeft: TextView = view.findViewById(R.id.tvDaysLeft)
        val tvWorkInProgress: TextView = view.findViewById(R.id.tvWorkInProgress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
            ItemViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            holder.tvHeader.text = itemList[position] as String
        } else if (holder is ItemViewHolder) {
            val notification = itemList[position] as NotificationModel
            bindNotificationItem(holder, notification)
        }
    }

    private fun bindNotificationItem(holder: ItemViewHolder, notification: NotificationModel) {
        val currentUserId = auth.currentUser?.uid
        val context = holder.itemView.context
        val status = notification.status.lowercase()
        val type = notification.type.lowercase()
        val isSender = notification.senderId == currentUserId

        setupTheme(holder, type, isSender, notification)

        if (TranslatorUtil.isUrduEnabled(context)) {
            TranslatorUtil.translateText(notification.message) { translated ->
                holder.message.text = translated
            }
        } else {
            holder.message.text = notification.message
        }

        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        holder.timestamp.text = notification.timestamp?.toDate()?.let { sdf.format(it) } ?: context.getString(R.string.just_now)

        val isSeeker = if (notification.type == "hire") notification.senderId == currentUserId else notification.receiverId == currentUserId
        val hasUserFinished = if (isSeeker) notification.seekerConfirmed else notification.workerConfirmed
        val isFullyCompleted = status == "completed"
        val isDisputed = status == "disputed"
        val isHistoryItem = isFullyCompleted || isDisputed || hasUserFinished

        if (isHistoryItem) {
            holder.statusLabel.text = when {
                isDisputed -> "DISPUTED"
                isFullyCompleted -> "COMPLETED"
                else -> "FINISHED (Waiting)"
            }
            holder.statusLabel.backgroundTintList = ColorStateList.valueOf(
                if (isDisputed) Color.parseColor("#F44336") else Color.parseColor("#2196F3")
            )
            
            holder.llActiveInfo.visibility = View.VISIBLE
            holder.tvWorkInProgress.text = if(isDisputed) "⚠️ ISSUE REPORTED" else "✅ " + context.getString(R.string.history)
            holder.tvDaysLeft.visibility = View.GONE
            holder.btnEmergency.visibility = View.GONE
            holder.btnChat.visibility = View.GONE
            holder.btnComplete.visibility = View.GONE
            holder.btnCancel.visibility = View.GONE
            holder.itemView.alpha = 0.8f
        } else {
            val statusText = when(status) {
                "pending" -> context.getString(R.string.pending)
                "accepted", "approved" -> "ACTIVE"
                "rejected", "cancelled" -> context.getString(R.string.cancel)
                else -> status.uppercase()
            }
            holder.statusLabel.text = statusText
            holder.statusLabel.backgroundTintList = ColorStateList.valueOf(getStatusColor(status))
            
            val isActive = status == "accepted" || status == "approved"
            val isPending = status == "pending"

            holder.btnEmergency.visibility = if (isActive) View.VISIBLE else View.GONE
            holder.llActiveInfo.visibility = if (isActive) View.VISIBLE else View.GONE
            holder.tvDaysLeft.visibility = if (isActive) View.VISIBLE else View.GONE
            holder.btnCancel.visibility = if (isPending && isSender) View.VISIBLE else View.GONE
            holder.btnChat.visibility = if (isActive) View.VISIBLE else View.GONE
            holder.btnComplete.visibility = if (isActive) View.VISIBLE else View.GONE
            holder.tvWorkInProgress.text = "⚒️ " + context.getString(R.string.work_in_progress)
            holder.itemView.alpha = 1.0f
        }

        notification.deadlineDate?.let {
            val diff = it.toDate().time - System.currentTimeMillis()
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            val daysText = if (days >= 0) "$days " + context.getString(R.string.days_left) else "Overdue"
            holder.tvDaysLeft.text = daysText
        }

        holder.btnEmergency.setOnClickListener {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:15")))
        }

        holder.btnComplete.setOnClickListener { showCompletionDialog(context, notification) }

        holder.btnCancel.setOnClickListener {
            db.collection("notifications").document(notification.notificationId)
                .update("status", "cancelled")
        }

        holder.btnChat.setOnClickListener {
            val intent = Intent(context, ChatActivity::class.java).apply {
                val rId = if (isSender) notification.receiverId else notification.senderId
                putExtra("RECEIVER_ID", rId)
                putExtra("RECEIVER_NAME", if (isSender) "User" else notification.senderName)
                putExtra("JOB_ID", notification.jobId)
            }
            context.startActivity(intent)
        }

        holder.itemView.setOnClickListener { onNotificationClick(notification) }
    }

    private fun showCompletionDialog(context: android.content.Context, notification: NotificationModel) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_job_completion, null)
        val dialog = AlertDialog.Builder(context).setView(dialogView).create()
        
        val llSeeker = dialogView.findViewById<LinearLayout>(R.id.llSeekerChecklist)
        val llWorker = dialogView.findViewById<LinearLayout>(R.id.llWorkerChecklist)
        val btnSubmit = dialogView.findViewById<Button>(R.id.btnSubmitCompletion)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnDismiss)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
        val etReview = dialogView.findViewById<EditText>(R.id.etReview)

        val rgTask = dialogView.findViewById<RadioGroup>(R.id.rgTask)
        val rgDamage = dialogView.findViewById<RadioGroup>(R.id.rgDamage)
        val rgPaymentSettled = dialogView.findViewById<RadioGroup>(R.id.rgPaymentSettled)
        val rgPaymentReceived = dialogView.findViewById<RadioGroup>(R.id.rgPaymentReceived)
        val rgBehavior = dialogView.findViewById<RadioGroup>(R.id.rgBehavior)

        val currentUid = auth.currentUser?.uid ?: return
        val isSeeker = if (notification.type == "hire") notification.senderId == currentUid else notification.receiverId == currentUid

        if (isSeeker) llSeeker.visibility = View.VISIBLE else llWorker.visibility = View.VISIBLE
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSubmit.setOnClickListener {
            if (ratingBar.rating == 0f) {
                Toast.makeText(context, "Please rate and provide feedback!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var isDisputed = false
            val updateMap = mutableMapOf<String, Any>()

            if (isSeeker) {
                if (rgTask.checkedRadioButtonId == -1 || rgDamage.checkedRadioButtonId == -1 || rgPaymentSettled.checkedRadioButtonId == -1) {
                    Toast.makeText(context, "Please answer all questions!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val hasDamages = (rgDamage.checkedRadioButtonId == R.id.rbDamageYes)
                val taskUnfinished = (rgTask.checkedRadioButtonId == R.id.rbTaskNo)
                
                if (hasDamages || taskUnfinished) isDisputed = true
                
                updateMap["seekerConfirmed"] = true
                updateMap["hasDamages"] = hasDamages
                updateMap["taskNotCompleted"] = taskUnfinished
                updateMap["paymentSettled"] = (rgPaymentSettled.checkedRadioButtonId == R.id.rbPaySettledYes)
                updateMap["ratingToWorker"] = ratingBar.rating
                updateMap["reviewToWorker"] = etReview.text.toString()
            } else {
                if (rgPaymentReceived.checkedRadioButtonId == -1 || rgBehavior.checkedRadioButtonId == -1) {
                    Toast.makeText(context, "Please answer all questions!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val paymentIssue = (rgPaymentReceived.checkedRadioButtonId == R.id.rbPayRecNo)
                if (paymentIssue) isDisputed = true

                updateMap["workerConfirmed"] = true
                updateMap["paymentFullReceived"] = !paymentIssue
                updateMap["behaviorGood"] = (rgBehavior.checkedRadioButtonId == R.id.rbBehYes)
                updateMap["ratingToSeeker"] = ratingBar.rating
                updateMap["reviewToSeeker"] = etReview.text.toString()
            }

            if (isDisputed) {
                updateMap["status"] = "disputed"
                updateMap["disputeReason"] = "Reported during Job Completion: " + etReview.text.toString()
            }

            db.collection("notifications").document(notification.notificationId)
                .set(updateMap, SetOptions.merge())
                .addOnSuccessListener {
                    val targetUserId = if (isSeeker) {
                        if (notification.type == "hire") notification.receiverId else notification.senderId
                    } else {
                        if (notification.type == "hire") notification.senderId else notification.receiverId
                    }
                    updateUserRating(targetUserId, ratingBar.rating)
                    checkIfBothConfirmed(notification.notificationId)
                    dialog.dismiss()
                    Toast.makeText(context, if(isDisputed) "Reported! Admin will review." else "Job Finished!", Toast.LENGTH_SHORT).show()
                }
        }
        dialog.show()
    }

    private fun updateUserRating(userId: String, newRating: Float) {
        if (userId.isEmpty() || newRating == 0f) return
        db.runTransaction { transaction ->
            val userRef = db.collection("users").document(userId)
            val snapshot = transaction.get(userRef)
            if (snapshot.exists()) {
                val oldTotal = (snapshot.getDouble("totalRating") ?: 0.0).toFloat()
                val oldCount = (snapshot.getLong("ratingCount") ?: 0L).toInt()
                val newTotal = oldTotal + newRating
                val newCount = oldCount + 1
                transaction.update(userRef, "totalRating", newTotal)
                transaction.update(userRef, "ratingCount", newCount)
                transaction.update(userRef, "averageRating", newTotal / newCount)
            }
        }
    }

    private fun checkIfBothConfirmed(id: String) {
        db.collection("notifications").document(id).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val status = doc.getString("status") ?: ""
                if (status == "disputed") return@addOnSuccessListener
                
                val sc = doc.getBoolean("seekerConfirmed") ?: false
                val wc = doc.getBoolean("workerConfirmed") ?: false
                if (sc && wc) {
                    db.collection("notifications").document(id).update("status", "completed")
                }
            }
        }
    }

    private fun setupTheme(holder: ItemViewHolder, type: String, isSender: Boolean, notification: NotificationModel) {
        val context = holder.itemView.context
        when (type) {
            "hire" -> {
                holder.ivIcon.setImageResource(R.drawable.ic_profile)
                holder.ivIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E3F2FD"))
                holder.title.text = if (isSender) context.getString(R.string.sent_offers) else context.getString(R.string.job_offers)
            }
            "job" -> {
                holder.ivIcon.setImageResource(R.drawable.ic_settings)
                holder.ivIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E8F5E9"))
                holder.title.text = if (isSender) context.getString(R.string.my_apps) else context.getString(R.string.apps_received)
            }
            else -> {
                holder.ivIcon.setImageResource(R.drawable.ic_notification)
                holder.ivIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F5F5F5"))
                holder.title.text = notification.title.ifEmpty { context.getString(R.string.notif_header) }
            }
        }
    }

    private fun getStatusColor(status: String): Int {
        return when (status) {
            "accepted", "approved" -> Color.parseColor("#4CAF50")
            "rejected", "cancelled" -> Color.parseColor("#F44336")
            "completed" -> Color.parseColor("#2196F3")
            "disputed" -> Color.parseColor("#F44336")
            else -> Color.parseColor("#FF9800")
        }
    }

    fun updateData(newList: List<Any>) {
        this.itemList = newList
        notifyDataSetChanged()
    }

    override fun getItemCount() = itemList.size
}