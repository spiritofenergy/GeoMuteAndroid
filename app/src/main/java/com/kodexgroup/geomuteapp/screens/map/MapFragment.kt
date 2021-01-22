package com.kodexgroup.geomuteapp.screens.map

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.observe
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.kodexgroup.geomuteapp.R
import com.kodexgroup.geomuteapp.database.AppDatabase
import com.kodexgroup.geomuteapp.database.dao.AreasDAO
import com.kodexgroup.geomuteapp.database.entities.Areas
import com.kodexgroup.geomuteapp.screens.map.controllers.BottomSheetController
import com.kodexgroup.geomuteapp.screens.map.controllers.MapController
import com.kodexgroup.geomuteapp.screens.map.interfaces.SetMarkerListener
import com.kodexgroup.geomuteapp.MainViewModel
import com.kodexgroup.geomuteapp.utils.App
import com.kodexgroup.geomuteapp.utils.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
import com.kodexgroup.geomuteapp.utils.getBitmapFromVector

class MapFragment : Fragment() {

    private var position: CameraPosition? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var db: AppDatabase
    private lateinit var areasDao: AreasDAO
    private val mapViewModel: MainViewModel by activityViewModels()

    private lateinit var addBtn: Button

    private lateinit var mBottomSheetBehavior: BottomSheetBehavior<NestedScrollView>
    private lateinit var frame: LinearLayout

    private lateinit var mapController: MapController
    private lateinit var bottomController: BottomSheetController

    private val markers: MutableList<Pair<MarkerOptions, CircleOptions>> = mutableListOf()

    private lateinit var areasLiveData: LiveData<List<Areas>>

    private var settingOpened = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_map, container, false)

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

        mapViewModel.checkLocation.observe(viewLifecycleOwner) {
            if (it == null) {
                Log.d("locationSet", "NULL")
                addBtn.isEnabled = false
            } else {
                Log.d("locationSet", it.toString())
                addBtn.isEnabled = it
            }
        }

        addBtn.setOnClickListener {
            if (mapController.mLastKnownLocation != null) {
                val latLngCur = LatLng(
                        mapController.mLastKnownLocation!!.latitude,
                        mapController.mLastKnownLocation!!.longitude
                )
                mapController.setMarkerByLatLng(latLngCur)
                bottomController.setAddAreaMode(latLngCur)
            }
        }

        mapController.getLocationPermission()

        return root
    }

    override fun onResume() {
        super.onResume()
        if (settingOpened) {
            settingOpened = false
            mapController.getDeviceLocation()
        }
    }

    override fun onPause() {
        super.onPause()
        mapController.stopLocationUpdates()
    }

    override fun onStop() {
        super.onStop()
        Log.d("clearMap", "creal")
        mapController.clearMap()
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String?>,
            grantResults: IntArray
    ) {
        mapController.mLocationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    mapController.mLocationPermissionGranted = true
                }
            }
        }
        mapController.updateLocationUI()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (mapController.mMap != null) {
            outState.putDouble("lat", mapController.mMap!!.cameraPosition.target.latitude)
            outState.putDouble("lng", mapController.mMap!!.cameraPosition.target.longitude)
            outState.putFloat("zoom", mapController.mMap!!.cameraPosition.zoom)
            outState.putFloat("tilt", mapController.mMap!!.cameraPosition.tilt)
            outState.putFloat("bearing", mapController.mMap!!.cameraPosition.bearing)
        }
    }

    fun openSetting() {
        settingOpened = true
        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }
}