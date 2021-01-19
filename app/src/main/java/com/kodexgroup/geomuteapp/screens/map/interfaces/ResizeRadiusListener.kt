package com.kodexgroup.geomuteapp.screens.map.interfaces

import com.google.android.gms.maps.model.LatLng

interface ResizeRadiusListener {
    fun onResize(center: LatLng, radius: Int)
}