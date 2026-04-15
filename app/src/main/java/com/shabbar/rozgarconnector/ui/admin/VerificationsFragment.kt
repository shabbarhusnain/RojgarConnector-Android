package com.shabbar.rozgarconnector.ui.admin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.adapters.UserAdminAdapter
import com.shabbar.rozgarconnector.databinding.FragmentAdminVerificationsBinding
import com.shabbar.rozgarconnector.models.UserModel

class VerificationsFragment : Fragment() {

    private var _binding: FragmentAdminVerificationsBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val pendingUsers = mutableListOf<UserModel>()
    private lateinit var adapter: UserAdminAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminVerificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = UserAdminAdapter(pendingUsers) { user ->
            val intent = Intent(requireContext(), VerificationDetailActivity::class.java)
            intent.putExtra("USER_ID", user.uid)
            startActivity(intent)
        }

        binding.rvPendingUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPendingUsers.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { loadPendingUsers() }

        loadPendingUsers()
    }

    private fun loadPendingUsers() {
        if (!isAdded) return
        binding.swipeRefresh.isRefreshing = true
        
        // Simple fetch to debug what is causing the hang
        db.collection("users").addSnapshotListener { snapshots, error ->
            if (!isAdded) return@addSnapshotListener
            binding.swipeRefresh.isRefreshing = false
            
            if (error != null) {
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            if (snapshots == null) return@addSnapshotListener

            pendingUsers.clear()
            for (doc in snapshots) {
                try {
                    // Manual parsing for safety
                    val role = doc.getString("role") ?: ""
                    val isVerified = doc.getBoolean("isVerified") ?: false
                    
                    if (!isVerified && role.lowercase() != "admin" && role.isNotEmpty()) {
                        val user = doc.toObject(UserModel::class.java).apply { uid = doc.id }
                        pendingUsers.add(user)
                    }
                } catch (e: Exception) {
                    Log.e("ADMIN_DEBUG", "Error parsing user: ${doc.id}", e)
                }
            }

            adapter.notifyDataSetChanged()
            binding.lytNoData.visibility = if (pendingUsers.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}