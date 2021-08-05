package com.aadevelopers.bestwalls.models

import com.google.firebase.Timestamp

data class Wallpaper(
    val id:String,
    val image: String,
    val thumbnail: String,
    val isPopular: Boolean,
    val isPremium: Boolean?,
    val source: String,
    val userId: String,
    val categoryId: String,
    val timestamp: Timestamp
) {

}