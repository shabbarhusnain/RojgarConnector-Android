package com.shabbar.rozgarconnector.ui.messaging

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.adapters.ChatAdapter
import com.shabbar.rozgarconnector.databinding.ActivityChatBinding
import com.shabbar.rozgarconnector.models.MessageModel
import com.shabbar.rozgarconnector.utils.TranslatorUtil
import com.shabbar.rozgarconnector.utils.decodeBase64BitmapAsync

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
            Toast.makeText(this, getString(R.string.user_not_found), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Toolbar Setup
        binding.btnBack.setOnClickListener { finish() }
        binding.tvReceiverName.text = receiverName

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
        
        if (TranslatorUtil.isUrduEnabled(this)) {
            TranslatorUtil.initTranslator({}, {})
        }
    }

    private fun fetchReceiverDetails(uid: String) {
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val name = doc.getString("fullName") ?: "User"
                val role = doc.getString("role")?.lowercase() ?: ""
                val workerType = doc.getString("workerType")?.lowercase() ?: ""
                val dpBase64 = doc.getString("dpBase64") ?: ""
                
                binding.tvReceiverName.text = name
                
                // Load DP from Base64
                if (dpBase64.isNotEmpty()) {
                    decodeBase64BitmapAsync(dpBase64, {
                        binding.ivReceiverDp.setImageBitmap(it)
                    }, {
                        binding.ivReceiverDp.setImageResource(R.drawable.ic_profile)
                    })
                } else {
                    binding.ivReceiverDp.setImageResource(R.drawable.ic_profile)
                }
                
                val subtitle = when {
                    role == "seeker" -> getString(R.string.service_seeker)
                    role == "provider" && workerType == "educated" -> getString(R.string.provider_educated)
                    role == "provider" && workerType == "uneducated" -> getString(R.string.provider_uneducated)
                    role == "provider" -> getString(R.string.service_provider)
                    else -> getString(R.string.rozgar_user)
                }
                binding.tvReceiverSubtitle.text = subtitle
            }
        }
    }

    private fun sendMessage(receiverId: String, msg: String) {
        if (isChatLocked) return
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

    private fun checkChatSecurity(receiverId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        db.collection("notifications")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                
                var hasActiveContract = false
                var hasFinishedContract = false
                
                snapshots?.forEach { doc ->
                    val sId = doc.getString("senderId") ?: ""
                    val rId = doc.getString("receiverId") ?: ""
                    val status = doc.getString("status")?.lowercase() ?: ""
                    val sc = doc.getBoolean("seekerConfirmed") ?: false
                    val wc = doc.getBoolean("workerConfirmed") ?: false
                    
                    if ((sId == currentUid && rId == receiverId) || (sId == receiverId && rId == currentUid)) {
                        val isActive = (status == "accepted" || status == "approved")
                        val isFullyFinished = (status == "completed" || (sc && wc))
                        
                        if (isActive && !isFullyFinished && !sc && !wc) {
                            hasActiveContract = true
                        }
                        
                        if (isFullyFinished || status == "disputed") {
                            hasFinishedContract = true
                        }
                    }
                }
                
                if (hasActiveContract) {
                    unlockChat()
                } else if (hasFinishedContract) {
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
        binding.etMessage.hint = if (finished) getString(R.string.job_finished_read_only) else getString(R.string.chat_locked_no_job)
    }

    private fun unlockChat() {
        isChatLocked = false
        binding.etMessage.isEnabled = true
        binding.btnSend.isEnabled = true
        binding.btnSend.alpha = 1.0f
        binding.etMessage.hint = getString(R.string.type_message_hint)
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
                chatAdapter.updateData(messageList)
                if (messageList.isNotEmpty()) {
                    binding.rvChat.smoothScrollToPosition(messageList.size - 1)
                }
            }
    }
}