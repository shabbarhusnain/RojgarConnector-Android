package com.shabbar.rozgarconnector.ui.help

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.adapters.ChatAdapter
import com.shabbar.rozgarconnector.databinding.ActivityChatWithAdminBinding
import com.shabbar.rozgarconnector.models.MessageModel

class ChatWithAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatWithAdminBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val messageList = mutableListOf<MessageModel>()
    private lateinit var adapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatWithAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        // Fixed: ChatAdapter only takes messageList as argument
        adapter = ChatAdapter(messageList)
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.chatRecyclerView.adapter = adapter
    }

    private fun loadMessages() {
        val currentUid = auth.currentUser?.uid ?: return
        
        db.collection("admin_chats").document(currentUid).collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    messageList.clear()
                    
                    // 1. Add System Default Welcome Message if chat is empty
                    if (snapshots.isEmpty) {
                        val welcomeMsg = MessageModel(
                            messageId = "system_1",
                            senderId = "agent",
                            receiverId = currentUid,
                            message = "Hello! Welcome to Rozgar Support. Please describe your issue, and an agent will be with you in a minute.",
                            timestamp = System.currentTimeMillis()
                        )
                        messageList.add(welcomeMsg)
                    }

                    for (doc in snapshots) {
                        messageList.add(doc.toObject(MessageModel::class.java))
                    }
                    adapter.notifyDataSetChanged()
                    binding.chatRecyclerView.scrollToPosition(messageList.size - 1)
                }
            }
    }

    private fun sendMessage(text: String) {
        val currentUid = auth.currentUser?.uid ?: return
        val timestamp = System.currentTimeMillis()
        
        val messageObj = MessageModel(
            messageId = "",
            senderId = currentUid,
            receiverId = "agent",
            message = text,
            timestamp = timestamp
        )

        db.collection("admin_chats").document(currentUid).collection("messages")
            .add(messageObj)
            .addOnSuccessListener {
                // 2. Auto-reply from Agent (Default Logic)
                // Filter only user messages to check if it's the first real message
                val userMsgs = messageList.count { it.senderId == currentUid }
                if (userMsgs <= 1) { 
                    Handler(Looper.getMainLooper()).postDelayed({
                        sendAgentAutoReply(currentUid)
                    }, 1500)
                }
            }
    }

    private fun sendAgentAutoReply(userId: String) {
        val autoReply = MessageModel(
            messageId = "system_reply",
            senderId = "agent",
            receiverId = userId,
            message = "Thank you for the details. Please wait for a moment, an agent is connecting with you...",
            timestamp = System.currentTimeMillis()
        )
        
        db.collection("admin_chats").document(userId).collection("messages").add(autoReply)
    }
}