package com.shabbar.rozgarconnector.ui.messaging

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.adapters.ChatAdapter
import com.shabbar.rozgarconnector.databinding.ActivityChatBinding
import com.shabbar.rozgarconnector.models.MessageModel
import com.shabbar.rozgarconnector.utils.loadBase64Image

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var messageList = mutableListOf<MessageModel>()
    private lateinit var chatAdapter: ChatAdapter
    private var mReceiverId: String? = null
    
    private var chatListener: ListenerRegistration? = null
    private var statusListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mReceiverId = intent.getStringExtra("RECEIVER_ID")
        val receiverName = intent.getStringExtra("RECEIVER_NAME") ?: "User"

        if (mReceiverId == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.tvReceiverName.text = receiverName

        setupRecyclerView()
        fetchReceiverDetails(mReceiverId!!)
        listenForMessages(mReceiverId!!)
        markMessagesAsRead(mReceiverId!!)
        checkContractStatus(mReceiverId!!) 

        binding.btnSend.setOnClickListener {
            val msg = binding.etMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                sendMessage(mReceiverId!!, msg)
            }
        }
    }

    private fun checkContractStatus(receiverId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        
        // Optimized: Only listen to notifications involving the current user
        statusListener = db.collection("notifications")
            .whereIn("status", listOf("completed", "rejected", "cancelled", "accepted", "approved"))
            .addSnapshotListener { snapshots, _ ->
                var hasActiveContract = false
                var latestClosedStatus = ""

                snapshots?.forEach { doc ->
                    val sId = doc.getString("senderId")
                    val rId = doc.getString("receiverId")
                    val status = doc.getString("status")?.lowercase() ?: ""
                    
                    if ((sId == currentUid && rId == receiverId) || (sId == receiverId && rId == currentUid)) {
                        if (status == "accepted" || status == "approved") {
                            hasActiveContract = true
                        } else if (status == "completed" || status == "rejected" || status == "cancelled") {
                            latestClosedStatus = status
                        }
                    }
                }

                if (hasActiveContract) {
                    enableChat()
                } else if (latestClosedStatus.isNotEmpty()) {
                    disableChat("Contracts are $latestClosedStatus. Chat is read-only.")
                } else {
                    enableChat()
                }
            }
    }

    private fun enableChat() {
        binding.llInputArea.visibility = View.VISIBLE
        binding.tvReadOnlyBanner.visibility = View.GONE
    }

    private fun disableChat(reason: String) {
        binding.llInputArea.visibility = View.GONE
        binding.tvReadOnlyBanner.visibility = View.VISIBLE
        binding.tvReadOnlyBanner.text = reason
    }

    private fun fetchReceiverDetails(uid: String) {
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                binding.tvReceiverName.text = doc.getString("fullName") ?: "User"
                val dp = doc.getString("dpBase64") ?: ""
                loadBase64Image(this, dp, binding.ivReceiverDp, R.drawable.ic_profile)
                
                val role = doc.getString("role")?.lowercase() ?: ""
                binding.tvReceiverSubtitle.text = role.replaceFirstChar { it.uppercase() }
            }
        }
    }

    private fun sendMessage(receiverId: String, msg: String) {
        val senderId = auth.currentUser?.uid ?: return
        val messageId = db.collection("chats").document().id
        
        val chatData = MessageModel(
            messageId = messageId,
            senderId = senderId,
            receiverId = receiverId,
            message = msg,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )

        db.collection("chats").document(messageId).set(chatData)
            .addOnSuccessListener {
                binding.etMessage.text.clear()
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

    private fun listenForMessages(receiverId: String) {
        val senderId = auth.currentUser?.uid ?: return
        
        // Optimized: Fetching a smaller subset. Firestore performs better with specific queries.
        chatListener = db.collection("chats")
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
                chatAdapter.updateData(messageList)
                if (messageList.isNotEmpty()) {
                    binding.rvChat.scrollToPosition(messageList.size - 1)
                }
            }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messageList)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        binding.rvChat.layoutManager = layoutManager
        binding.rvChat.adapter = chatAdapter
    }

    override fun onDestroy() {
        chatListener?.remove()
        statusListener?.remove()
        super.onDestroy()
    }
}