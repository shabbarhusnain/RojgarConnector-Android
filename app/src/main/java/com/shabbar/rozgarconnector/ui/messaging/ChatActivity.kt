package com.shabbar.rozgarconnector.ui.messaging

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.shabbar.rozgarconnector.adapters.ChatAdapter
import com.shabbar.rozgarconnector.databinding.ActivityChatBinding
import com.shabbar.rozgarconnector.models.MessageModel

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var messageList = mutableListOf<MessageModel>()
    private lateinit var chatAdapter: ChatAdapter
    private var isChatLocked = true
    private var mReceiverId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mReceiverId = intent.getStringExtra("RECEIVER_ID")
        val receiverName = intent.getStringExtra("RECEIVER_NAME") ?: "User"

        if (mReceiverId == null) {
            Toast.makeText(this, "Error: User not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setSupportActionBar(binding.chatToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.chatToolbar.setNavigationOnClickListener { finish() }
        binding.chatToolbar.title = receiverName

        setupRecyclerView()
        fetchReceiverDetails(mReceiverId!!)
        checkChatSecurity(mReceiverId!!)
        listenForMessages(mReceiverId!!)
        markMessagesAsRead(mReceiverId!!)

        binding.btnSend.setOnClickListener {
            val msg = binding.etMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                sendMessage(mReceiverId!!, msg)
            }
        }
    }

    private fun fetchReceiverDetails(uid: String) {
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val name = doc.getString("fullName") ?: "User"
                val role = doc.getString("role")?.lowercase() ?: ""
                val category = doc.getString("workerCategory")?.lowercase() ?: ""
                binding.chatToolbar.title = name
                val subtitle = when {
                    role == "seeker" -> "Service Seeker"
                    role == "provider" && category == "educated" -> "Provider (Educated)"
                    role == "provider" && category == "uneducated" -> "Provider (Uneducated)"
                    role == "provider" -> "Service Provider"
                    else -> "Rozgar User"
                }
                binding.chatToolbar.subtitle = subtitle
            }
        }
    }

    private fun sendMessage(receiverId: String, msg: String) {
        if (isChatLocked) return
        val senderId = auth.currentUser?.uid ?: return
        val senderName = auth.currentUser?.displayName ?: "Someone"
        val messageId = db.collection("chats").document().id
        
        val chatData = MessageModel(
            messageId = messageId,
            senderId = senderId,
            receiverId = receiverId,
            message = msg,
            timestamp = System.currentTimeMillis()
        )

        db.collection("chats").document(messageId).set(chatData)
            .addOnSuccessListener {
                binding.etMessage.text.clear()
                sendNotificationRequest(receiverId, senderName, msg)
            }
    }

    private fun sendNotificationRequest(receiverId: String, senderName: String, message: String) {
        val notificationData = hashMapOf(
            "receiverId" to receiverId,
            "title" to "New message from $senderName",
            "body" to message,
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("push_notifications").add(notificationData)
    }

    private fun checkChatSecurity(receiverId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        db.collection("notifications")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                var isCurrentlyActive = false
                var isFinished = false
                
                snapshots?.forEach { doc ->
                    val sId = doc.getString("senderId") ?: ""
                    val rId = doc.getString("receiverId") ?: ""
                    val status = doc.getString("status")?.lowercase() ?: ""
                    val sc = doc.getBoolean("seekerConfirmed") ?: false
                    val wc = doc.getBoolean("workerConfirmed") ?: false
                    
                    if ((sId == currentUid && rId == receiverId) || (sId == receiverId && rId == currentUid)) {
                        if ((status == "accepted" || status == "approved") && !sc && !wc) {
                            isCurrentlyActive = true
                        }
                        if (status == "completed" || sc || wc) {
                            isFinished = true
                        }
                    }
                }
                
                if (isCurrentlyActive && !isFinished) {
                    unlockChat()
                } else if (isFinished) {
                    lockChat(finished = true)
                } else {
                    lockChat(finished = false)
                }
            }
    }

    private fun markMessagesAsRead(receiverId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        db.collection("chats")
            .whereEqualTo("senderId", receiverId)
            .whereEqualTo("receiverId", currentUid)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { snapshots ->
                for (doc in snapshots) {
                    db.collection("chats").document(doc.id).update("isRead", true)
                }
            }
    }

    private fun lockChat(finished: Boolean) {
        isChatLocked = true
        binding.etMessage.isEnabled = false
        binding.btnSend.isEnabled = false
        binding.btnSend.alpha = 0.3f
        binding.etMessage.hint = if (finished) "Job Finished (Read Only)" else "Chat Locked (No Active Job)"
    }

    private fun unlockChat() {
        isChatLocked = false
        binding.etMessage.isEnabled = true
        binding.btnSend.isEnabled = true
        binding.btnSend.alpha = 1.0f
        binding.etMessage.hint = "Type a message..."
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messageList)
        binding.rvChat.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.rvChat.adapter = chatAdapter
    }

    private fun listenForMessages(receiverId: String) {
        val senderId = auth.currentUser?.uid ?: return
        db.collection("chats")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                messageList.clear()
                snapshots?.forEach { doc ->
                    val m = doc.toObject(MessageModel::class.java)
                    if ((m.senderId == senderId && m.receiverId == receiverId) ||
                        (m.senderId == receiverId && m.receiverId == senderId)) {
                        messageList.add(m)
                    }
                }
                chatAdapter.notifyDataSetChanged()
                if (messageList.isNotEmpty()) {
                    binding.rvChat.smoothScrollToPosition(messageList.size - 1)
                }
            }
    }
}