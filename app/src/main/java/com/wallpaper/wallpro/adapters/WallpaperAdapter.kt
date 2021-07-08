package com.wallpaper.wallpro.adapters

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.wallpaper.wallpro.R
import com.wallpaper.wallpro.activities.FullWallpaperActivity
import com.wallpaper.wallpro.models.Wallpaper
import java.util.*

class WallpaperAdapter(val context: Context, private val wallpaperList: MutableList<Wallpaper>) :
    RecyclerView.Adapter<WallpaperAdapter.WallViewHolder>() {

    private var favoriteArray: MutableList<String> = mutableListOf()

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
            val intent = Intent(context, FullWallpaperActivity::class.java)
            intent.putExtra("url", wallpaperList[position].image)
            intent.putExtra("thumbnail", wallpaperList[position].thumbnail)
            intent.putExtra("wallpaperId", wallpaperList[position].id)
            context.startActivity(intent)
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