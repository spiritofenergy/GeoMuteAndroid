package com.kodexgroup.geomuteapp.screens.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.kodexgroup.geomuteapp.MainViewModel
import com.kodexgroup.geomuteapp.R
import com.kodexgroup.geomuteapp.utils.DrawerLayoutStatus

class SettingsFragment : Fragment() {

    private val mainViewModel: MainViewModel by activityViewModels()

    private lateinit var progressBar: ProgressBar
    private lateinit var mainFrame: View
    private lateinit var openNotification: Button

    private var isOpen = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_settings, container, false)

        progressBar = root.findViewById(R.id.progressBar3)
        mainFrame = root.findViewById(R.id.frame_setting)
        openNotification = root.findViewById(R.id.notification_setting)

        val drawerStatus = mainViewModel.getDrawerStatusLiveData()
        drawerStatus.observe(viewLifecycleOwner, object : Observer<String> {
            override fun onChanged(it: String) {

                if (!isOpen) {
                    if (it.isEmpty() || it == DrawerLayoutStatus.DRAWER_CLOSED) {
                        progressBar.visibility = View.GONE
                        mainFrame.visibility = View.VISIBLE
                        isOpen = true
                        drawerStatus.removeObserver(this)
                    }
                } else {
                    drawerStatus.removeObserver(this)
                }

            }

        })

        openNotification.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_ASSISTANT_SETTINGS))
        }

        return root
    }
}