package com.kodexgroup.geomuteapp.screens.areaslist

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kodexgroup.geomuteapp.MainViewModel
import com.kodexgroup.geomuteapp.R
import com.kodexgroup.geomuteapp.database.AppDatabase
import com.kodexgroup.geomuteapp.database.dao.AreasDAO
import com.kodexgroup.geomuteapp.database.entities.Areas
import com.kodexgroup.geomuteapp.screens.areaslist.adapters.AreaListAdapter
import com.kodexgroup.geomuteapp.screens.areaslist.viewmodel.AreasListViewModel
import com.kodexgroup.geomuteapp.utils.App


class AreasListFragment : Fragment() {

    private lateinit var db: AppDatabase
    private lateinit var areasDao: AreasDAO

    private lateinit var listItem: RecyclerView

    private val mainViewModel: MainViewModel by activityViewModels()

    private lateinit var listLiveData: LiveData<List<Areas>>

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_list, container, false)

        db = (requireContext().applicationContext as App).getDatabase()
        areasDao = db.areasDao()

        listItem = root.findViewById(R.id.list_view)

        val adapter = AreaListAdapter(mainViewModel, areasDao)
        val layoutManager = LinearLayoutManager(requireContext())
        val dividerItemDecoration = DividerItemDecoration(listItem.context,
                layoutManager.orientation)

        listItem.adapter = adapter
        listItem.layoutManager = layoutManager
        listItem.addItemDecoration(dividerItemDecoration)

        listLiveData = mainViewModel.getAreas()
        listLiveData.observe(viewLifecycleOwner) {
            adapter.refresh(it)
        }

        return root
    }
}