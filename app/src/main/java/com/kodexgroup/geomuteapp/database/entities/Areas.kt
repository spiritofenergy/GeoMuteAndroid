package com.kodexgroup.geomuteapp.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.android.gms.maps.model.LatLng
import com.kodexgroup.geomuteapp.database.converters.AreasConverters

@Entity(tableName = "areas")
@TypeConverters(AreasConverters::class)
class Areas(
    @PrimaryKey
    @ColumnInfo(name = "areas_title")
    var title: String,

    @ColumnInfo(name = "lat_lng")
    var latLng: LatLng,

    var radius: Double,

    @ColumnInfo(name = "date_added")
    var date: Long)