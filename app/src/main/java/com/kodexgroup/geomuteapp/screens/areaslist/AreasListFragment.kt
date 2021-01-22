package com.kodexgroup.geomuteapp.screens.areaslist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kodexgroup.geomuteapp.R
import com.kodexgroup.geomuteapp.database.entities.Areas
import com.kodexgroup.geomuteapp.screens.areaslist.adapters.AreaListAdapter
import com.kodexgroup.geomuteapp.screens.areaslist.viewmodel.AreasListViewModel


class AreasListFragment : Fragment() {

    private lateinit var listItem: RecyclerView

    private lateinit var areasListViewModel: AreasListViewModel

    private lateinit var listLiveData: LiveData<List<Areas>>

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_list, container, false)

        listItem = root.findViewById(R.id.list_view)

        val adapter = AreaListAdapter()
        val layoutManager = LinearLayoutManager(requireContext())
        val dividerItemDecoration = DividerItemDecoration(listItem.context,
                layoutManager.orientation)
        listItem.adapter = adapter
        listItem.layoutManager = layoutManager
        listItem.addItemDecoration(dividerItemDecoration)

        areasListViewModel = ViewModelProvider(this).get(AreasListViewModel::class.java)
        listLiveData = areasListViewModel.getAreas()
        listLiveData.observe(viewLifecycleOwner) {
            adapter.refresh(it)
        }

        return root
    }
}