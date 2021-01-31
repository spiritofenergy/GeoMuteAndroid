package com.kodexgroup.geomuteapp.utils.controllers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kodexgroup.geomuteapp.activity.MainActivity
import com.kodexgroup.geomuteapp.R
import com.kodexgroup.geomuteapp.utils.CHANNEL_ID
import com.kodexgroup.geomuteapp.utils.NOTIFICATION_ID


class AudioController(private val context: Context) {
    private val mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?

    fun setMute() : Int {

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_volume_off_black_24)
            .setContentTitle("Звук отключен")
            .setContentText("Вы находитесь в беззвучной зоне...")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setOngoing(true)

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, builder.build())
        }

        val cacheMode: Int = mAudioManager?.ringerMode ?: 2

        mAudioManager!!.ringerMode = AudioManager.RINGER_MODE_SILENT

        return cacheMode
    }

    fun setUnmute(cacheMode: Int) {
        with(NotificationManagerCompat.from(context)) {
            cancel(NOTIFICATION_ID)
        }

        val audioSharedPref = context.getSharedPreferences(
            "audio_cache", Context.MODE_PRIVATE)

        audioSharedPref.edit()?.apply {
            putInt("cache_mode", 0)
            apply()
        }

        mAudioManager!!.ringerMode = cacheMode
    }

}