package com.wallpaper.wallpro.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.wallpaper.wallpro.R
import com.wallpaper.wallpro.adapters.WallpaperAdapter
import com.wallpaper.wallpro.models.Wallpaper

class CategoryWallpaperActivity : AppCompatActivity() {
    private var lastDocument: DocumentSnapshot? = null
    private lateinit var wallpaperList: MutableList<Wallpaper>
    private lateinit var adapter: WallpaperAdapter
    private lateinit var toolbar: Toolbar
    private var categoryName: String? = null
    private var categoryId: String? = null
    private lateinit var progressBar: ProgressBar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wallpaperList = mutableListOf<Wallpaper>()
        setContentView(R.layout.activity_category_wallpaper)
        toolbar = findViewById(R.id.category_toolbar)
        setSupportActionBar(toolbar)
        categoryName = intent.getStringExtra("categoryName")
        categoryId = intent.getStringExtra("categoryId")
        supportActionBar?.title = categoryName ?: "Category"
        val recyclerView: RecyclerView = findViewById(R.id.category_wallpaper_recyclerView)
        progressBar=findViewById(R.id.category_wallpaper_progressbar)
        adapter = WallpaperAdapter(this, wallpaperList)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(View.SCROLL_INDICATOR_BOTTOM)) {
                    if (wallpaperList.size > 11)
                        loadMoreWallpapers()
                }
            }
        })
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        loadFirstWallpapers()


    }

    private fun loadMoreWallpapers() {
        progressBar.visibility=View.VISIBLE
        Firebase.firestore.collection("wallpapers")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .whereEqualTo("categoryId", categoryId)
            .startAfter(lastDocument!!)
            .limit(10)
            .get()
            .addOnSuccessListener {
                for (document in it) {
                    val wallpaper = Wallpaper(
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
                    wallpaperList.add(wallpaper)
                    lastDocument=document
                }
                adapter.notifyDataSetChanged()
                progressBar.visibility=View.GONE
            }
    }

    private fun loadFirstWallpapers() {
        var isFirst = true

        Firebase.firestore.collection("wallpapers")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .whereEqualTo("categoryId", categoryId)
            .limit(12)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    for (doc in snapshot.documentChanges) {
                        when (doc.type) {
                            DocumentChange.Type.ADDED -> {
                                val document = doc.document
                                val wallpaper = Wallpaper(
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
                                } else {
                                    wallpaperList.add(0, wallpaper)
                                }
                            }
                            else -> {
                            }
                        }
                    }
                    if (isFirst) {
                        lastDocument = snapshot.documents[snapshot.size() - 1]

                        Log.d("firebase last h:", lastDocument!!.id)
                    }
                    isFirst = false
                    adapter.notifyDataSetChanged()
                }
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }


    override fun onStart() {
        super.onStart()
        val user = Firebase.auth.currentUser
        if (user == null) {
            Firebase.auth.signInAnonymously()
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Firebase.firestore.collection("wallpaper-data")
                            .document("data")
                            .update("clients", FieldValue.increment(1))
                        Firebase.firestore.collection("favoriteArray")
                            .document((Firebase.auth.currentUser?.uid.toString())!!)
                            .set(hashMapOf("favoriteIds" to mutableListOf<String>()))
                        Log.d("Authentication :", "signInAnonymously:success")
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("Authentication :", "signInAnonymously:failure", task.exception)
                    }
                }
        }
    }
}