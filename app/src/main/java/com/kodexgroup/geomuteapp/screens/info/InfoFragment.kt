package com.kodexgroup.geomuteapp.screens.info

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.kodexgroup.geomuteapp.BuildConfig
import com.kodexgroup.geomuteapp.R
import com.kodexgroup.geomuteapp.utils.DrawerLayoutStatus
import com.kodexgroup.geomuteapp.utils.viewmodels.MainViewModel


class InfoFragment : Fragment() {

    private val mainViewModel: MainViewModel by activityViewModels()

    private lateinit var progressBar: ProgressBar
    private lateinit var mainFrame: View
    private lateinit var version: TextView
    private lateinit var support: Button

    private var isOpen = false

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_info, container, false)

        progressBar = root.findViewById(R.id.progressBar4)
        mainFrame = root.findViewById(R.id.main_info)
        version = root.findViewById(R.id.version)
        support = root.findViewById(R.id.support_btn)

        version.text = getString(R.string.version, BuildConfig.VERSION_NAME)

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

        support.setOnClickListener {
            openMailToSupport()
        }

        return root
    }

    fun openMailToSupport() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "plain/text"
            putExtra(Intent.EXTRA_EMAIL, arrayOf("welcome@seostor.ru"))
            putExtra(Intent.EXTRA_SUBJECT, "GeoMute Android Support")
        }
        startActivity(Intent.createChooser(intent, ""))
    }
}