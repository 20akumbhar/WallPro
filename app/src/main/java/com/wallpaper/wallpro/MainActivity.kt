package com.wallpaper.wallpro

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.widget.Toolbar
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.wallpaper.wallpro.adapters.ViewPagerAdapter
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator
import me.ibrahimsn.lib.SmoothBottomBar

class MainActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: ViewPagerAdapter
    private lateinit var bottomNavigation: SmoothBottomBar
    private lateinit var toolbar:Toolbar
    private lateinit var indicator:WormDotsIndicator
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewPager = findViewById(R.id.viewPager)
        bottomNavigation = findViewById(R.id.bottomBar)
        toolbar=findViewById(R.id.toolbar)
        indicator=findViewById(R.id.indicator)
        setSupportActionBar(toolbar)
        supportActionBar?.title="Home"
        adapter = ViewPagerAdapter(supportFragmentManager, lifecycle)
        viewPager.adapter = adapter
indicator.setViewPager2(viewPager)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0,1,2 ->  setBottomNavigation(0,position)
                    3 ->  setBottomNavigation(1,position)
                    4 ->  setBottomNavigation(2,position)
                    else-> setBottomNavigation(0,0)
                }
            }
        })



        bottomNavigation.onItemSelected = {
            when(it){
                0->viewPager.setCurrentItem(0,true)
                1->viewPager.setCurrentItem(3,true)
                2->viewPager.setCurrentItem(4,true)
                else->viewPager.setCurrentItem(0,true)
            }
        }

    }

    private fun setBottomNavigation(index: Int, position: Int) {
        bottomNavigation.itemActiveIndex=index
        val title: String = when(position){
            0-> "Latest"
            1-> "Top Choice"
            2->"Random"
            3-> "Categories"
            4-> "Favorite"
            else-> "Random"
        }
        supportActionBar?.title=title
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
                                .update("clients",FieldValue.increment(1))
                        Firebase.firestore.collection("favoriteArray")
                            .document((Firebase.auth.currentUser?.uid.toString()))
                            .set(hashMapOf("favoriteIds" to mutableListOf<String>()))
                        Log.d("Authentication :", "signInAnonymously:success")
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("Authentication :", "signInAnonymously:failure", task.exception)
                        finish()
                    }
                }
        }
    }
}