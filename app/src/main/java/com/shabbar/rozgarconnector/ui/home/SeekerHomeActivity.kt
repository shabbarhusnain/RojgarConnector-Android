package com.shabbar.rozgarconnector.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.ActivitySeekerHomeBinding
import com.shabbar.rozgarconnector.ui.fragments.SeekerHomeFragment
import com.shabbar.rozgarconnector.ui.fragments.MessagesFragment
import com.shabbar.rozgarconnector.ui.fragments.NotificationsFragment
import com.shabbar.rozgarconnector.ui.fragments.ProfileFragment
import com.shabbar.rozgarconnector.ui.job.JobPostActivity

class SeekerHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySeekerHomeBinding

    private val seekerHomeFragment = SeekerHomeFragment()
    private val notificationsFragment = NotificationsFragment()
    private val messagesFragment = MessagesFragment()
    private val profileFragment = ProfileFragment()
    private var activeFragment: Fragment = seekerHomeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeekerHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Add all fragments to the container, but show only the seekerHomeFragment.
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, profileFragment, "4").hide(profileFragment)
            add(R.id.fragment_container, messagesFragment, "3").hide(messagesFragment)
            add(R.id.fragment_container, notificationsFragment, "2").hide(notificationsFragment)
            add(R.id.fragment_container, seekerHomeFragment, "1")
        }.commit()

        binding.bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> showFragment(seekerHomeFragment)
                R.id.nav_post_job -> {
                    startActivity(Intent(this, JobPostActivity::class.java))
                    return@setOnItemSelectedListener false // Do not select the item
                }
                R.id.nav_notifications -> showFragment(notificationsFragment)
                R.id.nav_messages -> showFragment(messagesFragment)
                R.id.nav_profile -> showFragment(profileFragment)
            }
            true
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