package com.example.terramaster

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class JobsTabAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 3 // Two tabs

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> RequestTabFragment()
            1 -> OnGoingFragment()
            2 -> FragmentBookingHistory()
            else -> throw IllegalArgumentException("Invalid tab position")
        }
    }
}