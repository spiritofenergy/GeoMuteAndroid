package com.kodexgroup.geomuteapp.utils.recievers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.app.NotificationManagerCompat
import com.kodexgroup.geomuteapp.utils.controllers.AudioController

class AudioCancelReceiver : BroadcastReceiver() {
    private lateinit var audioController: AudioController
    private var audioSharedPref: SharedPreferences? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.apply {
            val notificationId = getIntExtra("notificationId",0)

            context?.apply {
                audioController = AudioController(this)
                audioSharedPref = getSharedPreferences(
                    "audio_cache", Context.MODE_PRIVATE)

                NotificationManagerCompat.from(this).cancel(notificationId)

                audioController.setUnmute(audioSharedPref?.getInt("cache_mode", 0)
                    ?: 0)
            }
        }
    }
}