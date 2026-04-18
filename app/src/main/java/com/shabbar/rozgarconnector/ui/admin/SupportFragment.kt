package com.shabbar.rozgarconnector.ui.admin

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.FragmentAdminSupportBinding
import com.shabbar.rozgarconnector.databinding.ItemAdminChatListBinding
import com.shabbar.rozgarconnector.models.MessageModel
import com.shabbar.rozgarconnector.models.UserModel
import com.shabbar.rozgarconnector.ui.help.ChatWithAdminActivity
import com.shabbar.rozgarconnector.utils.decodeBase64BitmapAsync
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SupportFragment : Fragment() {

    private var _binding: FragmentAdminSupportBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val chatUsersList = mutableListOf<ChatUserItem>()
    private lateinit var adapter: AdminChatListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminSupportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AdminChatListAdapter(chatUsersList) { chatUser ->
            db.collection("admin_chats").document(chatUser.user.uid).update("isRead", true)
            val intent = Intent(requireContext(), ChatWithAdminActivity::class.java)
            intent.putExtra("USER_ID", chatUser.user.uid)
            intent.putExtra("IS_ADMIN_SIDE", true)
            startActivity(intent)
        }

        binding.rvAdminChats.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAdminChats.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { loadChatList() }

        loadChatList()
    }

    private fun loadChatList() {
        if (!isAdded) return
        binding.swipeRefresh.isRefreshing = true

        db.collection("admin_chats")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (!isAdded) return@addSnapshotListener
                binding.swipeRefresh.isRefreshing = false
                
                if (e != null || snapshots == null) return@addSnapshotListener
                
                chatUsersList.clear()
                if (snapshots.isEmpty) {
                    binding.lytNoMessages.visibility = View.VISIBLE
                    adapter.notifyDataSetChanged()
                    return@addSnapshotListener
                }

                var fetchedCount = 0
                val totalDocs = snapshots.size()

                snapshots.forEach { chatDoc ->
                    val userId = chatDoc.id
                    val lastMsgText = chatDoc.getString("lastMessage") ?: ""
                    val time = chatDoc.getLong("timestamp") ?: 0L
                    val isRead = chatDoc.getBoolean("isRead") ?: true

                    db.collection("users").document(userId).get().addOnSuccessListener { userDoc ->
                        val user = userDoc.toObject(UserModel::class.java)?.apply { uid = userDoc.id }
                        if (user != null) {
                            val dummyMsg = MessageModel(message = lastMsgText, timestamp = time)
                            chatUsersList.add(ChatUserItem(user, dummyMsg, isRead))
                        }
                        
                        fetchedCount++
                        if (fetchedCount == totalDocs) {
                            chatUsersList.sortByDescending { it.lastMessage.timestamp }
                            adapter.notifyDataSetChanged()
                            binding.lytNoMessages.visibility = if (chatUsersList.isEmpty()) View.VISIBLE else View.GONE
                        }
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class ChatUserItem(val user: UserModel, val lastMessage: MessageModel, val isRead: Boolean)

    inner class AdminChatListAdapter(
        private val list: List<ChatUserItem>,
        private val onItemClick: (ChatUserItem) -> Unit
    ) : RecyclerView.Adapter<AdminChatListAdapter.ChatVH>() {

        inner class ChatVH(val b: ItemAdminChatListBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatVH {
            return ChatVH(ItemAdminChatListBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: ChatVH, position: Int) {
            val item = list[position]
            holder.b.apply {
                // Fix: Use 'fullName' to match updated UserModel
                tvUserName.text = item.user.fullName
                tvLastMessage.text = item.lastMessage.message
                
                val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                tvTime.text = sdf.format(Date(item.lastMessage.timestamp))

                if (!item.isRead) {
                    tvUnreadBadge.visibility = View.VISIBLE
                    tvUnreadBadge.text = "1"
                    tvTime.setTextColor(Color.parseColor("#4CAF50"))
                    tvUserName.setTypeface(null, Typeface.BOLD)
                    tvLastMessage.setTextColor(Color.BLACK)
                } else {
                    tvUnreadBadge.visibility = View.GONE
                    tvTime.setTextColor(Color.GRAY)
                    tvUserName.setTypeface(null, Typeface.NORMAL)
                    tvLastMessage.setTextColor(Color.GRAY)
                }

                val avatarBase64 = item.user.dpBase64
                if (!avatarBase64.isNullOrEmpty()) {
                    decodeBase64BitmapAsync(avatarBase64, {
                        imgUserAvatar.setImageBitmap(it)
                    }, {
                        imgUserAvatar.setImageResource(R.drawable.ic_profile)
                    })
                } else {
                    imgUserAvatar.setImageResource(R.drawable.ic_profile)
                }

                root.setOnClickListener { onItemClick(item) }
            }
        }

        override fun getItemCount() = list.size
    }
}