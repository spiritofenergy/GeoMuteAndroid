package com.kodexgroup.geomuteapp.screens.areaslist

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
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
import com.kodexgroup.geomuteapp.utils.App
import com.kodexgroup.geomuteapp.utils.DrawerLayoutStatus


class AreasListFragment : Fragment() {

    private lateinit var db: AppDatabase
    private lateinit var areasDao: AreasDAO

    private lateinit var listItem: RecyclerView
    private lateinit var progressBar: ProgressBar

    private val mainViewModel: MainViewModel by activityViewModels()

    private lateinit var listLiveData: LiveData<List<Areas>>

    private var isOpen = false

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_list, container, false)

        db = (requireContext().applicationContext as App).getDatabase()
        areasDao = db.areasDao()

        listItem = root.findViewById(R.id.list_view)
        progressBar = root.findViewById(R.id.progressBar2)

        val adapter = AreaListAdapter(requireContext(), mainViewModel, areasDao)
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

        val drawerStatus = mainViewModel.getDrawerStatusLiveData()
        drawerStatus.observe(viewLifecycleOwner, object : Observer<String> {
            override fun onChanged(it: String) {

                if (!isOpen) {
                    if (it.isEmpty() || it == DrawerLayoutStatus.DRAWER_CLOSED) {
                        progressBar.visibility = View.GONE
                        listItem.visibility = View.VISIBLE
                        isOpen = true
                        drawerStatus.removeObserver(this)
                    }
                } else {
                    drawerStatus.removeObserver(this)
                }

            }

        })

        return root
    }
}