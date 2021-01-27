package com.kodexgroup.geomuteapp.utils.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kodexgroup.geomuteapp.utils.viewmodels.MainViewModel
import com.kodexgroup.geomuteapp.R
import com.kodexgroup.geomuteapp.database.dao.AreasDAO
import com.kodexgroup.geomuteapp.database.entities.Areas
import com.kodexgroup.geomuteapp.utils.holders.AreaHolder

class AreaListAdapter(private val context: Context, private val mainViewModel: MainViewModel, private val areasDAO: AreasDAO) : RecyclerView.Adapter<AreaHolder>() {

    private var items: List<Areas> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AreaHolder {
        return AreaHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_area, parent, false), context, mainViewModel, areasDAO)
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