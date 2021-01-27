package com.kodexgroup.geomuteapp.utils.interfaces

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker

interface SetMarkerListener {
    fun onSetMarker(latLng: LatLng)
    fun onSetExistMarker(marker: Marker)
}