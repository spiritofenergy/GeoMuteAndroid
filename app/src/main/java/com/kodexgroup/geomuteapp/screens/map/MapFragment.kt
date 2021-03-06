package com.kodexgroup.geomuteapp.screens.map

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.activity.addCallback
import androidx.annotation.RequiresApi
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.kodexgroup.geomuteapp.R
import com.kodexgroup.geomuteapp.database.AppDatabase
import com.kodexgroup.geomuteapp.database.dao.AreasDAO
import com.kodexgroup.geomuteapp.database.entities.Areas
import com.kodexgroup.geomuteapp.utils.App
import com.kodexgroup.geomuteapp.utils.DrawerLayoutStatus
import com.kodexgroup.geomuteapp.utils.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
import com.kodexgroup.geomuteapp.utils.controllers.AudioController
import com.kodexgroup.geomuteapp.utils.controllers.BottomSheetController
import com.kodexgroup.geomuteapp.utils.controllers.MapController
import com.kodexgroup.geomuteapp.utils.getBitmapFromVector
import com.kodexgroup.geomuteapp.utils.interfaces.SetMarkerListener
import com.kodexgroup.geomuteapp.utils.viewmodels.MainViewModel
import com.kodexgroup.geomuteapp.utils.viewmodels.MapViewModel


class MapFragment : Fragment() {

    private var position: CameraPosition? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var db: AppDatabase
    private lateinit var areasDao: AreasDAO
    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var mapViewModel: MapViewModel

    private lateinit var addBtn: Button

    private lateinit var mainFrame: View

    private lateinit var mBottomSheetBehavior: BottomSheetBehavior<NestedScrollView>
    private lateinit var frame: LinearLayout
    private lateinit var progressBar: ProgressBar

    private lateinit var mapController: MapController
    private lateinit var bottomController: BottomSheetController
    private lateinit var audioManager: AudioController

    private val markers: MutableList<Pair<MarkerOptions, CircleOptions>> = mutableListOf()

    private lateinit var areasLiveData: LiveData<List<Areas>>

    private var settingOpened = false
    private var isOpen = false

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_map, container, false)

        mapViewModel = ViewModelProvider(this).get(MapViewModel::class.java)
        mainViewModel.setOpenMap(true)

        audioManager = AudioController(requireContext())

        if (savedInstanceState != null) {
            Log.d("lat", savedInstanceState.getDouble("lat").toString())

            position = CameraPosition(
                    LatLng(
                            savedInstanceState.getDouble("lat"),
                            savedInstanceState.getDouble("lng")
                    ),
                    savedInstanceState.getFloat("zoom"),
                    savedInstanceState.getFloat("tilt"),
                    savedInstanceState.getFloat("bearing")
            )
        }

        mBottomSheetBehavior = BottomSheetBehavior.from(root.findViewById(R.id.bottom_map_area))
        frame = root.findViewById(R.id.frame)
        addBtn = root.findViewById(R.id.add_area)
        mainFrame = root.findViewById(R.id.main_content)
        progressBar = root.findViewById(R.id.progressBar)

        mainViewModel.setBottomDialog(mBottomSheetBehavior)

        bottomController = BottomSheetController(
                this,
                container,
                inflater,
                mBottomSheetBehavior,
                frame
        )

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        mapController = MapController(
                this,
                requireActivity(),
                requireContext(),
                fusedLocationClient
        )
        mapController.position = position

        mapController.setOnSetMarkerListener(object : SetMarkerListener {
            override fun onSetMarker(latLng: LatLng) {
                bottomController.setAddAreaMode(latLng)
            }

            override fun onSetExistMarker(marker: Marker) {
                bottomController.setEditAreaMode(marker)
            }
        })

        bottomController.setOnResizeRadiusListener(mapController.resizeRadiusListener)

        db = (requireContext().applicationContext as App).getDatabase()
        areasDao = db.areasDao()
        areasLiveData = areasDao.getAll()

        areasLiveData.observe(viewLifecycleOwner) { areas ->
            markers.clear()

            for (area in areas) {
                markers.add(
                        Pair(
                                MarkerOptions()
                                        .position(area.latLng)
                                        .title(area.title)
                                        .icon(
                                                getBitmapFromVector(
                                                        requireContext(),
                                                        R.drawable.ic_baseline_volume_off_24
                                                )
                                        ),
                                CircleOptions()
                                        .center(area.latLng)
                                        .radius(area.radius)
                                        .strokeWidth(3f)
                                        .strokeColor(R.color.blue_500)
                                        .fillColor(R.color.blue_500_a)
                        )
                )
            }

            mapController.updateMap(markers)

            val marker = mapViewModel.lastChoosePosition.value
            val circle = mapViewModel.lastChooseRadius.value

            if (marker != null) {
                if (marker.title == "new") {
                    mapController.setMarker(
                            MarkerOptions()
                                    .position(marker.position)
                    )

                    if (circle != null) {
                        bottomController.setAddAreaMode(marker.position, circle.radius.toInt() / 100)
                    } else {
                        bottomController.setAddAreaMode(marker.position, 0)
                    }

                } else {
                    bottomController.setEditAreaMode(marker)
                }
            }
        }

        bottomController.setStartMode()

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(mapController)

        addBtn.setOnClickListener {
            if (mapController.mLastKnownLocation != null) {
                val latLngCur = LatLng(
                        mapController.mLastKnownLocation!!.latitude,
                        mapController.mLastKnownLocation!!.longitude
                )
                mapController.setMarkerByLatLng(latLngCur)
            }
        }

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
                    progressBar.visibility = View.GONE
                    mainFrame.visibility = View.VISIBLE
                    isOpen = true
                    drawerStatus.removeObserver(this)
                }

            }

        })

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (mBottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            } else {
                requireActivity().onBackPressed()
            }
        }

        return root
    }

    override fun onResume() {
        super.onResume()
        if (settingOpened) {
            settingOpened = false
            mapController.getDeviceLocation()
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("clearMap", "creal")
        mapController.clearMap()
    }

    override fun onDestroy() {
        mainViewModel.setOpenMap(false)
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String?>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        mapController.mLocationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    mapController.mLocationPermissionGranted = true

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        getSettingsPermissions()
                    }

                    mapController.getDeviceLocation()
                }
            }
        }
        mapController.updateLocationUI()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (isAdded) {
            if (mapController.mMap != null) {
                outState.putDouble("lat", mapController.mMap!!.cameraPosition.target.latitude)
                outState.putDouble("lng", mapController.mMap!!.cameraPosition.target.longitude)
                outState.putFloat("zoom", mapController.mMap!!.cameraPosition.zoom)
                outState.putFloat("tilt", mapController.mMap!!.cameraPosition.tilt)
                outState.putFloat("bearing", mapController.mMap!!.cameraPosition.bearing)
            }
        }
    }

    fun getPermissions() {
        requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getSettingsPermissions() {
        if (requireActivity().shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            Log.d("TESTPER", "TRUE")
            getAlert()
        } else {
            Log.d("TESTPER", "FALSE")
        }
    }

    fun openSetting() {
        settingOpened = true
        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }

    private fun getAlert() {
        val alertDialog = AlertDialog.Builder(requireContext())
                .setTitle("Требуется дополнительное разрешение")
                .setMessage("Для полной функциональности приложения необходимо разрешение получения вашего местоположения, когда приложение закрыто.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Включить", null)
                .setNegativeButton("Отмена", null)
                .create()

        alertDialog.setOnShowListener {
            Log.d("openAlert", "OPEN")
            val positiveBtn = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeBtn = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveBtn.setOnClickListener {
                alertDialog.dismiss()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
                }
            }
            negativeBtn.setOnClickListener {
                alertDialog.dismiss()

            }
        }

        alertDialog.show()
    }
}