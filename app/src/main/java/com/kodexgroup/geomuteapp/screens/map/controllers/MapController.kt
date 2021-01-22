package com.kodexgroup.geomuteapp.screens.map.controllers

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.Task
import com.kodexgroup.geomuteapp.R
import com.kodexgroup.geomuteapp.screens.map.MapFragment
import com.kodexgroup.geomuteapp.screens.map.interfaces.ResizeRadiusListener
import com.kodexgroup.geomuteapp.screens.map.interfaces.SetMarkerListener
import com.kodexgroup.geomuteapp.MainViewModel
import com.kodexgroup.geomuteapp.utils.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
import java.util.*

class MapController(
    private val fragment: MapFragment,
    private val activity: Activity,
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) : OnMapReadyCallback {

    var mMap: GoogleMap? = null
    var mLocationPermissionGranted: Boolean = false
    private var mDefaultLocation: LatLng = LatLng(0.0, 0.0)
    var mLastKnownLocation: Location? = null
    var position: CameraPosition? = null

    private val mainViewModel: MainViewModel by fragment.activityViewModels()

    private var markersList: MutableList<Pair<Marker?, Circle?>> = mutableListOf()

    var mLastMarker: Marker? = null
    var mLastCircle: Circle? = null

    private var setListener: SetMarkerListener? = null

    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            if (locationResult == null) {
                mainViewModel.checkLocation.value = null
                return
            } else {
                for (location in locationResult.locations) {
                    mLastKnownLocation = location
                    mainViewModel.checkLocation.value = !checkIsNear(LatLng(mLastKnownLocation!!.latitude, mLastKnownLocation!!.longitude))
                }
            }
        }
    }

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

            mainViewModel.lastChooseRadius.value = mLastCircle

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
            setExistMarker(it)

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

            mainViewModel.lastChoosePosition.value = mLastMarker
            mainViewModel.lastChooseRadius.value = mLastCircle

            setCameraInPosition(latLng)


            setListener?.onSetMarker(latLng)
        } else {
            isExist = true
            mainViewModel.lastChoosePosition.value = mLastMarker
            mainViewModel.lastChooseRadius.value = mLastCircle

            setCameraInPosition(mLastMarker!!.position)

            setListener?.onSetExistMarker(mLastMarker!!)
        }
    }

    private fun setExistMarker(marker: Marker) {
        if (isExist) {
            mLastMarker = null
            mLastCircle = null
        }

        if (marker.position.latitude != mLastMarker?.position?.latitude &&
                marker.position.longitude != mLastMarker?.position?.longitude) {

            isExist = true

            mLastMarker?.remove()
            mLastCircle?.remove()

            mLastMarker = marker

            mainViewModel.lastChoosePosition.value = mLastMarker

            for ((_, circle) in markersList) {

                if (circle != null && marker.position.latitude == circle.center.latitude &&
                        marker.position.longitude == circle.center.longitude) {
                    mLastCircle = circle
                    mainViewModel.lastChooseRadius.value = mLastCircle
                }
            }

            setCameraInPosition(marker.position)

            setListener?.onSetExistMarker(marker)
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
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
                == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("per", "true1")
            mLocationPermissionGranted = true
        } else {
            Log.d("per", "false1")
            mLocationPermissionGranted = false
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                )
                == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("per", "true2")
            mLocationPermissionGranted = true and mLocationPermissionGranted
        } else {
            Log.d("per", "false2")
            mLocationPermissionGranted = false
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                    == PackageManager.PERMISSION_GRANTED
            ) {
                mLocationPermissionGranted = true and mLocationPermissionGranted
            } else {
                mLocationPermissionGranted = false
                    permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }

        if (!mLocationPermissionGranted) {
            Log.d("per", mLocationPermissionGranted.toString())
            ActivityCompat.requestPermissions(
                    activity, permissions.toTypedArray(),
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

    fun getDeviceLocation() {
        try {
            if (mLocationPermissionGranted) {

                fusedLocationClient.requestLocationUpdates(LocationRequest().apply {
                    priority = PRIORITY_BALANCED_POWER_ACCURACY
                    interval = 10000
                    fastestInterval = 5000
                    maxWaitTime = 20000
                },
                locationCallback,
                Looper.getMainLooper())

                val locationResult: Task<Location> = fusedLocationClient.lastLocation

                locationResult.addOnCompleteListener(activity) { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        mLastKnownLocation = task.result
                        if (mLastKnownLocation != null) {
                            mMap?.isMyLocationEnabled = true
                            mMap?.uiSettings?.isMyLocationButtonEnabled = true
                            mainViewModel.checkLocation.value = !checkIsNear(LatLng(mLastKnownLocation!!.latitude, mLastKnownLocation!!.longitude))

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
                            Log.d("mapReadyLog", "READY")
                            getOpenMarker()
                        } else {
                            getAlertDialog()
                            mMap?.isMyLocationEnabled = false
                            mMap?.uiSettings?.isMyLocationButtonEnabled = false
                            mLastKnownLocation = null
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
            mainViewModel.checkLocation.value = null
        } else {
            mainViewModel.checkLocation.value = !checkIsNear(LatLng(mLastKnownLocation!!.latitude, mLastKnownLocation!!.longitude))
        }

        Log.d("mapReadyLog", "READY")
    }

    fun setMarker(options: MarkerOptions?) {
        mLastMarker = mMap?.addMarker(options)
    }

    private fun getMarkerByTitle(title: String) : Marker? {
        Log.d("opens", markersList.size.toString())
        for ((marker, _) in markersList) {
            Log.d("opens", marker?.title.toString())
            if (marker != null && marker.title == title) {
                Log.d("opens", marker.title)
                return marker
            }
        }
        return null
    }

    private fun getOpenMarker() {
        val opensTitle = mainViewModel.getOpenMarker()
        if (opensTitle != null) {
            Log.d("opens", opensTitle)
            val marker = getMarkerByTitle(opensTitle)
            if (marker != null) {
                Log.d("opens", marker.toString())
                setExistMarker(marker)
                mainViewModel.clearOpenMarker()
            }
        }
    }

    private fun getAlertDialog() {

        val alertDialog = AlertDialog.Builder(fragment.requireContext())
            .setTitle("Позиция устройства не определена")
            .setMessage("Включите геолокацию или обновите карту")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Настройки", null)
            .setNegativeButton("Обновить", null)
            .create()

        alertDialog.setOnShowListener {
            Log.d("openAlert", "OPEN")
            val positiveBtn = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeBtn = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveBtn.setOnClickListener {
                alertDialog.dismiss()
                fragment.openSetting()
            }
            negativeBtn.setOnClickListener {
                getDeviceLocation()
            }
        }

        alertDialog.show()
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    fun clearMap() {
        mainViewModel.lastChoosePosition.value = null
        mainViewModel.lastChooseRadius.value = null
    }
}