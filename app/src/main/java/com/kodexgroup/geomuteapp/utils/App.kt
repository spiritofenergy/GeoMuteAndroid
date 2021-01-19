package com.kodexgroup.geomuteapp.utils

import android.app.Application
import android.util.Log
import androidx.room.Room
import com.kodexgroup.geomuteapp.database.AppDatabase

class App : Application() {
    private lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        Log.d("APP CREATE", "CREATE")
        database = Room.databaseBuilder(this, AppDatabase::class.java, "db_points")
                .fallbackToDestructiveMigration()
                .build()
    }


    fun getDatabase() : AppDatabase {
        return database
    }
}