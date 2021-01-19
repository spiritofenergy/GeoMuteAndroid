package com.kodexgroup.geomuteapp.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kodexgroup.geomuteapp.database.dao.AreasDAO
import com.kodexgroup.geomuteapp.database.entities.Areas

@Database(entities = [Areas::class], version = 5)
abstract class AppDatabase : RoomDatabase() {
    abstract fun areasDao() : AreasDAO
}