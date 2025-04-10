package com.example.terramaster

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class FragmentJobs : Fragment() {

    private lateinit var viewPager: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_jobs, container, false)

        // Set up the toolbar
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        (requireActivity() as MainActivity).setSupportActionBar(toolbar)

        // Enable options menu in Fragment
        setHasOptionsMenu(true)

        // Get references to TabLayout and ViewPager2
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        viewPager = view.findViewById(R.id.viewPager)

        // Set up the adapter for ViewPager
        val adapter = JobsTabAdapter(requireActivity())
        viewPager.adapter = adapter

        // Link TabLayout with ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Requests"
                1 -> "Ongoing"
                2 -> "Completed"
                else -> null
            }
        }.attach()

        // Get the selected tab index passed from the parent fragment
        val selectedTab = arguments?.getInt("selectedTab", 0) // Default to 0 if not passed

        // If a valid tab index is passed, set the current tab in the ViewPager


// If a valid tab index is passed, set the current tab in the ViewPager
        if (selectedTab != null) {
            viewPager.setCurrentItem(selectedTab, true)
        } else {
            // Ensure the first tab is selected by default if no tab index is passed
            viewPager.setCurrentItem(0, true)
        }


        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.jobs_tool_bar_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                // Navigate to the SearchFragment
                val fragment = SearchFragment()
                val transaction = requireActivity().supportFragmentManager.beginTransaction()
                transaction.replace(R.id.fragment_container, fragment)
                transaction.addToBackStack(null)
                transaction.commit()

                // Optionally show bottom navigation bar (if needed)
                (requireActivity() as MainActivity).showBottomNavigationBar()

                true
            }
            R.id.action_notification -> {
                val fragment = FragmentNotification()
                val transaction = requireActivity().supportFragmentManager.beginTransaction()
                transaction.replace(R.id.fragment_container, fragment)
                transaction.addToBackStack(null)
                transaction.commit()

                // Optionally show bottom navigation bar (if needed)
                (requireActivity() as MainActivity).showBottomNavigationBar()

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
