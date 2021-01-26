package com.kodexgroup.geomuteapp.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kodexgroup.geomuteapp.MainActivity
import com.kodexgroup.geomuteapp.R


class AudioManagerMy(private val context: Context) {
    private val mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
    private var isSoundOff = false

    private var cacheMode: Int = 0
    private val notificationId = 24645

    fun createNotification() {
        if (!isSoundOff) {

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

            var builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_baseline_volume_off_black_24)
                    .setContentTitle("Звук отключен")
                    .setContentText("Вы находитесь в беззвучной зоне...")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)

            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, builder.build())
            }

            cacheMode = mAudioManager?.ringerMode ?: 2

            mAudioManager!!.ringerMode = AudioManager.RINGER_MODE_SILENT
            isSoundOff = true
        }
    }

    fun cancelNotification() {
        if (isSoundOff) {

            with(NotificationManagerCompat.from(context)) {
                cancel(notificationId)
            }

            mAudioManager!!.ringerMode = cacheMode
            isSoundOff = false
        }
    }

}