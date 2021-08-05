package com.aadevelopers.bestwalls.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.aadevelopers.bestwalls.R
import com.aadevelopers.bestwalls.adapters.AdTestAdapter
import com.aadevelopers.bestwalls.adapters.WallpaperAdapter
import com.aadevelopers.bestwalls.models.Wallpaper

class FavoriteFragment : Fragment() {
    private lateinit var progressBar: ProgressBar
    private lateinit var wallpaperList: MutableList<Any>
    private lateinit var adapter: AdTestAdapter
    private var lastDocument: DocumentSnapshot? = null
    private var adIndex = 0
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_favorite, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        wallpaperList = mutableListOf()
        val recyclerView: RecyclerView = view.findViewById(R.id.favorite_recyclerView)
        progressBar=view.findViewById(R.id.favorite_progressbar)
        adapter = AdTestAdapter(requireActivity(), wallpaperList)
        recyclerView.adapter = adapter
        val gridLayoutManager:GridLayoutManager= GridLayoutManager(view.context, 2)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {

            override fun getSpanSize(position: Int): Int {
                if (wallpaperList[position] is String){
                    return 2
                }
                return 1
            }
        }
        recyclerView.layoutManager = gridLayoutManager

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
            .limit(16)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("Firebase  :", "listen:error", e)
                    return@addSnapshotListener
                }
                wallpaperList.clear()
                adIndex = 0

                for (dc in snapshots!!) {
                    adIndex++

                    val wallpaper =
                        Wallpaper(
                            dc.data["wallpaperId"].toString(),
                            dc.data["image"].toString(),
                            dc.data["thumbnail"].toString(),
                            false,
                            dc.data["isPremium"] as Boolean?,
                            "",
                            dc.data["userId"].toString(),
                            "",
                            dc.data["timestamp"] as Timestamp,
                        )

                    wallpaperList.add(wallpaper)
                    if (adIndex % 8 == 0 && adIndex!=0) {
                        wallpaperList.add("ads")
                    }
                    lastDocument = dc
                }
                if (snapshots.size()<16){
                    wallpaperList.add("ads")
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun loadMoreWallpapers() {
        progressBar.visibility=View.VISIBLE
        Firebase.firestore.collection("favorites")
            .whereEqualTo("userId", Firebase.auth.currentUser!!.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .startAfter(lastDocument!!)
            .limit(16)
            .get()
            .addOnSuccessListener { snapshots ->
                adIndex = 0

                for (dc in snapshots!!) {
                    adIndex++
                    val wallpaper =
                        Wallpaper(
                            dc.data["wallpaperId"].toString(),
                            dc.data["image"].toString(),
                            dc.data["thumbnail"].toString(),
                            false,
                            dc.data["isPremium"] as Boolean?,
                            "",
                            dc.data["userId"].toString(),
                            "",
                            dc.data["timestamp"] as Timestamp,
                        )

                    wallpaperList.add(wallpaper)
                    if (adIndex % 8 == 0 && adIndex!=0) {
                        Log.d("Adding to",""+adIndex)
                        wallpaperList.add("ads")
                    }
                    lastDocument = dc
                }
                adapter.notifyDataSetChanged()
                progressBar.visibility=View.GONE
            }

    }

}