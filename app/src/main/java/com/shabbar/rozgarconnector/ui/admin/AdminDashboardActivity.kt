package com.shabbar.rozgarconnector.ui.admin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.adapters.UserAdminAdapter
import com.shabbar.rozgarconnector.databinding.ActivityAdminDashboardBinding
import com.shabbar.rozgarconnector.models.UserModel
import com.shabbar.rozgarconnector.ui.auth.LoginActivity

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val pendingUsers = mutableListOf<UserModel>()
    private lateinit var adapter: UserAdminAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Admin: User Verification"

        // RecyclerView Setup
        adapter = UserAdminAdapter(pendingUsers) { user ->
            approveUser(user.uid)
        }

        binding.rvPendingUsers.layoutManager = LinearLayoutManager(this)
        binding.rvPendingUsers.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { loadPendingUsers() }

        loadPendingUsers()
    }

    // Logic Fix: Fixed Overload resolution ambiguity and missing icon
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Adding Logout with text "Logout" since ic_logout is not available
        val logoutItem = menu?.add(Menu.NONE, 1, Menu.NONE, "Logout")
        logoutItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 1) {
            handleLogout()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleLogout() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun loadPendingUsers() {
        binding.swipeRefresh.isRefreshing = true
        db.collection("users")
            .whereEqualTo("isVerified", false)
            .addSnapshotListener { snapshots, error ->
                binding.swipeRefresh.isRefreshing = false
                if (error != null) {
                    Log.e("ADMIN_DEBUG", "Error: ${error.message}")
                    return@addSnapshotListener
                }

                pendingUsers.clear()
                snapshots?.forEach { doc ->
                    val user = doc.toObject(UserModel::class.java)
                    user.uid = doc.id
                    // Show only workers who need verification
                    if (user.role?.lowercase() == "worker" || user.workerType?.lowercase() in listOf("educated", "uneducated")) {
                        pendingUsers.add(user)
                    }
                }

                adapter.notifyDataSetChanged()
                binding.tvNoData.visibility = if (pendingUsers.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    private fun approveUser(uid: String) {
        if (uid.isEmpty()) return

        db.collection("users").document(uid)
            .update("isVerified", true)
            .addOnSuccessListener {
                Toast.makeText(this, "User Approved Successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Approval Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}