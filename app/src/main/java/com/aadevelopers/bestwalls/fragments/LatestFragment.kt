package com.aadevelopers.bestwalls.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.aadevelopers.bestwalls.R
import com.aadevelopers.bestwalls.adapters.AdTestAdapter
import com.aadevelopers.bestwalls.adapters.WallpaperAdapter
import com.aadevelopers.bestwalls.models.Wallpaper


class LatestFragment : Fragment() {
    private lateinit var progressBar: ProgressBar
    private lateinit var wallpaperList: MutableList<Any>
    private var lastDocument: DocumentSnapshot? = null
    private lateinit var adapter: AdTestAdapter
    private var adIndex = 0
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_latest, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        wallpaperList = mutableListOf<Any>()
        val recyclerView: RecyclerView = view.findViewById(R.id.home_recyclerView)
        progressBar = view.findViewById(R.id.latest_progressbar)
        adapter = AdTestAdapter(requireActivity(), wallpaperList)
        var isFirst = true
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
        //gridLayoutManager.spanSizeLookup=GridLayoutManager.SpanSizeLookup()
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
        Firebase.firestore.collection("wallpapers")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(16)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("Firebase  :", "listen:error", e)
                    return@addSnapshotListener
                }
                adIndex = 0
                for (dc in snapshots!!.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> {
                            adIndex++

                            val document = dc.document
                            val wallpaper =
                                Wallpaper(
                                    document.id,
                                    document.data["image"].toString(),
                                    document.data["thumbnail"].toString(),
                                    document.data["isPopular"] as Boolean,
                                    document.data["isPremium"] as Boolean?,
                                    document.data["source"].toString(),
                                    document.data["userId"].toString(),
                                    document.data["categoryId"].toString(),
                                    document.data["timestamp"] as Timestamp,
                                )
                            if (isFirst) {
                                Log.e("firebase last :", "changing again")
                                wallpaperList.add(wallpaper)
                                if (adIndex % 8 == 0) {
                                    wallpaperList.add("ads")
                                }
                            } else {
                                wallpaperList.add(0, wallpaper)
                            }


                            // Log.d("Firebase  Added:", "New city: ${dc.document.data}")
                        }
                        else -> {
                        }
                    }
                }
                if (isFirst && snapshots.size() > 0) {
                    lastDocument = snapshots.documents[snapshots.size() - 1]

                    Log.d("firebase last h:", lastDocument!!.id)
                }
//                wallpaperList.add("Ads")
                isFirst = false
                //addAdsData()
                adapter.notifyDataSetChanged()
            }
    }


    private fun loadMoreWallpapers() {
        progressBar.visibility = View.VISIBLE
        Log.d("firebase last lm:", lastDocument!!.id)
        val moreWallpapers = mutableListOf<Any>()
        val query: Query = Firebase.firestore.collection("wallpapers")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(16)
            .startAfter(lastDocument!!)

        query
            .get()
            .addOnSuccessListener { documents ->
                adIndex = 0
                for (doc in documents) {
                    adIndex++
                    val wallpaperMore =
                        Wallpaper(
                            doc.id,
                            doc.data["image"].toString(),
                            doc.data["thumbnail"].toString(),
                            doc.data["isPopular"] as Boolean,
                            doc.data["isPremium"] as Boolean?,
                            doc.data["source"].toString(),
                            doc.data["userId"].toString(),
                            doc.data["categoryId"].toString(),
                            doc.data["timestamp"] as Timestamp,
                        )
                    moreWallpapers.add(wallpaperMore)
                    lastDocument = doc
                    if (adIndex % 8 == 0 && adIndex!=2) {
                        Log.d("Adding to",""+adIndex)
                        moreWallpapers.add("ads")
                    }
                    Log.d("firebase last :", doc!!.id)
                }
                wallpaperList.addAll(moreWallpapers)
//                if (moreWallpapers.size > 0) {
//                    wallpaperList.add("Ads")
//                }
                //Log.d("firebase last after:", lastDocument!!.id)
                //addMoreAdsData(baseSize)
                adapter.notifyDataSetChanged()
                progressBar.visibility = View.GONE
            }

    }

}