package com.aadevelopers.bestwalls.activities

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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.aadevelopers.bestwalls.R
import com.aadevelopers.bestwalls.adapters.AdTestAdapter
import com.aadevelopers.bestwalls.adapters.CategoryAdAdapter
import com.aadevelopers.bestwalls.adapters.WallpaperAdapter
import com.aadevelopers.bestwalls.models.Wallpaper
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.firebase.firestore.*

class CategoryWallpaperActivity : AppCompatActivity() {
    private var lastDocument: DocumentSnapshot? = null
    private lateinit var wallpaperList: MutableList<Any>
    private lateinit var adapter: CategoryAdAdapter
    private lateinit var toolbar: Toolbar
    private var categoryName: String? = null
    private var categoryId: String? = null
    private lateinit var progressBar: ProgressBar
    private var adIndex = 0
    lateinit var mAdView : AdView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wallpaperList = mutableListOf()
        setContentView(R.layout.activity_category_wallpaper)
        toolbar = findViewById(R.id.category_toolbar)
        setSupportActionBar(toolbar)
        categoryName = intent.getStringExtra("categoryName")
        categoryId = intent.getStringExtra("categoryId")
        supportActionBar?.title = categoryName ?: "Category"
        val recyclerView: RecyclerView = findViewById(R.id.category_wallpaper_recyclerView)
        progressBar = findViewById(R.id.category_wallpaper_progressbar)
        adapter = CategoryAdAdapter(this, wallpaperList)
        recyclerView.adapter = adapter
        MobileAds.initialize(this) {}

        mAdView = findViewById(R.id.category_adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
        val gridLayoutManager: GridLayoutManager = GridLayoutManager(this, 2)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {

            override fun getSpanSize(position: Int): Int {
                if (wallpaperList[position] is String) {
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
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        loadFirstWallpapers()


    }

    private fun loadMoreWallpapers() {
        progressBar.visibility = View.VISIBLE
        Firebase.firestore.collection("wallpapers")
            .orderBy(FieldPath.documentId())
            .whereEqualTo("categoryId", categoryId)
            .startAfter(lastDocument!!)
            .limit(16)
            .get()
            .addOnSuccessListener {
                adIndex = 0
                for (document in it) {
                    adIndex++
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
                    lastDocument = document
                    if (adIndex % 8 == 0 && adIndex!=0) {
                        wallpaperList.add("ads")
                    }
                }
                adapter.notifyDataSetChanged()
                progressBar.visibility = View.GONE
            }
    }

    private fun loadFirstWallpapers() {
        var isFirst = true

        Firebase.firestore.collection("wallpapers")
            .orderBy(FieldPath.documentId())
            .whereEqualTo("categoryId", categoryId)
            .limit(16)
            .addSnapshotListener { snapshot, error ->
                adIndex = 0
                if (snapshot != null) {
                    for (doc in snapshot.documentChanges) {
                        when (doc.type) {
                            DocumentChange.Type.ADDED -> {
                                adIndex++
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
                                    if (adIndex % 8 == 0) {
                                        Log.d("Adding to",""+adIndex)
                                        wallpaperList.add("ads")
                                    }
                                } else {
                                    wallpaperList.add(0, wallpaper)
                                }
                            }
                            else -> {
                            }
                        }
                    }
                    if (isFirst && snapshot.size() > 0) {
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