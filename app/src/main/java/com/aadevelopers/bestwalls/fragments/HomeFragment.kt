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
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.aadevelopers.bestwalls.R
import com.aadevelopers.bestwalls.adapters.AdTestAdapter
import com.aadevelopers.bestwalls.adapters.WallpaperAdapter
import com.aadevelopers.bestwalls.models.Wallpaper


class HomeFragment : Fragment() {
    private lateinit var wallpaperList: MutableList<Any>
    private lateinit var adapter: AdTestAdapter
    private var lastDocument: DocumentSnapshot? = null
    private lateinit var progressBar: ProgressBar
    private var adIndex = 0
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var isFirst: Boolean = true
        progressBar = view.findViewById(R.id.home_progressbar)
        wallpaperList = mutableListOf<Any>()
        var recyclerView: RecyclerView = view.findViewById(R.id.home_recyclerView)
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
                    if (wallpaperList.size > 15)
                        loadMoreWallpapers()
                }
            }
        })


        Firebase.firestore.collection("wallpapers")
            .orderBy(FieldPath.documentId())
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
                                wallpaperList.add(wallpaper)
                                if (adIndex % 8 == 0 && adIndex!=0) {
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
                if (isFirst && snapshots.size()>0) {
                    Log.d("assigning ", "last doc")
                    lastDocument = snapshots.documents[snapshots.size() - 1]
                }
                adapter.notifyDataSetChanged()
                isFirst = false

            }
    }

    private fun loadMoreWallpapers() {
        progressBar.visibility = View.VISIBLE
        Log.d("random data:", "started")
        Firebase.firestore.collection("wallpapers")
            .orderBy(FieldPath.documentId())
            .startAfter(lastDocument!!)
            .limit(16)
            .get()
            .addOnSuccessListener { snapshots ->
                adIndex = 0
                for (dc in snapshots!!) {
                    adIndex++
                    val wallpaper =
                        Wallpaper(
                            dc.id,
                            dc.data["image"].toString(),
                            dc.data["thumbnail"].toString(),
                            dc.data["isPopular"] as Boolean,
                            dc.data["isPremium"] as Boolean?,
                            dc.data["source"].toString(),
                            dc.data["userId"].toString(),
                            dc.data["categoryId"].toString(),
                            dc.data["timestamp"] as Timestamp,
                        )
                    lastDocument = dc
                    wallpaperList.add(wallpaper)
                    //Log.d("random data:", wallpaper.id)
                    if (adIndex % 8 == 0 && adIndex!=0) {
                        Log.d("Adding to",""+adIndex)
                        wallpaperList.add("ads")
                    }

                }
                adapter.notifyDataSetChanged()
                progressBar.visibility = View.GONE
            }
            .addOnFailureListener {
                Log.e("firebase", it.message!!)
            }

    }


}