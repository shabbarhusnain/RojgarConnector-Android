package com.shabbar.rozgarconnector.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateOvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.badge.BadgeDrawable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.ActivitySeekerHomeBinding
import com.shabbar.rozgarconnector.ui.fragments.SeekerHomeFragment
import com.shabbar.rozgarconnector.ui.fragments.MessagesFragment
import com.shabbar.rozgarconnector.ui.fragments.NotificationsFragment
import com.shabbar.rozgarconnector.ui.fragments.ProfileFragment
import com.shabbar.rozgarconnector.ui.job.JobPostActivity

class SeekerHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySeekerHomeBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val seekerHomeFragment = SeekerHomeFragment()
    private val notificationsFragment = NotificationsFragment()
    private val messagesFragment = MessagesFragment()
    private val profileFragment = ProfileFragment()
    private var activeFragment: Fragment = seekerHomeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeekerHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, profileFragment, "4").hide(profileFragment)
            add(R.id.fragment_container, messagesFragment, "3").hide(messagesFragment)
            add(R.id.fragment_container, notificationsFragment, "2").hide(notificationsFragment)
            add(R.id.fragment_container, seekerHomeFragment, "1")
        }.commit()

        binding.bottomNav.setOnItemSelectedListener {
            animateBottomNavIcon(it.itemId)
            when (it.itemId) {
                R.id.nav_home -> showFragment(seekerHomeFragment)
                R.id.nav_post_job -> {
                    startActivity(Intent(this, JobPostActivity::class.java))
                    return@setOnItemSelectedListener false
                }
                R.id.nav_notifications -> {
                    showFragment(notificationsFragment)
                }
                R.id.nav_messages -> {
                    showFragment(messagesFragment)
                }
                R.id.nav_profile -> showFragment(profileFragment)
            }
            true
        }

        listenForBadges()
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

        // 1. Listen for Unread Messages (Flat collection query)
        db.collection("chats")
            .whereEqualTo("receiverId", uid)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshots, _ ->
                val count = snapshots?.size() ?: 0
                updateBadge(R.id.nav_messages, count)
            }

        // 2. Listen for Unread Notifications
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

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(fragment)
            .commit()
        activeFragment = fragment
    }
}