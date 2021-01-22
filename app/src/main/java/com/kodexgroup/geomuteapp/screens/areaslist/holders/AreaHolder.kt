package com.kodexgroup.geomuteapp.screens.areaslist.holders

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kodexgroup.geomuteapp.R
import com.kodexgroup.geomuteapp.database.entities.Areas
import com.kodexgroup.geomuteapp.utils.format
import java.sql.Date
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class AreaHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val title: TextView = itemView.findViewById(R.id.title_point_list)
    private val coords: TextView = itemView.findViewById(R.id.coords_point_list)
    private val radius: TextView = itemView.findViewById(R.id.radius_list)
    private val dateAdded: TextView = itemView.findViewById(R.id.date_added_list)

    fun onBind(area: Areas) {
        title.text = area.title
        coords.text = "${area.latLng.latitude.format(6)}, ${area.latLng.longitude.format(6)}"
        radius.text = area.radius.toString()

        val date = Date(area.date)
        val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        dateAdded.text = format.format(date)
    }

}