package com.kodexgroup.geomuteapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

fun Double.format(digit: Int) = "%.${digit}f".format(this)

fun getBitmapFromVector(context: Context, vectorDrawable: Int): BitmapDescriptor? {
    val vector = ContextCompat.getDrawable(context, vectorDrawable)
    val bitmap: Bitmap
    val canvas: Canvas

    if (vector != null) {
        vector.bounds = Rect(0, 0, vector.intrinsicWidth, vector.intrinsicHeight)
        bitmap = Bitmap.createBitmap(
                vector.intrinsicWidth,
                vector.intrinsicHeight,
                Bitmap.Config.ARGB_8888
        )

        canvas = Canvas(bitmap)
        vector.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    return null
}
