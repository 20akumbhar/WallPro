package com.wallpaper.wallpro.fragments

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
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.wallpaper.wallpro.R
import com.wallpaper.wallpro.adapters.WallpaperAdapter
import com.wallpaper.wallpro.models.Wallpaper

class PopularFragment : Fragment() {
    private var lastDocument: DocumentSnapshot?=null
    private lateinit var progressbar: ProgressBar
    private lateinit var wallpaperList: MutableList<Wallpaper>
    private lateinit var adapter: WallpaperAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_popular, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        wallpaperList = mutableListOf()
        var isFirst = true
        val recyclerView: RecyclerView = view.findViewById(R.id.home_recyclerView)
        progressbar = view.findViewById(R.id.popular_progressbar)
        adapter = WallpaperAdapter(requireActivity(), wallpaperList)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = GridLayoutManager(view.context, 2)

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(View.SCROLL_INDICATOR_BOTTOM)) {
                    loadMoreWallpapers()
                }
            }
        })


        Firebase.firestore.collection("wallpapers")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(12)
            .whereEqualTo("isPopular", true)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("Firebase  :", "listen:error", e)
                    return@addSnapshotListener
                }
                for (dc in snapshots!!.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> {
                            val document = dc.document
                            val wallpaper =
                                Wallpaper(
                                    document.id,
                                    document.data["image"].toString(),
                                    document.data["thumbnail"].toString(),
                                    document.data["isPopular"] as Boolean,
                                    document.data["source"].toString(),
                                    document.data["userId"].toString(),
                                    document.data["categoryId"].toString(),
                                    document.data["timestamp"] as Timestamp,
                                )
                            if (isFirst) {
                                wallpaperList.add(wallpaper)
                            } else {
                                wallpaperList.add(0, wallpaper)
                            }

                            Log.d("Firebase  Added:", "New city: ${dc.document.data}")
                        }
//                        DocumentChange.Type.REMOVED->{
//                            val document = dc.document
//
//                            val wallpaper =
//                                Wallpaper(
//                                    document.id,
//                                    document.data["image"].toString(),
//                                    document.data["thumbnail"].toString(),
//                                    document.data["isPopular"] as Boolean,
//                                    document.data["source"].toString(),
//                                    document.data["userId"].toString(),
//                                    document.data["categoryId"].toString(),
//                                    document.data["timestamp"] as Timestamp,
//                                )
//                            val p=wallpaperList.indexOf(wallpaper)
//                            adapter.notifyItemRemoved(p)
//                        }
                        else -> {
                        }
                    }
                }
                if (isFirst){
                    Log.d("assigning ","last doc")
                    lastDocument = snapshots.documents[snapshots.size()-1]
                }
                isFirst = false
                adapter.notifyDataSetChanged()
            }
    }

    private fun loadMoreWallpapers() {
        progressbar.visibility = View.VISIBLE
        Log.d("random data:", "started")
        Firebase.firestore.collection("wallpapers")
            .orderBy(FieldPath.documentId())
            .whereEqualTo("isPopular", true)
            .startAfter(lastDocument!!)
            .limit(10)
            .get()
            .addOnSuccessListener { snapshots ->
                for (dc in snapshots!!) {

                    val wallpaper =
                        Wallpaper(
                            dc.id,
                            dc.data["image"].toString(),
                            dc.data["thumbnail"].toString(),
                            dc.data["isPopular"] as Boolean,
                            dc.data["source"].toString(),
                            dc.data["userId"].toString(),
                            dc.data["categoryId"].toString(),
                            dc.data["timestamp"] as Timestamp,
                        )
                    lastDocument = dc
                    wallpaperList.add(wallpaper)
                    Log.d("random data:", wallpaper.id)

                }
                adapter.notifyDataSetChanged()
                progressbar.visibility = View.GONE
            }
            .addOnFailureListener {
                Log.e("firebase", it.message!!)
            }

    }
}