package com.kodexgroup.geomuteapp.screens.areaslist.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kodexgroup.geomuteapp.R
import com.kodexgroup.geomuteapp.database.entities.Areas
import com.kodexgroup.geomuteapp.screens.areaslist.holders.AreaHolder

class AreaListAdapter : RecyclerView.Adapter<AreaHolder>() {

    private var items: List<Areas> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AreaHolder {
        return AreaHolder(
                LayoutInflater.from(parent.context)
                        .inflate(R.layout.list_item_area, parent, false)
        )
    }

    override fun onBindViewHolder(holder: AreaHolder, position: Int) {
        holder.onBind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun refresh(items: List<Areas>) {
        this.items = items
        notifyDataSetChanged()
    }
}