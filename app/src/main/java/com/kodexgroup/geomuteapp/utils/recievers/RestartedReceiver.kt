package com.kodexgroup.geomuteapp.utils.recievers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.kodexgroup.geomuteapp.utils.services.GeoMuteService


class RestartedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(Intent(context, GeoMuteService::class.java))
        } else {
            context.startService(Intent(context, GeoMuteService::class.java))
        }
    }
}