package com.kodexgroup.geomuteapp

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import android.location.Location
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

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private var db: AppDatabase = getApplication<App>().getDatabase()
    private var areasDao: AreasDAO = db.areasDao()

    var lastChoosePosition: MutableLiveData<Marker> = MutableLiveData()
    var lastChooseRadius: MutableLiveData<Circle> = MutableLiveData()
    var newTitleMarker: MutableLiveData<String> = MutableLiveData()
    var checkLocation: MutableLiveData<Boolean?> = MutableLiveData()

    private val _editOpenMarker: MutableLiveData<Boolean> = MutableLiveData()
    private val _openMarker: MutableLiveData<String?> = MutableLiveData()
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

    fun getAreas() : LiveData<List<Areas>> {
        return areasDao.getAllDescByDate()
    }

    fun setOpenMarker(title: String) {
        _openMarker.value = title
    }

    fun clearOpenMarker() {
        _openMarker.value = null
    }

    fun getOpenMarker() : String? {
        return _openMarker.value
    }

    fun setEditOpenMarker(isEdit: Boolean) {
        _editOpenMarker.value = isEdit
    }

    fun clearEditOpenMarker() {
        _editOpenMarker.value = false
    }

    fun getEditOpenMarker() : Boolean {
        return _editOpenMarker.value ?: false
    }
}