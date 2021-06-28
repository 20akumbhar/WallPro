package com.wallpaper.wallpro.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.wallpaper.wallpro.fragments.*

class ViewPagerAdapter(fm: FragmentManager, lifeCycle: Lifecycle) :
    FragmentStateAdapter(fm, lifeCycle) {
    override fun getItemCount() = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> LatestFragment()
            1 -> PopularFragment()
            2 -> HomeFragment()
            3 -> CategoriesFragment()
            4 -> FavoriteFragment()
            else-> HomeFragment()
        }
    }


}