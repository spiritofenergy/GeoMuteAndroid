package com.kodexgroup.geomuteapp.database.converters

import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng

class AreasConverters {
    @TypeConverter
    fun fromLatLng(latLng: LatLng): String {
        return "${latLng.latitude},${latLng.longitude}"
    }

    @TypeConverter
    fun toLatLng(data: String): LatLng {
        val list = data.split(",")
        return LatLng(list[0].toDouble(), list[1].toDouble())
    }
}