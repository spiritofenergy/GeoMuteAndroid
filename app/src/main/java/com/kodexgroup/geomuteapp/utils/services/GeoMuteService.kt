package com.kodexgroup.geomuteapp.utils.services

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.kodexgroup.geomuteapp.R
import com.kodexgroup.geomuteapp.database.AppDatabase
import com.kodexgroup.geomuteapp.database.dao.AreasDAO
import com.kodexgroup.geomuteapp.database.entities.Areas
import com.kodexgroup.geomuteapp.utils.CHANNEL_ID
import com.kodexgroup.geomuteapp.utils.controllers.AudioController
import java.util.*


class GeoMuteService : Service() {

    private lateinit var mHandler: Handler

    private lateinit var db: AppDatabase
    private lateinit var areasDao: AreasDAO

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var audioController: AudioController

    private lateinit var areasLiveData: LiveData<List<Areas>>
    private var areas: List<Areas>? = null

    private var audioSharedPref: SharedPreferences? = null
    private var isIn = false

    private lateinit var timer: Timer

    override fun onCreate() {
        super.onCreate()

        Log.d("MyService", "Creating...")

        mHandler = Handler(Looper.getMainLooper())

        db = Room.databaseBuilder(this, AppDatabase::class.java, "db_points")
                .build()
        areasDao = db.areasDao()

        audioSharedPref = getSharedPreferences(
                "audio_cache", Context.MODE_PRIVATE)

        timer = Timer()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        audioController = AudioController(this)

        areasLiveData = areasDao.getAll()

        areasLiveData.observeForever { areas ->
            this.areas = areas
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            startMyOwnForeground()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        timer.schedule(object : TimerTask() {
            override fun run() {
                getLocation {
                    if (it != null) {
                        if (checkIn(it)) {
                            if (!isIn) {
                                val mode = audioController.setMute()
                                isIn = true
                                if (audioSharedPref?.getInt("cache_mode", 0) == 0) {
                                    audioSharedPref?.edit()?.apply {
                                        putInt("cache_mode", mode)
                                        apply()
                                    }
                                }
                            }
                        } else {
                            if (isIn) {
                                isIn = false
                                audioController.setUnmute(audioSharedPref?.getInt("cache_mode", 0)
                                        ?: 0)
                            }
                        }
                    } else {
                        if (isIn) {
                            isIn = false
                            audioController.setUnmute(audioSharedPref?.getInt("cache_mode", 0)
                                    ?: 0)
                        }
                    }
                }
            }
        }, 0, 3000)



        return START_STICKY_COMPATIBILITY
    }

    override fun onDestroy() {
        if (isIn) {
            audioController.setUnmute(audioSharedPref?.getInt("cache_mode", 0)
                    ?: 0)
        }
        timer.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder = Binder()

    @Throws(SecurityException::class)
    private fun getLocation(task: (location: Location?) -> Unit) {
        fusedLocationClient.lastLocation
                .addOnSuccessListener {
                    task(it)
                }
    }

    private fun checkIn(location: Location) : Boolean {

        for (area in areas.orEmpty()) {
            val lat = area.latLng
            val dist = FloatArray(1)
            Location.distanceBetween(lat.latitude, lat.longitude,
                    location.latitude, location.longitude, dist)
            if (dist[0] <= area.radius) {
                return true
            }
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startMyOwnForeground() {
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
        val notification: Notification = notificationBuilder
                .setOngoing(true)
                .setContentTitle("GeoMute работает")
                .setContentText("Отслеживание беззвучной зоны...")
                .setSmallIcon(R.drawable.ic_baseline_volume_off_black_24)
                .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build()
        startForeground(2, notification)
    }
}