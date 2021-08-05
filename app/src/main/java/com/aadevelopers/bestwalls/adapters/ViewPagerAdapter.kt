package com.aadevelopers.bestwalls.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.aadevelopers.bestwalls.fragments.*

class ViewPagerAdapter(fm: FragmentManager, lifeCycle: Lifecycle) :
    FragmentStateAdapter(fm, lifeCycle) {
    override fun getItemCount() = 6

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> LatestFragment()
            1 -> HomeFragment()
            2 -> PopularFragment()
            3 -> PremiumFragment()
            4 -> CategoriesFragment()
            5 -> FavoriteFragment()
            else-> HomeFragment()
        }
    }


}