package com.kodexgroup.geomuteapp.screens.map

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.Marker

class MapViewModel : ViewModel() {
    var lastChoosePosition: MutableLiveData<Marker> = MutableLiveData()
    var lastChooseRadius: MutableLiveData<Circle> = MutableLiveData()
    var newTitleMarker: MutableLiveData<String> = MutableLiveData()
    var checkLocation: MutableLiveData<Boolean?> = MutableLiveData()
}