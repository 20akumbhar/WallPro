package com.wallpaper.wallpro.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Wallpaper(
    val id:String,
    val image: String,
    val thumbnail: String,
    val isPopular: Boolean,
    val source: String,
    val userId: String,
    val categoryId: String,
    val timestamp: Timestamp
)