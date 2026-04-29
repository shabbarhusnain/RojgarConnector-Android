package com.shabbar.rozgarconnector.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import com.shabbar.rozgarconnector.ui.job.WorkPostActivity

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
            // Restore fragments after recreation (e.g., after language change)
            seekerHomeFragment = supportFragmentManager.findFragmentByTag("home") as SeekerHomeFragment
            activitiesFragment = supportFragmentManager.findFragmentByTag("activities") as ActivitiesFragment
            messagesFragment = supportFragmentManager.findFragmentByTag("messages") as MessagesFragment
            profileFragment = supportFragmentManager.findFragmentByTag("profile") as ProfileFragment
            
            // Find which one was active
            activeFragment = when {
                !seekerHomeFragment.isHidden -> seekerHomeFragment
                !activitiesFragment.isHidden -> activitiesFragment
                !messagesFragment.isHidden -> messagesFragment
                !profileFragment.isHidden -> profileFragment
                else -> seekerHomeFragment
            }
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> showFragment(seekerHomeFragment)
                R.id.nav_post_job -> {
                    showPostChoiceDialog()
                    return@setOnItemSelectedListener false
                }
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

    private fun showPostChoiceDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_post_choice, null)
        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialog).setView(view).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        view.findViewById<View>(R.id.btnChoiceEducated).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, JobPostActivity::class.java))
        }

        view.findViewById<View>(R.id.btnChoiceManual).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, WorkPostActivity::class.java))
        }

        view.findViewById<View>(R.id.btnCancelChoice).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
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
