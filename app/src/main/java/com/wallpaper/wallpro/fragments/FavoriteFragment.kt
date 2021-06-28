package com.wallpaper.wallpro.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.wallpaper.wallpro.R
import com.wallpaper.wallpro.adapters.WallpaperAdapter
import com.wallpaper.wallpro.models.Wallpaper

class FavoriteFragment : Fragment() {

    private lateinit var wallpaperList: MutableList<Wallpaper>
    private lateinit var adapter: WallpaperAdapter
    private var lastDocument: DocumentSnapshot? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_favorite, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        wallpaperList = mutableListOf<Wallpaper>()
        val recyclerView: RecyclerView = view.findViewById(R.id.favorite_recyclerView)
        adapter = WallpaperAdapter(requireActivity(), wallpaperList)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = GridLayoutManager(view.context, 2)

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(View.SCROLL_INDICATOR_BOTTOM)) {
                    if (wallpaperList.size > 11)
                        loadMoreWallpapers()
                }
            }
        })
        Firebase.firestore.collection("favorites")
            .whereEqualTo("userId", Firebase.auth.currentUser!!.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(12)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("Firebase  :", "listen:error", e)
                    return@addSnapshotListener
                }
                wallpaperList.clear()
                for (dc in snapshots!!) {
                    val wallpaper =
                        Wallpaper(
                            dc.data["wallpaperId"].toString(),
                            dc.data["image"].toString(),
                            dc.data["thumbnail"].toString(),
                            false,
                            "",
                            dc.data["userId"].toString(),
                            "",
                            dc.data["timestamp"] as Timestamp,
                        )

                    wallpaperList.add(wallpaper)
                    lastDocument = dc
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun loadMoreWallpapers() {
        Firebase.firestore.collection("favorites")
            .whereEqualTo("userId", Firebase.auth.currentUser!!.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .startAfter(lastDocument!!)
            .limit(12)
            .get()
            .addOnSuccessListener { snapshots ->
                for (dc in snapshots!!) {
                    val wallpaper =
                        Wallpaper(
                            dc.data["wallpaperId"].toString(),
                            dc.data["image"].toString(),
                            dc.data["thumbnail"].toString(),
                            false,
                            "",
                            dc.data["userId"].toString(),
                            "",
                            dc.data["timestamp"] as Timestamp,
                        )

                    wallpaperList.add(wallpaper)
                    lastDocument = dc
                }
                adapter.notifyDataSetChanged()
            }

    }

}