package com.kodexgroup.geomuteapp.utils.recievers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.kodexgroup.geomuteapp.utils.services.GeoMuteService

class ServiceStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.apply {
            val notificationId = getIntExtra("notificationId",0)

            context?.apply {
                NotificationManagerCompat.from(this).cancel(notificationId)

                val service = Intent(this, GeoMuteService::class.java)
                stopService(service)
            }
        }
    }
}