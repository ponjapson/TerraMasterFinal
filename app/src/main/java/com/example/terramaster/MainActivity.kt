package com.example.terramaster

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var fragmentContainer: FrameLayout
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fragmentContainer = findViewById(R.id.fragment_container)
        bottomNavigationView = findViewById(R.id.bottom_navigation)



        // Disable icon tinting programmatically
        bottomNavigationView.itemIconTintList = null

        // Initialize the default fragment (Feed)
        replaceFragment(FragmentJobs(), false, true)

        // Explicitly set the selected item in the bottom navigation to "Feed"
        bottomNavigationView.selectedItemId = R.id.nav_jobs

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_jobs -> {
                    Log.d("MainActivity", "Navigating to feed fragment")
                    replaceFragment(FragmentJobs(), false, true)
                    true
                }

                R.id.nav_home -> {
                    Log.d("MainActivity", "Navigating to jobs fragment")
                    replaceFragment(FragmentHome(), true)
                    true
                }

                R.id.nav_chatbot -> {
                    replaceFragment(FragmentChatbot(), true)
                    true
                }

                R.id.nav_messages -> {
                    Log.d("MainActivity", "Navigating to messages fragment")
                    replaceFragment(FragmentMessage(), true)
                    true
                }

                R.id.nav_profile -> {
                    Log.d("MainActivity", "Navigating to messages fragment")
                    replaceFragment(FragmentProfile(), true)
                    true
                }

                else -> false
            }
        }

    }

    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        // Handle the back press for FeedHostFragment
        if (currentFragment is FragmentJobs) {
            finish()  // Close the activity when on FeedHostFragment
        } else {
            val backStackEntryCount = supportFragmentManager.backStackEntryCount

            // If there are more than 1 fragment in the back stack, pop the most recent one
            if (backStackEntryCount > 1) {
                supportFragmentManager.popBackStack()

                // Check which fragment is now visible and update BottomNavigation visibility
                val newFragment = supportFragmentManager.fragments.lastOrNull()
                newFragment?.let {
                    updateBottomNavigationVisibility(it)
                }
            } else {
                // If only 1 fragment is in the back stack, navigate to FeedHostFragment
                replaceFragment(FragmentJobs(), false, true)
                updateBottomNavigationVisibility(FragmentHome())  // Ensure BottomNavigationView is visible
            }
        }
    }

    fun replaceFragment(fragment: Fragment, addToBackStack: Boolean = true, clearBackStack: Boolean = false) {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment != null && currentFragment::class == fragment::class) {
            return  // Avoid replacing with the same fragment
        }

        // Clear back stack if needed
        if (clearBackStack) {
            clearFragmentBackStack()
        }

        // Replace the fragment
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)

        if (addToBackStack) {
            transaction.addToBackStack(null)
        }

        transaction.commit()

        // Update BottomNavigationView visibility after fragment replacement
        updateBottomNavigationVisibility(fragment)
    }

    private fun updateBottomNavigationVisibility(fragment: Fragment) {
        // Show BottomNavigationView for main fragments
        if (fragment is FragmentHome || fragment is FragmentChatbot ||
            fragment is FragmentMessage || fragment is FragmentProfile || fragment is FragmentJobs) {
            showBottomNavigationBar()  // Show BottomNavigationView
        } else {
            hideBottomNavigationBar()  // Hide BottomNavigationView
        }
    }

    fun showBottomNavigationBar() {
        bottomNavigationView.visibility = View.VISIBLE
    }

    fun hideBottomNavigationBar() {
        bottomNavigationView.visibility = View.GONE
    }

    fun clearFragmentBackStack() {
        val fragmentManager = supportFragmentManager
        // Pop all fragments from back stack
        if (fragmentManager.backStackEntryCount > 0) {
            val first = fragmentManager.getBackStackEntryAt(0)
            fragmentManager.popBackStack(first.id, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }

}