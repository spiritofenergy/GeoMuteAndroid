package com.kodexgroup.geomuteapp.utils.interfaces

import com.google.android.gms.maps.model.LatLng

interface ResizeRadiusListener {
    fun onResize(center: LatLng, radius: Int)
}