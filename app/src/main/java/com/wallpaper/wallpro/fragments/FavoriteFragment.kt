package com.wallpaper.wallpro.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wallpaper.wallpro.R
import com.wallpaper.wallpro.adapters.WallpaperAdapter
import com.wallpaper.wallpro.models.Wallpaper

class FavoriteFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_favorite, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val wallpaperList = mutableListOf<Wallpaper>()
        var recyclerView: RecyclerView =view.findViewById(R.id.home_recyclerView)
        var adapter= WallpaperAdapter(requireActivity(), wallpaperList)
        recyclerView.adapter=adapter
        recyclerView.layoutManager= GridLayoutManager(view.context,2)
    }

}