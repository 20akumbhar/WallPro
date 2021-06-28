package com.wallpaper.wallpro.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.wallpaper.wallpro.R
import com.wallpaper.wallpro.adapters.WallpaperAdapter
import com.wallpaper.wallpro.models.Wallpaper


class LatestFragment : Fragment() {
    private lateinit var progressBar: ProgressBar
    private lateinit var wallpaperList: MutableList<Wallpaper>
    private var lastDocument: DocumentSnapshot? = null
    private lateinit var adapter: WallpaperAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_latest, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        wallpaperList = mutableListOf<Wallpaper>()
        val recyclerView: RecyclerView = view.findViewById(R.id.home_recyclerView)
        progressBar = view.findViewById(R.id.latest_progressbar)
        adapter = WallpaperAdapter(requireActivity(), wallpaperList)
        var isFirst = true
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
                                Log.e("firebase last :", "changing again")
                                wallpaperList.add(wallpaper)
                            } else {
                                wallpaperList.add(0, wallpaper)
                            }
                            Log.d("Firebase  Added:", "New city: ${dc.document.data}")
                        }
                        else -> {
                        }
                    }
                }
                if (isFirst) {
                    lastDocument = snapshots.documents[snapshots.size() - 1]

                    Log.d("firebase last h:", lastDocument!!.id)
                }
                isFirst = false
                adapter.notifyDataSetChanged()
            }
    }

    private fun loadMoreWallpapers() {
        progressBar.visibility = View.VISIBLE
        Log.d("firebase last lm:", lastDocument!!.id)
        val moreWallpapers = mutableListOf<Wallpaper>()
        val query:Query=Firebase.firestore.collection("wallpapers")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .startAfter(lastDocument!!)
        query
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val wallpaperMore =
                        Wallpaper(
                            doc.id,
                            doc.data["image"].toString(),
                            doc.data["thumbnail"].toString(),
                            doc.data["isPopular"] as Boolean,
                            doc.data["source"].toString(),
                            doc.data["userId"].toString(),
                            doc.data["categoryId"].toString(),
                            doc.data["timestamp"] as Timestamp,
                        )
                    moreWallpapers.add(wallpaperMore)
                    lastDocument=doc
                    Log.d("firebase last :", doc!!.id)
                }
                wallpaperList.addAll(moreWallpapers)
                Log.d("firebase last after:", lastDocument!!.id)

                adapter.notifyDataSetChanged()
                progressBar.visibility = View.GONE
            }

    }

}