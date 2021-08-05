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
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import io.grpc.internal.JsonUtil
import java.util.*

class WallpaperAdapter(val context: Context, private val wallpaperList: MutableList<Wallpaper>) :
    RecyclerView.Adapter<WallpaperAdapter.WallViewHolder>() {
    private var favoriteArray: MutableList<String> = mutableListOf()
    private var mRewardedAd: RewardedAd? = null
    private val TAG = "MainActivity"
    inner class WallViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WallViewHolder {

        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.wallpaper_item, parent, false)
        return WallViewHolder(view)
    }

    override fun onBindViewHolder(holder: WallViewHolder, position: Int) {
        val imageView = holder.itemView.findViewById<ImageView>(R.id.wall_imageView)
        val isPremiumImageView = holder.itemView.findViewById<ImageView>(R.id.isPremium_imageView)
        val favoriteButton = holder.itemView.findViewById<ImageButton>(R.id.favorite_button)
        imageView.clipToOutline = true
        val url = wallpaperList[position].thumbnail
        Glide.with(context)
            .load(url)
            .centerCrop()
            .into(imageView)
        imageView.setOnClickListener {
            if (wallpaperList[position].isPremium == true) {
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
                                intent.putExtra("url", wallpaperList[position].image)
                                intent.putExtra("thumbnail", wallpaperList[position].thumbnail)
                                intent.putExtra("wallpaperId", wallpaperList[position].id)
                                intent.putExtra("source", wallpaperList[position].source)
                                intent.putExtra("timestamp", wallpaperList[position].timestamp.toDate().toString())
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
                intent.putExtra("url", wallpaperList[position].image)
                intent.putExtra("thumbnail", wallpaperList[position].thumbnail)
                intent.putExtra("wallpaperId", wallpaperList[position].id)
                intent.putExtra("source", wallpaperList[position].source)
                intent.putExtra("timestamp", wallpaperList[position].timestamp.toDate().toString())
                context.startActivity(intent)
            }
        }
        if (wallpaperList[position].isPremium != null && wallpaperList[position].isPremium == true) {
            isPremiumImageView.visibility = View.VISIBLE
        } else {
            isPremiumImageView.visibility = View.GONE
        }
        var isFavorite = false
        val index = favoriteArray.indexOf(wallpaperList[position].id)
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
                    .whereEqualTo("wallpaperId", wallpaperList[position].id)
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
                                FieldValue.arrayRemove(wallpaperList[position].id)
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
                val wallpaper = hashMapOf(
                    "image" to wallpaperList[position].image,
                    "thumbnail" to wallpaperList[position].thumbnail,
                    "userId" to (Firebase.auth.currentUser?.uid ?: ""),
                    "wallpaperId" to wallpaperList[position].id,
                    "timestamp" to Timestamp(Date())
                )
                Firebase.firestore.collection("favorites")
                    .add(
                        wallpaper
                    ).addOnSuccessListener { documentReference ->
                        Firebase.firestore.collection("favoriteArray")
                            .document(Firebase.auth.currentUser!!.uid)
                            .update(
                                "favoriteIds",
                                FieldValue.arrayUnion(wallpaperList[position].id)
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


}