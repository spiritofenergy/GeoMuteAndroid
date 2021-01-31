package com.kodexgroup.geomuteapp.activity

import android.app.ActivityManager
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import com.kodexgroup.geomuteapp.R
import com.kodexgroup.geomuteapp.utils.DrawerLayoutStatus
import com.kodexgroup.geomuteapp.utils.createNotificationChannel
import com.kodexgroup.geomuteapp.utils.services.GeoMuteService
import com.kodexgroup.geomuteapp.utils.viewmodels.MainViewModel


class MainActivity : AppCompatActivity() {

    private var switchService: SwitchCompat? = null
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val viewModel: MainViewModel by viewModels()

    private var isServiceRun = false

    private val connection: ServiceConnection = object  : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            isServiceRun = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceRun = false
            switchService?.isChecked = false
        }

        override fun onNullBinding(name: ComponentName?) {
            isServiceRun = false
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_MyApplication_NoActionBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        appBarConfiguration = AppBarConfiguration(setOf(
                R.id.nav_map, R.id.nav_list, R.id.nav_info, R.id.nav_settings
        ), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        val service = Intent(this, GeoMuteService::class.java)
        bindService(service, connection, 0)

        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                viewModel.setDrawerStatus(DrawerLayoutStatus.DRAWER_SLIDE)
            }

            override fun onDrawerOpened(drawerView: View) {
                viewModel.setDrawerStatus(DrawerLayoutStatus.DRAWER_OPENED)
            }

            override fun onDrawerClosed(drawerView: View) {
                viewModel.setDrawerStatus(DrawerLayoutStatus.DRAWER_CLOSED)
            }

            override fun onDrawerStateChanged(newState: Int) {
                viewModel.setDrawerStatus(DrawerLayoutStatus.DRAWER_CHANGED)
            }
        })

        createNotificationChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (!notificationManager.isNotificationPolicyAccessGranted) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                startActivity(intent)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_service_switcher, menu)

        val item = menu?.findItem(R.id.switcher)
        switchService = item?.actionView?.findViewById(R.id.service_switch)

        if (isServiceRun) {
            switchService?.isChecked = true
        }

        switchService?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                getAlertService()
            } else {
                val intent = Intent(this, GeoMuteService::class.java)
                unbindService(connection)
                stopService(intent)
            }
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        viewModel.setDrawerStatus("")

    }

    override fun onStop() {
        super.onStop()
        viewModel.setDrawerStatus(DrawerLayoutStatus.DRAWER_NULL)
    }

    override fun onSupportNavigateUp() : Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun getAlertService() : AlertDialog {
        val alertDialog = AlertDialog.Builder(this)
                .setTitle("Подтвердите включение сервиса")
                .setMessage("Мы не сохраняем вашу геолокацию. Подтвердите временное фоновое использование вашей позиции")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Включить", null)
                .setNegativeButton("Отмена", null)
                .create()

        alertDialog.setOnShowListener {
            val positiveBtn = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeBtn = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveBtn.setOnClickListener {
                val intent = Intent(this, GeoMuteService::class.java)
                startService(intent)
                bindService(intent, connection, 0)

                alertDialog.dismiss()
            }
            negativeBtn.setOnClickListener {
                switchService?.isChecked = false
                alertDialog.dismiss()
            }
        }

        alertDialog.setOnCancelListener {
            switchService?.isChecked = false
        }

        alertDialog.show()

        return alertDialog
    }
}