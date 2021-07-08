package com.wallpaper.wallpro.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.wallpaper.wallpro.R
import com.wallpaper.wallpro.activities.CategoryWallpaperActivity
import com.wallpaper.wallpro.models.Category

class CategoryAdapter(val context:Context, private val categoriesList: MutableList<Category>): RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(itemView:View):RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view:View=LayoutInflater.from(parent.context).inflate(R.layout.cat_item,parent,false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val imageView = holder.itemView.findViewById<ImageView>(R.id.cat_imageView)
        holder.itemView.findViewById<TextView>(R.id.cat_textView).text=categoriesList[position].name
        val url=categoriesList[position].downloadURL
        Glide.with(context)
            .load(url)
            .centerCrop()
            .into(imageView)

        imageView.setOnClickListener {
            var intent: Intent = Intent(context, CategoryWallpaperActivity::class.java)
            intent.putExtra("categoryName",categoriesList[position].name)
            intent.putExtra("categoryId",categoriesList[position].id)
            context.startActivity(intent)
        }
    }
    override fun getItemCount() = categoriesList.size
}