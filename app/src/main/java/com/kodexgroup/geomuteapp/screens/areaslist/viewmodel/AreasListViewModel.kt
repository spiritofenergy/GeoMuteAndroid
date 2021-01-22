package com.kodexgroup.geomuteapp.screens.areaslist.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.kodexgroup.geomuteapp.database.AppDatabase
import com.kodexgroup.geomuteapp.database.dao.AreasDAO
import com.kodexgroup.geomuteapp.database.entities.Areas
import com.kodexgroup.geomuteapp.utils.App

class AreasListViewModel(application: Application) : AndroidViewModel(application) {
    private var db: AppDatabase = getApplication<App>().getDatabase()
    private var areasDao: AreasDAO = db.areasDao()

    fun getAreas() : LiveData<List<Areas>> {
        return areasDao.getAllDescByDate()
    }

}