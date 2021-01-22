package com.kodexgroup.geomuteapp.screens.map.controllers

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.Task
import com.kodexgroup.geomuteapp.R
import com.kodexgroup.geomuteapp.screens.map.MapFragment
import com.kodexgroup.geomuteapp.screens.map.interfaces.ResizeRadiusListener
import com.kodexgroup.geomuteapp.screens.map.interfaces.SetMarkerListener
import com.kodexgroup.geomuteapp.screens.map.viewmodel.MapViewModel
import com.kodexgroup.geomuteapp.utils.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION

class MapController(
    fragment: MapFragment,
    private val activity: Activity,
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) : OnMapReadyCallback {

    var mMap: GoogleMap? = null
    var mLocationPermissionGranted: Boolean = false
    private var mDefaultLocation: LatLng = LatLng(0.0, 0.0)
    var mLastKnownLocation: Location? = null
    var position: CameraPosition? = null

    private val mapViewModel: MapViewModel = ViewModelProvider(fragment).get(MapViewModel::class.java)

    private var markersList: MutableList<Pair<Marker?, Circle?>> = mutableListOf()

    var mLastMarker: Marker? = null
    var mLastCircle: Circle? = null

    private var setListener: SetMarkerListener? = null



    var isExist = false

    val resizeRadiusListener: ResizeRadiusListener = object : ResizeRadiusListener {
        override fun onResize(center: LatLng, radius: Int) {
            for ((_, circle) in markersList) {
                if (circle != null && center.latitude == circle.center.latitude &&
                        center.longitude == circle.center.longitude) {
                    mLastCircle = circle
                }
            }

            mLastCircle?.remove()

            Log.d("circle", "CREATE_1")
            mLastCircle = mMap?.addCircle(
                CircleOptions()
                    .center(center)
                    .radius(radius.toDouble())
                    .strokeWidth(3f)
                    .strokeColor(R.color.blue_500)
                    .fillColor(R.color.blue_500_a)
            )

            mapViewModel.lastChooseRadius.value = mLastCircle

            markersList.add(Pair(null, mLastCircle!!))
        }
    }

    override fun onMapReady(map: GoogleMap?) {
        mMap = map

        // event of click on map
        mMap?.setOnMapClickListener {
            setMarkerByLatLng(it)
        }

        mMap?.setOnMarkerClickListener {
            if (isExist) {
                mLastMarker = null
                mLastCircle = null
            }

            if (it.position.latitude != mLastMarker?.position?.latitude &&
                    it.position.longitude != mLastMarker?.position?.longitude) {

                isExist = true

                mLastMarker?.remove()
                mLastCircle?.remove()

                mLastMarker = it

                mapViewModel.lastChoosePosition.value = mLastMarker

                for ((_, circle) in markersList) {

                    if (circle != null && it.position.latitude == circle.center.latitude &&
                            it.position.longitude == circle.center.longitude) {
                        mLastCircle = circle
                        mapViewModel.lastChooseRadius.value = mLastCircle
                    }
                }

                setCameraInPosition(it.position)

                setListener?.onSetExistMarker(it)
            }

            return@setOnMarkerClickListener true
        }

        updateLocationUI()
        getDeviceLocation()
    }

    fun setMarkerByLatLng(latLng: LatLng) {
        if (isExist) {
            isExist = false
            mLastMarker = null
            mLastCircle = null
        }

        mLastMarker?.remove()
        mLastCircle?.remove()

        mLastMarker = mMap?.addMarker(
                MarkerOptions()
                        .position(latLng)
                        .title("new")
        )

        val isNear = checkIsNear(mLastMarker!!.position, true)

        if (!isNear) {
            Log.d("circle", "CREATE_2")
            mLastCircle = mMap?.addCircle(
                    CircleOptions()
                            .center(latLng)
                            .radius(0.0)
                            .strokeWidth(3f)
                            .strokeColor(R.color.blue_500)
                            .fillColor(R.color.blue_500_a)
            )

            mapViewModel.lastChoosePosition.value = mLastMarker
            mapViewModel.lastChooseRadius.value = mLastCircle

            setCameraInPosition(latLng)


            setListener?.onSetMarker(latLng)
        } else {
            isExist = true
            mapViewModel.lastChoosePosition.value = mLastMarker
            mapViewModel.lastChooseRadius.value = mLastCircle

            setCameraInPosition(mLastMarker!!.position)

            setListener?.onSetExistMarker(mLastMarker!!)
        }
    }

    private fun checkIsNear(latLng: LatLng, replace: Boolean = false) : Boolean {
        var isNear = false

        for ((marker, circle) in markersList) {
            val dist = FloatArray(1)
            if (marker != null) {
                Location.distanceBetween(latLng.latitude, latLng.longitude,
                        marker.position.latitude, marker.position.longitude, dist)
                if ((circle != null && dist[0] <= circle.radius) && !isNear) {
                    if (replace) {
                        mLastMarker?.remove()
                        mLastCircle?.remove()
                        mLastMarker = marker
                        mLastCircle = circle
                    }
                    isNear = true
                    break
                }
            }
        }

        return isNear
    }

    fun setOnSetMarkerListener(listener: SetMarkerListener) {
        setListener = listener
    }

    fun getLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
                == PackageManager.PERMISSION_GRANTED
        ) {
            mLocationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(
                    activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }
    }

    fun updateLocationUI() {
        if (mMap == null) {
            return
        }
        try {
            if (mLocationPermissionGranted) {
                mMap?.isMyLocationEnabled = true
                mMap?.uiSettings?.isMyLocationButtonEnabled = true
            } else {
                mMap?.isMyLocationEnabled = false
                mMap?.uiSettings?.isMyLocationButtonEnabled = false
                mLastKnownLocation = null
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message.toString())
        }
    }

    private fun setCameraInPosition(position: LatLng) {
        val projection = mMap!!.projection
        val centerPoint = projection.toScreenLocation(position)

        val outMetrics = DisplayMetrics()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = activity.display
            display?.getRealMetrics(outMetrics)
        } else {
            @Suppress("DEPRECATION")
            val display = activity.windowManager?.defaultDisplay
            @Suppress("DEPRECATION")
            display?.getMetrics(outMetrics)
        }
        val displayHeight: Int = outMetrics.heightPixels

        centerPoint.y = centerPoint.y + (displayHeight / 4.5).toInt() // move center down for approx 22%


        val newCenterPoint = projection.fromScreenLocation(centerPoint)

        mMap!!.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                        newCenterPoint,
                        mMap!!.cameraPosition.zoom
                ),
                1000,
                null
        )
    }

    private fun getDeviceLocation() {
        try {
            if (mLocationPermissionGranted) {
                val locationResult: Task<Location> = fusedLocationClient.lastLocation

                locationResult.addOnCompleteListener(activity) { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        mLastKnownLocation = task.result
                        if (mLastKnownLocation != null) {
                            mapViewModel.checkLocation.value = !checkIsNear(LatLng(mLastKnownLocation!!.latitude, mLastKnownLocation!!.longitude))

                            Log.d("lat", position.toString())
                            if (position != null) {
                                Log.d("lat", "EXIST")
                                mMap!!.moveCamera(
                                        CameraUpdateFactory.newCameraPosition(position)
                                )
                            } else {
                                mMap!!.moveCamera(
                                        CameraUpdateFactory.newLatLngZoom(
                                                LatLng(
                                                        mLastKnownLocation!!.latitude,
                                                        mLastKnownLocation!!.longitude
                                                ), 15F
                                        )
                                )
                            }
                        } else {
                            getDeviceLocation()
                        }
                    } else {
                        Log.d("geo", "Current location is null. Using defaults.")
                        Log.e("geo", "Exception: %s", task.exception)
                        mMap!!.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                        mDefaultLocation,
                                        15f
                                )
                        )
                        mMap!!.uiSettings.isMyLocationButtonEnabled = false

                    }
                }

            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message!!)
        }
    }

    fun updateMap(markers: List<Pair<MarkerOptions, CircleOptions>>) {
        mMap?.clear()
        markersList.clear()

        mLastCircle = null
        mLastMarker = null

        for ((marker, circle) in markers ) {
            val m = mMap?.addMarker(marker)
            val c = mMap?.addCircle(circle)

            markersList.add(Pair(m, c))
        }

        if (mLastKnownLocation == null) {
            mapViewModel.checkLocation.value = null
        } else {
            mapViewModel.checkLocation.value = !checkIsNear(LatLng(mLastKnownLocation!!.latitude, mLastKnownLocation!!.longitude))
        }
    }

    fun setMarker(options: MarkerOptions?) {
        mLastMarker = mMap?.addMarker(options)
    }
}