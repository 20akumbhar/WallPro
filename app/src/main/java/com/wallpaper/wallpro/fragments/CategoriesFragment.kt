package com.wallpaper.wallpro.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.wallpaper.wallpro.R
import com.wallpaper.wallpro.adapters.CategoryAdapter
import com.wallpaper.wallpro.models.Category

class CategoriesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_categories, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var categoriesList = mutableListOf<Category>()
        var recyclerView: RecyclerView = view.findViewById(R.id.cat_recyclerView)
        var adapter = CategoryAdapter(
            requireActivity(),
            categoriesList
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(view.context)

        Firebase.firestore.collection("categories")
            .orderBy("timestamp")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("Firebase  :", "listen:error", e)
                    return@addSnapshotListener
                }
                categoriesList.clear()
                for (document in snapshots!!) {

                    val category = Category(
                        document.id,
                        document.data["downloadURL"].toString(),
                        document.data["name"].toString()
                    )
                    categoriesList.add(category)
                }
                adapter.notifyDataSetChanged()
            }
    }

}