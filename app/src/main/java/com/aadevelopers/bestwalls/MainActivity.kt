package com.aadevelopers.bestwalls

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.viewpager2.widget.ViewPager2
import com.aadevelopers.bestwalls.adapters.ViewPagerAdapter
import com.google.android.gms.ads.MobileAds
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.testing.FakeReviewManager
import com.google.android.play.core.tasks.Task
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator
import me.ibrahimsn.lib.SmoothBottomBar


class MainActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: ViewPagerAdapter
    private lateinit var bottomNavigation: SmoothBottomBar
    private lateinit var toolbar: Toolbar
    private lateinit var reviewManager: FakeReviewManager
    private var reviewInfo: ReviewInfo? = null
    private lateinit var indicator: WormDotsIndicator
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewPager = findViewById(R.id.viewPager)
        bottomNavigation = findViewById(R.id.bottomBar)
        toolbar = findViewById(R.id.toolbar)
        indicator = findViewById(R.id.indicator)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Home"
        MobileAds.initialize(this)

        reviewManager = FakeReviewManager(this)
        val request = reviewManager.requestReviewFlow()
        request.addOnCompleteListener { request ->
            reviewInfo = if (request.isSuccessful) {
                //Received ReviewInfo object
                    Log.d("rating",request.result.toString())
                    Log.d("rating","success")
                request.result
            } else {
                //Problem in receiving object
                    Log.d("rating","failed")
                null
            }
        }

        reviewInfo?.let {
            val flow = reviewManager.launchReviewFlow(this@MainActivity, it)
            flow.addOnCompleteListener {
                //Irrespective of the result, the app flow should continue
            }
        }


//        findViewById<View>(R.id.btn_rate_app).setOnClickListener { view: View? -> showRateApp() }
        adapter = ViewPagerAdapter(supportFragmentManager, lifecycle)
        viewPager.adapter = adapter
        indicator.setViewPager2(viewPager)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0, 1, 2 ,3-> setBottomNavigation(0, position)
                    4 -> setBottomNavigation(1, position)
                    5 -> setBottomNavigation(2, position)
                    else -> setBottomNavigation(0, 0)
                }
            }
        })



        bottomNavigation.onItemSelected = {
            when (it) {
                0 -> viewPager.setCurrentItem(0, true)
                1 -> viewPager.setCurrentItem(4, true)
                2 -> viewPager.setCurrentItem(5, true)
                else -> viewPager.setCurrentItem(0, true)
            }
        }

    }

    private fun setBottomNavigation(index: Int, position: Int) {
        bottomNavigation.itemActiveIndex = index
        val title: String = when (position) {
            0 -> "Latest"
            1 -> "Random"
            2 -> "Top Choice"
            3 -> "Premium"
            4 -> "Categories"
            5 -> "Favorite"
            else -> "Random"
        }
        supportActionBar?.title = title
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