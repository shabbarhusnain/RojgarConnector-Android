package com.shabbar.rozgarconnector.ui.help

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.shabbar.rozgarconnector.adapters.ChatAdapter
import com.shabbar.rozgarconnector.databinding.ActivityChatWithAdminBinding
import com.shabbar.rozgarconnector.models.MessageModel

class ChatWithAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatWithAdminBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val messageList = mutableListOf<MessageModel>()
    private lateinit var adapter: ChatAdapter
    
    private var chatWithUserId: String? = null
    private var isAdminSide: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatWithAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isAdminSide = intent.getBooleanExtra("IS_ADMIN_SIDE", false)
        chatWithUserId = intent.getStringExtra("USER_ID") ?: auth.currentUser?.uid

        if (isAdminSide) {
            binding.tvAgentName.text = "Loading..."
            chatWithUserId?.let { uid ->
                db.collection("users").document(uid).get().addOnSuccessListener { 
                    binding.tvAgentName.text = it.getString("fullName") ?: "User Support"
                }
                // Mark as read when admin opens chat
                db.collection("admin_chats").document(uid).update("isRead", true)
            }
        } else {
            binding.tvAgentName.text = "Rozgar Support"
        }

        setupChat()
        loadMessages()
        
        binding.btnBack.setOnClickListener { finish() }

        binding.sendButton.setOnClickListener {
            val msg = binding.messageEditText.text.toString().trim()
            if (msg.isNotEmpty()) {
                sendMessage(msg)
                binding.messageEditText.text.clear()
            }
        }
    }

    private fun setupChat() {
        adapter = ChatAdapter(messageList)
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.chatRecyclerView.adapter = adapter
    }

    private fun loadMessages() {
        val targetChatId = chatWithUserId ?: return
        
        db.collection("admin_chats").document(targetChatId).collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    messageList.clear()
                    
                    if (snapshots.isEmpty && !isAdminSide) {
                        val welcomeMsg = MessageModel(
                            messageId = "system_1",
                            senderId = "agent",
                            receiverId = targetChatId,
                            message = "Hello! Welcome to Rozgar Support. How can we help you?",
                            timestamp = System.currentTimeMillis()
                        )
                        messageList.add(welcomeMsg)
                    }

                    for (doc in snapshots) {
                        messageList.add(doc.toObject(MessageModel::class.java))
                    }
                    adapter.notifyDataSetChanged()
                    if (messageList.isNotEmpty()) {
                        binding.chatRecyclerView.scrollToPosition(messageList.size - 1)
                    }
                }
            }
    }

    private fun sendMessage(text: String) {
        val targetChatId = chatWithUserId ?: return
        val currentUid = auth.currentUser?.uid ?: return
        val timestamp = System.currentTimeMillis()
        
        val senderId = currentUid
        val receiverId = if (isAdminSide) targetChatId else "agent"

        val messageObj = MessageModel(
            messageId = "",
            senderId = senderId,
            receiverId = receiverId,
            message = text,
            timestamp = timestamp
        )

        db.collection("admin_chats").document(targetChatId).collection("messages")
            .add(messageObj)
            .addOnSuccessListener {
                // IMPORTANT: Update Inbox for Admin
                val updateMap = hashMapOf(
                    "lastMessage" to text,
                    "timestamp" to timestamp,
                    "userId" to targetChatId,
                    "isRead" to isAdminSide // If Admin sends, it's read. If User sends, isRead = false.
                )
                db.collection("admin_chats").document(targetChatId).set(updateMap, SetOptions.merge())

                if (!isAdminSide) {
                    // Check if auto-reply needed
                    db.collection("admin_chats").document(targetChatId).collection("messages")
                        .whereEqualTo("senderId", currentUid).get().addOnSuccessListener {
                            if (it.size() <= 1) {
                                Handler(Looper.getMainLooper()).postDelayed({
                                    sendAgentAutoReply(targetChatId)
                                }, 1500)
                            }
                        }
                }
            }
    }

    private fun sendAgentAutoReply(userId: String) {
        val autoReply = MessageModel(
            messageId = "system_reply",
            senderId = "agent",
            receiverId = userId,
            message = "Thank you! Please wait, an agent is connecting with you...",
            timestamp = System.currentTimeMillis()
        )
        db.collection("admin_chats").document(userId).collection("messages").add(autoReply)
    }
}