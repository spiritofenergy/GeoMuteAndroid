package com.kodexgroup.geomuteapp.utils.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.kodexgroup.geomuteapp.R
import com.kodexgroup.geomuteapp.activity.MainActivity
import com.kodexgroup.geomuteapp.database.AppDatabase
import com.kodexgroup.geomuteapp.database.dao.AreasDAO
import com.kodexgroup.geomuteapp.database.entities.Areas
import com.kodexgroup.geomuteapp.utils.CHANNEL_ID
import com.kodexgroup.geomuteapp.utils.controllers.AudioController
import com.kodexgroup.geomuteapp.utils.recievers.ServiceStopReceiver
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

    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            if (locationResult != null) {
                for (location in locationResult.locations) {
                    Log.d("MyService", location.toString())
                    if (checkIn(location)) {
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
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        Log.d("MyService", "Creating...")

        mHandler = Handler(Looper.getMainLooper())

        db = Room.databaseBuilder(this, AppDatabase::class.java, "db_points")
                .build()
        areasDao = db.areasDao()

        audioSharedPref = getSharedPreferences(
                "audio_cache", Context.MODE_PRIVATE)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        audioController = AudioController(this)

        areasLiveData = areasDao.getAll()

        areasLiveData.observeForever { areas ->
            this.areas = areas
        }


        startMyOwnForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        getLocation()

        return START_STICKY_COMPATIBILITY
    }

    override fun onDestroy() {
        Log.d("MyService", "Destroy....")
        if (isIn) {
            audioController.setUnmute(audioSharedPref?.getInt("cache_mode", 0)
                    ?: 0)
        }
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder = Binder()

    @Throws(SecurityException::class)
    private fun getLocation() {

        fusedLocationClient.requestLocationUpdates(
            LocationRequest().apply {
                priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
                interval = 3000
                fastestInterval = 1000
                maxWaitTime = 5000
            },
            locationCallback,
            Looper.getMainLooper()
        )
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

    private fun startMyOwnForeground() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        val stopIntent = Intent(this, ServiceStopReceiver::class.java)
        stopIntent.apply {
            action = "Delete"
            putExtra("notificationId", 2)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(this,0, stopIntent,0)

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setOngoing(true)
                .setContentTitle("GeoMute работает")
                .setContentText("Отслеживание беззвучной зоны...")
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_baseline_close_24, "Выключить", stopPendingIntent)
                .setSmallIcon(R.drawable.ic_baseline_volume_off_black_24)

        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationBuilder
                    .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .build()
        } else {
            notificationBuilder
                    .build()
        }


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            startForeground(2, notification)
        } else {
            startForeground(2, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        }
    }
}