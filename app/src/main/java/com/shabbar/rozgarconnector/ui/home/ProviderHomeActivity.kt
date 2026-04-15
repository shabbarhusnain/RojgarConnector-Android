package com.shabbar.rozgarconnector.ui.home

import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateOvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.badge.BadgeDrawable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.ActivityProviderHomeBinding
import com.shabbar.rozgarconnector.ui.fragments.ProviderHomeFragment
import com.shabbar.rozgarconnector.ui.fragments.MessagesFragment
import com.shabbar.rozgarconnector.ui.fragments.NotificationsFragment
import com.shabbar.rozgarconnector.ui.fragments.ProfileFragment

class ProviderHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProviderHomeBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val homeFragment = ProviderHomeFragment()
    private val notificationsFragment = NotificationsFragment()
    private val messagesFragment = MessagesFragment()
    private val profileFragment = ProfileFragment()
    private var activeFragment: Fragment = homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProviderHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, profileFragment, "4").hide(profileFragment)
            add(R.id.fragment_container, messagesFragment, "3").hide(messagesFragment)
            add(R.id.fragment_container, notificationsFragment, "2").hide(notificationsFragment)
            add(R.id.fragment_container, homeFragment, "1")
        }.commit()

        binding.bottomNav.setOnItemSelectedListener {
            animateBottomNavIcon(it.itemId)
            when (it.itemId) {
                R.id.nav_home -> showFragment(homeFragment)
                R.id.nav_notifications -> {
                    clearBadge(R.id.nav_notifications)
                    showFragment(notificationsFragment)
                }
                R.id.nav_messages -> {
                    clearBadge(R.id.nav_messages)
                    showFragment(messagesFragment)
                }
                R.id.nav_profile -> showFragment(profileFragment)
            }
            true
        }

        listenForBadges()
    }

    override fun onStart() {
        super.onStart()
        updateOnlineStatus(true)
    }

    override fun onStop() {
        super.onStop()
        updateOnlineStatus(false)
    }

    private fun updateOnlineStatus(isOnline: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update("isOnline", isOnline)
    }

    private fun animateBottomNavIcon(itemId: Int) {
        val itemView = binding.bottomNav.findViewById<View>(itemId)
        itemView?.let {
            it.animate()
                .translationY(-15f)
                .setDuration(200)
                .setInterpolator(AnticipateOvershootInterpolator())
                .withEndAction {
                    it.animate()
                        .translationY(0f)
                        .setDuration(200)
                        .start()
                }
                .start()
        }
    }

    private fun listenForBadges() {
        val uid = auth.currentUser?.uid ?: return

        // 1. Unread Messages Badge
        db.collection("chats").whereArrayContains("participants", uid)
            .addSnapshotListener { snapshots, _ ->
                var totalUnread = 0
                snapshots?.documents?.forEach { doc ->
                    val lastMessageSender = doc.getString("lastMessageSenderId") ?: ""
                    val isRead = doc.getBoolean("isRead") ?: true
                    if (lastMessageSender != uid && !isRead) {
                        totalUnread++
                    }
                }
                updateBadge(R.id.nav_messages, totalUnread)
            }

        // 2. Unread Notifications Badge
        db.collection("notifications").whereEqualTo("receiverId", uid)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshots, _ ->
                val count = snapshots?.size() ?: 0
                updateBadge(R.id.nav_notifications, count)
            }
    }

    private fun updateBadge(menuItemId: Int, count: Int) {
        val badge = binding.bottomNav.getOrCreateBadge(menuItemId)
        if (count > 0) {
            badge.isVisible = true
            badge.number = count
            badge.backgroundColor = getColor(R.color.error_red)
            badge.badgeTextColor = getColor(R.color.white)
        } else {
            badge.isVisible = false
        }
    }

    private fun clearBadge(menuItemId: Int) {
        binding.bottomNav.getBadge(menuItemId)?.let {
            it.isVisible = false
            it.clearNumber()
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(fragment)
            .commit()
        activeFragment = fragment
    }
}