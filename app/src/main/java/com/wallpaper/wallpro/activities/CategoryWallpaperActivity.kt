package com.wallpaper.wallpro.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.wallpaper.wallpro.R
import com.wallpaper.wallpro.adapters.WallpaperAdapter
import com.wallpaper.wallpro.models.Wallpaper

class CategoryWallpaperActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val wallpaperList = mutableListOf<Wallpaper>()
        setContentView(R.layout.activity_category_wallpaper)
        toolbar=findViewById(R.id.category_toolbar)
        setSupportActionBar(toolbar)
        val categoryName=intent.getStringExtra("categoryName")
        supportActionBar?.title=categoryName?:"Category"
        val recyclerView: RecyclerView =findViewById(R.id.category_wallpaper_recyclerView)
        val adapter= WallpaperAdapter(this, wallpaperList)
        recyclerView.adapter=adapter
        recyclerView.layoutManager= GridLayoutManager(this,2)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
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
                        Log.d("Authentication :", "signInAnonymously:success")
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("Authentication :", "signInAnonymously:failure", task.exception)
                    }
                }
        }
    }
}