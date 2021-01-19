package com.kodexgroup.geomuteapp.screens.map.viewmodel

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.Marker
import com.kodexgroup.geomuteapp.database.AppDatabase
import com.kodexgroup.geomuteapp.database.dao.AreasDAO
import com.kodexgroup.geomuteapp.database.entities.Areas
import com.kodexgroup.geomuteapp.utils.App

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private var db: AppDatabase = getApplication<App>().getDatabase()
    private var areasDao: AreasDAO = db.areasDao()

    var lastChoosePosition: MutableLiveData<Marker> = MutableLiveData()
    var lastChooseRadius: MutableLiveData<Circle> = MutableLiveData()
    var newTitleMarker: MutableLiveData<String> = MutableLiveData()

    private val _addCheckLiveData: MutableLiveData<Boolean> = MutableLiveData()

    fun tryAddAreas(area: Areas) {
        Thread {
            try {
                areasDao.insert(area)
                _addCheckLiveData.postValue(true)
            } catch (e: SQLiteConstraintException) {
                Log.d("add", "false")
                _addCheckLiveData.postValue(false)
            }
        }.start()
    }

    fun getCheckLiveData() : LiveData<Boolean> {
        return _addCheckLiveData
    }
}