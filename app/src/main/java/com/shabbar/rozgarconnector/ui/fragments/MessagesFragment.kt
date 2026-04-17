package com.shabbar.rozgarconnector.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.shabbar.rozgarconnector.adapters.ChatListAdapter
import com.shabbar.rozgarconnector.databinding.FragmentMessagesBinding
import com.shabbar.rozgarconnector.models.ChatListModel
import com.shabbar.rozgarconnector.models.MessageModel
import com.shabbar.rozgarconnector.ui.messaging.ChatActivity

class MessagesFragment : Fragment() {

    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val chatList = mutableListOf<ChatListModel>()
    private lateinit var adapter: ChatListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadInbox()
    }

    private fun setupRecyclerView() {
        adapter = ChatListAdapter(chatList) { chat ->
            val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra("RECEIVER_ID", chat.userId)
                putExtra("RECEIVER_NAME", chat.userName)
            }
            startActivity(intent)
        }
        binding.messagesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.messagesRecyclerView.adapter = adapter
    }

    private fun loadInbox() {
        val currentUid = auth.currentUser?.uid ?: return
        
        db.collection("chats")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (!isAdded || snapshots == null) return@addSnapshotListener
                
                val inboxMap = mutableMapOf<String, ChatListModel>()
                
                snapshots.forEach { doc ->
                    val msg = doc.toObject(MessageModel::class.java)
                    val otherUserId = if (msg.senderId == currentUid) msg.receiverId else msg.senderId
                    
                    if ((msg.senderId == currentUid || msg.receiverId == currentUid) && otherUserId.isNotEmpty()) {
                        if (!inboxMap.containsKey(otherUserId)) {
                            inboxMap[otherUserId] = ChatListModel(
                                userId = otherUserId,
                                lastMessage = msg.message,
                                timestamp = msg.timestamp,
                                hasUnread = !msg.isRead && msg.receiverId == currentUid
                            )
                            fetchUserInfo(otherUserId)
                        }
                    }
                }
                
                chatList.clear()
                chatList.addAll(inboxMap.values.sortedByDescending { it.timestamp })
                adapter.notifyDataSetChanged()
                binding.tvEmptyMessage.visibility = if (chatList.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    private fun fetchUserInfo(uid: String) {
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (!isAdded || !doc.exists()) return@addOnSuccessListener
            val name = doc.getString("fullName") ?: "User"
            // Get DP from Base64 field
            val image = doc.getString("dpBase64")
            
            chatList.find { it.userId == uid }?.let {
                val index = chatList.indexOf(it)
                chatList[index] = it.copy(userName = name, profileImage = image)
                adapter.notifyItemChanged(index)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
