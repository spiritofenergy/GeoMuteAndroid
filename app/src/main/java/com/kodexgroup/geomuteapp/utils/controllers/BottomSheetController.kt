package com.kodexgroup.geomuteapp.utils.controllers

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.kodexgroup.geomuteapp.R
import com.kodexgroup.geomuteapp.database.entities.Areas
import com.kodexgroup.geomuteapp.screens.map.MapFragment
import com.kodexgroup.geomuteapp.utils.interfaces.ResizeRadiusListener
import com.kodexgroup.geomuteapp.utils.viewmodels.MainViewModel
import com.kodexgroup.geomuteapp.utils.viewmodels.MapViewModel
import com.kodexgroup.geomuteapp.utils.App
import com.kodexgroup.geomuteapp.utils.format
import java.util.*

class BottomSheetController(
    private val fragment: MapFragment,
    container: ViewGroup?,
    inflater: LayoutInflater,
    private val mBottomSheetBehavior: BottomSheetBehavior<NestedScrollView>,
    private val frame: LinearLayout) {

    private val mainViewModel: MainViewModel by fragment.activityViewModels()
    private val mapViewModel: MapViewModel = ViewModelProvider(fragment).get(MapViewModel::class.java)
    private var checkAdd: LiveData<Boolean> = mainViewModel.getCheckLiveData()

    private val db = (fragment.requireContext().applicationContext as App).getDatabase()
    private val areasDao = db.areasDao()

    private var startViewBottom: View =
        inflater.inflate(R.layout.fragment_bottom_start, container, false)
    private var setAreaViewBottom: View =
        inflater.inflate(R.layout.fragment_bottom_set_area, container, false)

    private var titlePoint: TextView = setAreaViewBottom.findViewById(R.id.title_point)
    private var titleViewEditText: EditText = setAreaViewBottom.findViewById(R.id.set_title_point)
    private var mainBtn: Button = setAreaViewBottom.findViewById(R.id.add_or_delete)
    private val choordsTxt: TextView = setAreaViewBottom.findViewById(R.id.coords_point)
    private val seekRadius: SeekBar = setAreaViewBottom.findViewById(R.id.radius_point)
    private val progressView: TextView = setAreaViewBottom.findViewById(R.id.current_progress)
    private val editBtn: ImageButton = setAreaViewBottom.findViewById(R.id.edit_area_btn)

    private var resizeRadiusListener: ResizeRadiusListener? = null

    private var currentMarker: Marker? = null
    private lateinit var currentLatLng: LatLng
    private var currentArea: Areas? = null

    private val minRadius = 50
    private var radius = 0
    private var isEdit = false
    private var isModify = false

    init {
        seekRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                resizeRadiusListener?.onResize(currentLatLng, progress * minRadius)
                radius = progress * minRadius
                progressView.text = (progress * minRadius).toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null) {
                    resizeRadiusListener?.onResize(currentLatLng, seekBar.progress * minRadius)
                    radius = seekBar.progress * minRadius
                    progressView.text = (seekBar.progress * minRadius).toString()
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null) {
                    if (isEdit) {
                        isModify = true
                    }

                    resizeRadiusListener?.onResize(currentLatLng, seekBar.progress * minRadius)
                    radius = seekBar.progress * minRadius
                    progressView.text = (seekBar.progress * minRadius).toString()
                }
            }
        })

        titleViewEditText.doOnTextChanged { text, _, _, _ ->
            Log.d("title", text.toString())
            mapViewModel.newTitleMarker.value = text.toString()
        }

        checkAdd.observe(fragment.viewLifecycleOwner) {
            Log.d("testingBug.fix", it.toString())

            if (it) {
                mapViewModel.newTitleMarker.value = ""
                mapViewModel.lastChoosePosition.value = null
                mapViewModel.lastChooseRadius.value = null
                setStartMode()
            } else {
                titleViewEditText.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.errors))
                Timer().schedule(object : TimerTask() {
                    override fun run() {
                        if (fragment.isAdded) {
                            titleViewEditText.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.black))
                        }
                    }
                }, 4000)
            }
        }

        mainBtn.setOnClickListener {
            if (currentMarker == null) {
                if (titleViewEditText.text.isNotEmpty()) {
                    if (radius == 0) {
                        progressView.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.errors))
                        Timer().schedule(object : TimerTask() {
                            override fun run() {
                                if (fragment.isAdded) {
                                    progressView.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.black))
                                }
                            }
                        }, 4000)
                    } else {
                        addMarker(titleViewEditText.text.toString(), currentLatLng, radius.toDouble())
                    }
                } else {
                    titleViewEditText.setHintTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.errors))
                    Timer().schedule(object : TimerTask() {
                        override fun run() {
                            if (fragment.isAdded) {
                                titleViewEditText.setHintTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.hint_color))
                            }
                        }
                    }, 4000)
                }
            } else {
                Thread {
                    if (isEdit) {
                        if (currentArea != null && currentArea!!.radius != radius.toDouble()) {
                            currentArea!!.radius = radius.toDouble()
                            areasDao.update(currentArea!!)
                        }
                    } else {
                        if (currentArea != null) {
                            areasDao.delete(currentArea!!)
                        }
                    }
                    currentArea = null
                }.start()

                mapViewModel.newTitleMarker.value = ""
                mapViewModel.lastChoosePosition.value = null
                mapViewModel.lastChooseRadius.value = null
                setStartMode()
            }
        }

        editBtn.setOnClickListener {
            setEditMode()
        }
    }


    fun setOnResizeRadiusListener(listener: ResizeRadiusListener) {
        resizeRadiusListener = listener
    }

    fun setStartMode() {
        currentMarker = null

        mBottomSheetBehavior.peekHeight = fragment.requireContext().resources.getDimension(R.dimen.start_peek).toInt()
        mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        frame.removeAllViews()
        frame.addView(startViewBottom)
    }

    fun setAddAreaMode(latLng: LatLng, radius: Int? = null) {
        currentLatLng = latLng
        currentMarker = null
        currentArea = null

        titleViewEditText.setText(mapViewModel.newTitleMarker.value)

        titlePoint.visibility = View.GONE
        titleViewEditText.visibility = View.VISIBLE

        titlePoint.text = ""

        seekRadius.isEnabled = true

        mainBtn.setBackgroundColor(ContextCompat.getColor(fragment.requireContext(), R.color.sub_main_500))
        mainBtn.text = "Сохранить"

        editBtn.visibility = View.GONE

        choordsTxt.text = fragment.getString(R.string.coords, latLng.latitude, latLng.longitude)
        if (radius != null) {
            seekRadius.progress = radius
            this.radius = radius
        } else {
            seekRadius.progress = 0
            this.radius = 0
        }

        frame.removeAllViews()
        frame.addView(setAreaViewBottom)
        mBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        mBottomSheetBehavior.peekHeight = fragment.requireContext().resources.getDimension(R.dimen.area_peek).toInt()
    }

    fun setEditAreaMode(marker: Marker) {
        currentLatLng = marker.position
        currentMarker = marker

        mapViewModel.newTitleMarker.value = ""

        val area = areasDao.getByTitle(marker.title)
        area.observe(fragment.viewLifecycleOwner, object : Observer<Areas> {
            override fun onChanged(it: Areas) {
                currentArea = it
                seekRadius.progress = it.radius.toInt() / minRadius

                area.removeObserver(this)
            }
        })

        titlePoint.visibility = View.VISIBLE
        titleViewEditText.visibility = View.GONE

        titlePoint.text = marker.title

        seekRadius.isEnabled = false

        editBtn.setBackgroundResource(R.drawable.button_style)
        editBtn.setImageResource(R.drawable.ic_baseline_edit_location_24)
        isEdit = false

        mainBtn.setBackgroundColor(ContextCompat.getColor(fragment.requireContext(), R.color.errors))
        mainBtn.text = "Удалить"

        editBtn.visibility = View.VISIBLE

        choordsTxt.text = fragment.getString(R.string.coords, marker.position.latitude, marker.position.longitude)

        val edit = mainViewModel.getEditOpenMarker()
        if (edit) {
            setEditMode()
            mainViewModel.clearEditOpenMarker()
        }

        frame.removeAllViews()
        frame.addView(setAreaViewBottom)
        mBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        mBottomSheetBehavior.peekHeight = fragment.requireContext().resources.getDimension(R.dimen.area_peek).toInt()
    }

    private fun setEditMode() {
        if (!isEdit) {
            editBtn.setBackgroundResource(R.drawable.button_style_close)
            editBtn.setImageResource(R.drawable.ic_baseline_close_24)
            isEdit = true

            seekRadius.isEnabled = true

            mainBtn.setBackgroundColor(ContextCompat.getColor(fragment.requireContext(), R.color.sub_main_500))
            mainBtn.text = "Сохранить"
        } else {
            editBtn.setBackgroundResource(R.drawable.button_style)
            editBtn.setImageResource(R.drawable.ic_baseline_edit_location_24)
            isEdit = false

            if (currentArea != null && isModify) {
                seekRadius.progress = currentArea!!.radius.toInt() / minRadius
            }

            seekRadius.isEnabled = false

            mainBtn.setBackgroundColor(ContextCompat.getColor(fragment.requireContext(), R.color.errors))
            mainBtn.text = "Удалить"
        }
    }

    private fun addMarker(title: String, latLng: LatLng, radius: Double) {
        val area = Areas(title, latLng, radius, Date().time)

        mainViewModel.tryAddAreas(area)
    }
}