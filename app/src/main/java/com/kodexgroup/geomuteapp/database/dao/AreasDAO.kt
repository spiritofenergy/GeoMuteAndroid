package com.kodexgroup.geomuteapp.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.kodexgroup.geomuteapp.database.entities.Areas

@Dao
interface AreasDAO {
    @Query("SELECT * FROM areas")
    fun getAll() : LiveData<List<Areas>>

    @Query("SELECT * FROM areas WHERE areas_title = :title")
    fun getByTitle(title: String) : LiveData<Areas>

    @Insert
    fun insert(area: Areas)

    @Update
    fun update(area: Areas)

    @Delete
    fun delete(area: Areas)
}