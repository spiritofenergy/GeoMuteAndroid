package com.kodexgroup.geomuteapp.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.kodexgroup.geomuteapp.R

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

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = context.getString(R.string.channel_name)
        val descriptionText = context.getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager? =
                getSystemService(context, NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)
    }
}