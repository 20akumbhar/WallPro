package com.wallpaper.wallpro.utils

import android.content.Context
import android.util.AttributeSet

public final class AspectImageView : androidx.appcompat.widget.AppCompatImageView{

    constructor(context: Context) : super(context) {
    }
    constructor(context: Context,attrs:AttributeSet):super(context,attrs) {
    }

    constructor(context: Context,attrs:AttributeSet,defStyle: Int):super(context,attrs,defStyle) {
    }

//    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
//
//    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = width * drawable.intrinsicHeight / drawable.intrinsicWidth
        setMeasuredDimension(width, height)
        super.onMeasure(width,height)
    }
}