package com.shabbar.rozgarconnector.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.ActivitySeekerHomeBinding
import com.shabbar.rozgarconnector.ui.fragments.SeekerHomeFragment
import com.shabbar.rozgarconnector.ui.fragments.MessagesFragment
import com.shabbar.rozgarconnector.ui.fragments.ActivitiesFragment
import com.shabbar.rozgarconnector.ui.fragments.ProfileFragment
import com.shabbar.rozgarconnector.ui.job.JobPostActivity

class SeekerHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySeekerHomeBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var seekerHomeFragment: SeekerHomeFragment
    private lateinit var activitiesFragment: ActivitiesFragment
    private lateinit var messagesFragment: MessagesFragment
    private lateinit var profileFragment: ProfileFragment
    private var activeFragment: Fragment? = null
    
    private var messageListener: ListenerRegistration? = null
    private var notificationListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeekerHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            seekerHomeFragment = SeekerHomeFragment()
            activitiesFragment = ActivitiesFragment()
            messagesFragment = MessagesFragment()
            profileFragment = ProfileFragment()

            supportFragmentManager.beginTransaction().apply {
                add(R.id.fragment_container, profileFragment, "profile").hide(profileFragment)
                add(R.id.fragment_container, messagesFragment, "messages").hide(messagesFragment)
                add(R.id.fragment_container, activitiesFragment, "activities").hide(activitiesFragment)
                add(R.id.fragment_container, seekerHomeFragment, "home")
            }.commit()
            activeFragment = seekerHomeFragment
        } else {
            seekerHomeFragment = supportFragmentManager.findFragmentByTag("home") as SeekerHomeFragment
            activitiesFragment = supportFragmentManager.findFragmentByTag("activities") as ActivitiesFragment
            messagesFragment = supportFragmentManager.findFragmentByTag("messages") as MessagesFragment
            profileFragment = supportFragmentManager.findFragmentByTag("profile") as ProfileFragment
            
            // Hide all and show only the selected one to avoid overlapping
            supportFragmentManager.beginTransaction().hide(seekerHomeFragment).hide(activitiesFragment).hide(messagesFragment).hide(profileFragment).commit()
            
            activeFragment = when (binding.bottomNav.selectedItemId) {
                R.id.nav_home -> seekerHomeFragment
                R.id.nav_notifications -> activitiesFragment
                R.id.nav_messages -> messagesFragment
                R.id.nav_profile -> profileFragment
                else -> seekerHomeFragment
            }
            supportFragmentManager.beginTransaction().show(activeFragment!!).commit()
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> showFragment(seekerHomeFragment)
                R.id.nav_post_job -> {
                    startActivity(Intent(this, JobPostActivity::class.java))
                    return@setOnItemSelectedListener false
                }
                R.id.nav_notifications -> showFragment(activitiesFragment)
                R.id.nav_messages -> showFragment(messagesFragment)
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

    override fun onStart() {
        super.onStart()
        updateOnlineStatus(true)
    }

    override fun onStop() {
        super.onStop()
        updateOnlineStatus(false)
        messageListener?.remove()
        notificationListener?.remove()
    }

    private fun updateOnlineStatus(isOnline: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update("isOnline", isOnline)
    }

    private fun listenForBadges() {
        val uid = auth.currentUser?.uid ?: return

        messageListener = db.collection("chats")
            .whereEqualTo("receiverId", uid)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshots, _ ->
                updateBadge(R.id.nav_messages, snapshots?.size() ?: 0)
            }

        notificationListener = db.collection("notifications")
            .whereEqualTo("receiverId", uid)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshots, _ ->
                updateBadge(R.id.nav_notifications, snapshots?.size() ?: 0)
            }
    }

    private fun updateBadge(menuItemId: Int, count: Int) {
        if (isFinishing || isDestroyed) return
        val badge = binding.bottomNav.getOrCreateBadge(menuItemId)
        if (count > 0) {
            badge.isVisible = true
            badge.number = count
        } else {
            badge.isVisible = false
        }
    }
}