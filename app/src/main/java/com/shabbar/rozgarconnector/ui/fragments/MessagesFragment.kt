package com.shabbar.rozgarconnector.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.adapters.ChatListAdapter
import com.shabbar.rozgarconnector.databinding.FragmentMessagesBinding
import com.shabbar.rozgarconnector.models.ChatListModel
import com.shabbar.rozgarconnector.models.MessageModel
import com.shabbar.rozgarconnector.ui.messaging.ChatActivity
import com.shabbar.rozgarconnector.ui.settings.SettingsActivity

class MessagesFragment : Fragment(R.layout.fragment_messages) {

    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val chatList = mutableListOf<ChatListModel>()
    private lateinit var adapter: ChatListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMessagesBinding.bind(view)

        setupRecyclerView()
        loadChatList()

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
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

    private fun loadChatList() {
        val currentUid = auth.currentUser?.uid ?: return
        
        db.collection("chats")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                
                val conversationMap = mutableMapOf<String, ChatListModel>()
                
                snapshots?.forEach { doc ->
                    val m = doc.toObject(MessageModel::class.java)
                    val otherUserId = if (m.senderId == currentUid) m.receiverId else m.senderId
                    
                    // Only process messages involving current user
                    if (m.senderId == currentUid || m.receiverId == currentUid) {
                        if (!conversationMap.containsKey(otherUserId)) {
                            // Fetch User Details for the first message found (most recent)
                            db.collection("users").document(otherUserId).get().addOnSuccessListener { userDoc ->
                                val name = userDoc.getString("fullName") ?: "Unknown User"
                                val dp = userDoc.getString("dpBase64")
                                
                                val chatItem = ChatListModel(
                                    userId = otherUserId,
                                    userName = name,
                                    lastMessage = m.message,
                                    timestamp = m.timestamp,
                                    profileImage = dp
                                )
                                conversationMap[otherUserId] = chatItem
                                updateUI(conversationMap.values.toList())
                            }
                        }
                    }
                }
            }
    }

    private fun updateUI(list: List<ChatListModel>) {
        if (_binding == null) return
        chatList.clear()
        chatList.addAll(list.sortedByDescending { it.timestamp })
        adapter.notifyDataSetChanged()
        binding.tvEmptyMessage.visibility = if (chatList.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}