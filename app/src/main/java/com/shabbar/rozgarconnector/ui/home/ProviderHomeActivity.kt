package com.shabbar.rozgarconnector.ui.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.ActivityProviderHomeBinding
import com.shabbar.rozgarconnector.ui.fragments.HomeFragment
import com.shabbar.rozgarconnector.ui.fragments.MessagesFragment
import com.shabbar.rozgarconnector.ui.fragments.NotificationsFragment
import com.shabbar.rozgarconnector.ui.fragments.ProfileFragment

class ProviderHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProviderHomeBinding

    // Create instances of all fragments.
    private val homeFragment = HomeFragment()
    private val notificationsFragment = NotificationsFragment()
    private val messagesFragment = MessagesFragment()
    private val profileFragment = ProfileFragment()
    private var activeFragment: Fragment = homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProviderHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Add all fragments to the container, but show only the homeFragment.
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, profileFragment, "4").hide(profileFragment)
            add(R.id.fragment_container, messagesFragment, "3").hide(messagesFragment)
            add(R.id.fragment_container, notificationsFragment, "2").hide(notificationsFragment)
            add(R.id.fragment_container, homeFragment, "1")
        }.commit()

        binding.bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> showFragment(homeFragment)
                R.id.nav_notifications -> showFragment(notificationsFragment)
                R.id.nav_messages -> showFragment(messagesFragment)
                R.id.nav_profile -> showFragment(profileFragment)
            }
            true
        }
    }

    /**
     * Hides the currently active fragment and shows the new one.
     * This preserves the state of each fragment.
     */
    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(fragment)
            .commit()
        activeFragment = fragment
    }
}