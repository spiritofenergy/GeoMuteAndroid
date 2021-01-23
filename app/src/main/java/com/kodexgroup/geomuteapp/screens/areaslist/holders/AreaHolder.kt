package com.kodexgroup.geomuteapp.screens.areaslist.holders

import android.app.Application
import android.content.Context
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.kodexgroup.geomuteapp.MainViewModel
import com.kodexgroup.geomuteapp.R
import com.kodexgroup.geomuteapp.database.dao.AreasDAO
import com.kodexgroup.geomuteapp.database.entities.Areas
import com.kodexgroup.geomuteapp.screens.areaslist.AreasListFragment
import com.kodexgroup.geomuteapp.utils.format
import java.sql.Date
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class AreaHolder(itemView: View, private val context: Context, private val mainViewModel: MainViewModel, private val areasDAO: AreasDAO) : RecyclerView.ViewHolder(itemView) {

    val view: View = itemView.findViewById(R.id.card_area)
    private val title: TextView = itemView.findViewById(R.id.title_point_list)
    private val coords: TextView = itemView.findViewById(R.id.coords_point_list)
    private val radius: TextView = itemView.findViewById(R.id.radius_list)
    private val dateAdded: TextView = itemView.findViewById(R.id.date_added_list)
    private val editBtn: ImageButton = itemView.findViewById(R.id.edit_area_btn_list)
    private val deleteBtn: ImageButton = itemView.findViewById(R.id.delete_area_btn_list)

    fun onBind(area: Areas) {

        view.setOnClickListener {
            mainViewModel.setOpenMarker(area.title)
            Navigation.findNavController(it).navigate(R.id.next_fragment)
        }

        editBtn.setOnClickListener {
            mainViewModel.setOpenMarker(area.title)
            mainViewModel.setEditOpenMarker(true)
            Navigation.findNavController(it).navigate(R.id.next_fragment)
        }

        deleteBtn.setOnClickListener {

            val anim = AnimationUtils.loadAnimation(context, R.anim.fade_out)
            anim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}

                override fun onAnimationEnd(animation: Animation?) {
                    Thread {
                        areasDAO.delete(area)
                    }.start()
                }

                override fun onAnimationRepeat(animation: Animation?) {}
            })

            itemView.startAnimation(anim)
        }

        title.text = area.title
        coords.text = "${area.latLng.latitude.format(6)}, ${area.latLng.longitude.format(6)}"
        radius.text = area.radius.toString()

        val date = Date(area.date)
        val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        dateAdded.text = format.format(date)
    }

}