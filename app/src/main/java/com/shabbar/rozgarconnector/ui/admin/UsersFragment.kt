package com.shabbar.rozgarconnector.ui.admin

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.FragmentAdminUsersBinding
import com.shabbar.rozgarconnector.databinding.ItemUserListAdminBinding
import com.shabbar.rozgarconnector.models.UserModel
import com.shabbar.rozgarconnector.utils.decodeBase64BitmapAsync

class UsersFragment : Fragment() {

    private var _binding: FragmentAdminUsersBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val allUsers = mutableListOf<UserModel>()
    private val filteredUsers = mutableListOf<UserModel>()
    private lateinit var adapter: UserListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = UserListAdapter(filteredUsers) { user, action ->
            when (action) {
                "DETAIL" -> {
                    val intent = Intent(requireContext(), VerificationDetailActivity::class.java)
                    intent.putExtra("USER_ID", user.uid ?: "")
                    startActivity(intent)
                }
                "BLOCK" -> toggleBlockStatus(user)
                "DELETE" -> deleteUser(user)
            }
        }

        binding.rvAllUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAllUsers.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { loadAllUsers() }

        binding.etSearchUser.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterUsers(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadAllUsers()
    }

    private fun loadAllUsers() {
        if (!isAdded) return
        binding.swipeRefresh.isRefreshing = true
        
        db.collection("users")
            .whereNotEqualTo("role", "admin")
            .addSnapshotListener { snapshots, error ->
                if (!isAdded) return@addSnapshotListener
                binding.swipeRefresh.isRefreshing = false
                
                if (error != null) return@addSnapshotListener

                allUsers.clear()
                snapshots?.forEach { doc ->
                    val user = doc.toObject(UserModel::class.java).apply { uid = doc.id }
                    allUsers.add(user)
                }
                
                filterUsers(binding.etSearchUser.text.toString())
            }
    }

    private fun toggleBlockStatus(user: UserModel) {
        val uid = user.uid
        if (uid.isNullOrEmpty()) return
        
        val newStatus = !user.isBlocked
        db.collection("users").document(uid).update("isBlocked", newStatus)
            .addOnSuccessListener {
                val msg = if (newStatus) "User Blocked" else "User Unblocked"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteUser(user: UserModel) {
        val uid = user.uid
        if (uid.isNullOrEmpty()) return

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete ${user.fullName ?: "this user"}?")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("users").document(uid).delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "User Deleted", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun filterUsers(query: String) {
        filteredUsers.clear()
        if (query.isEmpty()) {
            filteredUsers.addAll(allUsers)
        } else {
            val q = query.lowercase()
            allUsers.forEach { user ->
                val nameMatch = user.fullName?.lowercase()?.contains(q) ?: false
                val cnicMatch = user.cnic?.contains(q) ?: false
                if (nameMatch || cnicMatch) {
                    filteredUsers.add(user)
                }
            }
        }
        adapter.notifyDataSetChanged()
        binding.lytNoUsers.visibility = if (filteredUsers.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class UserListAdapter(
        private val list: List<UserModel>,
        private val onAction: (UserModel, String) -> Unit
    ) : RecyclerView.Adapter<UserListAdapter.UserVH>() {

        inner class UserVH(val b: ItemUserListAdminBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserVH {
            return UserVH(ItemUserListAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: UserVH, position: Int) {
            val user = list[position]
            holder.b.apply {
                tvUserName.text = user.fullName ?: "No Name"
                tvUserRole.text = user.role?.uppercase() ?: "USER"
                tvUserCnic.text = "CNIC: ${user.cnic ?: "N/A"}"
                
                if (user.isVerified) {
                    tvStatusBadge.text = "VERIFIED"
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_status_accepted)
                } else {
                    tvStatusBadge.text = "PENDING"
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_status_pending)
                }

                viewOnlineDot.visibility = if (user.isOnline) View.VISIBLE else View.GONE

                val avatarBase64 = user.dpBase64
                if (!avatarBase64.isNullOrEmpty()) {
                    decodeBase64BitmapAsync(avatarBase64, {
                        imgUserAvatar.setImageBitmap(it)
                    }, {
                        imgUserAvatar.setImageResource(R.drawable.ic_profile)
                    })
                } else {
                    imgUserAvatar.setImageResource(R.drawable.ic_profile)
                }

                imgOptions.setOnClickListener {
                    val popup = PopupMenu(root.context, it)
                    popup.menu.add("View Profile")
                    popup.menu.add(if (user.isBlocked) "Unblock User" else "Block User")
                    popup.menu.add("Delete Account")
                    
                    popup.setOnMenuItemClickListener { item ->
                        when (item.title) {
                            "View Profile" -> onAction(user, "DETAIL")
                            "Unblock User", "Block User" -> onAction(user, "BLOCK")
                            "Delete Account" -> onAction(user, "DELETE")
                        }
                        true
                    }
                    popup.show()
                }

                root.setOnClickListener { onAction(user, "DETAIL") }
            }
        }

        override fun getItemCount() = list.size
    }
}
