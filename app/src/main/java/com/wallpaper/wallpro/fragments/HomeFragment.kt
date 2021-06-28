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
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.wallpaper.wallpro.R
import com.wallpaper.wallpro.adapters.WallpaperAdapter
import com.wallpaper.wallpro.models.Wallpaper


class HomeFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var isFirst:Boolean
        val wallpaperList = mutableListOf<Wallpaper>()
        var recyclerView:RecyclerView=view.findViewById(R.id.home_recyclerView)
        var adapter= WallpaperAdapter(requireActivity(), wallpaperList)
        recyclerView.adapter=adapter
        recyclerView.layoutManager=GridLayoutManager(view.context,2)

        Firebase.firestore.collection("wallpapers")
            .limit(16)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("Firebase  :", "listen:error", e)
                    return@addSnapshotListener
                }
                isFirst = false
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
                            }else{
                                wallpaperList.add(0,wallpaper)
                            }
                            Log.d("Firebase  Added:", "New city: ${dc.document.data}")
                        }
                        else -> {
                        }
                    }
                }
                adapter.notifyDataSetChanged()
            }
    }
}