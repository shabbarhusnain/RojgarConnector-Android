package com.shabbar.rozgarconnector.ui.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.ActivityProviderHomeBinding
import com.shabbar.rozgarconnector.ui.fragments.ProviderHomeFragment
import com.shabbar.rozgarconnector.ui.fragments.MessagesFragment
import com.shabbar.rozgarconnector.ui.fragments.ActivitiesFragment
import com.shabbar.rozgarconnector.ui.fragments.ProfileFragment

class ProviderHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProviderHomeBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var providerHomeFragment: ProviderHomeFragment
    private lateinit var activitiesFragment: ActivitiesFragment
    private lateinit var messagesFragment: MessagesFragment
    private lateinit var profileFragment: ProfileFragment
    private var activeFragment: Fragment? = null

    private var messageListener: ListenerRegistration? = null
    private var notificationListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProviderHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            providerHomeFragment = ProviderHomeFragment()
            activitiesFragment = ActivitiesFragment()
            messagesFragment = MessagesFragment()
            profileFragment = ProfileFragment()

            supportFragmentManager.beginTransaction().apply {
                add(R.id.fragment_container, profileFragment, "profile").hide(profileFragment)
                add(R.id.fragment_container, messagesFragment, "messages").hide(messagesFragment)
                add(R.id.fragment_container, activitiesFragment, "activities").hide(activitiesFragment)
                add(R.id.fragment_container, providerHomeFragment, "home")
            }.commit()
            activeFragment = providerHomeFragment
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> showFragment(providerHomeFragment)
                R.id.nav_notifications -> {
                    showFragment(activitiesFragment)
                    clearBadge(R.id.nav_notifications)
                }
                R.id.nav_messages -> {
                    showFragment(messagesFragment)
                    clearBadge(R.id.nav_messages)
                }
                R.id.nav_profile -> showFragment(profileFragment)
            }
            true
        }

        listenForBadges()
    }

    private fun showFragment(fragment: Fragment) {
        if (activeFragment == fragment) return
        supportFragmentManager.beginTransaction().hide(activeFragment!!).show(fragment).commit()
        activeFragment = fragment
    }

    private fun listenForBadges() {
        val uid = auth.currentUser?.uid ?: return

        messageListener = db.collection("chats")
            .whereEqualTo("receiverId", uid)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshots, _ ->
                val unreadCount = snapshots?.size() ?: 0
                updateBadge(R.id.nav_messages, unreadCount)
            }

        notificationListener = db.collection("notifications")
            .whereEqualTo("receiverId", uid)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshots, _ ->
                val unreadCount = snapshots?.size() ?: 0
                updateBadge(R.id.nav_notifications, unreadCount)
            }
    }

    private fun updateBadge(menuItemId: Int, count: Int) {
        if (isFinishing || isDestroyed) return
        val badge = binding.bottomNav.getOrCreateBadge(menuItemId)
        if (count > 0) {
            badge.isVisible = true
            badge.number = count
            badge.backgroundColor = ContextCompat.getColor(this, R.color.red)
        } else {
            badge.isVisible = false
        }
    }

    private fun clearBadge(menuItemId: Int) {
        binding.bottomNav.getBadge(menuItemId)?.isVisible = false
    }

    override fun onDestroy() {
        super.onDestroy()
        messageListener?.remove()
        notificationListener?.remove()
    }
}