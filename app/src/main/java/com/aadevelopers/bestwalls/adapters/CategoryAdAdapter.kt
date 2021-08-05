package com.aadevelopers.bestwalls.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.aadevelopers.bestwalls.R
import com.aadevelopers.bestwalls.activities.FullWallpaperActivity
import com.aadevelopers.bestwalls.models.Wallpaper
import com.bumptech.glide.Glide
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*


class CategoryAdAdapter(val context: Context, private val wallpaperList: MutableList<Any>) :
    RecyclerView.Adapter<CategoryAdAdapter.WallViewHolder>() {
    private val ITEM_TYPE_WALLPAPER = 0
    private val ITEM_TYPE_AD = 1
    private var favoriteArray: MutableList<String> = mutableListOf()
    lateinit var mAdView : AdView
    private val TAG="AdMob"
    private var mRewardedAd: RewardedAd? = null

//    private lateinit var adView: AdView



    inner class WallViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WallViewHolder{
        when (viewType) {
            ITEM_TYPE_WALLPAPER -> {
                val view =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.wallpaper_item, parent, false)
                return WallViewHolder(view)
            }
            else -> {
                val view =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.banner_ad_layout, parent, false)
                return WallViewHolder(view)
            }


        }

    }



    override fun onBindViewHolder(holder: CategoryAdAdapter.WallViewHolder, position: Int) {
        var viewType = getItemViewType(position)
        when (viewType) {
            ITEM_TYPE_WALLPAPER -> {
                if (wallpaperList[position] is Wallpaper) {
                    val wallpaper: Wallpaper = wallpaperList[position] as Wallpaper
                    val imageView = holder.itemView.findViewById<ImageView>(R.id.wall_imageView)
                    val isPremiumImageView =
                        holder.itemView.findViewById<ImageView>(R.id.isPremium_imageView)
                    val favoriteButton =
                        holder.itemView.findViewById<ImageButton>(R.id.favorite_button)
                    imageView.clipToOutline = true
                    val url = wallpaper.thumbnail
                    Glide.with(context)
                        .load(url)
                        .centerCrop()
                        .into(imageView)
                    imageView.setOnClickListener {
                        if (wallpaper.isPremium == true) {
                            holder.itemView.findViewById<RelativeLayout>(R.id.ad_progressbar).visibility=View.VISIBLE
                            var adRequest = AdRequest.Builder().build()

                            RewardedAd.load(context, context.getString(R.string.Rewarded_Ad_Id), adRequest, object : RewardedAdLoadCallback() {
                                override fun onAdFailedToLoad(adError: LoadAdError) {
                                    Log.d(TAG, adError?.message)
                                    holder.itemView.findViewById<RelativeLayout>(R.id.ad_progressbar).visibility=View.GONE
                                    Toast.makeText(context,"Ad not loaded",Toast.LENGTH_SHORT).show()
                                    mRewardedAd = null
                                }

                                override fun onAdLoaded(rewardedAd: RewardedAd) {
                                    Log.d(TAG, "Ad was loaded.")
                                    mRewardedAd = rewardedAd
                                    if (mRewardedAd != null) {
                                        mRewardedAd?.show(context as Activity, OnUserEarnedRewardListener() {
                                            Log.d(TAG, "User earned the reward.")
                                            val intent = Intent(context, FullWallpaperActivity::class.java)
                                            intent.putExtra("url", wallpaper.image)
                                            intent.putExtra("thumbnail", wallpaper.thumbnail)
                                            intent.putExtra("wallpaperId", wallpaper.id)
                                            intent.putExtra("source", wallpaper.source)
                                            intent.putExtra("timestamp", wallpaper.timestamp.toDate().toString())
                                            context.startActivity(intent)
                                            holder.itemView.findViewById<RelativeLayout>(R.id.ad_progressbar).visibility=View.GONE
                                        })

                                    } else {
                                        Log.d(TAG, "The rewarded ad wasn't ready yet.")
                                    }
                                }


                            })

                            mRewardedAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                                override fun onAdShowedFullScreenContent() {
                                    // Called when ad is shown.
                                    Log.d(TAG, "Ad was shown.")
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                                    // Called when ad fails to show.
                                    Log.d(TAG, "Ad failed to show.")
                                    holder.itemView.findViewById<RelativeLayout>(R.id.ad_progressbar).visibility=View.GONE
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    // Called when ad is dismissed.
                                    // Set the ad reference to null so you don't show the ad a second time.
                                    Log.d(TAG, "Ad was dismissed.")
                                    holder.itemView.findViewById<RelativeLayout>(R.id.ad_progressbar).visibility=View.GONE
                                    //Toast.makeText(context,"As closed",Toast.LENGTH_SHORT).show()
                                    //mRewardedAd = null
                                }
                            }


                        }else {
                            val intent = Intent(context, FullWallpaperActivity::class.java)
                            intent.putExtra("url", wallpaper.image)
                            intent.putExtra("thumbnail", wallpaper.thumbnail)
                            intent.putExtra("wallpaperId", wallpaper.id)
                            intent.putExtra("source", wallpaper.source)
                            intent.putExtra(
                                "timestamp",
                                wallpaper.timestamp.toDate().toString()
                            )
                            context.startActivity(intent)
                        }
                    }
                    if (wallpaper.isPremium != null && wallpaper.isPremium == true) {
                        isPremiumImageView.visibility = View.VISIBLE
                    } else {
                        isPremiumImageView.visibility = View.GONE
                    }
                    var isFavorite = false
                    val index = favoriteArray.indexOf(wallpaper.id)
                    if (index > -1) {
                        favoriteButton.setImageResource(R.drawable.ic_heart_red)
                        isFavorite = true
                    } else {
                        isFavorite = false
                        favoriteButton.setImageResource(R.drawable.ic_heart_outline)
                    }
                    Log.d("firebase :", "checking array $index")
                    favoriteButton.setOnClickListener {
                        if (isFavorite) {
                            favoriteButton.setImageResource(R.drawable.ic_heart_outline)
                            var wallId = ""
                            Firebase.firestore.collection("favorites")
                                .whereEqualTo("userId", Firebase.auth.currentUser!!.uid)
                                .whereEqualTo("wallpaperId", wallpaper.id)
                                .get()
                                .addOnSuccessListener {
                                    for (dc in it) {
                                        wallId = dc.id
                                    }
                                    Firebase.firestore.collection("favorites")
                                        .document(wallId)
                                        .delete()
                                    Firebase.firestore.collection("favoriteArray")
                                        .document(Firebase.auth.currentUser!!.uid)
                                        .update(
                                            "favoriteIds",
                                            FieldValue.arrayRemove(wallpaper.id)
                                        )
                                }

                                .addOnFailureListener { _ ->
                                    favoriteButton.setImageResource(R.drawable.ic_heart_red)
                                    Toast.makeText(
                                        context,
                                        "Not removed from favorite",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        } else {
                            favoriteButton.setImageResource(R.drawable.ic_heart_red)
                            val wallpaperData = hashMapOf(
                                "image" to wallpaper.image,
                                "thumbnail" to wallpaper.thumbnail,
                                "userId" to (Firebase.auth.currentUser?.uid ?: ""),
                                "wallpaperId" to wallpaper.id,
                                "timestamp" to Timestamp(Date())
                            )
                            Firebase.firestore.collection("favorites")
                                .add(
                                    wallpaperData
                                ).addOnSuccessListener { documentReference ->
                                    Firebase.firestore.collection("favoriteArray")
                                        .document(Firebase.auth.currentUser!!.uid)
                                        .update(
                                            "favoriteIds",
                                            FieldValue.arrayUnion(wallpaper.id)
                                        )
                                    Log.d(
                                        "firebase :",
                                        "DocumentSnapshot written with ID: ${documentReference.id}"
                                    )
                                }
                                .addOnFailureListener { _ ->
                                    favoriteButton.setImageResource(R.drawable.ic_heart_outline)
                                    Toast.makeText(
                                        context,
                                        "Not added to favorite",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                        }
                    }
                }

            }
            else -> {
                mAdView = holder.itemView.findViewById(R.id.adView)
                val adRequest = AdRequest.Builder().build()
                mAdView.loadAd(adRequest)
//                adView =
//                    AdView(context, "IMG_16_9_APP_INSTALL#YOUR_PLACEMENT_ID", AdSize.RECTANGLE_HEIGHT_250)
//                var adContainer:CardView = holder.itemView.findViewById(R.id.banner_container)
//                adContainer.addView(adView)
//                adView.loadAd()

            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        if (Firebase.auth.currentUser != null) {
            Firebase.firestore.collection("favoriteArray")
                .document(Firebase.auth.currentUser!!.uid.toString())
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Log.d("firebase :", "getting array")
                        favoriteArray = snapshot["favoriteIds"] as MutableList<String>
                        notifyDataSetChanged()

                    } else {
                        Log.d("firebase :", "Current data: null")
                    }
                }
        } else {
            Log.e("Firebase auth adapter :", "no user")
        }
    }

    override fun getItemCount() = wallpaperList.size

    override fun getItemViewType(position: Int): Int {
        if (wallpaperList[position] is Wallpaper) {
            return ITEM_TYPE_WALLPAPER
        } else {
            return ITEM_TYPE_AD
        }
        return super.getItemViewType(position)
    }


}