package com.wallpaper.wallpro.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.wallpaper.wallpro.R
import com.wallpaper.wallpro.activities.FullWallpaperActivity
import com.wallpaper.wallpro.models.Wallpaper

class WallpaperAdapter(val context: Context, private val wallpaperList: MutableList<Wallpaper>) : RecyclerView.Adapter<WallpaperAdapter.WallViewHolder>() {

    inner class WallViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WallViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.wallpaper_item, parent, false)
        return WallViewHolder(view)
    }

    override fun onBindViewHolder(holder: WallViewHolder, position: Int) {
        val imageView = holder.itemView.findViewById<ImageView>(R.id.wall_imageView)
        imageView.clipToOutline = true
        val url=wallpaperList[position].thumbnail
        Glide.with(context)
            .load(url)
            .centerCrop()
            .into(imageView)
        imageView.setOnClickListener {
            val intent = Intent(context, FullWallpaperActivity::class.java)
            intent.putExtra("url",wallpaperList[position].image)
            intent.putExtra("thumbnail",wallpaperList[position].thumbnail)
            intent.putExtra("wallpaperId",wallpaperList[position].id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount()=wallpaperList.size
}